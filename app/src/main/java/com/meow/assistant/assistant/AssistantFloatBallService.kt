package com.meow.assistant.assistant

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.content.edit
import com.meow.assistant.R
import kotlin.math.abs
import kotlin.math.roundToInt

class AssistantFloatBallService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var preferences: SharedPreferences
    private var ball: ImageView? = null
    private var params: WindowManager.LayoutParams? = null
    private var downRawX = 0f
    private var downRawY = 0f
    private var dragging = false
    private var dragThreshold = 8f

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == AssistantConfig.KEY_MODE ||
            key == AssistantConfig.KEY_FLOAT_BALL_SIZE ||
            key == AssistantConfig.KEY_FLOAT_BALL_ALPHA ||
            key == AssistantConfig.KEY_FLOAT_BALL_X ||
            key == AssistantConfig.KEY_FLOAT_BALL_Y
        ) {
            refreshFromConfig()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        preferences = getSharedPreferences(AssistantConfig.PREFS_NAME, MODE_PRIVATE)
        if (AssistantConfig.load(this).processingMode != ProcessingMode.FLOATING) {
            stopSelf()
            return
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        dragThreshold = 8f * resources.displayMetrics.density
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        addBall(AssistantConfig.load(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        refreshFromConfig()
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshFromConfig()
    }

    override fun onDestroy() {
        if (::preferences.isInitialized) {
            preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        }
        ball?.let { runCatching { windowManager.removeView(it) } }
        ball = null
        super.onDestroy()
    }

    private fun addBall(config: AssistantConfig) {
        val size = dpToPx(config.floatBallSize)
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            val padding = (size * 0.2f).toInt()
            setPadding(padding, padding, padding, padding)
            setImageResource(R.drawable.ic_cat_paw)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(220, 96, 96, 96))
                setStroke(dpToPx(1), Color.argb(150, 230, 230, 230))
            }
            clipToOutline = true
            contentDescription = getString(R.string.assistant_float_ball_content_description)
            alpha = config.floatBallAlpha
            setOnTouchListener { view, event -> handleTouch(view, event) }
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        params = WindowManager.LayoutParams(
            size,
            size,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (config.floatBallX >= 0) config.floatBallX else dpToPx(16)
            y = if (config.floatBallY >= 0) config.floatBallY else dpToPx(180)
            clampPosition(this, size)
        }
        ball = image
        runCatching { windowManager.addView(image, params) }.onFailure { stopSelf() }
    }

    private fun refreshFromConfig() {
        if (!::windowManager.isInitialized) return
        val config = AssistantConfig.load(this)
        if (config.processingMode != ProcessingMode.FLOATING) {
            stopSelf()
            return
        }
        val image = ball ?: return
        val layout = params ?: return
        val size = dpToPx(config.floatBallSize)
        val previousX = layout.x
        val previousY = layout.y
        layout.width = size
        layout.height = size
        clampPosition(layout, size)
        image.alpha = config.floatBallAlpha
        val padding = (size * 0.2f).toInt()
        image.setPadding(padding, padding, padding, padding)
        runCatching { windowManager.updateViewLayout(image, layout) }
        if (layout.x != previousX || layout.y != previousY) savePosition(layout)
    }

    private fun handleTouch(view: View, event: MotionEvent): Boolean {
        val layout = params ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                dragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                if (abs(event.rawX - downRawX) > dragThreshold || abs(event.rawY - downRawY) > dragThreshold) {
                    dragging = true
                }
                if (dragging) {
                    layout.x += dx.toInt()
                    layout.y += dy.toInt()
                    clampPosition(layout, view.width)
                    runCatching { windowManager.updateViewLayout(view, layout) }
                    downRawX = event.rawX
                    downRawY = event.rawY
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!dragging) {
                    AssistantAccessibilityService.requestFloatingReplacement()
                } else {
                    savePosition(layout)
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                if (dragging) savePosition(layout)
                return true
            }
        }
        return false
    }

    private fun savePosition(layout: WindowManager.LayoutParams) {
        AssistantConfig.load(this).copy(
            floatBallX = layout.x,
            floatBallY = layout.y,
        ).save(this)
    }

    private fun clampPosition(layout: WindowManager.LayoutParams, size: Int) {
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        layout.x = layout.x.coerceIn(0, (width - size).coerceAtLeast(0))
        layout.y = layout.y.coerceIn(0, (height - size).coerceAtLeast(0))
    }

    private fun dpToPx(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
}
