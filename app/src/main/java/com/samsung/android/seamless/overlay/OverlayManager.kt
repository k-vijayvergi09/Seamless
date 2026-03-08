package com.samsung.android.seamless.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.samsung.android.seamless.overlay.ui.CollapsedBubbleView
import kotlin.math.abs

class OverlayManager(
    context: Context
) {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var bubbleView: CollapsedBubbleView? = null
    private var bubbleLayoutParams: WindowManager.LayoutParams? = null

    fun render(state: OverlayUiState) {
        if (!state.overlayVisible) {
            hide()
            return
        }

        showCollapsedIfNeeded()
        bubbleView?.setRecording(state.isRecording)
    }

    fun hide() {
        bubbleView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        bubbleView = null
        bubbleLayoutParams = null
    }

    private fun showCollapsedIfNeeded() {
        if (bubbleView != null) return

        val params = createLayoutParams()
        val bubble = CollapsedBubbleView(appContext)
        bubble.setOnTouchListener(createDragTouchListener(params))

        windowManager.addView(bubble, params)
        bubbleView = bubble
        bubbleLayoutParams = params
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
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

    companion object {
        private const val PREFS_NAME = "seamless_overlay_prefs"
        private const val KEY_BUBBLE_X = "bubble_x"
        private const val KEY_BUBBLE_Y = "bubble_y"
        private const val DEFAULT_X = 40
        private const val DEFAULT_Y = 240
        private const val TAP_SLOP_PX = 12
    }
}
