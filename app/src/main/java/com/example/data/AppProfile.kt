package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_profiles"
)
data class AppProfile(
    @PrimaryKey val packageName: String, // "global" or e.g. "com.google.android.youtube"
    val appName: String,
    val isActive: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    companion object {
        const val GLOBAL_PACKAGE = "global"
    }
}

@Entity(
    tableName = "gesture_mappings",
    foreignKeys = [
        ForeignKey(
            entity = AppProfile::class,
            parentColumns = ["packageName"],
            childColumns = ["profilePackageName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profilePackageName", "triggerType"], unique = true)]
)
data class GestureMapping(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profilePackageName: String,
    val triggerType: String, // "SINGLE_CLICK", "DOUBLE_CLICK", "TRIPLE_CLICK", "QUADRUPLE_CLICK", "LONG_PRESS", "FLICK_UP", "FLICK_DOWN", "FLICK_LEFT", "FLICK_RIGHT", "CIRCLE_CW", "CIRCLE_CCW"
    val actionType: String, // "NONE", "SYSTEM", "MEDIA", "GESTURE", "LAUNCH_APP", "TEXT"
    val actionValue: String, // e.g. "BACK", "PLAY_PAUSE", "SWIPE_UP", "com.android.chrome", or a custom text
    val vibrationPattern: String = "TICK", // "NONE", "TICK", "DOUBLE_TICK", "HEAVY_CLICK", "PULSE"
    val vibrationIntensity: Int = 50 // 0 to 100
)

// Constants and Helpers
object SPenTriggers {
    const val SINGLE_CLICK = "SINGLE_CLICK"
    const val DOUBLE_CLICK = "DOUBLE_CLICK"
    const val TRIPLE_CLICK = "TRIPLE_CLICK"
    const val QUADRUPLE_CLICK = "QUADRUPLE_CLICK"
    const val LONG_PRESS = "LONG_PRESS"
    const val FLICK_UP = "FLICK_UP"
    const val FLICK_DOWN = "FLICK_DOWN"
    const val FLICK_LEFT = "FLICK_LEFT"
    const val FLICK_RIGHT = "FLICK_RIGHT"
    const val DIAGONAL_TOP_LEFT = "DIAGONAL_TOP_LEFT"
    const val DIAGONAL_TOP_RIGHT = "DIAGONAL_TOP_RIGHT"
    const val DIAGONAL_BOTTOM_LEFT = "DIAGONAL_BOTTOM_LEFT"
    const val DIAGONAL_BOTTOM_RIGHT = "DIAGONAL_BOTTOM_RIGHT"
    const val POINTY_LEFT = "POINTY_LEFT"
    const val POINTY_RIGHT = "POINTY_RIGHT"
    const val POINTY_UP = "POINTY_UP"
    const val POINTY_DOWN = "POINTY_DOWN"
    const val TRIANGLE_CW = "TRIANGLE_CW"
    const val TRIANGLE_CCW = "TRIANGLE_CCW"
    const val CIRCLE_CW = "CIRCLE_CW"
    const val CIRCLE_CCW = "CIRCLE_CCW"
    const val SHAKE = "SHAKE"
    const val SQUARE_CW = "SQUARE_CW"
    const val SQUARE_CCW = "SQUARE_CCW"
    const val DOUBLE_FLICK_UP = "DOUBLE_FLICK_UP"
    const val DOUBLE_FLICK_DOWN = "DOUBLE_FLICK_DOWN"
    const val DOUBLE_FLICK_LEFT = "DOUBLE_FLICK_LEFT"
    const val DOUBLE_FLICK_RIGHT = "DOUBLE_FLICK_RIGHT"
    const val ZIGZAG = "ZIGZAG"
    const val INFINITY = "INFINITY"
    const val TAP_SCREEN = "TAP_SCREEN"
    const val DOUBLE_TAP_SCREEN = "DOUBLE_TAP_SCREEN"
    const val SWIPE_SCREEN = "SWIPE_SCREEN"

    val ALL = listOf(
        SINGLE_CLICK,
        DOUBLE_CLICK,
        TRIPLE_CLICK,
        QUADRUPLE_CLICK,
        LONG_PRESS,
        FLICK_UP,
        FLICK_DOWN,
        FLICK_LEFT,
        FLICK_RIGHT,
        DOUBLE_FLICK_UP,
        DOUBLE_FLICK_DOWN,
        DOUBLE_FLICK_LEFT,
        DOUBLE_FLICK_RIGHT,
        DIAGONAL_TOP_LEFT,
        DIAGONAL_TOP_RIGHT,
        DIAGONAL_BOTTOM_LEFT,
        DIAGONAL_BOTTOM_RIGHT,
        POINTY_LEFT,
        POINTY_RIGHT,
        POINTY_UP,
        POINTY_DOWN,
        TRIANGLE_CW,
        TRIANGLE_CCW,
        CIRCLE_CW,
        CIRCLE_CCW,
        SQUARE_CW,
        SQUARE_CCW,
        ZIGZAG,
        INFINITY,
        SHAKE
    )

    fun getLabel(trigger: String): String = when (trigger) {
        TAP_SCREEN -> "Tap on Screen"
        DOUBLE_TAP_SCREEN -> "Double Tap on Screen"
        SWIPE_SCREEN -> "Swipe on Screen"
        SINGLE_CLICK -> "Click"
        DOUBLE_CLICK -> "Double Click"
        TRIPLE_CLICK -> "Triple Click"
        QUADRUPLE_CLICK -> "Quadruple Click"
        LONG_PRESS -> "Long Click"
        FLICK_UP -> "Up"
        FLICK_DOWN -> "Down"
        FLICK_LEFT -> "Left"
        FLICK_RIGHT -> "Right"
        DOUBLE_FLICK_UP -> "Double Up"
        DOUBLE_FLICK_DOWN -> "Double Down"
        DOUBLE_FLICK_LEFT -> "Double Left"
        DOUBLE_FLICK_RIGHT -> "Double Right"
        DIAGONAL_TOP_LEFT -> "Diagonal Top Left"
        DIAGONAL_TOP_RIGHT -> "Diagonal Top Right"
        DIAGONAL_BOTTOM_LEFT -> "Diagonal Bottom Left"
        DIAGONAL_BOTTOM_RIGHT -> "Diagonal Bottom Right"
        POINTY_LEFT -> "Pointy Left"
        POINTY_RIGHT -> "Pointy Right"
        POINTY_UP -> "Pointy Up"
        POINTY_DOWN -> "Pointy Down"
        TRIANGLE_CW -> "Triangle Clockwise"
        TRIANGLE_CCW -> "Triangle Counter Clockwise"
        CIRCLE_CW -> "Circle Clockwise"
        CIRCLE_CCW -> "Circle Counter-Clockwise"
        SQUARE_CW -> "Square Clockwise"
        SQUARE_CCW -> "Square Counter-Clockwise"
        ZIGZAG -> "Zigzag"
        INFINITY -> "Infinity Loop"
        SHAKE -> "Shake"
        else -> trigger
    }
}

object SPenActions {
    // Action types
    const val TYPE_NONE = "NONE"
    const val TYPE_SYSTEM = "SYSTEM"
    const val TYPE_MEDIA = "MEDIA"
    const val TYPE_GESTURE = "GESTURE"
    const val TYPE_LAUNCH_APP = "LAUNCH_APP"
    const val TYPE_TEXT = "TEXT"
    const val TYPE_LAST_APP = "LAST_APP"
    const val TYPE_AUTO_ROTATE = "AUTO_ROTATE"
    const val TYPE_SOUND_MODE = "SOUND_MODE"
    const val TYPE_DND = "DND"
    const val TYPE_TORCH = "TORCH"
    const val TYPE_ASSISTANT = "ASSISTANT"
    const val TYPE_TOUCH_SEQUENCE = "TOUCH_SEQUENCE"
    const val TYPE_MACRO = "MACRO"

    // Action values for SYSTEM
    const val VAL_SYSTEM_BACK = "BACK"
    const val VAL_SYSTEM_HOME = "HOME"
    const val VAL_SYSTEM_RECENTS = "RECENTS"
    const val VAL_SYSTEM_NOTIFICATIONS = "NOTIFICATIONS"
    const val VAL_SYSTEM_QUICK_SETTINGS = "QUICK_SETTINGS"
    const val VAL_SYSTEM_POWER_DIALOG = "POWER_DIALOG"
    const val VAL_SYSTEM_LOCK_SCREEN = "LOCK_SCREEN"
    const val VAL_SYSTEM_SCREENSHOT = "SCREENSHOT"
    const val VAL_SYSTEM_SPLIT_SCREEN = "SPLIT_SCREEN"

    // Action values for MEDIA
    const val VAL_MEDIA_PLAY_PAUSE = "PLAY_PAUSE"
    const val VAL_MEDIA_TOGGLE = "TOGGLE"
    const val VAL_MEDIA_NEXT = "NEXT"
    const val VAL_MEDIA_PREVIOUS = "PREVIOUS"
    const val VAL_MEDIA_VOLUME_DOWN = "VOLUME_DOWN"
    const val VAL_MEDIA_VOLUME_UP = "VOLUME_UP"

    // Action values for GESTURE
    const val VAL_GESTURE_SWIPE_UP = "SWIPE_UP"
    const val VAL_GESTURE_SWIPE_DOWN = "SWIPE_DOWN"
    const val VAL_GESTURE_SWIPE_LEFT = "SWIPE_LEFT"
    const val VAL_GESTURE_SWIPE_RIGHT = "SWIPE_RIGHT"
    const val VAL_GESTURE_DOUBLE_TAP = "DOUBLE_TAP"

    // Values for Auto Rotate
    const val VAL_ROTATION_ENABLE = "ENABLE"
    const val VAL_ROTATION_DISABLE = "DISABLE"
    const val VAL_ROTATION_TOGGLE = "TOGGLE"

    // Values for Sound Mode
    const val VAL_SOUND_ON = "SOUND"
    const val VAL_SOUND_VIBRATE = "VIBRATE"
    const val VAL_SOUND_SILENT = "SILENT"
    const val VAL_SOUND_CYCLE = "CYCLE"

    // Values for Do Not Disturb
    const val VAL_DND_ENABLE = "ENABLE"
    const val VAL_DND_DISABLE = "DISABLE"
    const val VAL_DND_TOGGLE = "TOGGLE"

    // Values for Torch
    const val VAL_TORCH_ENABLE = "ENABLE"
    const val VAL_TORCH_DISABLE = "DISABLE"
    const val VAL_TORCH_TOGGLE = "TOGGLE"

    // Values for Touch Sequence
    const val VAL_TOUCH_TAP = "TAP"
    const val VAL_TOUCH_SWIPE = "SWIPE"
    const val VAL_TOUCH_COMBINATION = "COMBINATION"

    val ACTIONS_MAP = mapOf(
        TYPE_NONE to listOf("Do Nothing"),
        TYPE_LAST_APP to listOf("LAST_APP"),
        TYPE_SYSTEM to listOf(
            VAL_SYSTEM_BACK,
            VAL_SYSTEM_HOME,
            VAL_SYSTEM_RECENTS,
            VAL_SYSTEM_NOTIFICATIONS,
            VAL_SYSTEM_QUICK_SETTINGS,
            VAL_SYSTEM_POWER_DIALOG,
            VAL_SYSTEM_LOCK_SCREEN,
            VAL_SYSTEM_SCREENSHOT,
            VAL_SYSTEM_SPLIT_SCREEN
        ),
        TYPE_MEDIA to listOf(
            VAL_MEDIA_PLAY_PAUSE,
            VAL_MEDIA_TOGGLE,
            VAL_MEDIA_NEXT,
            VAL_MEDIA_PREVIOUS,
            VAL_MEDIA_VOLUME_DOWN,
            VAL_MEDIA_VOLUME_UP
        ),
        TYPE_GESTURE to listOf(
            VAL_GESTURE_SWIPE_UP,
            VAL_GESTURE_SWIPE_DOWN,
            VAL_GESTURE_SWIPE_LEFT,
            VAL_GESTURE_SWIPE_RIGHT,
            VAL_GESTURE_DOUBLE_TAP
        ),
        TYPE_AUTO_ROTATE to listOf(
            VAL_ROTATION_ENABLE,
            VAL_ROTATION_DISABLE,
            VAL_ROTATION_TOGGLE
        ),
        TYPE_SOUND_MODE to listOf(
            VAL_SOUND_ON,
            VAL_SOUND_VIBRATE,
            VAL_SOUND_SILENT,
            VAL_SOUND_CYCLE
        ),
        TYPE_DND to listOf(
            VAL_DND_ENABLE,
            VAL_DND_DISABLE,
            VAL_DND_TOGGLE
        ),
        TYPE_TORCH to listOf(
            VAL_TORCH_ENABLE,
            VAL_TORCH_DISABLE,
            VAL_TORCH_TOGGLE
        ),
        TYPE_ASSISTANT to listOf("LAUNCH"),
        TYPE_TOUCH_SEQUENCE to listOf(
            VAL_TOUCH_TAP,
            VAL_TOUCH_SWIPE,
            VAL_TOUCH_COMBINATION
        )
    )

    fun getActionLabel(type: String, value: String): String = when (type) {
        TYPE_NONE -> "No Action"
        TYPE_LAST_APP -> "Switch to Last App"
        TYPE_SYSTEM -> when (value) {
            VAL_SYSTEM_BACK -> "System Back"
            VAL_SYSTEM_HOME -> "System Home"
            VAL_SYSTEM_RECENTS -> "Recent Apps"
            VAL_SYSTEM_NOTIFICATIONS -> "Open Notifications"
            VAL_SYSTEM_QUICK_SETTINGS -> "Open Quick Settings"
            VAL_SYSTEM_POWER_DIALOG -> "Show Power Dialog"
            VAL_SYSTEM_LOCK_SCREEN -> "Lock Screen"
            VAL_SYSTEM_SCREENSHOT -> "Take Screenshot"
            VAL_SYSTEM_SPLIT_SCREEN -> "Toggle Split Screen"
            else -> "System: $value"
        }
        TYPE_MEDIA -> when (value) {
            VAL_MEDIA_PLAY_PAUSE -> "Play / Pause"
            VAL_MEDIA_TOGGLE -> "Toggle Playback State"
            VAL_MEDIA_NEXT -> "Next Track"
            VAL_MEDIA_PREVIOUS -> "Previous Track"
            VAL_MEDIA_VOLUME_DOWN -> "Lower Volume"
            VAL_MEDIA_VOLUME_UP -> "Raise Volume"
            else -> "Media: $value"
        }
        TYPE_GESTURE -> when (value) {
            VAL_GESTURE_SWIPE_UP -> "Simulate Swipe Up"
            VAL_GESTURE_SWIPE_DOWN -> "Simulate Swipe Down"
            VAL_GESTURE_SWIPE_LEFT -> "Simulate Swipe Left"
            VAL_GESTURE_SWIPE_RIGHT -> "Simulate Swipe Right"
            VAL_GESTURE_DOUBLE_TAP -> "Simulate Double Tap"
            else -> "Gesture: $value"
        }
        TYPE_LAUNCH_APP -> "Open App: $value"
        TYPE_TEXT -> "Insert Text: \"$value\""
        TYPE_AUTO_ROTATE -> when (value) {
            VAL_ROTATION_ENABLE -> "Auto Rotate: Enable"
            VAL_ROTATION_DISABLE -> "Auto Rotate: Disable"
            VAL_ROTATION_TOGGLE -> "Auto Rotate: Toggle"
            else -> "Auto Rotate: $value"
        }
        TYPE_SOUND_MODE -> when (value) {
            VAL_SOUND_ON -> "Sound Mode: Sound"
            VAL_SOUND_VIBRATE -> "Sound Mode: Vibrate"
            VAL_SOUND_SILENT -> "Sound Mode: Silent"
            VAL_SOUND_CYCLE -> "Sound Mode: Cycle All"
            else -> "Sound Mode: $value"
        }
        TYPE_DND -> when (value) {
            VAL_DND_ENABLE -> "Do Not Disturb: Enable"
            VAL_DND_DISABLE -> "Do Not Disturb: Disable"
            VAL_DND_TOGGLE -> "Do Not Disturb: Toggle"
            else -> "Do Not Disturb: $value"
        }
        TYPE_TORCH -> when (value) {
            VAL_TORCH_ENABLE -> "Torch: Enable"
            VAL_TORCH_DISABLE -> "Torch: Disable"
            VAL_TORCH_TOGGLE -> "Torch: Toggle"
            else -> "Torch: $value"
        }
        TYPE_ASSISTANT -> "Trigger Digital Assistant"
        TYPE_TOUCH_SEQUENCE -> when (value) {
            VAL_TOUCH_TAP -> "Touch Sequence: Tap"
            VAL_TOUCH_SWIPE -> "Touch Sequence: Swipe"
            VAL_TOUCH_COMBINATION -> "Touch Sequence: Combination"
            else -> "Touch Sequence: $value"
        }
        TYPE_MACRO -> "Deploy Macro: Sequence [${value.replace(";", " ➔ ")}]"
        else -> "Unknown Action"
    }
}
