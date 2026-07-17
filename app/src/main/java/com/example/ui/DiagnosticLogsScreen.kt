package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.service.AdbManager
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticLogsScreen(
    adbManager: AdbManager,
    modifier: Modifier = Modifier
) {
    var isStreaming by remember { mutableStateOf(false) }
    val logs = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    var logJob by remember { mutableStateOf<Job?>(null) }
    
    // Auto-scroll to bottom
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            logJob?.cancel()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FiberglassBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Logcat Diagnostics",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { logs.clear() }) {
                    Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = Color.Gray)
                }
                if (isStreaming) {
                    IconButton(onClick = { 
                        isStreaming = false
                        logJob?.cancel()
                    }) {
                        Icon(Icons.Rounded.Stop, contentDescription = "Stop", tint = ChromeRed)
                    }
                } else {
                    IconButton(onClick = { 
                        isStreaming = true
                        logJob = scope.launch(Dispatchers.IO) {
                            adbManager.getLogcatFlow()
                                .catch { e -> logs.add("Error: ${e.message}") }
                                .onCompletion { logs.add("Logcat stream ended.") }
                                .collect { line ->
                                    withContext(Dispatchers.Main) {
                                        logs.add(line)
                                        if (logs.size > 2000) {
                                            logs.removeRange(0, 500)
                                        }
                                    }
                                }
                        }
                    }) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Start", tint = Color.Green)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = FiberglassCard)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                items(logs) { log ->
                    val color = when {
                        log.contains(" E ") || log.contains("Error") -> ChromeRed
                        log.contains(" W ") || log.contains("Warning") -> Color.Yellow
                        else -> Color.LightGray
                    }
                    Text(
                        text = log,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = color,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                if (logs.isEmpty()) {
                    item {
                        Text(
                            text = "No logs yet. Press Start to stream.",
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
