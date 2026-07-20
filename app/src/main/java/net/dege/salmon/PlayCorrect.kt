package net.dege.salmon

import android.content.Context
import android.media.SoundPool
import kotlin.concurrent.thread

/**
 * Manages the loading and playback of the "correct tuning" audio notification.
 * Uses [SoundPool] for low-latency resource execution.
 *
 * @param context The application context used to load raw audio resources.
 */
class PlayCorrect(context: Context) {

    private val _soundPool: SoundPool
    private val _soundId: Int

    init {
        _soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .build()
        _soundId = _soundPool.load(context, R.raw.correct2, 1)
    }

    /**
     * Triggers the playback of the "correct tuning" audio effect.
     * Executes a delayed callback on a background thread to account for asynchronous playback.
     *
     * @param callback Evaluated 1000 milliseconds after the audio stream begins playing.
     */
    fun playCorrectSound(callback: () -> Unit) {
        _soundPool.play(
            _soundId,
            1f,
            1f,
            1,
            0,
            1f
        )

        // That's necessary because soundPool plays audio asynchronously.
        thread {
            Thread.sleep(1000)
            callback()
        }
    }

}