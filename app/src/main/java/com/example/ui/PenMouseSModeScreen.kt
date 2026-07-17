package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElegantDarkBg
import com.example.ui.theme.ElegantDarkHighlight
import com.example.ui.theme.SteelSurface
import com.example.ui.theme.SteelSurfaceVariant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PenMouseSModeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. Mouse State variables
    var isMouseActive by remember { mutableStateOf(true) }
    var pointerSpeed by remember { mutableStateOf(4.5f) }
    var scrollSpeed by remember { mutableStateOf(2.5f) }
    var cursorType by remember { mutableStateOf("Rounded Laser Dot") }
    var pointerSize by remember { mutableStateOf("Medium") }
    var pointerTrailsEnabled by remember { mutableStateOf(true) }

    // Colors
    val colorsList = listOf(
        Color(0xFFD0BCFF) to "Cyber Violet",
        Color(0xFF4ADE80) to "Bright Green",
        Color(0xFF22D3EE) to "Cyber Cyan",
        Color(0xFFF472B6) to "Sunset Pink",
        Color(0xFFFB923C) to "Sunset Orange",
        Color(0xFFFFFFFF) to "Classic White"
    )
    var selectedColorIndex by remember { mutableStateOf(2) } // Cyber Cyan

    // Mouse Button actions
    var clickAction by remember { mutableStateOf("Left Click") }
    var doubleClickAction by remember { mutableStateOf("Double Left Click") }
    var longPressAction by remember { mutableStateOf("Start/Stop Drag (Toggle)") }

    // Scroll settings
    var scrollDirection by remember { mutableStateOf("2D Omnidirectional") }
    var frictionLevel by remember { mutableStateOf("Medium") }

    // Calibration and precision states
    var isCalibrating by remember { mutableStateOf(false) }
    var autoCentering by remember { mutableStateOf(true) }
    var edgeBumping by remember { mutableStateOf(true) }

    // Animation for calibrating spinner
    val transition = rememberInfiniteTransition(label = "CalibrateSpinner")
    val spinAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinnerAngle"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ElegantDarkBg)
    ) {
        // Immersive Header Top Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "PenMouse S Mode",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Samsung S Pen Spatial Gyro Mouse",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("mouse_back_button")
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
            ),
            actions = {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .background(
                            if (isMouseActive) Color(0xFF4CAF50).copy(alpha = 0.15f)
                            else Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(100.dp)
                        )
                        .border(
                            1.dp,
                            if (isMouseActive) Color(0xFF4CAF50).copy(alpha = 0.3f)
                            else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isMouseActive) Color(0xFF4CAF50) else Color.Gray,
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (isMouseActive) "AIR POINTER ACTIVE" else "AIR POINTER IDLE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isMouseActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
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
            // 1. Air Mouse Service Control
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
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Mouse,
                                    contentDescription = "Mouse",
                                    tint = Color(0xFF22D3EE),
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "S Pen Gyro Motion Input",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Translate spatial gestures to screen cursor",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Switch(
                                checked = isMouseActive,
                                onCheckedChange = {
                                    isMouseActive = it
                                    Toast.makeText(
                                        context,
                                        if (it) "PenMouse S Mode Enabled" else "PenMouse S Mode Disabled",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.testTag("mouse_active_switch")
                            )
                        }
                    }
                }
            }

            // 2. Cursor Motion & Sensitivity Card
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
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = "Speed",
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Pointer Motion & Aesthetics",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Pointer Speed
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Cursor Velocity / DPI Sensitivity",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Text(
                                text = String.format("%.1fx", pointerSpeed),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF22D3EE)
                            )
                        }
                        Slider(
                            value = pointerSpeed,
                            onValueChange = { pointerSpeed = it },
                            valueRange = 1.0f..10.0f,
                            steps = 17,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF22D3EE),
                                activeTrackColor = Color(0xFF22D3EE),
                                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Pointer Size Selector
                        Text(
                            text = "Cursor Visual Size",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val sizes = listOf("Small", "Medium", "Large", "Dynamic Scale")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            sizes.forEach { size ->
                                val isSelected = pointerSize == size
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) ElegantDarkHighlight else Color.Black.copy(alpha = 0.1f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFFD0BCFF) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { pointerSize = size }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = size,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) Color(0xFFD0BCFF) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Pointer Color Swatch Selector
                        Text(
                            text = "Cursor Aura Color",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            colorsList.forEachIndexed { idx, (col, label) ->
                                val isSelected = selectedColorIndex == idx
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .padding(4.dp)
                                        .background(col, CircleShape)
                                        .clip(CircleShape)
                                        .clickable {
                                            selectedColorIndex = idx
                                        }
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = colorsList[selectedColorIndex].second,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Cursor Stylus Shape Dropdown
                        var cursorMenuExpanded by remember { mutableStateOf(false) }
                        Text(
                            text = "Pointer Cursor Shape Model",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { cursorMenuExpanded = true },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Black.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = when (cursorType) {
                                                "Classic Arrow" -> Icons.Rounded.Navigation
                                                "Rounded Laser Dot" -> Icons.Rounded.FilterCenterFocus
                                                "Crosshair Reticle" -> Icons.Rounded.Adjust
                                                else -> Icons.Rounded.PointOfSale
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = colorsList[selectedColorIndex].first
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(cursorType, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDropDown,
                                        contentDescription = "Dropdown",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = cursorMenuExpanded,
                                onDismissRequest = { cursorMenuExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(SteelSurfaceVariant)
                            ) {
                                val options = listOf("Classic Arrow", "Rounded Laser Dot", "Crosshair Reticle", "Stylus Pen Tip")
                                options.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            cursorType = option
                                            cursorMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Pointer Trails Toggle
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Hover Pointer Trails",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Draw neon dynamic trailing lines under cursor",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Switch(
                                checked = pointerTrailsEnabled,
                                onCheckedChange = { pointerTrailsEnabled = it }
                            )
                        }
                    }
                }
            }

            // 3. Mouse Buttons Mapping Card
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
                                imageVector = Icons.Rounded.TouchApp,
                                contentDescription = "Buttons",
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Button Mapping Settings",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Single Click Mapping
                        MouseActionRow(
                            triggerLabel = "Single Click / Pen Tap",
                            currentAction = clickAction,
                            options = listOf("Left Click", "Double Left Click", "Select Word", "None"),
                            onActionSelected = { clickAction = it }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Double Click Mapping
                        MouseActionRow(
                            triggerLabel = "Double Click",
                            currentAction = doubleClickAction,
                            options = listOf("Right Click", "Middle Click", "Toggle Scroll Mode", "None"),
                            onActionSelected = { doubleClickAction = it }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Long Press Mapping
                        MouseActionRow(
                            triggerLabel = "Long Press",
                            currentAction = longPressAction,
                            options = listOf("Start/Stop Drag (Toggle)", "System Home", "Lock Screen Pointer", "None"),
                            onActionSelected = { longPressAction = it }
                        )
                    }
                }
            }

            // 4. Scrolling and Friction Card
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
                                imageVector = Icons.Rounded.SwapVert,
                                contentDescription = "Scroll",
                                tint = Color(0xFFFB923C),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Air Scrolling & Friction",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Scroll Speed
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Scroll Force Multiplier",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Text(
                                text = String.format("%.1fx", scrollSpeed),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFB923C)
                            )
                        }
                        Slider(
                            value = scrollSpeed,
                            onValueChange = { scrollSpeed = it },
                            valueRange = 1.0f..5.0f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFB923C),
                                activeTrackColor = Color(0xFFFB923C),
                                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Scroll Orientation Dropdown
                        var scrollDirExpanded by remember { mutableStateOf(false) }
                        Text(
                            text = "Scroll Orientation Constraints",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { scrollDirExpanded = true },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Black.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(scrollDirection, color = MaterialTheme.colorScheme.onSurface)
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDropDown,
                                        contentDescription = "Dropdown",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = scrollDirExpanded,
                                onDismissRequest = { scrollDirExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(SteelSurfaceVariant)
                            ) {
                                val dirs = listOf("Vertical Scroll Only", "Horizontal Scroll Only", "2D Omnidirectional")
                                dirs.forEach { dir ->
                                    DropdownMenuItem(
                                        text = { Text(dir) },
                                        onClick = {
                                            scrollDirection = dir
                                            scrollDirExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Inertia Friction Selection
                        Text(
                            text = "Scroll Inertial Friction",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val frictions = listOf("None", "Low", "Medium", "High")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            frictions.forEach { f ->
                                val isSelected = frictionLevel == f
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) ElegantDarkHighlight else Color.Black.copy(alpha = 0.1f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFFFB923C) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { frictionLevel = f }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = f,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) Color(0xFFFB923C) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Gyro Calibration & Precision Core
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
                                imageVector = Icons.Rounded.SettingsBackupRestore,
                                contentDescription = "Calibration",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Gyro Sensor Core & Calibration",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Calibrating the IMU gyro chip inside your S Pen maintains high positional accuracy and eliminates drifting of the cursor across DeX/Screen layouts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dynamic Centering & Edge Action Toggles
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Auto-Centering Alignment",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Auto align cursor center on S Pen double tap",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Switch(
                                checked = autoCentering,
                                onCheckedChange = { autoCentering = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Screen Edge Actions",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Haptic pulse when cursor pushes boundary borders",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Switch(
                                checked = edgeBumping,
                                onCheckedChange = { edgeBumping = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Calibration Action
                        AnimatedVisibility(visible = isCalibrating) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Autorenew,
                                    contentDescription = "Calibrating",
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .rotate(spinAngle)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Recalibrating S Pen Gyroscope...",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFFF9800)
                                )
                                Text(
                                    text = "Please place your S Pen flat on a stable surface.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }

                        if (!isCalibrating) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isCalibrating = true
                                        delay(2500)
                                        isCalibrating = false
                                        Toast.makeText(context, "S Pen Gyroscope Recalibrated Perfectly!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("calibrate_gyro_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CompassCalibration,
                                    contentDescription = "Calibrate",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Recalibrate PenMouse Gyro Core", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MouseActionRow(
    triggerLabel: String,
    currentAction: String,
    options: List<String>,
    onActionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = triggerLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Black.copy(alpha = 0.1f))
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(currentAction, color = MaterialTheme.colorScheme.onSurface)
                    Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = "Dropdown",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(SteelSurfaceVariant)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onActionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
