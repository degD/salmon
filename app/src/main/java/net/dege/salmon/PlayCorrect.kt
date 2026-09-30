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

    private val _soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .build()

    private var _soundId: Int = 0
    private var _loaded: Boolean = false

    private val _lock = Any()

    init {
        _soundPool.setOnLoadCompleteListener { _, _, status ->
            synchronized(_lock) {
                _loaded = status == 0
            }
        }
        _soundId = _soundPool.load(context, R.raw.correct2, 1)
    }

    /**
     * Triggers the playback of the "correct tuning" audio effect.
     * The callback runs once the audible duration has elapsed, so the caller can
     * safely release its "audio in progress" lock.
     *
     * @param callback Evaluated roughly 1000ms after the audio begins.
     */
    fun playCorrectSound(callback: () -> Unit) {
        val ready = synchronized(_lock) { _loaded }

        if (!ready) {
            // Audio wasn't loaded in time; resolve the callback immediately so we
            // never deadlock the microphone lock.
            callback()
            return
        }

        _soundPool.play(_soundId, 1f, 1f, 1, 0, 1f)

        // SoundPool plays asynchronously and offers no simple completion callback,
        // so we approximate the completion moment with a sleep.
        thread(name = "correct-sound-callback") {
            try {
                Thread.sleep(1000)
            } catch (_: InterruptedException) {
                // Fall through and fire the callback anyway.
            }
            callback()
        }
    }

    /**
     * Immediately silences any in-progress playback.
     */
    fun stop() {
        try {
            _soundPool.autoPause()
        } catch (_: IllegalStateException) {
            // Ignore.
        }
    }

    /**
     * Releases the underlying [SoundPool]. Call once when the owner is torn down.
     */
    fun release() {
        _soundPool.release()
    }
}