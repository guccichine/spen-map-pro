package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppProfile
import com.example.data.GestureMapping
import com.example.data.SPenActions
import com.example.data.SPenTriggers
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SteelSurface
import com.example.ui.theme.SteelSurfaceVariant
import com.example.ui.theme.TitaniumGold
import com.example.viewmodel.DashboardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Internal data structure for macro editing steps
data class MacroStep(
    val type: String, // "DELAY", "SYSTEM", "MEDIA", "GESTURE", "TORCH", "SOUND_MODE", "LAUNCH_APP"
    val value: String, // e.g., "500", "BACK", "PLAY_PAUSE", "SWIPE_UP", "TOGGLE", "CYCLE"
    val delayMs: Int = 500 // helper timing
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroSequencerScreen(
    onBack: () -> Unit,
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Current macro being edited
    var macroName by remember { mutableStateOf("Quick Presentation flow") }
    val macroSteps = remember {
        mutableStateListOf(
            MacroStep("SYSTEM", "BACK"),
            MacroStep("DELAY", "800"),
            MacroStep("GESTURE", "SWIPE_RIGHT"),
            MacroStep("DELAY", "1000"),
            MacroStep("MEDIA", "VOLUME_UP")
        )
    }

    // Simulation/Sandbox Engine state
    var isSimulating by remember { mutableStateOf(false) }
    var currentSimulatingStepIdx by remember { mutableStateOf(-1) }
    var simulationProgress by remember { mutableStateOf(0f) }
    val simulationConsoleLogs = remember { mutableStateListOf<String>() }

    // Dropdown to quick-load presets
    val presets = listOf(
        "DeX Workspace Focus" to listOf(
            MacroStep("SOUND_MODE", "CYCLE"),
            MacroStep("DELAY", "400"),
            MacroStep("SYSTEM", "NOTIFICATIONS"),
            MacroStep("DELAY", "800"),
            MacroStep("SYSTEM", "RECENTS")
        ),
        "Media Binge Controller" to listOf(
            MacroStep("MEDIA", "PLAY_PAUSE"),
            MacroStep("DELAY", "500"),
            MacroStep("TORCH", "TOGGLE"),
            MacroStep("DELAY", "1000"),
            MacroStep("MEDIA", "VOLUME_UP")
        ),
        "Slide presentation Mode" to listOf(
            MacroStep("GESTURE", "SWIPE_RIGHT"),
            MacroStep("DELAY", "600"),
            MacroStep("GESTURE", "SWIPE_RIGHT"),
            MacroStep("DELAY", "1000"),
            MacroStep("SYSTEM", "SCREENSHOT")
        )
    )

    // Assign to Trigger helper states
    var selectedTriggerToAssign by remember { mutableStateOf(SPenTriggers.DOUBLE_CLICK) }
    var triggerDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Futuristic Screen Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CyberCyan.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.Black.copy(alpha = 0.2f), CircleShape)
                        .testTag("macro_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Macro Command Sequencer",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = "Chain multi-step triggers with precise microsecond delays",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Core Profile Details & Load Preset
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SteelSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Macro Name & Presets",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = macroName,
                            onValueChange = { macroName = it },
                            placeholder = { Text("E.g., Slide Deck Remote Flow") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Quick Load Automation Presets",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            presets.forEach { (name, steps) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(SteelSurfaceVariant, RoundedCornerShape(10.dp))
                                        .clickable {
                                            macroSteps.clear()
                                            macroSteps.addAll(steps)
                                            macroName = name
                                            Toast.makeText(context, "Loaded preset: $name", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = CyberCyan,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Timeline Editor Steps list
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Macro Timeline Steps (${macroSteps.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            macroSteps.add(MacroStep("DELAY", "500"))
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Step", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Steps Timeline Cards
            itemsIndexed(macroSteps) { index, step ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentSimulatingStepIdx == index) CyberCyan.copy(alpha = 0.08f) else SteelSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (currentSimulatingStepIdx == index) CyberCyan.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.04f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            if (currentSimulatingStepIdx == index) CyberCyan else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (currentSimulatingStepIdx == index) Color.Black else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = when (step.type) {
                                        "DELAY" -> "Microsecond Intermission (Delay)"
                                        "SYSTEM" -> "Trigger Android System Action"
                                        "MEDIA" -> "Media Control command"
                                        "GESTURE" -> "Perform Screen Swipe gesture"
                                        "TORCH" -> "Toggle Torchlight LED"
                                        "SOUND_MODE" -> "Set Ringer / Audio Profile"
                                        else -> "Macro Sequence Task"
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row {
                                if (index > 0) {
                                    IconButton(
                                        onClick = {
                                            val prev = macroSteps[index - 1]
                                            macroSteps[index - 1] = step
                                            macroSteps[index] = prev
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move Up", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                                if (index < macroSteps.size - 1) {
                                    IconButton(
                                        onClick = {
                                            val next = macroSteps[index + 1]
                                            macroSteps[index + 1] = step
                                            macroSteps[index] = next
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move Down", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                                IconButton(
                                    onClick = { macroSteps.removeAt(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Type dropdown selection
                        var stepTypeExpanded by remember { mutableStateOf(false) }
                        val stepTypes = listOf(
                            "DELAY" to "Wait / Delay Intermission",
                            "SYSTEM" to "System Event (Back, Home, Recents)",
                            "MEDIA" to "Media Command (Play, Track, Vol)",
                            "GESTURE" to "Screen Swipe Direction",
                            "TORCH" to "Flashlight / Torch Control",
                            "SOUND_MODE" to "Silent/Ringer Sound Mode"
                        )

                        ExposedDropdownMenuBox(
                            expanded = stepTypeExpanded,
                            onExpandedChange = { stepTypeExpanded = !stepTypeExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = stepTypes.find { it.first == step.type }?.second ?: step.type,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stepTypeExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                                ),
                                textStyle = MaterialTheme.typography.bodySmall,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = stepTypeExpanded,
                                onDismissRequest = { stepTypeExpanded = false },
                                modifier = Modifier.background(SteelSurfaceVariant)
                            ) {
                                stepTypes.forEach { (typeVal, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                        onClick = {
                                            stepTypeExpanded = false
                                            val defaultValue = when (typeVal) {
                                                "DELAY" -> "500"
                                                "SYSTEM" -> "BACK"
                                                "MEDIA" -> "PLAY_PAUSE"
                                                "GESTURE" -> "SWIPE_DOWN"
                                                "TORCH" -> "TOGGLE"
                                                "SOUND_MODE" -> "CYCLE"
                                                else -> ""
                                            }
                                            macroSteps[index] = step.copy(type = typeVal, value = defaultValue)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Render parameters based on selection
                        when (step.type) {
                            "DELAY" -> {
                                val delayFloat = step.value.toFloatOrNull() ?: 500f
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${delayFloat.toInt()} ms",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                        color = CyberCyan,
                                        modifier = Modifier.width(64.dp)
                                    )
                                    Slider(
                                        value = delayFloat,
                                        onValueChange = {
                                            macroSteps[index] = step.copy(value = it.toInt().toString())
                                        },
                                        valueRange = 100f..3000f,
                                        steps = 29,
                                        colors = SliderDefaults.colors(
                                            activeTrackColor = CyberCyan,
                                            thumbColor = CyberCyan
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            "SYSTEM" -> {
                                var sysExpanded by remember { mutableStateOf(false) }
                                val sysValues = listOf("BACK", "HOME", "RECENTS", "NOTIFICATIONS", "QUICK_SETTINGS", "SCREENSHOT")
                                ExposedDropdownMenuBox(
                                    expanded = sysExpanded,
                                    onExpandedChange = { sysExpanded = !sysExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = "System Action: ${step.value}",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sysExpanded) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyberCyan,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                                        ),
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = sysExpanded,
                                        onDismissRequest = { sysExpanded = false },
                                        modifier = Modifier.background(SteelSurfaceVariant)
                                    ) {
                                        sysValues.forEach { sys ->
                                            DropdownMenuItem(
                                                text = { Text(sys, style = MaterialTheme.typography.bodySmall) },
                                                onClick = {
                                                    sysExpanded = false
                                                    macroSteps[index] = step.copy(value = sys)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            "MEDIA" -> {
                                var mediaExpanded by remember { mutableStateOf(false) }
                                val mediaValues = listOf("PLAY_PAUSE", "NEXT", "PREVIOUS", "VOLUME_UP", "VOLUME_DOWN")
                                ExposedDropdownMenuBox(
                                    expanded = mediaExpanded,
                                    onExpandedChange = { mediaExpanded = !mediaExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = "Media Call: ${step.value}",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mediaExpanded) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyberCyan,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                                        ),
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = mediaExpanded,
                                        onDismissRequest = { mediaExpanded = false },
                                        modifier = Modifier.background(SteelSurfaceVariant)
                                    ) {
                                        mediaValues.forEach { rawVal ->
                                            DropdownMenuItem(
                                                text = { Text(rawVal, style = MaterialTheme.typography.bodySmall) },
                                                onClick = {
                                                    mediaExpanded = false
                                                    macroSteps[index] = step.copy(value = rawVal)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            "GESTURE" -> {
                                var gestureExpanded by remember { mutableStateOf(false) }
                                val gestureValues = listOf("SWIPE_UP", "SWIPE_DOWN", "SWIPE_LEFT", "SWIPE_RIGHT", "DOUBLE_TAP")
                                ExposedDropdownMenuBox(
                                    expanded = gestureExpanded,
                                    onExpandedChange = { gestureExpanded = !gestureExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = "Screen Gesture: ${step.value}",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gestureExpanded) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyberCyan,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                                        ),
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = gestureExpanded,
                                        onDismissRequest = { gestureExpanded = false },
                                        modifier = Modifier.background(SteelSurfaceVariant)
                                    ) {
                                        gestureValues.forEach { g ->
                                            DropdownMenuItem(
                                                text = { Text(g, style = MaterialTheme.typography.bodySmall) },
                                                onClick = {
                                                    gestureExpanded = false
                                                    macroSteps[index] = step.copy(value = g)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            "TORCH" -> {
                                var torchExpanded by remember { mutableStateOf(false) }
                                val torchValues = listOf("ENABLE", "DISABLE", "TOGGLE")
                                ExposedDropdownMenuBox(
                                    expanded = torchExpanded,
                                    onExpandedChange = { torchExpanded = !torchExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = "Torch Action: ${step.value}",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = torchExpanded) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyberCyan,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                                        ),
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = torchExpanded,
                                        onDismissRequest = { torchExpanded = false },
                                        modifier = Modifier.background(SteelSurfaceVariant)
                                    ) {
                                        torchValues.forEach { t ->
                                            DropdownMenuItem(
                                                text = { Text(t, style = MaterialTheme.typography.bodySmall) },
                                                onClick = {
                                                    torchExpanded = false
                                                    macroSteps[index] = step.copy(value = t)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            "SOUND_MODE" -> {
                                var soundExpanded by remember { mutableStateOf(false) }
                                val soundValues = listOf("SOUND", "VIBRATE", "SILENT", "CYCLE")
                                ExposedDropdownMenuBox(
                                    expanded = soundExpanded,
                                    onExpandedChange = { soundExpanded = !soundExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = "Set Sound Mode: ${step.value}",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = soundExpanded) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyberCyan,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                                        ),
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = soundExpanded,
                                        onDismissRequest = { soundExpanded = false },
                                        modifier = Modifier.background(SteelSurfaceVariant)
                                    ) {
                                        soundValues.forEach { s ->
                                            DropdownMenuItem(
                                                text = { Text(s, style = MaterialTheme.typography.bodySmall) },
                                                onClick = {
                                                    soundExpanded = false
                                                    macroSteps[index] = step.copy(value = s)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Sandbox Real-Time Simulation Engine Console
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (isSimulating) Color.Green else Color.Gray, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "SANDBOX SIMULATOR & LOGS",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color.White
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!isSimulating) {
                                    Button(
                                        onClick = {
                                            if (macroSteps.isEmpty()) {
                                                Toast.makeText(context, "Add steps first!", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            isSimulating = true
                                            simulationProgress = 0f
                                            simulationConsoleLogs.clear()
                                            simulationConsoleLogs.add("⚡ Initiating Macro Simulation for [${macroName}]")
                                            currentSimulatingStepIdx = 0

                                            coroutineScope.launch {
                                                var i = 0
                                                while (i < macroSteps.size && isSimulating) {
                                                    currentSimulatingStepIdx = i
                                                    val step = macroSteps[i]
                                                    simulationConsoleLogs.add("▶️ Step ${i+1}: Executing [${step.type}] [${step.value}]")
                                                    
                                                    // Simulation delay step
                                                    if (step.type == "DELAY") {
                                                        val ms = step.value.toLongOrNull() ?: 500L
                                                        delay(ms)
                                                    } else {
                                                        delay(400) // general mock animation timing
                                                    }

                                                    simulationConsoleLogs.add("✅ Step ${i+1} Finished successfully.")
                                                    simulationProgress = (i + 1).toFloat() / macroSteps.size
                                                    i++
                                                }
                                                currentSimulatingStepIdx = -1
                                                isSimulating = false
                                                simulationConsoleLogs.add("\uD83C\uDFC6 Sequence Execution Complete! 100% stable.")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Run Test", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            isSimulating = false
                                            currentSimulatingStepIdx = -1
                                            simulationConsoleLogs.add("⏹️ Simulation halted by user.")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Stop", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isSimulating) {
                            LinearProgressIndicator(
                                progress = { simulationProgress },
                                color = CyberCyan,
                                trackColor = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp, max = 150.dp)
                                .background(Color.Black, RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            LazyColumn(reverseLayout = true, modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(simulationConsoleLogs.reversed().toList()) { _, log ->
                                    Text(
                                        text = log,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = if (log.contains("✅") || log.contains("Complete")) CyberCyan else Color.LightGray,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Bind Macro To S Pen Action Trigger Direct mapping setup
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SteelSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Save,
                                contentDescription = null,
                                tint = TitaniumGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Deploy Macro mapping to S Pen",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Instantly assign this multi-action script chain to any physical S Pen button press or spatial gesture:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ExposedDropdownMenuBox(
                            expanded = triggerDropdownExpanded,
                            onExpandedChange = { triggerDropdownExpanded = !triggerDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = "S Pen Trigger: ${SPenTriggers.getLabel(selectedTriggerToAssign)}",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = triggerDropdownExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = triggerDropdownExpanded,
                                onDismissRequest = { triggerDropdownExpanded = false },
                                modifier = Modifier.background(SteelSurfaceVariant)
                            ) {
                                SPenTriggers.ALL.forEach { trig ->
                                    DropdownMenuItem(
                                        text = { Text(SPenTriggers.getLabel(trig), style = MaterialTheme.typography.bodyMedium) },
                                        onClick = {
                                            selectedTriggerToAssign = trig
                                            triggerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (macroSteps.isEmpty()) {
                                    Toast.makeText(context, "Cannot deploy an empty macro timeline!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                // Serialize steps: TYPE:VALUE;TYPE:VALUE
                                val serializedVal = macroSteps.joinToString(";") { "${it.type}:${it.value}" }
                                val newMapping = GestureMapping(
                                    profilePackageName = AppProfile.GLOBAL_PACKAGE,
                                    triggerType = selectedTriggerToAssign,
                                    actionType = "MACRO", // Custom type Macro!
                                    actionValue = serializedVal,
                                    vibrationPattern = "HEAVY_CLICK",
                                    vibrationIntensity = 75
                                )

                                viewModel.updateMapping(newMapping)
                                Toast.makeText(context, "Deploy successful! Assigned to ${SPenTriggers.getLabel(selectedTriggerToAssign)}", Toast.LENGTH_LONG).show()
                                onBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("deploy_macro_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Deploy",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Deploy Command Sequence to S Pen",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
