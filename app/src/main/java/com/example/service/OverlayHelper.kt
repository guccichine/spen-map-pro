package com.example.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

object OverlayHelper {
    private var overlayView: View? = null

    fun showExecutionOverlay(context: Context, actionName: String) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        
        Handler(Looper.getMainLooper()).post {
            try {
                if (overlayView != null) {
                    windowManager.removeView(overlayView)
                    overlayView = null
                }
                
                val container = FrameLayout(context)
                
                val pill = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    
                    val drawable = GradientDrawable().apply {
                        setColor(Color.parseColor("#E61E2128"))
                        cornerRadius = 50f
                        setStroke(3, Color.parseColor("#00E5FF"))
                    }
                    background = drawable
                    setPadding(40, 20, 40, 20)
                }

                val textView = TextView(context).apply {
                    text = "✓ $actionName"
                    setTextColor(Color.WHITE)
                    textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                
                pill.addView(textView)
                
                val params = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    topMargin = 150
                }
                
                container.addView(pill, params)
                
                val wmParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                }
                
                windowManager.addView(container, wmParams)
                overlayView = container
                
                // Animation
                pill.alpha = 0f
                pill.translationY = -50f
                pill.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(250)
                    .withEndAction {
                        pill.postDelayed({
                            pill.animate()
                                .alpha(0f)
                                .translationY(-50f)
                                .setDuration(250)
                                .withEndAction {
                                    try {
                                        if (overlayView == container) {
                                            windowManager.removeView(container)
                                            overlayView = null
                                        }
                                    } catch(e: Exception) {}
                                }
                                .start()
                        }, 1200)
                    }
                    .start()
                    
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
