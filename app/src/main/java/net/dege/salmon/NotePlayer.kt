package net.dege.salmon

import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

/**
 * Manages the generation and audio playback of musical notes using [AudioTrack].
 * It synthesizes waveforms dynamically and handles audio thread execution.
 */
class NotePlayer {

    private val _player: AudioTrack
    private val _sampleRate: Int

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
     *
     * @param freq The target frequency of the note in Hertz.
     * @param duration The playback duration in seconds.
     * @param sampleRate The sampling rate of the output device in Hertz.
     * @return A [FloatArray] containing the PCM float audio samples.
     */
    private fun generateSineWaveNote(
        freq: Float,
        duration: Float,
        sampleRate: Int
    ): FloatArray {
        val numOfSamples = (sampleRate * duration).toInt()
        val sineArray = FloatArray(numOfSamples)

        // Fade at the start and end of the audio playing to prevent popping sound.
        // Fade at the 50ms of the start and end.
        val fadeRange = (sampleRate * 0.05).toInt()

        // At each second, the sine wave has to repeat "freq" times.
        // At every "2*PI" radians, another wave completes.
        // Therefore, it has to go through "2*PI*freq" radians in a second.
        // Sample rate is the number of samples at each second.
        // Therefore, each sampling step would be "(2*PI*freq) / sampleRate"
        val step = (2 * PI * freq) / sampleRate
        for (i in 0..<numOfSamples) {
            var amplitude = 1f

            // Fade at start and end by changing amplitude of audio wave.
            if (i < fadeRange) {
                amplitude = i.toFloat() / fadeRange
            }
            else if (i > numOfSamples - fadeRange) {
                amplitude = (numOfSamples - i).toFloat() / fadeRange
            }

            // At 1f amplitude max, audio is not always end at 0, which could
            // cause popping noise, which is undesired.
            sineArray[i] = (amplitude * sin(i * step)).toFloat()
        }
        return sineArray
    }

    /**
     * Synthesizes a square wave audio buffer by mapping a sine wave to binary amplitudes.
     * Includes a 50ms fade-in and fade-out to prevent audible pop artifacts.
     *
     * @param freq The target frequency of the note in Hertz.
     * @param duration The playback duration in seconds.
     * @param sampleRate The sampling rate of the output device in Hertz.
     * @return A [FloatArray] containing the square wave PCM float audio samples.
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
        for (i in 0..<numOfSamples) {
            var amplitude = 1f

            // Fade at start and end by changing amplitude of audio wave.
            if (i < fadeRange) {
                amplitude = i.toFloat() / fadeRange
            }
            else if (i > numOfSamples - fadeRange) {
                amplitude = (numOfSamples - i).toFloat() / fadeRange
            }

            squareArray[i] = (amplitude * squareArray[i])
        }
        return squareArray.toFloatArray()
    }

    /**
     * Writes the generated audio array buffer directly into the [AudioTrack] buffer.
     * This operation blocks until the data is successfully written.
     *
     * @param freq The frequency associated with the audio array.
     * @param audioArray The raw PCM float samples to write.
     */
    private fun writeNoteSin(freq: Float, audioArray: FloatArray) {
        val player = _player
        val numOfSamples = audioArray.size
        player.write(
            audioArray,
            0,
            numOfSamples,
            AudioTrack.WRITE_BLOCKING
        )
    }

    /**
     * Plays a generated note asynchronously on a background thread.
     * Stops any ongoing playback before starting the new note.
     *
     * @param freq The frequency of the note to play in Hertz.
     * @param callback Evaluated immediately after the audio buffer is written to the track.
     */
    fun playNote(
        freq: Float,
        callback: () -> Unit
    ) {
        val player = _player
        val sampleRate = _sampleRate
        thread {
            player.stop()
            player.play()
            writeNoteSin(
                freq,
                generateSquareWaveNote(
                    freq,
                    TunerConfig.NOTE_AUDIO_DURATION_SEC,
                    sampleRate
                )
            )
            // TODO: Square waves are louder than sine waves but they still sound mechanic.
            //  May use "softer" synthetic audio or even guitar recordings in the future.

            callback()
        }
    }
}