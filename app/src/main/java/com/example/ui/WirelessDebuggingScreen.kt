package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.service.AdbManager
import com.example.service.AdbServiceInfo

val ChromeRed = Color(0xFFFF2A2A)
val ChromeRedDark = Color(0xFF8B0000)
val FiberglassBg = Color(0xFF0F0F11)
val FiberglassCard = Color(0xFF1A1A1E)
val FiberglassBorder = Color(0xFF2A2A35)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WirelessDebuggingScreen(
    adbManager: AdbManager,
    modifier: Modifier = Modifier
) {
    val discoveredServices by adbManager.discoveredServices.collectAsState()
    val connectionStatus by adbManager.connectionStatus.collectAsState()
    
    var currentStep by remember { mutableIntStateOf(1) }
    var selectedService by remember { mutableStateOf<AdbServiceInfo?>(null) }
    var pairingCode by remember { mutableStateOf("") }
    
    DisposableEffect(Unit) {
        adbManager.startDiscovery()
        onDispose {
            adbManager.stopDiscovery()
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FiberglassBg)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ADB Setup Wizard",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { 
                adbManager.stopDiscovery()
                adbManager.startDiscovery() 
            }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = ChromeRed)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress Indicator
        LinearProgressIndicator(
            progress = { currentStep / 5f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = ChromeRed,
            trackColor = FiberglassBorder,
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Wizard Content
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            },
            label = "wizard_step"
        ) { step ->
            when (step) {
                1 -> StepOne(onNext = { currentStep = 2 })
                2 -> StepTwo(onNext = { currentStep = 3 }, onBack = { currentStep = 1 })
                3 -> StepThree(onNext = { currentStep = 4 }, onBack = { currentStep = 2 })
                4 -> StepFour(
                    discoveredServices = discoveredServices,
                    onServiceSelected = { 
                        selectedService = it
                        currentStep = 5 
                    },
                    onBack = { currentStep = 3 }
                )
                5 -> StepFive(
                    service = selectedService,
                    pairingCode = pairingCode,
                    onCodeChange = { pairingCode = it },
                    status = connectionStatus,
                    onPair = { 
                        selectedService?.let {
                            val ip = it.address?.hostAddress ?: "127.0.0.1"
                            if (it.type == "pairing") {
                                adbManager.pair(ip, it.port, pairingCode)
                            } else {
                                adbManager.connect(ip, it.port)
                            }
                        }
                    },
                    onBack = { currentStep = 4 }
                )
            }
        }
    }
}

@Composable
fun WizardCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, FiberglassBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = FiberglassCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Brush.linearGradient(listOf(ChromeRedDark, ChromeRed)),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun WizardButtons(
    onNext: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    nextText: String = "Next Step"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (onBack != null) {
            OutlinedButton(
                onClick = onBack,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, FiberglassBorder)
            ) {
                Text("Back")
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }
        
        if (onNext != null) {
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = ChromeRed)
            ) {
                Text(nextText, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StepOne(onNext: () -> Unit) {
    Column {
        WizardCard(
            title = "Enable Developer Options",
            icon = Icons.Rounded.Settings
        ) {
            Text(
                text = "1. Open your Samsung device's Settings.\n" +
                       "2. Scroll down and tap on 'About phone'.\n" +
                       "3. Tap 'Software information'.\n" +
                       "4. Tap 'Build number' 7 times rapidly to unlock Developer mode.",
                color = Color.LightGray,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
        }
        WizardButtons(onNext = onNext)
    }
}

@Composable
fun StepTwo(onNext: () -> Unit, onBack: () -> Unit) {
    Column {
        WizardCard(
            title = "Enable Wireless Debugging",
            icon = Icons.Rounded.WifiTethering
        ) {
            Text(
                text = "1. Go back to the main Settings menu.\n" +
                       "2. Scroll to the very bottom and tap 'Developer options'.\n" +
                       "3. Scroll down to the 'Debugging' section.\n" +
                       "4. Toggle 'Wireless debugging' to ON.\n" +
                       "5. If prompted, allow it on your current Wi-Fi network.",
                color = Color.LightGray,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
        }
        WizardButtons(onNext = onNext, onBack = onBack)
    }
}

@Composable
fun StepThree(onNext: () -> Unit, onBack: () -> Unit) {
    Column {
        WizardCard(
            title = "Get the Pairing Code",
            icon = Icons.Rounded.PhonelinkSetup
        ) {
            Text(
                text = "1. Tap the actual words 'Wireless debugging' (not the toggle) to open its menu.\n" +
                       "2. Tap 'Pair device with pairing code'.\n" +
                       "3. A popup will appear displaying a 6-digit Wi-Fi pairing code.\n" +
                       "4. Keep this popup open on your screen!",
                color = Color.LightGray,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
        }
        WizardButtons(onNext = onNext, onBack = onBack, nextText = "I have the code")
    }
}

@Composable
fun StepFour(
    discoveredServices: List<AdbServiceInfo>,
    onServiceSelected: (AdbServiceInfo) -> Unit,
    onBack: () -> Unit
) {
    Column {
        Text(
            text = "Select Your Device",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val pairingServices = discoveredServices.filter { it.type == "pairing" }
            
            items(pairingServices) { service ->
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, ChromeRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = FiberglassCard),
                    onClick = { onServiceSelected(service) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Cable, contentDescription = null, tint = ChromeRed)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(service.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Port: ${service.port}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Select", tint = Color.Gray)
                    }
                }
            }
            
            if (pairingServices.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ChromeRed, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Scanning local network for pairing services...", color = Color.Gray)
                            Text("Make sure the pairing popup is open!", color = ChromeRed, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            
            // Allow connecting directly if already paired
            val connectServices = discoveredServices.filter { it.type == "connect" }
            if (connectServices.isNotEmpty()) {
                item {
                    Text(
                        "Already Paired Devices",
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                items(connectServices) { service ->
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, FiberglassBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = FiberglassCard),
                        onClick = { onServiceSelected(service) }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Wifi, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(service.name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Port: ${service.port}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text("Connect", color = ChromeRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        
        WizardButtons(onBack = onBack, onNext = null)
    }
}

@Composable
fun StepFive(
    service: AdbServiceInfo?,
    pairingCode: String,
    onCodeChange: (String) -> Unit,
    status: String,
    onPair: () -> Unit,
    onBack: () -> Unit
) {
    Column {
        WizardCard(
            title = if (service?.type == "pairing") "Enter Pairing Code" else "Connect Device",
            icon = Icons.Rounded.Key
        ) {
            if (service?.type == "pairing") {
                val isError = pairingCode.isNotEmpty() && pairingCode.length < 6
                OutlinedTextField(
                    value = pairingCode,
                    onValueChange = { newValue ->
                        val digitsOnly = newValue.filter { it.isDigit() }
                        if (digitsOnly.length <= 6) {
                            onCodeChange(digitsOnly)
                        }
                    },
                    label = { Text("6-Digit Code") },
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("Code must be exactly 6 digits", color = ChromeRed)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ChromeRed,
                        unfocusedBorderColor = FiberglassBorder,
                        focusedLabelColor = ChromeRed,
                        unfocusedLabelColor = Color.Gray,
                        errorBorderColor = ChromeRed,
                        errorLabelColor = ChromeRed
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Text(
                text = status,
                color = if (status.contains("Granted") || status.contains("successfully")) Color.Green else Color.LightGray,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val isButtonEnabled = if (service?.type == "pairing") pairingCode.length == 6 else true
            Button(
                onClick = onPair,
                enabled = isButtonEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChromeRed,
                    disabledContainerColor = ChromeRed.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(
                    text = if (service?.type == "pairing") "Pair Device" else "Connect",
                    color = if (isButtonEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        WizardButtons(onBack = onBack, onNext = null)
    }
}
