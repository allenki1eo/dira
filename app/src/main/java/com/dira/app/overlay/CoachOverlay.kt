package com.dira.app.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.dira.app.R
import kotlin.math.min

/**
 * Chat-head style HUD over other apps: draggable bubble, ask panel (type/voice),
 * pass-through precision pointer (box + pulse), and a Hear-again control.
 */
class CoachOverlay(
    private val context: Context,
    private val callbacks: Callbacks,
) {
    interface Callbacks {
        fun onGuide(question: String)
        fun onStop()
        fun onMic()
        fun onHearAgain()
    }

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = context.resources.displayMetrics.density
    private val bubbleSize = (56 * density).toInt()

    private var bubbleView: TextView? = null
    private var panelView: View? = null
    private var pointerView: PointerView? = null
    private var chipView: TextView? = null
    private var hearView: TextView? = null
    private var questionField: EditText? = null
    private var statusView: TextView? = null
    private var guideButton: Button? = null
    private var micButton: Button? = null
    private var hearButton: Button? = null
    private var spinner: ProgressBar? = null

    private var bubbleX = 0
    private var bubbleY = 0

    fun attach() {
        if (bubbleView != null) return
        val metrics = displayMetrics()
        bubbleX = metrics.widthPixels - bubbleSize - (16 * density).toInt()
        bubbleY = (metrics.heightPixels * 0.35f).toInt()
        addBubble()
        addPointer()
        addChip()
        addHearChip()
        addPanel()
        showAsk(false)
    }

    fun destroy() {
        pointerView?.stopPulse()
        listOf(bubbleView, panelView, pointerView, chipView, hearView).forEach { view ->
            if (view != null) {
                runCatching { wm.removeView(view) }
            }
        }
        bubbleView = null
        panelView = null
        pointerView = null
        chipView = null
        hearView = null
        questionField = null
        statusView = null
        guideButton = null
        micButton = null
        hearButton = null
        spinner = null
    }

    fun showAsk(show: Boolean) {
        panelView?.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            pointerView?.visibility = View.GONE
            chipView?.visibility = View.GONE
            hearView?.visibility = View.GONE
        }
    }

    fun hideChromeForCapture() {
        bubbleView?.visibility = View.INVISIBLE
        panelView?.visibility = View.GONE
        pointerView?.visibility = View.GONE
        chipView?.visibility = View.GONE
        hearView?.visibility = View.GONE
    }

    fun restoreChrome(showAsk: Boolean) {
        bubbleView?.visibility = View.VISIBLE
        showAsk(showAsk)
    }

    fun setLoading(loading: Boolean) {
        spinner?.visibility = if (loading) View.VISIBLE else View.GONE
        guideButton?.isEnabled = !loading
        micButton?.isEnabled = !loading
        questionField?.isEnabled = !loading
        if (loading) {
            statusView?.setTextColor(Color.parseColor("#B2DFDB"))
            statusView?.text = context.getString(R.string.overlay_thinking)
            hearButton?.isEnabled = false
            hearView?.isEnabled = false
        }
    }

    fun setListening(listening: Boolean) {
        micButton?.text = if (listening) {
            context.getString(R.string.overlay_listening)
        } else {
            context.getString(R.string.overlay_voice)
        }
    }

    fun setQuestion(text: String) {
        questionField?.setText(text)
        questionField?.setSelection(text.length)
    }

    fun question(): String = questionField?.text?.toString().orEmpty()

    fun setInstruction(text: String) {
        statusView?.setTextColor(Color.WHITE)
        statusView?.text = text
        chipView?.text = text
        if (text.isNotBlank() && panelView?.visibility != View.VISIBLE) {
            chipView?.visibility = View.VISIBLE
        }
    }

    fun setHearEnabled(enabled: Boolean) {
        hearButton?.isEnabled = enabled
        hearView?.isEnabled = enabled
        hearView?.alpha = if (enabled) 1f else 0.4f
    }

    fun setError(text: String?) {
        if (text.isNullOrBlank()) return
        statusView?.setTextColor(Color.parseColor("#FFCDD2"))
        statusView?.text = text
        showAsk(true)
    }

    fun showPointer(
        xFraction: Float,
        yFraction: Float,
        boxX: Float = Float.NaN,
        boxY: Float = Float.NaN,
        boxW: Float = Float.NaN,
        boxH: Float = Float.NaN,
        label: String = "",
        confidence: Float = 0.7f,
    ) {
        pointerView?.setTarget(xFraction, yFraction, boxX, boxY, boxW, boxH, label, confidence)
        pointerView?.visibility = View.VISIBLE
        showAsk(false)
        if (chipView?.text?.isNotBlank() == true) {
            chipView?.visibility = View.VISIBLE
        }
        hearView?.visibility = View.VISIBLE
    }

    private fun addBubble() {
        val view = TextView(context).apply {
            text = "D"
            textSize = 22f
            setTextColor(Color.parseColor("#F4D35E"))
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#0D7377"))
            }
            elevation = 12 * density
            contentDescription = context.getString(R.string.app_name)
            setOnTouchListener(dragOrTapListener())
        }
        val params = overlayParams(
            width = bubbleSize,
            height = bubbleSize,
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bubbleX
            y = bubbleY
        }
        wm.addView(view, params)
        bubbleView = view
    }

    private fun addPointer() {
        val view = PointerView(context)
        val params = overlayParams(
            width = WindowManager.LayoutParams.MATCH_PARENT,
            height = WindowManager.LayoutParams.MATCH_PARENT,
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        )
        view.visibility = View.GONE
        wm.addView(view, params)
        pointerView = view
    }

    private fun addChip() {
        val view = TextView(context).apply {
            setPadding(dp(16), dp(10), dp(16), dp(10))
            textSize = 14f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = 16 * density
                setColor(Color.parseColor("#CC0D7377"))
            }
            maxLines = 4
        }
        val params = overlayParams(
            width = WindowManager.LayoutParams.MATCH_PARENT,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        ).apply {
            gravity = Gravity.TOP or Gravity.FILL_HORIZONTAL
            y = dp(48)
            x = 0
        }
        view.visibility = View.GONE
        wm.addView(view, params)
        chipView = view
    }

    private fun addHearChip() {
        val view = TextView(context).apply {
            text = context.getString(R.string.overlay_hear_again)
            textSize = 13f
            setTextColor(Color.parseColor("#F4D35E"))
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = 20 * density
                setColor(Color.parseColor("#F21B2A2E"))
                setStroke(dp(1), Color.parseColor("#F4D35E"))
            }
            setOnClickListener { callbacks.onHearAgain() }
        }
        val params = overlayParams(
            width = WindowManager.LayoutParams.WRAP_CONTENT,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            y = dp(132)
            x = dp(12)
        }
        view.visibility = View.GONE
        wm.addView(view, params)
        hearView = view
    }

    private fun addPanel() {
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(20))
            background = GradientDrawable().apply {
                cornerRadius = 20 * density
                setColor(Color.parseColor("#F21B2A2E"))
            }
        }
        statusView = TextView(context).apply {
            text = context.getString(R.string.overlay_ask_hint)
            setTextColor(Color.WHITE)
            textSize = 15f
        }
        questionField = EditText(context).apply {
            hint = context.getString(R.string.ask_hint)
            setHintTextColor(Color.parseColor("#90A4AE"))
            setTextColor(Color.WHITE)
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, _, _ ->
                callbacks.onGuide(text?.toString().orEmpty())
                true
            }
        }
        spinner = ProgressBar(context).apply {
            isIndeterminate = true
            visibility = View.GONE
        }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        guideButton = Button(context).apply {
            text = context.getString(R.string.guide_step)
            setOnClickListener { callbacks.onGuide(questionField?.text?.toString().orEmpty()) }
        }
        micButton = Button(context).apply {
            text = context.getString(R.string.overlay_voice)
            setOnClickListener { callbacks.onMic() }
        }
        hearButton = Button(context).apply {
            text = context.getString(R.string.overlay_hear_again)
            isEnabled = false
            setOnClickListener { callbacks.onHearAgain() }
        }
        val stop = Button(context).apply {
            text = context.getString(R.string.stop)
            setOnClickListener { callbacks.onStop() }
        }
        row.addView(guideButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(micButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        column.addView(statusView)
        column.addView(spinner)
        column.addView(questionField, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(8) })
        column.addView(row, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(8) })
        column.addView(hearButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
        column.addView(stop, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
        val params = overlayParams(
            width = WindowManager.LayoutParams.MATCH_PARENT,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.FILL_HORIZONTAL
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        wm.addView(column, params)
        panelView = column
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun dragOrTapListener() = View.OnTouchListener { v, event ->
        val params = v.layoutParams as WindowManager.LayoutParams
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                v.setTag(R.id.overlay_touch_start, floatArrayOf(event.rawX, event.rawY, params.x.toFloat(), params.y.toFloat()))
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val start = v.getTag(R.id.overlay_touch_start) as? FloatArray ?: return@OnTouchListener false
                params.x = (start[2] + event.rawX - start[0]).toInt()
                params.y = (start[3] + event.rawY - start[1]).toInt()
                wm.updateViewLayout(v, params)
                true
            }
            MotionEvent.ACTION_UP -> {
                val start = v.getTag(R.id.overlay_touch_start) as? FloatArray
                val dx = if (start != null) kotlin.math.abs(event.rawX - start[0]) else 0f
                val dy = if (start != null) kotlin.math.abs(event.rawY - start[1]) else 0f
                if (dx < 12 && dy < 12) {
                    val open = panelView?.visibility == View.VISIBLE
                    showAsk(!open)
                }
                true
            }
            else -> false
        }
    }

    private fun overlayParams(width: Int, height: Int, flags: Int): WindowManager.LayoutParams {
        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        return WindowManager.LayoutParams(width, height, type, flags, PixelFormat.TRANSLUCENT)
    }

    private fun displayMetrics(): DisplayMetrics {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        return dm
    }

    private fun dp(value: Int) = (value * density).toInt()
}

internal class PointerView(context: Context) : View(context) {
    private var xFrac = 0.5f
    private var yFrac = 0.5f
    private var boxX = Float.NaN
    private var boxY = Float.NaN
    private var boxW = Float.NaN
    private var boxH = Float.NaN
    private var label = ""
    private var confidence = 0.7f
    private var pulseT = 0f
    private val rect = RectF()

    private val pulse = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1600
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            pulseT = it.animatedValue as Float
            invalidate()
        }
    }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33E53935")
        style = Paint.Style.FILL
    }
    private val boxStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E53935")
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val corner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F4D35E")
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E53935")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E53935")
        style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
    }
    private val labelBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC0D7377")
        style = Paint.Style.FILL
    }

    fun setTarget(
        x: Float,
        y: Float,
        left: Float,
        top: Float,
        widthFrac: Float,
        heightFrac: Float,
        targetLabel: String,
        conf: Float,
    ) {
        xFrac = x.coerceIn(0f, 1f)
        yFrac = y.coerceIn(0f, 1f)
        boxX = left
        boxY = top
        boxW = widthFrac
        boxH = heightFrac
        label = targetLabel
        confidence = conf.coerceIn(0f, 1f)
        boxStroke.pathEffect = if (confidence < 0.4f) {
            DashPathEffect(floatArrayOf(18f, 12f), 0f)
        } else {
            null
        }
        invalidate()
        if (!pulse.isStarted) pulse.start()
    }

    fun stopPulse() {
        pulse.cancel()
    }

    override fun onDetachedFromWindow() {
        pulse.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width * xFrac
        val cy = height * yFrac
        val hasBox = boxW.isFinite() && boxH.isFinite() && boxW >= 0.02f && boxH >= 0.015f &&
            boxX.isFinite() && boxY.isFinite()
        val left: Float
        val top: Float
        val right: Float
        val bottom: Float
        if (hasBox) {
            left = width * boxX.coerceIn(0f, 1f)
            top = height * boxY.coerceIn(0f, 1f)
            right = (left + width * boxW).coerceAtMost(width.toFloat())
            bottom = (top + height * boxH).coerceAtMost(height.toFloat())
        } else {
            val w = width * 0.16f
            val h = height * 0.07f
            left = (cx - w / 2f).coerceAtLeast(0f)
            top = (cy - h / 2f).coerceAtLeast(0f)
            right = (left + w).coerceAtMost(width.toFloat())
            bottom = (top + h).coerceAtMost(height.toFloat())
        }
        rect.set(left, top, right, bottom)
        val radius = 18f
        canvas.drawRoundRect(rect, radius, radius, fill)
        canvas.drawRoundRect(rect, radius, radius, boxStroke)

        val tick = min(36f, min(rect.width(), rect.height()) * 0.35f)
        drawCorner(canvas, left, top, tick, 1, 1)
        drawCorner(canvas, right, top, tick, -1, 1)
        drawCorner(canvas, left, bottom, tick, 1, -1)
        drawCorner(canvas, right, bottom, tick, -1, -1)

        val pulseR = 22f + pulseT * 48f
        ring.alpha = ((1f - pulseT) * 180f).toInt().coerceIn(0, 180)
        canvas.drawCircle(cx, cy, pulseR, ring)
        val pulseR2 = 22f + ((pulseT + 0.5f) % 1f) * 48f
        ring.alpha = ((1f - ((pulseT + 0.5f) % 1f)) * 140f).toInt().coerceIn(0, 140)
        canvas.drawCircle(cx, cy, pulseR2, ring)
        ring.alpha = 255
        canvas.drawCircle(cx, cy, 26f, ring)
        canvas.drawCircle(cx, cy, 8f, dot)

        if (label.isNotBlank()) {
            val pad = 12f
            val tw = labelPaint.measureText(label)
            val th = labelPaint.textSize
            var lx = left
            var ly = top - th - pad * 2f - 8f
            if (ly < 8f) ly = bottom + pad + th
            val bg = RectF(lx, ly - th, lx + tw + pad * 2f, ly + pad)
            canvas.drawRoundRect(bg, 12f, 12f, labelBg)
            canvas.drawText(label, lx + pad, ly - 4f, labelPaint)
        }
    }

    private fun drawCorner(canvas: Canvas, x: Float, y: Float, tick: Float, dirX: Int, dirY: Int) {
        canvas.drawLine(x, y, x + dirX * tick, y, corner)
        canvas.drawLine(x, y, x, y + dirY * tick, corner)
    }
}
