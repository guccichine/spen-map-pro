package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.CyberCyan
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(
    viewModel: ChatViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var useComplexTask by remember { mutableStateOf(false) }
    var useLowLatency by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    val fiberglassBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF140000), // Dark red tinted black
            Color(0xFF2C0A0A), // Dark chrome red
            Color(0xFF0A0000)  // Almost black
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(fiberglassBrush)
    ) {
        // Header
        Surface(
            color = Color(0xFF1E2128).copy(alpha = 0.5f),
            shadowElevation = 4.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.SmartToy, contentDescription = "AI", tint = CyberCyan)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "S Pen AI Assistant",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                // Settings row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = useComplexTask,
                            onCheckedChange = { 
                                useComplexTask = it
                                if (it) useLowLatency = false 
                            },
                            colors = CheckboxDefaults.colors(checkedColor = CyberCyan)
                        )
                        Text("High Thinking", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = useLowLatency,
                            onCheckedChange = { 
                                useLowLatency = it
                                if (it) useComplexTask = false
                            },
                            colors = CheckboxDefaults.colors(checkedColor = CyberCyan)
                        )
                        Text("Low Latency", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Chat messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message = message)
            }
        }

        // Input area
        Surface(
            color = Color(0xFF1E2128).copy(alpha = 0.8f),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask about setup or troubleshooting...", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    enabled = !isTyping,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText, useComplexTask, useLowLatency)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isTyping,
                    modifier = Modifier
                        .background(if (inputText.isNotBlank() && !isTyping) CyberCyan else Color.DarkGray, CircleShape)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send",
                        tint = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: com.example.viewmodel.ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (message.isUser) CyberCyan.copy(alpha = 0.2f) else Color(0xFF1E2128)
    val textColor = if (message.isError) Color.Red else Color.White

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            PaddingValues(12.dp).let { padding ->
                if (message.isLoading) {
                    Row(modifier = Modifier.padding(padding)) {
                        CircularProgressIndicator(
                            color = CyberCyan,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Thinking...", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Text(
                        text = message.text,
                        color = textColor,
                        modifier = Modifier.padding(padding),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
