package net.dege.salmon

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.log2
import kotlin.time.TimeSource.Monotonic.markNow

/**
 * The primary architectural ViewModel managing the state machine of the instrument tuner.
 * Binds incoming pitch frequencies to mathematical note configurations, updates UI animation
 * intervals, handles tuning validation, and delegates synthetic note audio processing.
 *
 * IMPORTANT: every read-modify-write of [_tunerState] is serialized through [_stateLock].
 * Multiple threads write to it concurrently (audio callback thread, inactivity timer,
 * grid timer, UI thread). Without serialization, one thread's read-modify-write can be
 * clobbered by another's write-in between, silently losing state updates (e.g. the
 * "note is correct" flag).
 *
 * @param application The global application context used by the underlying AndroidViewModel.
 */
class TunerViewModel(application: Application) : AndroidViewModel(application) {
    private val _tunerState = mutableStateOf(defaultTunerState)
    val tunerState: State<TunerState> = _tunerState

    private val _settings = mutableStateOf(defaultSettings)
    val tunerSettings = _settings

    private val _notePlayer = NotePlayer()

    private val _correctPlayer = PlayCorrect(application)

    /**
     * Guards every read-modify-write of [_tunerState]. Held only for the duration of the
     * pure transform itself, never across I/O, audio playback, or UI work.
     */
    private val _stateLock = Any()

    /**
     * Tracks overlapping audio playback sessions so the "isPlayingAudio" lock is only
     * released once every active source (note playback, correct chime) has finished.
     */
    private val _audioPlaybackCount = AtomicInteger(0)

    /**
     * Atomically applies [transform] to the current tuner state and returns the new state.
     * All writers must go through this function; direct assignments to `_tunerState.value`
     * are unsafe under concurrent access.
     */
    private inline fun updateState(transform: (TunerState) -> TunerState): TunerState {
        return synchronized(_stateLock) {
            val next = transform(_tunerState.value)
            _tunerState.value = next
            next
        }
    }

    /**
     * Updates the active target note during manual selection modes. Resets validation states.
     */
    fun setSelectedNote(note: String) {
        if (note !in tableOfFreq) return
        updateState { it.copy(selectedNote = note, correctStartTime = null) }
    }

    /**
     * Configures the active tracking behavior of the tuner mechanism.
     */
    private fun setMode(mode: TunerMode) {
        updateState { current ->
            when (mode) {
                TunerMode.AUTO -> current.copy(
                    mode = TunerMode.AUTO,
                    selectedNote = null,
                    correctStartTime = null
                )
                TunerMode.MANUAL -> current.copy(
                    mode = TunerMode.MANUAL,
                    selectedNote = current.selectedNote ?: current.notes.getOrNull(2),
                    correctStartTime = null
                )
            }
        }
    }

    /**
     * Inverts the active evaluation strategy between auto-detection and manual selection.
     */
    fun toggleMode() {
        val current = synchronized(_stateLock) { _tunerState.value.mode }
        setMode(
            if (current == TunerMode.AUTO) TunerMode.MANUAL
            else TunerMode.AUTO
        )
    }

    /**
     * Forces the instrument tracking behavior into static manual selection.
     */
    fun setModeManual() {
        setMode(mode = TunerMode.MANUAL)
    }

    /**
     * Resets the presentation architecture state mapping back to configured defaults.
     */
    fun restoreDefaults() {
        _audioPlaybackCount.set(0)
        _notePlayer.stop()
        _correctPlayer.stop()
        updateState { defaultTunerState }
    }

    /**
     * Maps an arbitrary raw frequency to the closest note identifier using a logarithmic
     * (cents) distance for musical correctness.
     */
    private fun getClosestNote(notes: List<String>, freq: Float): String? {
        if (freq <= 0f) return null
        return notes.minByOrNull { note ->
            val noteFreq = tableOfFreq[note]
            if (noteFreq == null || noteFreq <= 0f) Float.MAX_VALUE
            else abs(1200f * log2(freq / noteFreq))
        }
    }

    /**
     * Computes the exponential moving average deviation in cents between the input signal
     * and target pitch. Reads the previous value from the supplied [current] snapshot.
     */
    private fun getPitchDeviation(
        current: TunerState,
        freqDetected: Float,
        freqTarget: Float,
        resetSmoothing: Boolean,
    ): Float {
        val prevCents = if (resetSmoothing) 0f else current.centsOffset
        if (freqDetected <= 0f || freqTarget <= 0f) return prevCents
        val cents = 1200f * log2(freqDetected / freqTarget)
        val smoothingFactor = TunerConfig.CENTS_SMOOTHING_FACTOR
        return prevCents + smoothingFactor * (cents - prevCents)
    }

    /**
     * Evaluates time elapsed since the last valid frequency signature packet.
     * Drops detection states if the silence duration threshold is exceeded.
     */
    fun checkLastDetectionTime() {
        updateState { current ->
            val lastDetectionTime = current.lastDetectionTime ?: return@updateState current
            val duration = lastDetectionTime.elapsedNow()
            if (duration.inWholeMilliseconds > TunerConfig.LAST_DETECT_TIME_MS) {
                current.copy(
                    lastDetectionTime = null,
                    correctStartTime = null,
                    centsOffset = 0f,
                    incomingFrequency = -1f,
                    incomingFrequencyProbability = 0f
                )
            } else {
                current
            }
        }
    }

    /**
     * Increments the drawing bounds offset parameter to drive the background canvas grid
     * flow animation loop.
     */
    fun updateGridShift() {
        updateState { current ->
            val newGridShift = ((current.gridShift.value.toInt() +
                    TunerConfig.GRID_FLOW_STEP_DP) % TunerConfig.GRID_SIZE_DP).dp
            current.copy(gridShift = newGridShift)
        }
    }

    /**
     * Main entry node processing raw audio evaluation data streams.
     *
     * @param freq The continuous signal component frequency calculated by the DSP module.
     * @param prob The statistical likelihood confidence rating of the isolated pitch calculation.
     */
    fun updateIncomingFrequency(freq: Float, prob: Float) {
        // Cheap pre-checks that don't need the lock. We re-read inside the lock below
        // for anything that must be consistent with the state we're about to write.
        if (_tunerState.value.isPlayingAudio) return
        if (prob < TunerConfig.PROBABILITY_THRESHOLD) return
        if (freq <= 0f) return

        val correctThreshold = _settings.value.isCorrectThreshold
        var playCorrectAfterUpdate = false

        updateState { current ->
            // Re-check under the lock, since isPlayingAudio may have flipped between
            // the pre-check above and acquiring the lock.
            if (current.isPlayingAudio) return@updateState current

            val prevSelectedNote = current.selectedNote

            val resolvedNote: String = when (current.mode) {
                TunerMode.AUTO -> getClosestNote(current.notes, freq) ?: return@updateState current
                TunerMode.MANUAL -> prevSelectedNote ?: return@updateState current
            }
            val selectedFreq = tableOfFreq[resolvedNote] ?: return@updateState current

            val noteChanged = resolvedNote != prevSelectedNote
            val cents = getPitchDeviation(current, freq, selectedFreq, resetSmoothing = noteChanged)

            var next = current.copy(
                lastDetectionTime = markNow(),
                incomingFrequency = freq,
                incomingFrequencyProbability = prob,
                selectedNote = resolvedNote,
                centsOffset = cents
            )

            if (abs(cents) <= correctThreshold) {
                val correctStartTime = current.correctStartTime
                if (correctStartTime != null && resolvedNote == prevSelectedNote) {
                    val duration = correctStartTime.elapsedNow()
                    if (duration.inWholeMilliseconds > TunerConfig.CORRECT_TIME_MS) {
                        val noteIndex = next.notes.indexOf(resolvedNote)
                        if (noteIndex >= 0) {
                            val newIsCorrect = next.isCorrect.toMutableList()
                            newIsCorrect[noteIndex] = true
                            next = next.copy(isCorrect = newIsCorrect)
                        }
                        next = next.copy(correctStartTime = null)
                        playCorrectAfterUpdate = true
                    }
                } else {
                    next = next.copy(correctStartTime = markNow())
                }
            } else {
                next = next.copy(correctStartTime = null)
            }

            next
        }

        // Side effect runs only after the state commit has succeeded, so the "green bar"
        // and the chime stay in sync and can't be lost to a concurrent writer.
        if (playCorrectAfterUpdate) {
            playCorrect()
        }
    }

    private fun beginAudioPlayback() {
        if (_audioPlaybackCount.incrementAndGet() == 1) {
            updateState { it.copy(isPlayingAudio = true) }
        }
    }

    private fun endAudioPlayback() {
        if (_audioPlaybackCount.decrementAndGet() <= 0) {
            _audioPlaybackCount.set(0)
            updateState { it.copy(isPlayingAudio = false) }
        }
    }

    /**
     * Executes the "correct tuning" audio and sets the concurrency lock variables.
     */
    private fun playCorrect() {
        beginAudioPlayback()
        _correctPlayer.playCorrectSound { endAudioPlayback() }
    }

    /**
     * Triggers dynamic playback generation for the given musical target pitch tone.
     */
    fun playNote(freq: Float) {
        if (freq <= 0f) return
        beginAudioPlayback()
        _notePlayer.playNote(freq) { endAudioPlayback() }
    }

    override fun onCleared() {
        super.onCleared()
        _notePlayer.release()
        _correctPlayer.release()
    }
}