package net.dege.salmon

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import net.dege.salmon.tableOfFreq
import kotlin.collections.listOf
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.round

class TunerViewModel : ViewModel() {
    private val _tunerState = mutableStateOf(defaultTunerState)
    val tunerState: State<TunerState> = _tunerState

    fun setSelectedNote(note: String) {
        if (note in tableOfFreq) {
            _tunerState.value = _tunerState.value.copy(selectedNote = note)
        }
    }

    fun toggleMode() {
        if (_tunerState.value.mode == TunerMode.AUTO) {
            _tunerState.value = _tunerState.value.copy(mode = TunerMode.MANUAL)
        }
        else {
            _tunerState.value = _tunerState.value.copy(mode = TunerMode.AUTO)
        }
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

    // Tuner gets audio, runs pitch (freq) detection. If detects
    // pitch, runs this function to update the state.
    fun updateIncomingFrequency(freq: Float, prob: Float) {
        if (prob >= TunerConfig.PROBABILITY_THRESHOLD) {
            _tunerState.value = _tunerState.value.copy(
                incomingFrequency = freq,
                incomingFrequencyProbability = prob
            )
        }

        if (_tunerState.value.mode == TunerMode.AUTO) {
            _tunerState.value = _tunerState.value.copy(selectedNote = getClosestNote(freq))
        }

        val selectedFreq = tableOfFreq[_tunerState.value.selectedNote] ?: Float.MAX_VALUE
        val cents = getPitchDeviation(freq, selectedFreq)
        _tunerState.value = _tunerState.value.copy(centsOffset = cents)
    }
}