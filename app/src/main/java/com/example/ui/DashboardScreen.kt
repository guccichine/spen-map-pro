package com.example.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppProfile
import com.example.data.GestureMapping
import com.example.data.SPenActions
import com.example.data.SPenTriggers
import com.example.ui.theme.*
import com.example.viewmodel.DashboardViewModel
import com.example.utils.DebugLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    onNavigateToScreen: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val isServiceRunning by viewModel.serviceStatusRunning.collectAsState()
    val mappings by viewModel.activeProfileMappings.collectAsState()
    
    // Select the global profile by default to simplify
    LaunchedEffect(Unit) {
        viewModel.selectProfile(AppProfile.GLOBAL_PACKAGE)
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
        // Simple Header
        Surface(
            color = Color(0xFF1E2128).copy(alpha = 0.5f),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "S Pen Commander",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Status card
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isServiceRunning) Color(0xFF1B3B24) else Color(0xFF3B1B1B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isServiceRunning) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                contentDescription = "Status",
                                tint = if (isServiceRunning) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isServiceRunning) "Service Active" else "Service Inactive",
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (!isServiceRunning) {
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                            ) {
                                Text("Enable", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }

        // Main List of Categories
        LazyColumn(
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                TestTriggerSection()
            }
            
            item {
                TriggerCategory(
                    title = "Button Pushes",
                    icon = Icons.Rounded.AdsClick,
                    triggers = listOf(
                        SPenTriggers.SINGLE_CLICK,
                        SPenTriggers.DOUBLE_CLICK,
                        SPenTriggers.LONG_PRESS,
                        SPenTriggers.TRIPLE_CLICK,
                        SPenTriggers.QUADRUPLE_CLICK
                    ),
                    mappings = mappings,
                    viewModel = viewModel
                )
            }
            
            item {
                TriggerCategory(
                    title = "Tap Options (Screen)",
                    icon = Icons.Rounded.TouchApp,
                    triggers = listOf(
                        "TAP_SCREEN",
                        "DOUBLE_TAP_SCREEN",
                        "SWIPE_SCREEN"
                    ),
                    mappings = mappings,
                    viewModel = viewModel
                )
            }

            item {
                TriggerCategory(
                    title = "Air Actions",
                    icon = Icons.Rounded.Air,
                    triggers = listOf(
                        SPenTriggers.FLICK_UP,
                        SPenTriggers.FLICK_DOWN,
                        SPenTriggers.FLICK_LEFT,
                        SPenTriggers.FLICK_RIGHT,
                        SPenTriggers.CIRCLE_CW,
                        SPenTriggers.CIRCLE_CCW,
                        SPenTriggers.ZIGZAG,
                        SPenTriggers.SHAKE
                    ),
                    mappings = mappings,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun TestTriggerSection() {
    val debugLogs by DebugLogger.logs.collectAsState()
    val lastTrigger = debugLogs.find { it.level == "TRIGGER" || it.level == "INPUT" }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Science, "Test", tint = CyberCyan)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Test Connection & Inputs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Perform an S Pen action to test if it's detected properly:",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0A0A), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (lastTrigger != null) {
                    Text(
                        text = "Detected: ${lastTrigger.message}",
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Awaiting S Pen input...",
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun TriggerCategory(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    triggers: List<String>,
    mappings: List<GestureMapping>,
    viewModel: DashboardViewModel
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SteelSurface.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = CyberCyan
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            triggers.forEach { triggerName ->
                val mapping = mappings.find { it.triggerType == triggerName }
                MappingRow(
                    trigger = triggerName,
                    mapping = mapping,
                    viewModel = viewModel
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            }
        }
    }
}

fun getHelpTextForTrigger(trigger: String): String {
    return when (trigger) {
        SPenTriggers.SINGLE_CLICK -> "Press the S Pen button once."
        SPenTriggers.DOUBLE_CLICK -> "Press the S Pen button twice quickly."
        SPenTriggers.LONG_PRESS -> "Press and hold the S Pen button."
        SPenTriggers.TRIPLE_CLICK -> "Press the S Pen button three times quickly."
        SPenTriggers.QUADRUPLE_CLICK -> "Press the S Pen button four times quickly."
        "TAP_SCREEN" -> "Tap the S Pen tip anywhere on the screen."
        "DOUBLE_TAP_SCREEN" -> "Double tap the S Pen tip on the screen."
        "SWIPE_SCREEN" -> "Swipe the S Pen tip across the screen."
        SPenTriggers.FLICK_UP -> "Hold the button and flick the S Pen up in the air."
        SPenTriggers.FLICK_DOWN -> "Hold the button and flick the S Pen down in the air."
        SPenTriggers.FLICK_LEFT -> "Hold the button and flick the S Pen left in the air."
        SPenTriggers.FLICK_RIGHT -> "Hold the button and flick the S Pen right in the air."
        SPenTriggers.CIRCLE_CW -> "Hold the button and draw a circle clockwise in the air."
        SPenTriggers.CIRCLE_CCW -> "Hold the button and draw a circle counter-clockwise in the air."
        SPenTriggers.ZIGZAG -> "Hold the button and draw a zigzag pattern in the air."
        SPenTriggers.SHAKE -> "Hold the button and shake the S Pen side to side."
        else -> "Follow standard S Pen gesture patterns."
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MappingRow(
    trigger: String,
    mapping: GestureMapping?,
    viewModel: DashboardViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    
    val currentActionLabel = if (mapping != null && mapping.actionType != SPenActions.TYPE_NONE) {
        SPenActions.getActionLabel(mapping.actionType, mapping.actionValue)
    } else {
        "Unassigned"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = SPenTriggers.getLabel(trigger),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = currentActionLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = if (currentActionLabel == "Unassigned") Color.Gray else CyberCyan
            )
        }
        
        IconButton(onClick = { showHelpDialog = true }) {
            Icon(
                imageVector = Icons.Rounded.PlayCircleOutline,
                contentDescription = "How to perform",
                tint = Color.White.copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Rounded.KeyboardArrowRight,
            contentDescription = "Edit",
            tint = Color.Gray
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = Color(0xFF1E2128),
            title = {
                Text(
                    text = "How to: ${SPenTriggers.getLabel(trigger)}",
                    color = Color.White
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Rounded.Animation,
                        contentDescription = "Clip",
                        tint = CyberCyan,
                        modifier = Modifier.size(64.dp).padding(bottom = 16.dp)
                    )
                    Text(
                        text = getHelpTextForTrigger(trigger),
                        color = Color.LightGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Got it", color = CyberCyan)
                }
            }
        )
    }

    if (expanded) {
        ModalBottomSheet(
            onDismissRequest = { expanded = false },
            containerColor = Color(0xFF1E2128)
        ) {
            ActionSelectionSheet(
                trigger = trigger,
                currentMapping = mapping,
                onSelectAction = { type, value ->
                    viewModel.updateMapping(
                        GestureMapping(
                            profilePackageName = AppProfile.GLOBAL_PACKAGE,
                            triggerType = trigger,
                            actionType = type,
                            actionValue = value
                        )
                    )
                    expanded = false
                },
                onClear = {
                    viewModel.updateMapping(
                        GestureMapping(
                            profilePackageName = AppProfile.GLOBAL_PACKAGE,
                            triggerType = trigger,
                            actionType = SPenActions.TYPE_NONE,
                            actionValue = ""
                        )
                    )
                    expanded = false
                }
            )
        }
    }
}

@Composable
fun ActionSelectionSheet(
    trigger: String,
    currentMapping: GestureMapping?,
    onSelectAction: (String, String) -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Set Action for ${SPenTriggers.getLabel(trigger)}",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(24.dp)
        )

        LazyColumn(
            modifier = Modifier.heightIn(max = 400.dp),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            item {
                Button(
                    onClick = onClear,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text("Clear Assignment", color = Color.Red)
                }
            }

            SPenActions.ACTIONS_MAP.forEach { (type, values) ->
                if (type == SPenActions.TYPE_NONE) return@forEach
                
                item {
                    Text(
                        text = type.replace("_", " "),
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberCyan,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                
                items(values) { value ->
                    val isSelected = currentMapping?.actionType == type && currentMapping.actionValue == value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectAction(type, value) }
                            .background(if (isSelected) CyberCyan.copy(alpha = 0.1f) else Color.Transparent)
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = SPenActions.getActionLabel(type, value),
                            color = if (isSelected) CyberCyan else Color.White
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.CheckCircle, "Selected", tint = CyberCyan, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
