package com.samsung.android.seamless.overlay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.samsung.android.seamless.overlay.ui.CollapsedBubbleView
import com.samsung.android.seamless.overlay.ui.ExpandedOverlayView
import com.samsung.android.seamless.service.SpeechRecognitionService
import com.samsung.android.seamless.widget.WidgetStateManager
import kotlinx.coroutines.runBlocking
import kotlin.math.abs

class OverlayManager(
    context: Context
) {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var bubbleView: CollapsedBubbleView? = null
    private var expandedView: ExpandedOverlayView? = null

    fun render(state: OverlayUiState) {
        if (!state.overlayVisible) {
            hide()
            return
        }

        if (state.expanded) {
            showExpanded(state)
        } else {
            showCollapsed(state)
        }
    }

    fun hide() {
        bubbleView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        expandedView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        bubbleView = null
        expandedView = null
    }

    private fun showCollapsed(state: OverlayUiState) {
        expandedView?.let { view ->
            runCatching { windowManager.removeView(view) }
            expandedView = null
        }

        if (bubbleView == null) {
            val params = createBubbleLayoutParams()
            val bubble = CollapsedBubbleView(appContext)
            bubble.setOnTouchListener(createDragTouchListener(params))

            windowManager.addView(bubble, params)
            bubbleView = bubble
        }
        bubbleView?.setRecording(state.isRecording)
    }

    private fun showExpanded(state: OverlayUiState) {
        bubbleView?.let { view ->
            runCatching { windowManager.removeView(view) }
            bubbleView = null
        }

        val expanded = expandedView
        if (expanded == null) {
            val view = ExpandedOverlayView(
                context = appContext,
                onCollapse = { OverlayStateStore.setExpanded(false) },
                onDismiss = {
                    if (OverlayStateStore.state.value.isRecording) {
                        OverlayStateStore.setExpanded(false)
                    } else {
                        OverlayStateStore.setOverlayVisible(false)
                    }
                },
                onToggleRecognition = {
                    if (OverlayStateStore.state.value.isRecording) {
                        SpeechRecognitionService.stopRecognition(appContext)
                    } else {
                        SpeechRecognitionService.startRecognition(appContext)
                    }
                },
                onCopy = { copyTranscriptToClipboard() },
                onClear = { clearTranscript() }
            )
            windowManager.addView(view, createExpandedLayoutParams())
            expandedView = view
            view.bind(state)
        } else {
            expanded.bind(state)
        }
    }

    private fun createBubbleLayoutParams(): WindowManager.LayoutParams {
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt(KEY_BUBBLE_X, DEFAULT_X)
            y = prefs.getInt(KEY_BUBBLE_Y, DEFAULT_Y)
        }
    }

    private fun createExpandedLayoutParams(): WindowManager.LayoutParams {
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            dp(PANEL_WIDTH_DP),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = EXPANDED_Y
        }
    }

    private fun createDragTouchListener(
        params: WindowManager.LayoutParams
    ): View.OnTouchListener {
        return object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var touchDownRawX = 0f
            private var touchDownRawY = 0f

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        touchDownRawX = event.rawX
                        touchDownRawY = event.rawY
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - touchDownRawX).toInt()
                        params.y = initialY + (event.rawY - touchDownRawY).toInt()
                        windowManager.updateViewLayout(view, params)
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        persistBubblePosition(params.x, params.y)
                        val deltaX = abs(event.rawX - touchDownRawX)
                        val deltaY = abs(event.rawY - touchDownRawY)
                        if (deltaX < TAP_SLOP_PX && deltaY < TAP_SLOP_PX) {
                            OverlayStateStore.setExpanded(true)
                        }
                        return true
                    }
                }
                return false
            }
        }
    }

    private fun persistBubblePosition(x: Int, y: Int) {
        prefs.edit()
            .putInt(KEY_BUBBLE_X, x)
            .putInt(KEY_BUBBLE_Y, y)
            .apply()
    }

    private fun copyTranscriptToClipboard() {
        val transcript = OverlayStateStore.state.value.run {
            committedTranscript.ifBlank { partialTranscript }
        }.trim()
        if (transcript.isBlank()) return

        val clipboard = appContext.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("Transcript", transcript))
    }

    private fun clearTranscript() {
        OverlayStateStore.clearTranscript()
        runBlocking {
            WidgetStateManager(appContext).clearTranscriptAndRefresh(
                targetState = OverlayStateStore.state.value.recognitionState
            )
        }
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            appContext.resources.displayMetrics
        ).toInt()

    companion object {
        private const val PREFS_NAME = "seamless_overlay_prefs"
        private const val KEY_BUBBLE_X = "bubble_x"
        private const val KEY_BUBBLE_Y = "bubble_y"
        private const val DEFAULT_X = 40
        private const val DEFAULT_Y = 240
        private const val TAP_SLOP_PX = 12
        private const val EXPANDED_Y = 120
        private const val PANEL_WIDTH_DP = 340
    }
}
