package net.dege.salmon

import androidx.compose.runtime.MutableFloatState
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm
import kotlin.concurrent.thread

class TunerFunctionality(
    detectedPitch: MutableFloatState,
    detectedPitchProbability: MutableFloatState,
) {
    private val _detectedPitch = detectedPitch
    private val _detectedPitchProbability = detectedPitchProbability
    private val _sampleRate = 22050
    private val _audioBufferSize = 1024
    private val _bufferOverlap = 0

    fun startTuner() {
        val audioDispatcher = AudioDispatcherFactory.fromDefaultMicrophone(
            _sampleRate, _audioBufferSize, _bufferOverlap
        )
        val pdh = PitchDetectionHandler {
                result, _ ->
                    _detectedPitch.floatValue = result.pitch
                    _detectedPitchProbability.floatValue = result.probability
        }
        val audioProcessor = PitchProcessor(
            PitchEstimationAlgorithm.FFT_YIN,
            _sampleRate.toFloat(), _audioBufferSize, pdh
        )

        println("Tuner started!")
        audioDispatcher.addAudioProcessor(audioProcessor)
        thread(name = "tuning-thread") {
            audioDispatcher.run()
        }
    }
}