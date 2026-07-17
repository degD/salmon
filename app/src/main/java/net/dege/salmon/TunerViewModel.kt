package net.dege.salmon

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dege.salmon.tableOfFreq
import kotlin.collections.listOf
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.round
import kotlin.time.TimeSource.Monotonic.markNow

class TunerViewModel : ViewModel() {
    private val _tunerState = mutableStateOf(defaultTunerState)
    val tunerState: State<TunerState> = _tunerState

    private val _settings = mutableStateOf(defaultSettings)
    val tunerSettings = _settings

    private val notePlayer = NotePlayer()

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
    // TODO: It can be further divided by a "simplifier" number for easier representation.
    //  For example, it seems that GuitarTuna divides that number by "5".
    private fun getPitchDeviation(
        freqDetected: Float,
        freqTarget: Float,
    ): Float {
        // TODO: In future, may update state to store a setting about sensitivity.
        //  Can even represent in float rather than int.
        val cents = 1200 * log2(freqDetected / freqTarget)
        return cents
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
                    }
                }
                else {
                    _tunerState.value = _tunerState.value.copy(correctStartTime = markNow())
                }
            }
        }
    }

    private fun setIsPlayingAudio(isPlayingAudio: Boolean) {
        _tunerState.value = _tunerState.value.copy(isPlayingAudio = isPlayingAudio)
    }

    fun playNote(freq: Float) {
        setIsPlayingAudio(true)
        notePlayer.playNote(freq) { setIsPlayingAudio(false) }
        println("Playing note with frequency: $freq")
    }

    // TODO: Write function documentations...
}

