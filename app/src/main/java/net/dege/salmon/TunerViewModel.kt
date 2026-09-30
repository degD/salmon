package net.dege.salmon

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import kotlin.math.abs
import kotlin.math.log2
import kotlin.time.TimeSource.Monotonic.markNow

/**
 * The primary architectural ViewModel managing the state machine of the instrument tuner.
 * Binds incoming pitch frequencies to mathematical note configurations, updates UI animation
 * intervals, handles tuning validation, and delegates synthetic note audio processing.
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
     * Tracks overlapping audio playback sessions so the "isPlayingAudio" lock is only
     * released once every active source (note playback, correct chime) has finished.
     */
    private var _audioPlaybackCount = 0

    /**
     * Updates the active target note during manual selection modes. Resets validation states.
     *
     * @param note The scientific pitch notation identifier (e.g., "E2").
     */
    fun setSelectedNote(note: String) {
        if (note in tableOfFreq) {
            _tunerState.value = _tunerState.value.copy(selectedNote = note)
        }
        _tunerState.value = _tunerState.value.copy(correctStartTime = null)
    }

    /**
     * Configures the active tracking behavior of the tuner mechanism.
     *
     * @param mode The targeted operational strategy ([TunerMode.AUTO] or [TunerMode.MANUAL]).
     */
    private fun setMode(mode: TunerMode) {
        if (mode == TunerMode.AUTO) {
            _tunerState.value = _tunerState.value.copy(
                mode = TunerMode.AUTO,
                selectedNote = null
            )
        } else {
            val newSelected = _tunerState.value.selectedNote
                ?: _tunerState.value.notes.getOrNull(2)
            _tunerState.value = _tunerState.value.copy(
                mode = TunerMode.MANUAL,
                selectedNote = newSelected
            )
        }
    }

    /**
     * Inverts the active evaluation strategy between auto-detection and manual selection.
     */
    fun toggleMode() {
        if (_tunerState.value.mode == TunerMode.AUTO) {
            setMode(mode = TunerMode.MANUAL)
        } else {
            setMode(mode = TunerMode.AUTO)
        }
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
        _audioPlaybackCount = 0
        _notePlayer.stop()
        _correctPlayer.stop()
        _tunerState.value = defaultTunerState
    }

    /**
     * Maps an arbitrary raw frequency to the absolute mathematically the closest note string
     * identifier, using a logarithmic (cents) distance for musical correctness.
     *
     * @param freq The active signal pitch in Hertz.
     * @return The identifier string representing the closest matched musical pitch,
     *         or null if the frequency is invalid.
     */
    private fun getClosestNote(freq: Float): String? {
        if (freq <= 0f) return null
        return _tunerState.value.notes.minByOrNull { note ->
            val noteFreq = tableOfFreq[note]
            if (noteFreq == null || noteFreq <= 0f) Float.MAX_VALUE
            else abs(1200f * log2(freq / noteFreq))
        }
    }

    /**
     * Computes the exponential moving average deviation in cents between the input signal
     * and target pitch. Uses standard logarithmic conversion where 100 cents corresponds
     * to one equal-tempered semitone.
     *
     * @param freqDetected The parsed frequency signal received from the microphone layer.
     * @param freqTarget The perfect fundamental pitch frequency configuration of the musical note.
     * @param resetSmoothing When true, discards the previous EMA sample (used when the
     *        targeted note changes so smoothing does not carry over across notes).
     * @return The smoothed offset value bounded in pitch cents.
     */
    private fun getPitchDeviation(
        freqDetected: Float,
        freqTarget: Float,
        resetSmoothing: Boolean,
    ): Float {
        val prevCents = if (resetSmoothing) 0f else _tunerState.value.centsOffset
        if (freqDetected <= 0f || freqTarget <= 0f) return prevCents
        val cents = 1200f * log2(freqDetected / freqTarget)

        // Use an exponential moving average to prevent jumping and smooth the needle movement.
        // SmoothingFactor: 0.1f -> very smooth, 1.0f -> instant.
        val smoothingFactor = TunerConfig.CENTS_SMOOTHING_FACTOR
        return prevCents + smoothingFactor * (cents - prevCents)
    }

    /**
     * Evaluates time elapsed since the last valid frequency signature packet.
     * Drops detection states if the silence duration threshold is exceeded.
     */
    fun checkLastDetectionTime() {
        val lastDetectionTime = _tunerState.value.lastDetectionTime ?: return
        val duration = lastDetectionTime.elapsedNow()
        if (duration.inWholeMilliseconds > TunerConfig.LAST_DETECT_TIME_MS) {
            _tunerState.value = _tunerState.value.copy(
                lastDetectionTime = null,
                correctStartTime = null,
                centsOffset = 0f,
                incomingFrequency = -1f,
                incomingFrequencyProbability = 0f
            )
        }
    }

    /**
     * Increments the drawing bounds offset parameter to drive the background canvas grid
     * flow animation loop.
     */
    fun updateGridShift() {
        val newGridShift = ((_tunerState.value.gridShift.value.toInt() +
                TunerConfig.GRID_FLOW_STEP_DP) % TunerConfig.GRID_SIZE_DP).dp
        _tunerState.value = _tunerState.value.copy(gridShift = newGridShift)
    }

    /**
     * Main entry node processing raw audio evaluation data streams. Performs logarithmic pitch
     * conversion, applies stability filter smoothing, tracks chronological validation
     * thresholds, and triggers completion feedback.
     *
     * @param freq The continuous signal component frequency calculated by the DSP module.
     * @param prob The statistical likelihood confidence rating of the isolated pitch calculation.
     */
    fun updateIncomingFrequency(freq: Float, prob: Float) {
        val isPlayingAudio = _tunerState.value.isPlayingAudio
        val correctThreshold = _settings.value.isCorrectThreshold

        if (isPlayingAudio ||
            prob < TunerConfig.PROBABILITY_THRESHOLD ||
            freq <= 0f
        ) {
            return
        }

        val prevSelectedNote = _tunerState.value.selectedNote

        val resolvedNote: String = when (_tunerState.value.mode) {
            TunerMode.AUTO -> getClosestNote(freq) ?: return
            TunerMode.MANUAL -> prevSelectedNote ?: return
        }
        val selectedFreq = tableOfFreq[resolvedNote] ?: return

        val noteChanged = resolvedNote != prevSelectedNote
        val cents = getPitchDeviation(freq, selectedFreq, resetSmoothing = noteChanged)

        var newState = _tunerState.value.copy(
            lastDetectionTime = markNow(),
            incomingFrequency = freq,
            incomingFrequencyProbability = prob,
            selectedNote = resolvedNote,
            centsOffset = cents
        )

        // If correct and the selected note has been stable for CORRECT_TIME_MS or more,
        // mark it correct and play the confirmation chime.
        if (abs(cents) <= correctThreshold) {
            val correctStartTime = _tunerState.value.correctStartTime
            if (correctStartTime != null && resolvedNote == prevSelectedNote) {
                val duration = correctStartTime.elapsedNow()
                if (duration.inWholeMilliseconds > TunerConfig.CORRECT_TIME_MS) {
                    val noteIndex = newState.notes.indexOf(resolvedNote)
                    if (noteIndex >= 0) {
                        val newIsCorrect = newState.isCorrect.toMutableList()
                        newIsCorrect[noteIndex] = true
                        newState = newState.copy(isCorrect = newIsCorrect)
                    }
                    newState = newState.copy(correctStartTime = null)
                    _tunerState.value = newState
                    playCorrect()
                    return
                }
            } else {
                newState = newState.copy(correctStartTime = markNow())
            }
        } else {
            newState = newState.copy(correctStartTime = null)
        }

        _tunerState.value = newState
    }

    private fun beginAudioPlayback() {
        _audioPlaybackCount++
        _tunerState.value = _tunerState.value.copy(isPlayingAudio = true)
    }

    private fun endAudioPlayback() {
        _audioPlaybackCount = (_audioPlaybackCount - 1).coerceAtLeast(0)
        if (_audioPlaybackCount == 0) {
            _tunerState.value = _tunerState.value.copy(isPlayingAudio = false)
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
     *
     * @param freq The specific audio reference frequency to synthesize and write out.
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