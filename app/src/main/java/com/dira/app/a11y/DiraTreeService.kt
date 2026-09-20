package com.dira.app.a11y

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

/**
 * Read-only UI tree for the foreground app. Dira never performs clicks —
 * the dump is extra context for the guide model plus better tap targets.
 */
class DiraTreeService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            if (!pkg.isNullOrBlank() && pkg != packageName) {
                lastPackage.set(pkg)
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: DiraTreeService? = null
            private set

        private val lastPackage = AtomicReference("")

        fun isEnabled(): Boolean = instance != null

        fun lastForegroundPackage(): String = lastPackage.get()

        /**
         * Compact dump of interactive nodes with normalized 0–1 bounds.
         * Request-scoped — not stored.
         */
        fun dumpForegroundTree(maxNodes: Int = 48): String? {
            val service = instance ?: return null
            val root = service.rootInActiveWindow ?: return null
            val screen = Rect()
            root.getBoundsInScreen(screen)
            val sw = screen.width().coerceAtLeast(1)
            val sh = screen.height().coerceAtLeast(1)
            val pkg = root.packageName?.toString() ?: lastPackage.get()
            val lines = ArrayList<String>(maxNodes + 2)
            lines += "pkg=$pkg screen=${sw}x$sh"
            val seen = HashSet<String>()
            fun walk(node: AccessibilityNodeInfo?, depth: Int) {
                if (node == null) return
                try {
                    if (lines.size >= maxNodes + 1 || depth > 12) return
                    val interactive = node.isClickable || node.isCheckable || node.isEditable ||
                        node.isLongClickable || node.isFocusable
                    val text = sequenceOf(
                        node.text?.toString(),
                        node.contentDescription?.toString(),
                        node.hintText?.toString(),
                        node.viewIdResourceName?.substringAfterLast('/'),
                    ).firstOrNull { !it.isNullOrBlank() }.orEmpty().replace('\n', ' ').take(80)
                    if (interactive || text.isNotBlank()) {
                        val b = Rect()
                        node.getBoundsInScreen(b)
                        if (b.width() > 4 && b.height() > 4) {
                            val cx = ((b.exactCenterX() - screen.left) / sw).coerceIn(0f, 1f)
                            val cy = ((b.exactCenterY() - screen.top) / sh).coerceIn(0f, 1f)
                            val flags = buildString {
                                if (node.isClickable) append("tap ")
                                if (node.isEditable) append("edit ")
                                if (node.isCheckable) append("check ")
                                if (node.isSelected) append("sel ")
                            }.trim()
                            val line = "  [$flags] \"$text\" x=${String.format(Locale.US, "%.3f", cx)} y=${String.format(Locale.US, "%.3f", cy)}"
                            if (seen.add(line)) lines += line
                        }
                    }
                    for (i in 0 until node.childCount) {
                        walk(node.getChild(i), depth + 1)
                    }
                } finally {
                    node.recycle()
                }
            }
            walk(root, 0)
            return lines.joinToString("\n").take(6_000)
        }
    }
}
