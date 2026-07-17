package com.example.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogger {
    data class LogEntry(val timestamp: Long, val message: String, val level: String) {
        val timeString: String
            get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
    }

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    fun log(message: String, level: String = "INFO") {
        val entry = LogEntry(System.currentTimeMillis(), message, level)
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(0, entry)
        if (currentLogs.size > 100) {
            currentLogs.removeLast()
        }
        _logs.value = currentLogs
    }
    
    fun clear() {
        _logs.value = emptyList()
    }
}
