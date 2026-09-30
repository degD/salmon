package net.dege.salmon

import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

/**
 * Manages the generation and audio playback of musical notes using [AudioTrack].
 * It synthesizes waveforms dynamically and handles audio thread execution.
 *
 * Playback is serialized via an internal lock. When a new note is requested while an
 * older one is still playing, the older playback is aborted (its callback still fires)
 * so the newest tap always wins.
 */
class NotePlayer {

    private val _player: AudioTrack
    private val _sampleRate: Int

    private val _lock = Any()
    private var _playbackGeneration: Int = 0

    init {
        val player = AudioTrack.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .build()
        player.setVolume(1f)
        _player = player
        _sampleRate = player.sampleRate
    }

    /**
     * Synthesizes a sine wave audio buffer for a given frequency and duration.
     * Includes a 50ms fade-in and fade-out to prevent audible pop artifacts.
     */
    private fun generateSineWaveNote(
        freq: Float,
        duration: Float,
        sampleRate: Int
    ): FloatArray {
        val numOfSamples = (sampleRate * duration).toInt()
        val sineArray = FloatArray(numOfSamples)

        val fadeRange = (sampleRate * 0.05).toInt()
        val step = (2 * PI * freq) / sampleRate
        for (i in 0 until numOfSamples) {
            var amplitude = 1f

            if (i < fadeRange) {
                amplitude = i.toFloat() / fadeRange
            } else if (i > numOfSamples - fadeRange) {
                amplitude = (numOfSamples - i).toFloat() / fadeRange
            }

            sineArray[i] = (amplitude * sin(i * step)).toFloat()
        }
        return sineArray
    }

    /**
     * Synthesizes a square wave audio buffer by mapping a sine wave to binary amplitudes.
     * Includes a 50ms fade-in and fade-out to prevent audible pop artifacts.
     */
    private fun generateSquareWaveNote(
        freq: Float,
        duration: Float,
        sampleRate: Int
    ): FloatArray {
        val numOfSamples = (sampleRate * duration).toInt()
        val sineArray = generateSineWaveNote(freq, duration, sampleRate)
        val squareArray = sineArray.map { x -> if (x > 0) 0.4f else -0.4f }.toMutableList()

        val fadeRange = (sampleRate * 0.05).toInt()
        for (i in 0 until numOfSamples) {
            var amplitude = 1f

            if (i < fadeRange) {
                amplitude = i.toFloat() / fadeRange
            } else if (i > numOfSamples - fadeRange) {
                amplitude = (numOfSamples - i).toFloat() / fadeRange
            }

            squareArray[i] = (amplitude * squareArray[i])
        }
        return squareArray.toFloatArray()
    }

    /**
     * Writes the generated audio array buffer directly into the [AudioTrack] buffer.
     * This operation blocks until the data is successfully written.
     */
    private fun writeNoteSin(freq: Float, audioArray: FloatArray) {
        val numOfSamples = audioArray.size
        _player.write(
            audioArray,
            0,
            numOfSamples,
            AudioTrack.WRITE_BLOCKING
        )
    }

    /**
     * Plays a generated note asynchronously on a background thread.
     * If another note is currently playing, it is stopped before the new one begins.
     * The callback is invoked once this playback session has fully finished (or been aborted).
     *
     * @param freq The frequency of the note to play in Hertz.
     * @param callback Evaluated after playback completes or the session is superseded.
     */
    fun playNote(
        freq: Float,
        callback: () -> Unit
    ) {
        val generation: Int
        synchronized(_lock) {
            _playbackGeneration++
            generation = _playbackGeneration
        }

        thread(name = "note-playback") {
            var didStart = false
            try {
                synchronized(_lock) {
                    if (generation != _playbackGeneration) return@synchronized
                    _player.stop()
                    _player.play()
                    writeNoteSin(
                        freq,
                        generateSquareWaveNote(
                            freq,
                            TunerConfig.NOTE_AUDIO_DURATION_SEC,
                            _sampleRate
                        )
                    )
                    didStart = true
                }
                if (didStart) {
                    // AudioTrack continues draining its buffer in the background while we
                    // sleep here. This is what keeps the microphone lock engaged for the
                    // full audible duration instead of just the write duration.
                    Thread.sleep((TunerConfig.NOTE_AUDIO_DURATION_SEC * 1000).toLong())
                }
            } catch (_: InterruptedException) {
                // Session was superseded; fall through.
            } finally {
                if (didStart) {
                    synchronized(_lock) {
                        // Only stop the underlying track if we are still the latest session.
                        if (generation == _playbackGeneration) {
                            try {
                                _player.stop()
                                _player.flush()
                            } catch (_: IllegalStateException) {
                                // Track already released / invalid.
                            }
                        }
                    }
                }
                callback()
            }
        }
    }

    /**
     * Immediately stops any ongoing playback and invalidates pending sessions.
     */
    fun stop() {
        synchronized(_lock) {
            _playbackGeneration++
            try {
                _player.pause()
                _player.flush()
            } catch (_: IllegalStateException) {
                // Nothing to stop.
            }
        }
    }

    /**
     * Releases the underlying [AudioTrack]. Call once when the owner is torn down.
     */
    fun release() {
        synchronized(_lock) {
            _playbackGeneration++
            try {
                _player.stop()
            } catch (_: IllegalStateException) {
                // Already stopped.
            }
            _player.release()
        }
    }
}