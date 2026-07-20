package net.dege.salmon

import be.tarsos.dsp.GainProcessor
import be.tarsos.dsp.filters.LowPassFS
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.thread

/**
 * Handles real-time microphone input processing for musical pitch detection.
 * Configures the TarsosDSP audio processing pipeline with filtering and gain amplification.
 */
class TunerFunctionality {
    private val _sampleRate = TunerConfig.SAMPLE_RATE
    private val _audioBufferSize = TunerConfig.AUDIO_BUFFER_SIZE
    private val _bufferOverlap = TunerConfig.BUFFER_OVERLAP

    /**
     * Initializes the microphone dispatcher, sets up the DSP chain, and starts pitch detection.
     * The processing chain filters high frequencies, amplifies input, and runs asynchronously.
     *
     * @param callback Evaluated continuously with the detected frequency (in Hertz) and estimation confidence.
     */
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

        // A 400Hz cutoff preserves all standard guitar fundamentals (E4 is ~329Hz)
        // while stripping the higher harmonics that confuse the algorithm, especially
        // at lower frequencies like E2...
        audioDispatcher.addAudioProcessor(LowPassFS(400f, _sampleRate.toFloat()))

        audioDispatcher.addAudioProcessor(GainProcessor(TunerConfig.TUNER_FUNC_AMPLIFICATION_FACTOR))
        audioDispatcher.addAudioProcessor(audioProcessor)
        thread(name = "tuning-thread") {
            audioDispatcher.run()
        }
    }

    /**
     * Spawns a high-frequency background timer tasked with monitoring or handling inactivity states.
     *
     * @param callback Evaluated periodically every 10 milliseconds.
     */
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