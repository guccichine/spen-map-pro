package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.content.ComponentName
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.example.data.AppDatabase
import com.example.data.AppProfile
import com.example.data.GestureMapping
import com.example.data.SPenActions
import com.example.data.SPenRepository
import com.example.data.SPenTriggers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SPenAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: SPenRepository
    private var lastActivePackage: String = AppProfile.GLOBAL_PACKAGE
    private var cachedLauncherPackage: String? = null

    private fun isLauncherPackage(packageName: String): Boolean {
        if (cachedLauncherPackage == packageName) return true
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = packageManager.resolveActivity(intent, 0)
            val launcherPkg = resolveInfo?.activityInfo?.packageName
            if (launcherPkg != null) {
                cachedLauncherPackage = launcherPkg
                return launcherPkg == packageName
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving launcher package", e)
        }
        return false
    }

    private fun getNormalizedPackage(packageName: String): String {
        return when {
            isLauncherPackage(packageName) -> "com.android.launcher"
            packageName == "com.android.settings" || packageName.endsWith(".settings") -> "com.android.settings"
            else -> packageName
        }
    }

    // Click Detection State
    private var clickCount = 0
    private var lastKeyDownTime = 0L
    private val clickTimeoutHandler = Handler(Looper.getMainLooper())
    private var isLongPressTriggered = false

    // Button Bind Setting (Allows user to bind S Pen click to a custom keycode)
    // Common S Pen click keycodes: KEYCODE_VOLUME_DOWN, KEYCODE_VOLUME_UP, KEYCODE_MEDIA_PLAY_PAUSE, KEYCODE_BUTTON_A
    private var boundKeycode: Int = 104 // 104 is commonly STYLUS_BUTTON

    companion object {
        const val TAG = "SPenService"
        
        @Volatile
        var isServiceRunning = false
            private set

        @Volatile
        var lastCapturedKeycode = -1
            private set

        @Volatile
        var lastExecutedAction = "None"
            private set

        @Volatile
        var activeForegroundApp = "global"
            private set

        private var instance: SPenAccessibilityService? = null

        fun getRunningInstance(): SPenAccessibilityService? = instance

        fun updateBoundKeycode(keycode: Int) {
            instance?.boundKeycode = keycode
        }

        fun getBoundKeycode(): Int = instance?.boundKeycode ?: 104

        fun isAccessibilityPermissionGranted(context: Context): Boolean {
            val componentName = ComponentName(context, SPenAccessibilityService::class.java)
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val service = ComponentName.unflattenFromString(colonSplitter.next())
                if (service != null && service == componentName) {
                    return true
                }
            }
            return false
        }
    }

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(applicationContext, serviceScope)
        repository = SPenRepository(db.sPenDao())
        instance = this
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        Log.d(TAG, "S-Pen Command Service connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            if (pkg != null && pkg != packageName && pkg != "android") {
                lastActivePackage = pkg
                val normalizedPkg = getNormalizedPackage(pkg)
                activeForegroundApp = normalizedPkg
                Log.d(TAG, "Foreground App Switched to: $lastActivePackage (Normalized: $normalizedPkg)")
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        instance = null
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        lastCapturedKeycode = keyCode
        
        com.example.utils.DebugLogger.log("Captured Keycode: $keyCode", "INPUT")

        val isSPenEvent = (keyCode == boundKeycode) || 
                          (keyCode == 104) || // STYLUS_BUTTON
                          (keyCode == 274) // KEYCODE_STEM_PRIMARY

        // Check if this key code matches the bound S Pen keycode
        if (isSPenEvent) {
            val action = event.action

            if (action == KeyEvent.ACTION_DOWN) {
                if (event.repeatCount == 0) {
                    lastKeyDownTime = System.currentTimeMillis()
                    isLongPressTriggered = false
                } else if (event.repeatCount > 0 && !isLongPressTriggered) {
                    val duration = System.currentTimeMillis() - lastKeyDownTime
                    if (duration > 500) { // Long Press threshold
                        isLongPressTriggered = true
                        handleSPenTrigger(SPenTriggers.LONG_PRESS)
                    }
                }
            } else if (action == KeyEvent.ACTION_UP) {
                if (!isLongPressTriggered) {
                    // Start Multi-click window
                    clickCount++
                    clickTimeoutHandler.removeCallbacksAndMessages(null)
                    clickTimeoutHandler.postDelayed({
                        val trigger = when (clickCount) {
                            1 -> SPenTriggers.SINGLE_CLICK
                            2 -> SPenTriggers.DOUBLE_CLICK
                            3 -> SPenTriggers.TRIPLE_CLICK
                            4 -> SPenTriggers.QUADRUPLE_CLICK
                            else -> null
                        }
                        clickCount = 0
                        if (trigger != null) {
                            handleSPenTrigger(trigger)
                        }
                    }, 320) // Multi-click timeout
                }
            }
            return true // Consume the key event to block original action (e.g., volume change)
        }

        // Pass-through other keys
        return super.onKeyEvent(event)
    }

    fun handleSPenTriggerExternal(trigger: String) {
        handleSPenTrigger(trigger)
    }

    private fun handleSPenTrigger(trigger: String) {
        serviceScope.launch {
            val resolvedPackage = getNormalizedPackage(lastActivePackage)
            com.example.utils.DebugLogger.log("S Pen Trigger: $trigger in app $resolvedPackage", "TRIGGER")
            
            // 1. Resolve Mapping (Per-App Profile first, then global fallback)
            var mapping = repository.getMappingSync(resolvedPackage, trigger)
            val profile = repository.getProfile(resolvedPackage)
            
            // If mapping is not defined or profile is inactive/not present, fallback to global
            if (mapping == null || mapping.actionType == SPenActions.TYPE_NONE || profile?.isActive == false) {
                mapping = repository.getMappingSync(AppProfile.GLOBAL_PACKAGE, trigger)
            }

            if (mapping != null && mapping.actionType != SPenActions.TYPE_NONE) {
                // 2. Perform Haptic Feedback
                triggerHapticFeedback(mapping.vibrationPattern, mapping.vibrationIntensity)

                // 3. Execute Macro Action
                executeMacro(mapping.actionType, mapping.actionValue)
            } else {
                Log.d(TAG, "No mapping found for $trigger in $lastActivePackage (normalized to $resolvedPackage) (and no global default)")
            }
        }
    }

    private fun triggerHapticFeedback(pattern: String, intensity: Int) {
        if (pattern == "NONE" || intensity <= 0) return

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        val duration = when (pattern) {
            "TICK" -> 20L
            "DOUBLE_TICK" -> 150L
            "HEAVY_CLICK" -> 60L
            "PULSE" -> 300L
            else -> 40L
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Map 0-100 to 1-255 amplitude
            val amplitude = ((intensity / 100.0) * 255).coerceIn(1.0, 255.0).toInt()
            
            val effect = when (pattern) {
                "DOUBLE_TICK" -> {
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 15, 100, 15),
                        intArrayOf(0, amplitude, 0, amplitude),
                        -1
                    )
                }
                "PULSE" -> {
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 100, 50, 100),
                        intArrayOf(0, amplitude, 0, amplitude),
                        -1
                    )
                }
                else -> {
                    VibrationEffect.createOneShot(duration, amplitude)
                }
            }
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    private fun executeMacro(type: String, value: String) {
        lastExecutedAction = SPenActions.getActionLabel(type, value)
        Log.d(TAG, "Executing macro: Type=$type, Value=$value")
        com.example.utils.DebugLogger.log("Executing macro: Type=$type, Value=$value", "MACRO")
        com.example.service.OverlayHelper.showExecutionOverlay(this, lastExecutedAction ?: "Action")

        if (type == "MACRO") {
            serviceScope.launch {
                val steps = value.split(";")
                for (step in steps) {
                    if (step.isEmpty()) continue
                    val parts = step.split(":", limit = 2)
                    if (parts.size < 2) continue
                    val stepType = parts[0]
                    val stepVal = parts[1]
                    Log.d(TAG, "Executing Macro Step: Type=$stepType, Value=$stepVal")

                    if (stepType == "DELAY") {
                        val delayMs = stepVal.toLongOrNull() ?: 500L
                        kotlinx.coroutines.delay(delayMs)
                    } else if (stepType == "TORCH") {
                        executeTorchAction(stepVal)
                    } else if (stepType == "SOUND_MODE") {
                        executeSoundModeAction(stepVal)
                    } else {
                        // Map internal type names to SPenActions type constants if needed
                        val resolvedType = when (stepType) {
                            "SYSTEM" -> SPenActions.TYPE_SYSTEM
                            "MEDIA" -> SPenActions.TYPE_MEDIA
                            "GESTURE" -> SPenActions.TYPE_GESTURE
                            "LAUNCH_APP" -> SPenActions.TYPE_LAUNCH_APP
                            else -> stepType
                        }
                        executeIndividualStep(resolvedType, stepVal)
                    }
                }
            }
        } else {
            executeIndividualStep(type, value)
        }
    }

    private fun executeTorchAction(value: String) {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraManager != null && cameraId != null) {
                val turnOn = when (value) {
                    "ENABLE" -> true
                    "DISABLE" -> false
                    else -> true
                }
                cameraManager.setTorchMode(cameraId, turnOn)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle torch: ${e.message}")
        }
    }

    private fun executeSoundModeAction(value: String) {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                when (value) {
                    "SOUND" -> audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    "VIBRATE" -> audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    "SILENT" -> audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    "CYCLE" -> {
                        val current = audioManager.ringerMode
                        audioManager.ringerMode = when (current) {
                            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                            else -> AudioManager.RINGER_MODE_NORMAL
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set sound mode: ${e.message}")
        }
    }

    private fun executeIndividualStep(type: String, value: String) {
        when (type) {
            SPenActions.TYPE_SYSTEM -> {
                val actionId = when (value) {
                    SPenActions.VAL_SYSTEM_BACK -> GLOBAL_ACTION_BACK
                    SPenActions.VAL_SYSTEM_HOME -> GLOBAL_ACTION_HOME
                    SPenActions.VAL_SYSTEM_RECENTS -> GLOBAL_ACTION_RECENTS
                    SPenActions.VAL_SYSTEM_NOTIFICATIONS -> GLOBAL_ACTION_NOTIFICATIONS
                    SPenActions.VAL_SYSTEM_QUICK_SETTINGS -> GLOBAL_ACTION_QUICK_SETTINGS
                    SPenActions.VAL_SYSTEM_SPLIT_SCREEN -> GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN
                    SPenActions.VAL_SYSTEM_LOCK_SCREEN -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            GLOBAL_ACTION_LOCK_SCREEN
                        } else {
                            -1
                        }
                    }
                    else -> -1
                }
                if (actionId != -1) {
                    performGlobalAction(actionId)
                }
            }
            SPenActions.TYPE_MEDIA -> {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
                when (value) {
                    SPenActions.VAL_MEDIA_PLAY_PAUSE -> {
                        dispatchKeyEvents(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    }
                    SPenActions.VAL_MEDIA_NEXT -> {
                        dispatchKeyEvents(KeyEvent.KEYCODE_MEDIA_NEXT)
                    }
                    SPenActions.VAL_MEDIA_PREVIOUS -> {
                        dispatchKeyEvents(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                    }
                    SPenActions.VAL_MEDIA_VOLUME_UP -> {
                        audioManager.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE,
                            AudioManager.FLAG_SHOW_UI
                        )
                    }
                    SPenActions.VAL_MEDIA_VOLUME_DOWN -> {
                        audioManager.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER,
                            AudioManager.FLAG_SHOW_UI
                        )
                    }
                }
            }
            SPenActions.TYPE_GESTURE -> {
                simulateScreenGesture(value)
            }
            SPenActions.TYPE_LAUNCH_APP -> {
                try {
                    val launchIntent = packageManager.getLaunchIntentForPackage(value)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(launchIntent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error launching app: $value", e)
                }
            }
            SPenActions.TYPE_TEXT -> {
                // Can type custom text using a simulated keyboard paste
                Log.d(TAG, "Simulating text insertion: $value")
            }
        }
    }

    private fun dispatchKeyEvents(keycode: Int) {
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keycode)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, keycode)
        
        // Simulating standard hardware click behavior
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.dispatchMediaKeyEvent(downEvent)
        audioManager?.dispatchMediaKeyEvent(upEvent)
    }

    private fun simulateScreenGesture(gesture: String) {
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        val path = Path()
        var duration = 250L

        when (gesture) {
            SPenActions.VAL_GESTURE_SWIPE_UP -> {
                // Swipe UP (from 75% height to 25% height)
                path.moveTo(width / 2f, height * 0.75f)
                path.lineTo(width / 2f, height * 0.25f)
            }
            SPenActions.VAL_GESTURE_SWIPE_DOWN -> {
                // Swipe DOWN (from 25% height to 75% height)
                path.moveTo(width / 2f, height * 0.25f)
                path.lineTo(width / 2f, height * 0.75f)
            }
            SPenActions.VAL_GESTURE_SWIPE_LEFT -> {
                // Swipe LEFT (from 80% width to 20% width)
                path.moveTo(width * 0.8f, height / 2f)
                path.lineTo(width * 0.2f, height / 2f)
            }
            SPenActions.VAL_GESTURE_SWIPE_RIGHT -> {
                // Swipe RIGHT (from 20% width to 80% width)
                path.moveTo(width * 0.2f, height / 2f)
                path.lineTo(width * 0.8f, height / 2f)
            }
            SPenActions.VAL_GESTURE_DOUBLE_TAP -> {
                // Simulate Double Tap on the center of the screen
                val tapPath = Path()
                tapPath.moveTo(width / 2f, height / 2f)
                val stroke1 = GestureDescription.StrokeDescription(tapPath, 0, 50)
                val stroke2 = GestureDescription.StrokeDescription(tapPath, 150, 50)
                val doubleTapGesture = GestureDescription.Builder()
                    .addStroke(stroke1)
                    .addStroke(stroke2)
                    .build()
                dispatchGesture(doubleTapGesture, null, null)
                return
            }
            else -> return
        }

        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        val gestureDescription = GestureDescription.Builder()
            .addStroke(stroke)
            .build()

        dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.d(TAG, "Swipe gesture completed successfully")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.w(TAG, "Swipe gesture cancelled")
            }
        }, null)
    }
}
