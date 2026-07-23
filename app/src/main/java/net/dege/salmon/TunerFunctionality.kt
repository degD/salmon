package net.dege.salmon

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.GainProcessor
import be.tarsos.dsp.filters.LowPassFS
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.TarsosDSPAudioInputStream
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
     * Creates and initializes a TarsosDSP [AudioDispatcher] connected to the Android microphone input stream.
     *
     * Configures a native Android [AudioRecord] to capture 16-bit PCM mono audio from [MediaRecorder.
    AudioSource.MIC],
     * wraps it in a [TarsosDSPAudioInputStream], and prepares an [AudioDispatcher] for processing.
     *
     * @param sampleRate The audio sampling rate in Hertz (e.g., 44100 Hz).
     * @param bufferSize The number of audio samples per processing window (e.g., 2048). Must match the
    buffer size
     *			 used by downstream [be.tarsos.dsp.AudioProcessor] instances such as [PitchProcessor].
     *	@param bufferOverlap The number of overlapping samples between consecutive audio processing buffers
    (e.g., 0 for no overlap).
     * @return A configured [AudioDispatcher] ready to accept audio processors and run asynchronously.
     *
     * @throws IllegalArgumentException If [AudioRecord] initialization fails due to invalid parameters or
    unsupported hardware configuration.
     * @see AudioRecord
     * @see TarsosDSPAudioInputStream
     * @see AudioDispatcher
     */
    @SuppressLint("MissingPermission")
    fun createAndroidAudioDispatcher(
        sampleRate: Int,
        bufferSize: Int,
        bufferOverlap: Int
    ): AudioDispatcher {
        // If permission missing, app closes by default.
        // Therefore, there is no need for extra permission checks.
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBufferSize, bufferSize * 2)
        )

        audioRecord.startRecording()

        val tarsosFormat = TarsosDSPAudioFormat(
            sampleRate.toFloat(), 16, 1, true, false
        )

        val inputStream = object : TarsosDSPAudioInputStream {
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                return audioRecord.read(b, off, len)
            }
            override fun skip(bytesToSkip: Long): Long = 0
            override fun getFormat(): TarsosDSPAudioFormat = tarsosFormat
            override fun getFrameLength(): Long = -1
            override fun close() {
                audioRecord.stop()
                audioRecord.release()
            }
        }

        return AudioDispatcher(inputStream, bufferSize, bufferOverlap)
    }

    /**
     * Initializes the microphone dispatcher, sets up the DSP chain, and starts pitch detection.
     * The processing chain filters high frequencies, amplifies input, and runs asynchronously.
     *
     * @param callback Evaluated continuously with the detected frequency (in Hertz) and estimation confidence.
     */
    fun startTuner(callback: (pitch: Float, probability: Float) -> Unit) {
        val audioDispatcher = createAndroidAudioDispatcher(_sampleRate, _audioBufferSize, _bufferOverlap)
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