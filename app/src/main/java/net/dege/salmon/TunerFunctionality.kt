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
import java.util.Timer
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.thread

/**
 * Handles real-time microphone input processing for musical pitch detection.
 * Configures the TarsosDSP audio processing pipeline with filtering and gain amplification.
 *
 * A single instance owns the running [AudioDispatcher] and the inactivity [Timer] so
 * both can be cleanly stopped when the owner activity is destroyed.
 */
class TunerFunctionality {
    private val _sampleRate = TunerConfig.SAMPLE_RATE
    private val _audioBufferSize = TunerConfig.AUDIO_BUFFER_SIZE
    private val _bufferOverlap = TunerConfig.BUFFER_OVERLAP

    private var _dispatcher: AudioDispatcher? = null
    private var _inactivityTimer: Timer? = null

    /**
     * Creates and initializes a TarsosDSP [AudioDispatcher] connected to the Android
     * microphone input stream.
     *
     * @throws IllegalStateException If [AudioRecord] cannot be initialized.
     */
    @SuppressLint("MissingPermission")
    fun createAndroidAudioDispatcher(
        sampleRate: Int,
        bufferSize: Int,
        bufferOverlap: Int
    ): AudioDispatcher {
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

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            throw IllegalStateException("AudioRecord failed to initialize")
        }

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
                try {
                    audioRecord.stop()
                } finally {
                    audioRecord.release()
                }
            }
        }

        return AudioDispatcher(inputStream, bufferSize, bufferOverlap)
    }

    /**
     * Initializes the microphone dispatcher, sets up the DSP chain, and starts pitch
     * detection. The processing chain filters high frequencies, amplifies input, and
     * runs asynchronously. Idempotent: a second call while running is a no-op.
     *
     * @param callback Evaluated continuously with the detected frequency (in Hertz) and
     *                 estimation confidence.
     */
    fun startTuner(callback: (pitch: Float, probability: Float) -> Unit) {
        if (_dispatcher != null) return

        val audioDispatcher = createAndroidAudioDispatcher(
            _sampleRate, _audioBufferSize, _bufferOverlap
        )
        _dispatcher = audioDispatcher

        val pdh = PitchDetectionHandler { result, _ ->
            callback(result.pitch, result.probability)
        }
        val audioProcessor = PitchProcessor(
            PitchEstimationAlgorithm.FFT_YIN,
            _sampleRate.toFloat(), _audioBufferSize, pdh
        )

        // A 400Hz cutoff preserves standard guitar fundamentals (E4 ~329Hz) while
        // stripping higher harmonics that confuse the algorithm at low frequencies.
        audioDispatcher.addAudioProcessor(LowPassFS(400f, _sampleRate.toFloat()))
        audioDispatcher.addAudioProcessor(GainProcessor(TunerConfig.TUNER_FUNC_AMPLIFICATION_FACTOR))
        audioDispatcher.addAudioProcessor(audioProcessor)

        thread(name = "tuning-thread") {
            audioDispatcher.run()
        }
    }

    /**
     * Spawns a high-frequency background timer tasked with monitoring inactivity states.
     * Idempotent: a second call while running is a no-op.
     *
     * @param callback Evaluated periodically every 10 milliseconds.
     */
    fun startTunerInactivityLimit(callback: () -> Unit) {
        if (_inactivityTimer != null) return
        _inactivityTimer = fixedRateTimer(
            name = "inactivity-timer",
            initialDelay = 0L,
            period = 10L
        ) {
            callback()
        }
    }

    /**
     * Stops the audio dispatcher and inactivity timer. Safe to call multiple times.
     */
    fun stop() {
        _dispatcher?.stop()
        _dispatcher = null

        _inactivityTimer?.cancel()
        _inactivityTimer = null
    }
}