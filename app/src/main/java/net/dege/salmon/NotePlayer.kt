package net.dege.salmon

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

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

            // At 1f amplitude max, audio is not
            sineArray[i] = (amplitude * sin(i * step)).toFloat()
        }
        return sineArray
    }

    private fun generateSquareWaveNote(
        freq: Float,
        duration: Float,
        sampleRate: Int
    ): FloatArray {
        val sineArray = generateSineWaveNote(freq, duration, sampleRate)
        val squareArray = sineArray.map { x -> if (x > 0) 1f else -1f }
        return squareArray.toFloatArray()
    }

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