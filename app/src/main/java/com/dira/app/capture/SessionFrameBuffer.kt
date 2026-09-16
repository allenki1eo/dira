package com.dira.app.capture

import android.graphics.Bitmap
import java.util.concurrent.atomic.AtomicReference

/**
 * In-memory session frame buffer only — never written to disk/DB.
 * Cleared on Stop / task end / timeout / service destroy.
 */
class SessionFrameBuffer {
    private val latest = AtomicReference<Bitmap?>(null)

    fun put(frame: Bitmap) {
        val previous = latest.getAndSet(frame)
        if (previous != null && previous !== frame && !previous.isRecycled) {
            previous.recycle()
        }
    }

    fun snapshot(): Bitmap? = latest.get()?.takeUnless { it.isRecycled }

    fun clear() {
        val previous = latest.getAndSet(null)
        if (previous != null && !previous.isRecycled) {
            previous.recycle()
        }
    }

    fun hasFrame(): Boolean = latest.get()?.isRecycled == false

    companion object {
        @Volatile
        var shared: SessionFrameBuffer = SessionFrameBuffer()
            private set

        fun resetShared() {
            shared.clear()
            shared = SessionFrameBuffer()
        }
    }
}
