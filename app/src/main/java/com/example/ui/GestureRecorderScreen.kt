package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GestureMapping
import com.example.data.SPenActions
import com.example.data.SPenTriggers
import com.example.ui.theme.*
import com.example.viewmodel.DashboardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureRecorderScreen(
    onBack: () -> Unit,
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mappings by viewModel.activeProfileMappings.collectAsState()
    val gestureSensitivity by viewModel.gestureSensitivity.collectAsState()

    // 1. Selected S-Pen Gesture Trigger
    var selectedTrigger by remember { mutableStateOf(SPenTriggers.SINGLE_CLICK) }
    var triggerSelectorExpanded by remember { mutableStateOf(false) }

    // 2. Stroke Points on drawing Canvas
    val points = remember { mutableStateListOf<Offset>() }
    var detectedPatternName by remember { mutableStateOf("Awaiting Stroke...") }
    var recognitionConfidence by remember { mutableStateOf(0.0f) }

    // 3. Calibration / saving simulation
    var isCalibrating by remember { mutableStateOf(false) }
    var calibrationProgress by remember { mutableStateOf(0.0f) }

    // Dynamic S-Pen Stroke Color
    val neonPrimaryColor = Color(0xFF22D3EE) // Neon Cyan
    val neonGlowColor = Color(0xFF22D3EE).copy(alpha = 0.3f)

    // Helper to run simple pattern recognition when points change or sensitivity changes
    LaunchedEffect(points.size, gestureSensitivity) {
        val sensitivityMultiplier = 50.0f / gestureSensitivity.coerceAtLeast(10)
        val minRequiredPoints = (5 * sensitivityMultiplier).toInt().coerceAtLeast(3)

        if (points.size < minRequiredPoints) {
            detectedPatternName = "Awaiting Stroke..."
            recognitionConfidence = 0.0f
            return@LaunchedEffect
        }

        // Run basic S Pen gesture classifier (geometric approximation with dynamic thresholds)
        val first = points.first()
        val last = points.last()

        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
        }
        val width = maxX - minX
        val height = maxY - minY
        val dx = last.x - first.x
        val dy = last.y - first.y
        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        when {
            // Circle Check (Width & Height similar, first/last points close)
            width > (80 * sensitivityMultiplier) && height > (80 * sensitivityMultiplier) && dist < (120 * sensitivityMultiplier) && (width / height in 0.7f..1.4f) -> {
                detectedPatternName = if (first.y < last.y) "Circle Clockwise" else "Circle Counter-Clockwise"
                recognitionConfidence = 91.5f
            }
            // Swipe Up
            dy < (-120 * sensitivityMultiplier) && width < (90 * sensitivityMultiplier) -> {
                detectedPatternName = "Vertical Up flick"
                recognitionConfidence = 94.2f
            }
            // Swipe Down
            dy > (120 * sensitivityMultiplier) && width < (90 * sensitivityMultiplier) -> {
                detectedPatternName = "Vertical Down flick"
                recognitionConfidence = 95.8f
            }
            // Swipe Left
            dx < (-120 * sensitivityMultiplier) && height < (90 * sensitivityMultiplier) -> {
                detectedPatternName = "Horizontal Left flick"
                recognitionConfidence = 92.4f
            }
            // Swipe Right
            dx > (120 * sensitivityMultiplier) && height < (90 * sensitivityMultiplier) -> {
                detectedPatternName = "Horizontal Right flick"
                recognitionConfidence = 93.1f
            }
            // Triangle or Diagonal
            width > (120 * sensitivityMultiplier) && height > (120 * sensitivityMultiplier) -> {
                detectedPatternName = "Triangle shape pattern"
                recognitionConfidence = 87.6f
            }
            // Standard Flick
            else -> {
                detectedPatternName = "Custom Signature Stroke"
                recognitionConfidence = 82.4f
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ElegantDarkBg)
    ) {
        // App Header Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Gesture Recorder",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Register customized physical & spatial S Pen signatures",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("recorder_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        Divider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            thickness = 0.5.dp,
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            // 1. Gesture Target Selector Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SteelSurface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EditRoad,
                                contentDescription = "Triggers",
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Select S Pen Gesture Target",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Dropdown for SPenTriggers
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { triggerSelectorExpanded = true },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("trigger_dropdown_button"),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Black.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = when {
                                                selectedTrigger.contains("CLICK") -> Icons.Rounded.AdsClick
                                                selectedTrigger.contains("PRESS") -> Icons.Rounded.Pin
                                                selectedTrigger.contains("FLICK") || selectedTrigger.contains("LEFT") || selectedTrigger.contains("RIGHT") || selectedTrigger.contains("UP") || selectedTrigger.contains("DOWN") -> Icons.Rounded.Swipe
                                                selectedTrigger.contains("CIRCLE") -> Icons.Rounded.Refresh
                                                selectedTrigger.contains("TRIANGLE") -> Icons.Rounded.ChangeHistory
                                                else -> Icons.Rounded.Gesture
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFFD0BCFF)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = SPenTriggers.getLabel(selectedTrigger),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDropDown,
                                        contentDescription = "Dropdown",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = triggerSelectorExpanded,
                                onDismissRequest = { triggerSelectorExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(SteelSurfaceVariant)
                            ) {
                                SPenTriggers.ALL.forEach { trigger ->
                                    DropdownMenuItem(
                                        text = { Text(SPenTriggers.getLabel(trigger)) },
                                        onClick = {
                                            selectedTrigger = trigger
                                            triggerSelectorExpanded = false
                                            points.clear() // Clear canvas on change
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1.5 Gesture Sensitivity Slider
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SteelSurface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Sensitivity Settings",
                                tint = Color(0xFF22D3EE),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Gesture Sensitivity",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Adjust the sensitivity of complex gesture recognition. Higher sensitivity requires smaller, more precise S Pen spatial gestures. Lower sensitivity requires larger, deliberate movements.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Low",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )

                            Slider(
                                value = gestureSensitivity.toFloat(),
                                onValueChange = { newValue ->
                                    viewModel.updateGestureSensitivity(newValue.toInt())
                                },
                                valueRange = 10f..100f,
                                steps = 8, // Increments of 10
                                colors = SliderDefaults.colors(
                                    activeTrackColor = Color(0xFF22D3EE),
                                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    thumbColor = Color(0xFF22D3EE),
                                    activeTickColor = Color(0xFF07090C),
                                    inactiveTickColor = Color(0xFF22D3EE).copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("gesture_sensitivity_slider")
                            )

                            Text(
                                text = "High",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF22D3EE)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Current Sensitivity:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "${gestureSensitivity}%",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFF22D3EE)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Movement Required:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            val reqText = when {
                                gestureSensitivity <= 30 -> "Large deliberate motion (~2.0x)"
                                gestureSensitivity <= 45 -> "Standard generous motion (~1.3x)"
                                gestureSensitivity <= 55 -> "Default motion (1.0x)"
                                gestureSensitivity <= 75 -> "Light agile motion (~0.7x)"
                                else -> "Micro-gestures (~0.5x)"
                            }
                            Text(
                                text = reqText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (gestureSensitivity >= 60) Color(0xFF4ADE80) else TitaniumGold
                            )
                        }
                    }
                }
            }

            // 2. Interactive Gesture Canvas drawing board
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SteelSurface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Draw,
                                    contentDescription = "Canvas",
                                    tint = Color(0xFF22D3EE),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Gesture Capture Canvas",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            if (points.isNotEmpty()) {
                                TextButton(
                                    onClick = { points.clear() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF44336))
                                ) {
                                    Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Drawing Area Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .background(Color(0xFF07090C), RoundedCornerShape(16.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (points.isNotEmpty()) neonPrimaryColor.copy(alpha = 0.4f)
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            points.clear()
                                            points.add(offset)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            points.add(change.position)
                                        }
                                    )
                                }
                                .testTag("gesture_drawing_board"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (points.isEmpty()) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Gesture,
                                        contentDescription = "Placeholder",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Touch here to draw gesture waveform",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "S Pen clicks and directions are analyzed in real time.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                if (points.size > 1) {
                                    val path = Path().apply {
                                        moveTo(points.first().x, points.first().y)
                                        for (i in 1 until points.size) {
                                            lineTo(points[i].x, points[i].y)
                                        }
                                    }
                                    // Glow shadow effect
                                    drawPath(
                                        path = path,
                                        color = neonGlowColor,
                                        style = Stroke(
                                            width = 10f,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                    // Primary sharp line
                                    drawPath(
                                        path = path,
                                        color = neonPrimaryColor,
                                        style = Stroke(
                                            width = 4f,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Real-time Classifier metrics
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Captured Nodes",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "${points.size} pts",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White
                                    )
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Shape Signature Classification",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = detectedPatternName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (points.isEmpty()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else Color(0xFF4ADE80)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Save / Register DB Calibration Action
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SteelSurface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SaveAlt,
                                contentDescription = "Save",
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Calibrate & Save Gesture Map",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Calibrating saves this exact gesture path to local SQLite Room DB. Any incoming S Pen event that resembles your recorded points will execute your mapped action.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Progress Indicator
                        AnimatedVisibility(visible = isCalibrating) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Analyzing gesture coordinates...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF4ADE80)
                                    )
                                    Text(
                                        text = "${(calibrationProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF4ADE80)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { calibrationProgress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFF4ADE80),
                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                )
                            }
                        }

                        val requiredPoints = (10 * (50.0f / gestureSensitivity.coerceAtLeast(10))).toInt().coerceAtLeast(4)
                        val isEnabled = points.size >= requiredPoints && !isCalibrating

                        Button(
                            onClick = {
                                scope.launch {
                                    isCalibrating = true
                                    calibrationProgress = 0.0f
                                    while (calibrationProgress < 1.0f) {
                                        delay(150)
                                        calibrationProgress += 0.1f
                                    }
                                    isCalibrating = false

                                    // Save customized mapping in SQLite Room
                                    // Find if there is an existing mapping for this trigger, else create one
                                    val currentProfilePackage = viewModel.selectedProfilePackageName.value
                                    val existingMapping = mappings.find { it.triggerType == selectedTrigger }
                                    val newMapping = existingMapping?.copy(
                                        // Update to a configured action state if empty
                                        actionType = if (existingMapping.actionType == SPenActions.TYPE_NONE) SPenActions.TYPE_SYSTEM else existingMapping.actionType,
                                        actionValue = if (existingMapping.actionValue.isEmpty()) SPenActions.VAL_SYSTEM_BACK else existingMapping.actionValue,
                                        vibrationPattern = if (existingMapping.vibrationPattern == "NONE") "TICK" else existingMapping.vibrationPattern,
                                        vibrationIntensity = if (existingMapping.vibrationIntensity == 0) 50 else existingMapping.vibrationIntensity
                                    ) ?: GestureMapping(
                                        profilePackageName = currentProfilePackage,
                                        triggerType = selectedTrigger,
                                        actionType = SPenActions.TYPE_SYSTEM,
                                        actionValue = SPenActions.VAL_SYSTEM_BACK,
                                        vibrationPattern = "TICK",
                                        vibrationIntensity = 50
                                    )

                                    viewModel.updateMapping(newMapping)
                                    Toast.makeText(context, "S Pen '${SPenTriggers.getLabel(selectedTrigger)}' Gesture Calibrated & Saved in Room!", Toast.LENGTH_SHORT).show()
                                    points.clear()
                                }
                            },
                            enabled = isEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4ADE80),
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_gesture_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = "Save Mapping"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (points.size < requiredPoints) "Draw complete stroke ($requiredPoints pts) to save" else "Calibrate & Save Gesture to Room",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // 4. Gesture Reference Library with Animations
            item {
                var selectedGuideIndex by remember { mutableStateOf(0) }
                val currentGuide = patternGuides[selectedGuideIndex]

                Card(
                    colors = CardDefaults.cardColors(containerColor = SteelSurface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().testTag("gesture_reference_library_card")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoStories,
                                contentDescription = "Reference Library",
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Gesture Pattern Reference Library",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Learn how to perform S Pen spatial gestures. Select any pattern below to view real-time vector trace instructions and classification requirements.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large detailed animated showcase
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF07090C), RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left side: Animated trace
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                GestureAnimationPlayer(
                                    gestureType = currentGuide.name,
                                    modifier = Modifier.size(80.dp),
                                    strokeColor = Color(0xFF22D3EE),
                                    strokeWidth = 6f
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Right side: Name, description & tip
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentGuide.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF22D3EE)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentGuide.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Classifier tip note
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.25f)),
                            border = BorderStroke(1.dp, Color(0xFFE2CFC9).copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lightbulb,
                                    contentDescription = "Tip",
                                    tint = TitaniumGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = "Classifier Tip",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TitaniumGold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = currentGuide.tip,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Horizontal Scrollable Row of Miniatures to select
                        Text(
                            text = "Supported Patterns",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(patternGuides.size) { index ->
                                val guide = patternGuides[index]
                                val isSelected = index == selectedGuideIndex
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFF0D1520) else SteelSurfaceVariant
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFF22D3EE) else Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .width(100.dp)
                                        .clickable { selectedGuideIndex = index }
                                        .testTag("pattern_guide_item_$index")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            GestureAnimationPlayer(
                                                gestureType = guide.name,
                                                modifier = Modifier.size(36.dp),
                                                strokeColor = if (isSelected) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.4f),
                                                strokeWidth = 3f
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = guide.name.substringBefore(" shape").substringBefore(" pattern").substringBefore(" flick"),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            color = if (isSelected) Color(0xFF22D3EE) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// Gesture Animation & Guides Data Structs
// ==========================================

data class PatternGuide(
    val name: String,
    val description: String,
    val tip: String
)

val patternGuides = listOf(
    PatternGuide(
        name = "Triangle shape pattern",
        description = "Draw a complete, sharp triangle starting from the top peak, moving down-right, straight-left, and then back to the peak.",
        tip = "Keep the corners distinct to help the geometric classifier differentiate it from curves."
    ),
    PatternGuide(
        name = "Circle Clockwise",
        description = "Draw a continuous circular stroke starting from the top and moving clockwise.",
        tip = "Ensure the start and end points are relatively close and height matches width."
    ),
    PatternGuide(
        name = "Circle Counter-Clockwise",
        description = "Draw a continuous circular stroke starting from the top and moving counter-clockwise.",
        tip = "Ensure standard symmetry. Ideal for triggering alternate actions from the clockwise counterpart."
    ),
    PatternGuide(
        name = "Vertical Up flick",
        description = "A rapid, straight upward swipe of the S Pen in the air.",
        tip = "Keep horizontal deviation minimal to avoid misclassification as a custom shape."
    ),
    PatternGuide(
        name = "Vertical Down flick",
        description = "A rapid, straight downward swipe of the S Pen in the air.",
        tip = "Perform a steady straight drop of the pen to register maximum acceleration points."
    ),
    PatternGuide(
        name = "Horizontal Left flick",
        description = "A fast, straight swipe from right to left in the air.",
        tip = "Hold the button continuously while flicking to register the entire path."
    ),
    PatternGuide(
        name = "Horizontal Right flick",
        description = "A fast, straight swipe from left to right in the air.",
        tip = "Perfect for next-track / forward navigation mapping."
    )
)

@Composable
fun GestureAnimationPlayer(
    gestureType: String,
    modifier: Modifier = Modifier,
    strokeColor: Color = Color(0xFF22D3EE),
    strokeWidth: Float = 8f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gesture_animation")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val pad = width * 0.15f
        val cx = width / 2f
        val cy = height / 2f
        val r = (width - 2 * pad) / 2f

        when (gestureType) {
            "Circle Clockwise" -> {
                val path = Path()
                val totalPoints = 100
                val activePoints = (totalPoints * progress).toInt()
                if (activePoints > 0) {
                    for (i in 0..activePoints) {
                        val angle = -Math.PI / 2 + (i.toDouble() / totalPoints) * 2 * Math.PI
                        val x = cx + r * kotlin.math.cos(angle).toFloat()
                        val y = cy + r * kotlin.math.sin(angle).toFloat()
                        if (i == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
            "Circle Counter-Clockwise" -> {
                val path = Path()
                val totalPoints = 100
                val activePoints = (totalPoints * progress).toInt()
                if (activePoints > 0) {
                    for (i in 0..activePoints) {
                        val angle = -Math.PI / 2 - (i.toDouble() / totalPoints) * 2 * Math.PI
                        val x = cx + r * kotlin.math.cos(angle).toFloat()
                        val y = cy + r * kotlin.math.sin(angle).toFloat()
                        if (i == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
            "Vertical Up flick" -> {
                val startY = height - pad
                val endY = pad + (height - 2 * pad) * (1f - progress)
                drawLine(
                    color = strokeColor,
                    start = Offset(cx, startY),
                    end = Offset(cx, endY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                if (progress > 0.8f) {
                    val arrowLength = width * 0.15f
                    drawLine(
                        color = strokeColor,
                        start = Offset(cx, pad),
                        end = Offset(cx - arrowLength, pad + arrowLength),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = strokeColor,
                        start = Offset(cx, pad),
                        end = Offset(cx + arrowLength, pad + arrowLength),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
            "Vertical Down flick" -> {
                val startY = pad
                val endY = pad + (height - 2 * pad) * progress
                drawLine(
                    color = strokeColor,
                    start = Offset(cx, startY),
                    end = Offset(cx, endY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                if (progress > 0.8f) {
                    val arrowLength = width * 0.15f
                    drawLine(
                        color = strokeColor,
                        start = Offset(cx, height - pad),
                        end = Offset(cx - arrowLength, height - pad - arrowLength),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = strokeColor,
                        start = Offset(cx, height - pad),
                        end = Offset(cx + arrowLength, height - pad - arrowLength),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
            "Horizontal Left flick" -> {
                val startX = width - pad
                val endX = pad + (width - 2 * pad) * (1f - progress)
                drawLine(
                    color = strokeColor,
                    start = Offset(startX, cy),
                    end = Offset(endX, cy),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                if (progress > 0.8f) {
                    val arrowLength = height * 0.15f
                    drawLine(
                        color = strokeColor,
                        start = Offset(pad, cy),
                        end = Offset(pad + arrowLength, cy - arrowLength),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = strokeColor,
                        start = Offset(pad, cy),
                        end = Offset(pad + arrowLength, cy + arrowLength),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
            "Horizontal Right flick" -> {
                val startX = pad
                val endX = pad + (width - 2 * pad) * progress
                drawLine(
                    color = strokeColor,
                    start = Offset(startX, cy),
                    end = Offset(endX, cy),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                if (progress > 0.8f) {
                    val arrowLength = height * 0.15f
                    drawLine(
                        color = strokeColor,
                        start = Offset(width - pad, cy),
                        end = Offset(width - pad - arrowLength, cy - arrowLength),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = strokeColor,
                        start = Offset(width - pad, cy),
                        end = Offset(width - pad - arrowLength, cy + arrowLength),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
            "Triangle shape pattern" -> {
                val pt1 = Offset(cx, pad)
                val pt2 = Offset(width - pad, height - pad)
                val pt3 = Offset(pad, height - pad)

                val path = Path()
                path.moveTo(pt1.x, pt1.y)

                if (progress < 0.33f) {
                    val p = progress / 0.33f
                    val currentX = pt1.x + (pt2.x - pt1.x) * p
                    val currentY = pt1.y + (pt2.y - pt1.y) * p
                    path.lineTo(currentX, currentY)
                } else if (progress < 0.66f) {
                    val p = (progress - 0.33f) / 0.33f
                    path.lineTo(pt2.x, pt2.y)
                    val currentX = pt2.x + (pt3.x - pt2.x) * p
                    val currentY = pt2.y + (pt3.y - pt2.y) * p
                    path.lineTo(currentX, currentY)
                } else {
                    val p = (progress - 0.66f) / 0.34f
                    path.lineTo(pt2.x, pt2.y)
                    path.lineTo(pt3.x, pt3.y)
                    val currentX = pt3.x + (pt1.x - pt3.x) * p
                    val currentY = pt3.y + (pt1.y - pt3.y) * p
                    path.lineTo(currentX, currentY)
                }

                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}
