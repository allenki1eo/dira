package com.dira.app.capture

import android.content.Intent

/**
 * Holds the one-shot MediaProjection permission result Intent extras
 * long enough to hand off to [ScreenCaptureService]. Cleared after consume.
 */
object MediaProjectionHolder {
    @Volatile
    var resultCode: Int = 0
        private set

    @Volatile
    var resultData: Intent? = null
        private set

    fun set(resultCode: Int, data: Intent) {
        this.resultCode = resultCode
        this.resultData = data
    }

    fun clear() {
        resultCode = 0
        resultData = null
    }

    fun hasPermission(): Boolean = resultData != null && resultCode != 0
}
