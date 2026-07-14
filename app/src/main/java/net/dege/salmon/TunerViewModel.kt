package net.dege.salmon

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import net.dege.salmon.ui.settings.SettingsStore
import net.dege.salmon.ui.settings.TuningPreset
import net.dege.salmon.tableOfFreq
import kotlin.math.abs
import kotlin.math.log2
import kotlin.time.TimeSource.Monotonic.markNow

class TunerViewModel : ViewModel() {
    private val _tunerState = mutableStateOf(defaultTunerState)
    val tunerState: State<TunerState> = _tunerState

    private val _settings = mutableStateOf(SettingsStore.load())
    val tunerSettings: State<TunerSettings> = _settings

    init {
        applyPersistedMode()
        applyPresetNotes()
    }

    private fun applyPersistedMode() {
        if (!_settings.value.autoMode) {
            _tunerState.value = _tunerState.value.copy(mode = TunerMode.MANUAL)
        }
    }

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
        } else {
            _tunerState.value = _tunerState.value.copy(mode = TunerMode.MANUAL)
            if (_tunerState.value.selectedNote == null) {
                _tunerState.value = _tunerState.value.copy(
                    selectedNote = _tunerState.value.notes[2]
                )
            }
        }
    }

    fun toggleMode() {
        if (_tunerState.value.mode == TunerMode.AUTO) {
            setMode(mode = TunerMode.MANUAL)
        } else {
            setMode(mode = TunerMode.AUTO)
        }
    }

    fun setModeManual() {
        setMode(mode = TunerMode.MANUAL)
    }

    private fun getClosestNote(freq: Float): String {
        val ratio = _settings.value.referencePitch / 440f
        return _tunerState.value.notes.map {
            val baseFreq = tableOfFreq[it] ?: Float.MAX_VALUE
            Pair(abs(baseFreq * ratio - freq), it)
        }.minBy {
            it.first
        }.second
    }

    private fun getPitchDeviation(
        freqDetected: Float,
        freqTarget: Float,
    ): Float {
        val ratio = _settings.value.referencePitch / 440f
        val adjustedTarget = freqTarget * ratio
        return 1200 * log2(freqDetected / adjustedTarget)
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

    fun updateIncomingFrequency(freq: Float, prob: Float) {
        val correctThreshold = _settings.value.correctThreshold
        if (prob >= TunerConfig.PROBABILITY_THRESHOLD) {

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
                } else {
                    _tunerState.value = _tunerState.value.copy(correctStartTime = markNow())
                }
            }
        }
    }

    // ─── Settings ───

    fun updateSettings(settings: TunerSettings) {
        val old = _settings.value
        _settings.value = settings
        SettingsStore.save(settings)

        if (settings.tuningPreset != old.tuningPreset) {
            applyPresetNotes()
        }
    }

    private fun applyPresetNotes() {
        val preset = _settings.value.tuningPreset
        _tunerState.value = _tunerState.value.copy(
            notes = preset.notes,
            isCorrect = List(preset.notes.size) { false },
            selectedNote = null,
            correctStartTime = null
        )
    }

    fun restoreDefaults() {
        _tunerState.value = defaultTunerState
    }

    fun resetAll() {
        _settings.value = TunerSettings()
        _tunerState.value = defaultTunerState
        SettingsStore.save(_settings.value)
        applyPersistedMode()
    }
}
