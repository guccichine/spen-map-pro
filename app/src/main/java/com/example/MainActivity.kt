package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.SPenRepository
import com.example.ui.ChatbotScreen
import com.example.ui.DashboardScreen
import com.example.ui.WirelessDebuggingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DashboardViewModel
import com.example.viewmodel.DashboardViewModelFactory

import androidx.compose.material.icons.rounded.ReceiptLong
import com.example.ui.DiagnosticLogsScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Database & Repository
    val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
    val repository = SPenRepository(database.sPenDao())

    // Instantiate ViewModel via Factory
    val viewModel: DashboardViewModel by viewModels {
      DashboardViewModelFactory(repository, applicationContext)
    }

    setContent {
      MyApplicationTheme {
        var currentScreen by remember { mutableStateOf("dashboard") }

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          bottomBar = {
            NavigationBar(containerColor = androidx.compose.ui.graphics.Color(0xFF1E2128)) {
              NavigationBarItem(
                selected = currentScreen == "dashboard",
                onClick = { currentScreen = "dashboard" },
                icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
                label = { Text("Home") },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = com.example.ui.theme.CyberCyan,
                  selectedTextColor = com.example.ui.theme.CyberCyan,
                  unselectedIconColor = androidx.compose.ui.graphics.Color.Gray,
                  unselectedTextColor = androidx.compose.ui.graphics.Color.Gray,
                  indicatorColor = androidx.compose.ui.graphics.Color(0xFF2C2F36)
                )
              )
              NavigationBarItem(
                selected = currentScreen == "chat",
                onClick = { currentScreen = "chat" },
                icon = { Icon(Icons.Rounded.SmartToy, contentDescription = "AI Assistant") },
                label = { Text("Assistant") },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = com.example.ui.theme.CyberCyan,
                  selectedTextColor = com.example.ui.theme.CyberCyan,
                  unselectedIconColor = androidx.compose.ui.graphics.Color.Gray,
                  unselectedTextColor = androidx.compose.ui.graphics.Color.Gray,
                  indicatorColor = androidx.compose.ui.graphics.Color(0xFF2C2F36)
                )
              )
              NavigationBarItem(
                selected = currentScreen == "adb",
                onClick = { currentScreen = "adb" },
                icon = { Icon(Icons.Rounded.DeveloperMode, contentDescription = "Wireless Debugging") },
                label = { Text("ADB") },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = com.example.ui.theme.CyberCyan,
                  selectedTextColor = com.example.ui.theme.CyberCyan,
                  unselectedIconColor = androidx.compose.ui.graphics.Color.Gray,
                  unselectedTextColor = androidx.compose.ui.graphics.Color.Gray,
                  indicatorColor = androidx.compose.ui.graphics.Color(0xFF2C2F36)
                )
              )
              NavigationBarItem(
                selected = currentScreen == "logs",
                onClick = { currentScreen = "logs" },
                icon = { Icon(Icons.Rounded.ReceiptLong, contentDescription = "Diagnostic Logs") },
                label = { Text("Logs") },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = com.example.ui.theme.CyberCyan,
                  selectedTextColor = com.example.ui.theme.CyberCyan,
                  unselectedIconColor = androidx.compose.ui.graphics.Color.Gray,
                  unselectedTextColor = androidx.compose.ui.graphics.Color.Gray,
                  indicatorColor = androidx.compose.ui.graphics.Color(0xFF2C2F36)
                )
              )
            }
          }
        ) { innerPadding ->
          if (currentScreen == "dashboard") {
            DashboardScreen(
              viewModel = viewModel,
              modifier = Modifier.padding(innerPadding)
            )
          } else if (currentScreen == "chat") {
            ChatbotScreen(
              modifier = Modifier.padding(innerPadding)
            )
          } else if (currentScreen == "adb") {
            WirelessDebuggingScreen(
              adbManager = viewModel.adbManager,
              modifier = Modifier.padding(innerPadding)
            )
          } else if (currentScreen == "logs") {
            DiagnosticLogsScreen(
              adbManager = viewModel.adbManager,
              modifier = Modifier.padding(innerPadding)
            )
          }
        }
      }
    }
  }
}

