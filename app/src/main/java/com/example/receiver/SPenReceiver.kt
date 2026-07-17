package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.AppProfile
import com.example.data.SPenActions
import com.example.data.SPenTriggers
import com.example.data.SPenRepository
import com.example.service.SPenAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SPenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == "com.samsung.android.service.aircommand.remotespen.action.BUTTON_PRESS" ||
            action == "com.samsung.android.intent.action.AIR_COMMAND_ACTION") {
            
            val extras = intent.extras
            if (extras != null) {
                // The SDK sends the action id from remote_spen_actions.xml
                val actionId = extras.getString("action_id") ?: ""
                Log.d("SPenReceiver", "Received S Pen Remote Action: $actionId")
                android.widget.Toast.makeText(context, "S Pen Action Detected: $actionId", android.widget.Toast.LENGTH_SHORT).show()

                val trigger = when (actionId) {
                    "flick_up" -> SPenTriggers.FLICK_UP
                    "flick_down" -> SPenTriggers.FLICK_DOWN
                    "flick_left" -> SPenTriggers.FLICK_LEFT
                    "flick_right" -> SPenTriggers.FLICK_RIGHT
                    "double_flick_up" -> SPenTriggers.DOUBLE_FLICK_UP
                    "double_flick_down" -> SPenTriggers.DOUBLE_FLICK_DOWN
                    "single_click" -> SPenTriggers.SINGLE_CLICK
                    "double_click" -> SPenTriggers.DOUBLE_CLICK
                    else -> return
                }

                // Forward to our active Accessibility Service to execute macros if possible
                val service = SPenAccessibilityService.getRunningInstance()
                if (service != null) {
                    // We dispatch it to the service's coroutine scope
                    CoroutineScope(Dispatchers.IO).launch {
                        service.handleSPenTriggerExternal(trigger)
                    }
                }
            }
        }
    }
}
