package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false,
    val isLoading: Boolean = false
)

class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping = _isTyping.asStateFlow()

    private val conversationHistory = mutableListOf<Content>()

    private val systemInstruction = Content(
        parts = listOf(
            Part(text = "You are a helpful S Pen Commander troubleshooting and setup assistant. You help users configure their Samsung S Pen, set up custom gestures, remap button clicks, and troubleshoot accessibility service connection issues.")
        )
    )

    init {
        // Add an initial greeting message
        _messages.value = listOf(
            ChatMessage(text = "Hi! I'm the S Pen Commander assistant. I can help you set up actions, troubleshoot connection issues, or configure advanced gestures. What do you need help with?", isUser = false)
        )
    }

    fun sendMessage(text: String, isComplex: Boolean = false, isLowLatency: Boolean = false) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(text = text, isUser = true)
        _messages.value = _messages.value + userMessage
        
        conversationHistory.add(Content(role = "user", parts = listOf(Part(text = text))))

        _isTyping.value = true
        val placeholderMessage = ChatMessage(text = "...", isUser = false, isLoading = true)
        _messages.value = _messages.value + placeholderMessage

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Determine model
                val model = when {
                    isComplex -> "gemini-3.1-pro-preview"
                    isLowLatency -> "gemini-3.1-flash-lite-preview"
                    else -> "gemini-3.5-flash"
                }

                // Determine config
                val config = if (isComplex) {
                    GenerationConfig(thinkingConfig = ThinkingConfig(thinkingLevel = "high"))
                } else {
                    GenerationConfig()
                }

                val request = GenerateContentRequest(
                    contents = conversationHistory.toList(),
                    generationConfig = config,
                    systemInstruction = systemInstruction
                )

                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    throw Exception("API Key not configured. Please add it to your AI Studio secrets.")
                }

                val response = RetrofitClient.service.generateContentStream(model, apiKey, request)
                
                var fullResponse = ""
                
                response.byteStream().bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        try {
                            if (line!!.startsWith("data: ")) {
                                val jsonStr = line!!.substring(6)
                                val chunk = Json.parseToJsonElement(jsonStr).jsonObject
                                val textPart = chunk["candidates"]?.jsonArray
                                    ?.getOrNull(0)?.jsonObject
                                    ?.get("content")?.jsonObject
                                    ?.get("parts")?.jsonArray
                                    ?.getOrNull(0)?.jsonObject
                                    ?.get("text")?.jsonPrimitive?.content
                                
                                if (textPart != null) {
                                    fullResponse += textPart
                                    withContext(Dispatchers.Main) {
                                        updateLastMessage(fullResponse, false)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // ignore parsing errors for partial streams
                        }
                    }
                }
                
                if (fullResponse.isNotBlank()) {
                    conversationHistory.add(Content(role = "model", parts = listOf(Part(text = fullResponse))))
                } else {
                    withContext(Dispatchers.Main) {
                        updateLastMessage("I'm sorry, I couldn't generate a response.", isError = true)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateLastMessage("Error: ${e.message}", isError = true)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isTyping.value = false
                }
            }
        }
    }

    private fun updateLastMessage(newText: String, isError: Boolean) {
        val current = _messages.value.toMutableList()
        if (current.isNotEmpty()) {
            val last = current.last()
            current[current.size - 1] = last.copy(text = newText, isLoading = false, isError = isError)
            _messages.value = current
        }
    }
}
