package com.dira.app.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.provider.Settings
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.dira.app.MainActivity
import com.dira.app.R
import com.dira.app.overlay.CoachCoordinator

/**
 * Foreground service required for MediaProjection on modern Android.
 * Captures frames into [SessionFrameBuffer] only — never to files.
 */
class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null
    private var coordinator: CoachCoordinator? = null
    private var savedUseSwahili = false
    private var savedGuideBase = ""

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            tearDownCapture(clearBuffer = true)
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopCaptureAndSelf()
                return START_NOT_STICKY
            }
            ACTION_ENABLE_OVERLAY -> {
                savedUseSwahili = intent.getBooleanExtra(EXTRA_USE_SWAHILI, savedUseSwahili)
                savedGuideBase = intent.getStringExtra(EXTRA_GUIDE_BASE) ?: savedGuideBase
                attachOverlayIfAllowed()
                return START_STICKY
            }
            ACTION_START, null -> {
                val code = intent?.getIntExtra(EXTRA_RESULT_CODE, MediaProjectionHolder.resultCode)
                    ?: MediaProjectionHolder.resultCode
                val data = if (Build.VERSION.SDK_INT >= 33) {
                    intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent?.getParcelableExtra(EXTRA_RESULT_DATA)
                } ?: MediaProjectionHolder.resultData

                if (data == null || code == 0) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForegroundWithType()
                beginProjection(code, data)
                savedUseSwahili = intent?.getBooleanExtra(EXTRA_USE_SWAHILI, false) == true
                savedGuideBase = intent?.getStringExtra(EXTRA_GUIDE_BASE).orEmpty()
                // OS permission is the source of truth — not the extra from when Help was tapped.
                attachOverlayIfAllowed()
            }
        }
        return START_STICKY
    }

    private fun attachOverlayIfAllowed() {
        if (!Settings.canDrawOverlays(this)) return
        if (mediaProjection == null) return
        if (coordinator != null) return
        coordinator = CoachCoordinator(
            this,
            useSwahili = savedUseSwahili,
            guideBase = savedGuideBase,
        ).also { it.start() }
    }

    private fun startForegroundWithType() {
        ensureChannel()
        val pending = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.watching))
            .setContentText(getString(R.string.capture_notification_body))
            .setSmallIcon(R.drawable.ic_stat_dira)
            .setContentIntent(pending)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun beginProjection(resultCode: Int, data: Intent) {
        tearDownCapture(clearBuffer = false)
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = mgr.getMediaProjection(resultCode, data) ?: run {
            stopSelf()
            return
        }
        mediaProjection = projection
        projection.registerCallback(projectionCallback, Handler(mainLooper))

        val metrics = displayMetrics()
        val width = metrics.widthPixels.coerceAtLeast(1)
        val height = metrics.heightPixels.coerceAtLeast(1)
        val density = metrics.densityDpi

        captureThread = HandlerThread("dira-capture").also { it.start() }
        captureHandler = Handler(captureThread!!.looper)

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2).also { reader ->
            reader.setOnImageAvailableListener({ r ->
                var image: Image? = null
                try {
                    image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                    val bitmap = imageToBitmap(image) ?: return@setOnImageAvailableListener
                    SessionFrameBuffer.shared.put(bitmap)
                } catch (_: Exception) {
                    // Drop frame; never persist.
                } finally {
                    image?.close()
                }
            }, captureHandler)
        }

        virtualDisplay = projection.createVirtualDisplay(
            "dira-vd",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null,
            captureHandler,
        )
        isRunning = true
        MediaProjectionHolder.clear()
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888,
        )
        bitmap.copyPixelsFromBuffer(buffer)
        return if (rowPadding == 0) {
            bitmap
        } else {
            val cropped = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            if (cropped !== bitmap) bitmap.recycle()
            cropped
        }
    }

    private fun displayMetrics(): DisplayMetrics {
        val dm = DisplayMetrics()
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        return dm
    }

    private fun stopCaptureAndSelf() {
        tearDownCapture(clearBuffer = true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun tearDownCapture(clearBuffer: Boolean) {
        isRunning = false
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {
        }
        virtualDisplay = null
        try {
            imageReader?.setOnImageAvailableListener(null, null)
            imageReader?.close()
        } catch (_: Exception) {
        }
        imageReader = null
        try {
            mediaProjection?.unregisterCallback(projectionCallback)
            mediaProjection?.stop()
        } catch (_: Exception) {
        }
        mediaProjection = null
        captureThread?.quitSafely()
        captureThread = null
        captureHandler = null
        coordinator?.destroy()
        coordinator = null
        if (clearBuffer) {
            SessionFrameBuffer.shared.clear()
        }
    }

    private fun ensureChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.capture_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    override fun onDestroy() {
        tearDownCapture(clearBuffer = true)
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.dira.app.capture.START"
        const val ACTION_STOP = "com.dira.app.capture.STOP"
        const val ACTION_ENABLE_OVERLAY = "com.dira.app.capture.ENABLE_OVERLAY"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_DATA = "resultData"
        const val EXTRA_OVERLAY = "overlay"
        const val EXTRA_USE_SWAHILI = "useSwahili"
        const val EXTRA_GUIDE_BASE = "guideBase"
        private const val CHANNEL_ID = "dira_capture"
        private const val NOTIFICATION_ID = 42

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(
            context: Context,
            resultCode: Int,
            data: Intent,
            overlay: Boolean = false,
            useSwahili: Boolean = false,
            guideBase: String = "",
        ) {
            MediaProjectionHolder.set(resultCode, data)
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
                putExtra(EXTRA_OVERLAY, overlay)
                putExtra(EXTRA_USE_SWAHILI, useSwahili)
                putExtra(EXTRA_GUIDE_BASE, guideBase)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun enableOverlay(context: Context, useSwahili: Boolean, guideBase: String) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_ENABLE_OVERLAY
                putExtra(EXTRA_USE_SWAHILI, useSwahili)
                putExtra(EXTRA_GUIDE_BASE, guideBase)
            }
            context.startService(intent)
        }
    }
}
