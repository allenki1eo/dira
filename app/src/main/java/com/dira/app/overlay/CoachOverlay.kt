package com.dira.app.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.dira.app.R

/**
 * Chat-head style HUD over other apps: draggable bubble, ask panel (type/voice),
 * and a pass-through pointer on the real screen.
 */
class CoachOverlay(
    private val context: Context,
    private val callbacks: Callbacks,
) {
    interface Callbacks {
        fun onGuide(question: String)
        fun onStop()
        fun onMic()
    }

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = context.resources.displayMetrics.density
    private val bubbleSize = (56 * density).toInt()

    private var bubbleView: TextView? = null
    private var panelView: View? = null
    private var pointerView: PointerView? = null
    private var chipView: TextView? = null
    private var questionField: EditText? = null
    private var statusView: TextView? = null
    private var guideButton: Button? = null
    private var micButton: Button? = null
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
        addPanel()
        showAsk(false)
    }

    fun destroy() {
        listOf(bubbleView, panelView, pointerView, chipView).forEach { view ->
            if (view != null) {
                runCatching { wm.removeView(view) }
            }
        }
        bubbleView = null
        panelView = null
        pointerView = null
        chipView = null
        questionField = null
        statusView = null
        guideButton = null
        micButton = null
        spinner = null
    }

    fun showAsk(show: Boolean) {
        panelView?.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            pointerView?.visibility = View.GONE
            chipView?.visibility = View.GONE
        }
    }

    fun hideChromeForCapture() {
        bubbleView?.visibility = View.INVISIBLE
        panelView?.visibility = View.GONE
        pointerView?.visibility = View.GONE
        chipView?.visibility = View.GONE
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

    fun setError(text: String?) {
        if (text.isNullOrBlank()) return
        statusView?.setTextColor(Color.parseColor("#FFCDD2"))
        statusView?.text = text
        showAsk(true)
    }

    fun showPointer(xFraction: Float, yFraction: Float) {
        pointerView?.setTarget(xFraction, yFraction)
        pointerView?.visibility = View.VISIBLE
        showAsk(false)
        if (chipView?.text?.isNotBlank() == true) {
            chipView?.visibility = View.VISIBLE
        }
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
            maxLines = 3
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

private class PointerView(context: Context) : View(context) {
    private var xFrac = 0.5f
    private var yFrac = 0.5f

    fun setTarget(x: Float, y: Float) {
        xFrac = x.coerceIn(0f, 1f)
        yFrac = y.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        val cx = width * xFrac
        val cy = height * yFrac
        val fill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#40E53935")
            style = android.graphics.Paint.Style.FILL
        }
        val ring = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E53935")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 8f
        }
        val dot = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E53935")
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, 56f, fill)
        canvas.drawCircle(cx, cy, 28f, ring)
        canvas.drawCircle(cx, cy, 8f, dot)
    }
}
