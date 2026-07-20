package net.dege.salmon

import android.content.Context
import android.media.SoundPool
import kotlin.concurrent.thread

class PlayCorrect(context: Context) {

    private val _soundPool: SoundPool
    private val _soundId: Int

    init {
        _soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .build()
        _soundId = _soundPool.load(context, R.raw.correct2, 1)
    }

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