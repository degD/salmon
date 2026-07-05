package net.dege.salmon

import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm
import kotlinx.coroutines.delay
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.thread

class TunerFunctionality {
    private val _sampleRate = TunerConfig.SAMPLE_RATE
    private val _audioBufferSize = TunerConfig.AUDIO_BUFFER_SIZE
    private val _bufferOverlap = TunerConfig.BUFFER_OVERLAP

    fun startTuner(callback: (pitch: Float, probability: Float) -> Unit) {
        val audioDispatcher = AudioDispatcherFactory.fromDefaultMicrophone(
            _sampleRate, _audioBufferSize, _bufferOverlap
        )
        val pdh = PitchDetectionHandler {
                result, _ -> callback(result.pitch, result.probability)
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

    fun startTunerInactivityLimit(callback: () -> Unit) {
        fixedRateTimer(
            name = "inactivity-timer",
            initialDelay = 0.toLong(),
            period = 10
        ) {
            callback()
        }
    }
}