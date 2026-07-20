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
            _tunerState.value = _tunerState.value.copy(mode = TunerMode.AUTO)
            _tunerState.value = _tunerState.value.copy(selectedNote = null)
        }
        else {
            _tunerState.value = _tunerState.value.copy(mode = TunerMode.MANUAL)
            if (_tunerState.value.selectedNote == null) {
                _tunerState.value = _tunerState.value.copy(
                    selectedNote = _tunerState.value.notes[2]
                    // TODO: In the default case, E2. Maybe can be made more intuitive?
                )
            }
        }
    }

    /**
     * Inverts the active evaluation strategy between auto-detection and manual selection.
     */
    fun toggleMode() {
        if (_tunerState.value.mode == TunerMode.AUTO) {
            setMode(mode = TunerMode.MANUAL)
        }
        else {
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
        _tunerState.value = defaultTunerState
    }

    /**
     * Maps an arbitrary raw frequency to the absolute mathematically the closest note string identifier.
     *
     * @param freq The active signal pitch in Hertz.
     * @return The identifier string representing the closest matched musical pitch.
     */
    private fun getClosestNote(freq: Float): String {
        return _tunerState.value.notes.map {
            Pair(abs(tableOfFreq[it]?.minus(freq) ?: Float.MAX_VALUE), it)
        }.minBy {
            it.first
        }.second
    }

    /**
     * Computes the exponential moving average deviation in cents between the input signal and target pitch.
     * Uses standard logarithmic conversion where 100 cents corresponds to one equal-tempered semitone.
     *
     * @param freqDetected The parsed frequency signal received from the microphone layer.
     * @param freqTarget The perfect fundamental pitch frequency configuration of the musical note.
     * @return The smoothed offset value bounded in pitch cents.
     */
    private fun getPitchDeviation(
        freqDetected: Float,
        freqTarget: Float,
    ): Float {
        // TODO: In future, may update state to store a setting about sensitivity.
        //  Can even represent in float rather than int.
        val prevCents = _tunerState.value.centsOffset
        val cents = 1200 * log2(freqDetected / freqTarget)

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
        val lastDetectionTime = _tunerState.value.lastDetectionTime
        if (lastDetectionTime != null) {
            val duration = lastDetectionTime.elapsedNow()
            if (duration.inWholeMilliseconds > TunerConfig.LAST_DETECT_TIME_MS) {
                _tunerState.value = _tunerState.value.copy(lastDetectionTime = null)
            }
        }
    }

    /**
     * Increments the drawing bounds offset parameter to drive the background canvas grid flow animation loop.
     */
    fun updateGridShift() {
        val newGridShift = ((_tunerState.value.gridShift.value.toInt() +
                TunerConfig.GRID_FLOW_STEP_DP) % TunerConfig.GRID_SIZE_DP).dp
        _tunerState.value = _tunerState.value.copy(gridShift = newGridShift)
    }

    /**
     * Main entry node processing raw audio evaluation data streams. Performs logarithmic pitch conversion,
     * applies stability filter smoothing, tracks chronological validation thresholds, and triggers completion feedback.
     *
     * @param freq The continuous signal component frequency calculated by the DSP module.
     * @param prob The statistical likelihood confidence rating of the isolated pitch calculation.
     */
    fun updateIncomingFrequency(freq: Float, prob: Float) {
        val isPlayingAudio = _tunerState.value.isPlayingAudio
        val correctThreshold = _settings.value.isCorrectThreshold
        // TODO: Shall I use "tunerState" vs "_tunerState" for reading variables?

        if (!isPlayingAudio && prob >= TunerConfig.PROBABILITY_THRESHOLD) {

            // If time past since last detection is more than LAST_DETECT_TIME_MS,
            // reset the cursor offset.
            _tunerState.value = _tunerState.value.copy(
                lastDetectionTime = markNow()
            )

            _tunerState.value = _tunerState.value.copy(
                incomingFrequency = freq,
                incomingFrequencyProbability = prob
            )

            val prevSelectedNote = _tunerState.value.selectedNote
            if (_tunerState.value.mode == TunerMode.AUTO) {
                _tunerState.value = _tunerState.value.copy(selectedNote = getClosestNote(freq))
            }

            val selectedNote = _tunerState.value.selectedNote
            val selectedFreq = tableOfFreq[_tunerState.value.selectedNote] ?: Float.MAX_VALUE
            val cents = getPitchDeviation(freq, selectedFreq)
            _tunerState.value = _tunerState.value.copy(centsOffset = cents)

            // If correct and selected note the same for CORRECT_TIME_MS or more,
            // then set isCorrect[selectedNote] = true.
            if (abs(cents) <= correctThreshold) {

                val correctStartTime = _tunerState.value.correctStartTime
                if (correctStartTime != null && selectedNote == prevSelectedNote) {
                    val duration = correctStartTime.elapsedNow()
                    if (duration.inWholeMilliseconds > TunerConfig.CORRECT_TIME_MS) {

                        val noteIndex = _tunerState.value.notes.indexOf(selectedNote)
                        val newIsCorrect = _tunerState.value.isCorrect.toMutableList()
                        newIsCorrect[noteIndex] = true
                        _tunerState.value = _tunerState.value.copy(isCorrect = newIsCorrect)

                        // Reset correct checking variables.
                        _tunerState.value = _tunerState.value.copy(correctStartTime = null)

                        // Play correct sound.
                        playCorrect()
                    }
                }
                else {
                    _tunerState.value = _tunerState.value.copy(correctStartTime = markNow())
                }
            }
            else {
                // If out of threshold, reset correctStartTime
                _tunerState.value = _tunerState.value.copy(correctStartTime = null)
            }
        }
    }

    /**
     * Toggles the audio output flag state lock to prevent concurrent microphone listening
     * while playing an audio.
     *
     * @param isPlayingAudio Set to true to declare that active sound playback is taking place.
     */
    private fun setIsPlayingAudio(isPlayingAudio: Boolean) {
        _tunerState.value = _tunerState.value.copy(isPlayingAudio = isPlayingAudio)
    }

    /**
     * Executes the "correct tuning" audio and sets the concurrency lock variables.
     */
    private fun playCorrect() {
        setIsPlayingAudio(true)
        _correctPlayer.playCorrectSound { setIsPlayingAudio(false) }
        println("Playing correct audio...")
    }

    /**
     * Triggers dynamic playback generation for the given musical target pitch tone.
     *
     * @param freq The specific audio reference frequency to synthesize and write out.
     */
    fun playNote(freq: Float) {
        setIsPlayingAudio(true)
        _notePlayer.playNote(freq) { setIsPlayingAudio(false) }
        println("Playing note with frequency: $freq")
    }
}