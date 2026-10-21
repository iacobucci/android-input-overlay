package com.inputoverlay.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.inputoverlay.R
import com.inputoverlay.model.PresetData
import com.inputoverlay.model.SymbolCategory
import com.inputoverlay.ui.MainActivity
import com.inputoverlay.ui.OverlayViewHolder
import com.inputoverlay.utils.OverlayPreferences
import com.inputoverlay.utils.TextInjector

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: OverlayPreferences

    private var overlayViewHolder: OverlayViewHolder? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var currentCategory: SymbolCategory = PresetData.ITALIAN_ACCENTS
    private var isUppercase: Boolean = false
    private var isVisible: Boolean = false

    val isOverlayVisible: Boolean
        get() = isVisible

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = OverlayPreferences(this)
        isUppercase = prefs.isDefaultUppercase
        currentCategory = PresetData.findCategoryById(prefs.defaultPresetId)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_SHOW

        when {
            action.equals(ACTION_SHOW, ignoreCase = true) || action.equals(ACTION_SHOW_SHORT, ignoreCase = true) -> {
                parseIntentExtras(intent)
                showOverlay()
            }
            action.equals(ACTION_HIDE, ignoreCase = true) || action.equals(ACTION_HIDE_SHORT, ignoreCase = true) -> {
                hideOverlay()
            }
            action.equals(ACTION_TOGGLE, ignoreCase = true) || action.equals(ACTION_TOGGLE_SHORT, ignoreCase = true) -> {
                if (isVisible) {
                    hideOverlay()
                } else {
                    parseIntentExtras(intent)
                    showOverlay()
                }
            }
            else -> {
                parseIntentExtras(intent)
                showOverlay()
            }
        }

        return START_NOT_STICKY
    }

    private fun parseIntentExtras(intent: Intent?) {
        if (intent == null) return

        // Custom characters extra
        val customChars = intent.getStringExtra(EXTRA_CHARACTERS)
        if (!customChars.isNullOrBlank()) {
            val items = PresetData.parseCustomCharacters(customChars)
            currentCategory = SymbolCategory("custom", "Custom", "★", items)
        } else {
            // Category extra
            val categoryExtra = intent.getStringExtra(EXTRA_CATEGORY)
            if (!categoryExtra.isNullOrBlank()) {
                currentCategory = PresetData.findCategoryById(categoryExtra)
            }
        }

        // Case extra
        val caseExtra = intent.getStringExtra(EXTRA_CASE)
        if (caseExtra != null) {
            when (caseExtra.lowercase()) {
                "upper", "uppercase" -> isUppercase = true
                "lower", "lowercase" -> isUppercase = false
                "toggle" -> isUppercase = !isUppercase
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Log.e(TAG, "Cannot draw overlay: SYSTEM_ALERT_WINDOW permission not granted")
            return
        }

        if (isVisible && overlayViewHolder != null) {
            bindOverlayView()
            return
        }

        startForeground(NOTIFICATION_ID, buildNotification())

        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        // FLAG_NOT_FOCUSABLE keeps focus on the underlying app's active text field
        // while InputOverlayAccessibilityService handles physical keyboard shortcuts (Esc, Q..P)
        val windowFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val clampedX = if (prefs.overlayPosX in 0..(screenWidth - 100)) {
            prefs.overlayPosX
        } else {
            ((screenWidth - 600) / 2).coerceAtLeast(20)
        }

        val clampedY = if (prefs.overlayPosY in 50..(screenHeight - 100)) {
            prefs.overlayPosY
        } else {
            (screenHeight - 280).coerceAtLeast(100)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            windowFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = clampedX
            y = clampedY
        }
        layoutParams = params

        overlayViewHolder = OverlayViewHolder(this, prefs, windowManager, params)
        bindOverlayView()

        // Outside touch listener to auto-dismiss when tapping outside overlay
        overlayViewHolder?.rootView?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                hideOverlay()
                true
            } else {
                false
            }
        }

        try {
            windowManager.addView(overlayViewHolder?.rootView, params)
            isVisible = true
            overlayViewHolder?.rootView?.requestFocus()
            Log.d(TAG, "Overlay added to window manager at (${params.x}, ${params.y})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view", e)
        }
    }

    private fun bindOverlayView() {
        overlayViewHolder?.bind(
            category = currentCategory,
            isUppercase = isUppercase,
            onCharacterSelected = { char ->
                TextInjector.injectDirectly(this, char, currentCategory.id, prefs)
                if (prefs.isAutoDismissEnabled) {
                    hideOverlay()
                }
            },
            onShiftToggled = { newShift ->
                isUppercase = newShift
                bindOverlayView()
            },
            onCategorySwitchRequested = {
                cycleNextPreset()
                bindOverlayView()
            },
            onCloseRequested = {
                hideOverlay()
            }
        )
    }

    fun selectCharacterByIndex(index: Int, forceUppercase: Boolean? = null) {
        if (index in currentCategory.items.indices) {
            val uppercase = forceUppercase ?: isUppercase
            val char = currentCategory.items[index].getDisplay(uppercase)
            TextInjector.injectDirectly(this, char, currentCategory.id, prefs)
            if (prefs.isAutoDismissEnabled) {
                hideOverlay()
            }
        }
    }

    fun toggleShift() {
        isUppercase = !isUppercase
        bindOverlayView()
    }

    private fun cycleNextPreset() {
        val presets = PresetData.ALL_PRESETS
        val currentIndex = presets.indexOfFirst { it.id == currentCategory.id }
        val nextIndex = if (currentIndex != -1) (currentIndex + 1) % presets.size else 0
        currentCategory = presets[nextIndex]
    }

    fun hideOverlay() {
        if (isVisible && overlayViewHolder != null) {
            try {
                windowManager.removeView(overlayViewHolder?.rootView)
                Log.d(TAG, "Overlay removed from window manager")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view", e)
            } finally {
                overlayViewHolder = null
                isVisible = false
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Overlay Active Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifies when accent overlay is floating on screen"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val hideIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, OverlayService::class.java).apply { action = ACTION_HIDE },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Input Overlay")
            .setContentText("Accent overlay active on screen")
            .setSmallIcon(R.drawable.ic_keyboard)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_close, "Hide", hideIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        hideOverlay()
        if (instance == this) {
            instance = null
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "OverlayService"
        private const val CHANNEL_ID = "overlay_service_channel"
        private const val NOTIFICATION_ID = 1001

        @Volatile
        var instance: OverlayService? = null
            private set

        const val ACTION_SHOW = "com.inputoverlay.action.SHOW_OVERLAY"
        const val ACTION_SHOW_SHORT = "com.inputoverlay.SHOW"
        const val ACTION_HIDE = "com.inputoverlay.action.HIDE_OVERLAY"
        const val ACTION_HIDE_SHORT = "com.inputoverlay.HIDE"
        const val ACTION_TOGGLE = "com.inputoverlay.action.TOGGLE_OVERLAY"
        const val ACTION_TOGGLE_SHORT = "com.inputoverlay.TOGGLE"

        const val EXTRA_CATEGORY = "category"
        const val EXTRA_CHARACTERS = "characters"
        const val EXTRA_CASE = "case"
        const val EXTRA_AUTO_DISMISS = "auto_dismiss"
        const val EXTRA_POSITION = "position"
    }
}
