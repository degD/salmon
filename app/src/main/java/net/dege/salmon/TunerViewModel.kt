package net.dege.salmon

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import kotlin.math.abs
import kotlin.math.log2
import kotlin.time.TimeSource.Monotonic.markNow

class TunerViewModel(application: Application) : AndroidViewModel(application) {
    private val _tunerState = mutableStateOf(defaultTunerState)
    val tunerState: State<TunerState> = _tunerState

    private val _settings = mutableStateOf(defaultSettings)
    val tunerSettings = _settings

    private val _notePlayer = NotePlayer()

    private val _correctPlayer = PlayCorrect(application)

    fun setSelectedNote(note: String) {
        if (note in tableOfFreq) {
            _tunerState.value = _tunerState.value.copy(selectedNote = note)
        }
        _tunerState.value = _tunerState.value.copy(correctStartTime = null)
    }

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

    fun toggleMode() {
        if (_tunerState.value.mode == TunerMode.AUTO) {
            setMode(mode = TunerMode.MANUAL)
        }
        else {
            setMode(mode = TunerMode.AUTO)
        }
    }

    fun setModeManual() {
        setMode(mode = TunerMode.MANUAL)
    }

    fun restoreDefaults() {
        _tunerState.value = defaultTunerState
    }

    private fun getClosestNote(freq: Float): String {
        return _tunerState.value.notes.map {
            Pair(abs(tableOfFreq[it]?.minus(freq) ?: Float.MAX_VALUE), it)
        }.minBy {
            it.first
        }.second
    }

    // Used to get an easier to understand representation of tuning state.
    // See https://en.wikipedia.org/wiki/Cent_(music)#Use for more details on calculation.
    // 100 cents is the distance to another semitone. E to F is 100 cents, for example.
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

    fun checkLastDetectionTime() {
        val lastDetectionTime = _tunerState.value.lastDetectionTime
        if (lastDetectionTime != null) {
            val duration = lastDetectionTime.elapsedNow()
            if (duration.inWholeMilliseconds > TunerConfig.LAST_DETECT_TIME_MS) {
                _tunerState.value = _tunerState.value.copy(lastDetectionTime = null)
            }
        }
    }

    fun updateGridShift() {
        val newGridShift = ((_tunerState.value.gridShift.value.toInt() +
                TunerConfig.GRID_FLOW_STEP_DP) % TunerConfig.GRID_SIZE_DP).dp
        _tunerState.value = _tunerState.value.copy(gridShift = newGridShift)
    }

    // Tuner gets audio, runs pitch (freq) detection. If detects
    // pitch, runs this function to update the state.
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

    private fun setIsPlayingAudio(isPlayingAudio: Boolean) {
        _tunerState.value = _tunerState.value.copy(isPlayingAudio = isPlayingAudio)
    }

    private fun playCorrect() {
        setIsPlayingAudio(true)
        _correctPlayer.playCorrectSound { setIsPlayingAudio(false) }
        println("Playing correct audio...")
    }

    fun playNote(freq: Float) {
        setIsPlayingAudio(true)
        _notePlayer.playNote(freq) { setIsPlayingAudio(false) }
        println("Playing note with frequency: $freq")
    }

    // TODO: Write function documentations...
}

