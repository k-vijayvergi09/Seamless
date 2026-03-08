package com.samsung.android.seamless.overlay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.util.Log
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
    private val mainHandler = Handler(Looper.getMainLooper())

    private var bubbleView: CollapsedBubbleView? = null
    private var expandedView: ExpandedOverlayView? = null
    private var isAnimatingExpansion = false

    fun render(state: OverlayUiState) {
        Log.i(TAG, "render overlayVisible=${state.overlayVisible} expanded=${state.expanded}")
        if (!state.overlayVisible) {
            hide()
            return
        }

        if (state.expanded) {
            expandFromBubble(state)
        } else {
            showCollapsed(state)
        }
    }

    fun hide() {
        Log.i(TAG, "hide")
        mainHandler.removeCallbacksAndMessages(null)
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
        Log.i(TAG, "showCollapsed bubbleExists=${bubbleView != null}")
        isAnimatingExpansion = false
        expandedView?.let { view ->
            runCatching { windowManager.removeView(view) }
            expandedView = null
        }

        if (bubbleView == null) {
            val params = createBubbleLayoutParams()
            val bubble = CollapsedBubbleView(appContext)
            bubble.setOnTouchListener(createDragTouchListener(params))
            try {
                windowManager.addView(bubble, params)
                bubbleView = bubble
                Log.i(TAG, "Collapsed bubble added x=${params.x} y=${params.y}")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to add collapsed bubble", t)
            }
        }
        bubbleView?.setRecording(state.isRecording)
        bubbleView?.alpha = 1f
        bubbleView?.scaleX = 1f
        bubbleView?.scaleY = 1f
    }

    private fun expandFromBubble(state: OverlayUiState) {
        if (expandedView != null) {
            expandedView?.bind(state)
            return
        }

        if (!isAnimatingExpansion) {
            val bubble = bubbleView
            if (bubble != null) {
                isAnimatingExpansion = true
                bubble.animate()
                    .alpha(0f)
                    .scaleX(0.86f)
                    .scaleY(0.86f)
                    .setDuration(EXPAND_OUT_DURATION_MS)
                    .withEndAction {
                        bubbleView?.let { view ->
                            runCatching { windowManager.removeView(view) }
                            bubbleView = null
                        }
                        showExpanded(state)
                    }
                    .start()
                return
            }
        }

        showExpanded(state)
    }

    private fun showExpanded(state: OverlayUiState) {
        Log.i(TAG, "showExpanded expandedExists=${expandedView != null}")
        bubbleView?.let { view ->
            runCatching { windowManager.removeView(view) }
            bubbleView = null
        }
        isAnimatingExpansion = false

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
            try {
                windowManager.addView(view, createExpandedLayoutParams())
                expandedView = view
                view.bind(state)
                view.alpha = 0f
                view.translationY = dp(EXPANDED_ENTRY_OFFSET_DP).toFloat()
                view.scaleX = 0.96f
                view.scaleY = 0.96f
                view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(EXPAND_IN_DURATION_MS)
                    .start()
                Log.i(TAG, "Expanded panel added")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to add expanded panel", t)
            }
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
        private const val TAG = "OverlayManager"
        private const val PREFS_NAME = "seamless_overlay_prefs"
        private const val KEY_BUBBLE_X = "bubble_x"
        private const val KEY_BUBBLE_Y = "bubble_y"
        private const val DEFAULT_X = 40
        private const val DEFAULT_Y = 240
        private const val TAP_SLOP_PX = 12
        private const val EXPANDED_Y = 120
        private const val PANEL_WIDTH_DP = 340
        private const val EXPANDED_ENTRY_OFFSET_DP = 18
        private const val EXPAND_OUT_DURATION_MS = 120L
        private const val EXPAND_IN_DURATION_MS = 220L
    }
}
