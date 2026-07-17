package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppProfile
import com.example.data.GestureMapping
import com.example.data.SPenRepository
import com.example.data.SPenTriggers
import com.example.data.SPenActions
import com.example.service.AdbManager
import com.example.service.SPenAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: SPenRepository,
    context: android.content.Context
) : ViewModel() {

    val adbManager = AdbManager(context)

    private val sharedPrefs = context.getSharedPreferences("spen_settings", android.content.Context.MODE_PRIVATE)

    // Onboarding overlay visibility
    private val _showOnboarding = MutableStateFlow(!sharedPrefs.getBoolean("onboarding_completed", false))
    val showOnboarding = _showOnboarding.asStateFlow()

    fun completeOnboarding() {
        _showOnboarding.value = false
        sharedPrefs.edit().putBoolean("onboarding_completed", true).apply()
    }

    fun resetOnboarding() {
        _showOnboarding.value = true
        sharedPrefs.edit().putBoolean("onboarding_completed", false).apply()
    }

    // Gesture sensitivity (10 to 100, default is 50)
    private val _gestureSensitivity = MutableStateFlow(sharedPrefs.getInt("gesture_sensitivity", 50))
    val gestureSensitivity = _gestureSensitivity.asStateFlow()

    fun updateGestureSensitivity(sensitivity: Int) {
        _gestureSensitivity.value = sensitivity
        sharedPrefs.edit().putInt("gesture_sensitivity", sensitivity).apply()
    }

    // Selected profile package name ("global" by default)
    private val _selectedProfilePackageName = MutableStateFlow(AppProfile.GLOBAL_PACKAGE)
    val selectedProfilePackageName = _selectedProfilePackageName.asStateFlow()

    // Retrieve all app profiles
    val allProfiles: StateFlow<List<AppProfile>> = repository.allProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Retrieve mappings of currently selected profile
    val activeProfileMappings: StateFlow<List<GestureMapping>> = _selectedProfilePackageName
        .flatMapLatest { packageName ->
            repository.getMappingsForProfile(packageName)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Retrieve global mappings to cross-reference overrides
    val globalMappings: StateFlow<List<GestureMapping>> = repository.getMappingsForProfile(AppProfile.GLOBAL_PACKAGE)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reactive conflict logic engine warnings flow
    val mappingConflicts: StateFlow<List<MappingConflict>> = combine(
        _selectedProfilePackageName,
        activeProfileMappings,
        globalMappings
    ) { selectedPackage, activeMappings, globalMappings ->
        detectConflicts(selectedPackage, activeMappings, globalMappings)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Background engine diagnostics (polled for UI updates)
    private val _serviceStatusRunning = MutableStateFlow(false)
    val serviceStatusRunning = _serviceStatusRunning.asStateFlow()

    private val _lastCapturedKeycode = MutableStateFlow(-1)
    val lastCapturedKeycode = _lastCapturedKeycode.asStateFlow()

    private val _lastExecutedAction = MutableStateFlow("None")
    val lastExecutedAction = _lastExecutedAction.asStateFlow()

    private val _activeForegroundApp = MutableStateFlow("global")
    val activeForegroundApp = _activeForegroundApp.asStateFlow()

    private val _boundKeycode = MutableStateFlow(104) // 104 is commonly STYLUS_BUTTON
    val boundKeycode = _boundKeycode.asStateFlow()

    // Cloud Sync States
    sealed interface SyncState {
        object Idle : SyncState
        object Syncing : SyncState
        data class Success(val timestamp: Long) : SyncState
        data class Error(val message: String) : SyncState
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    init {
        // Poll S-Pen Service state every 1 second to update UI diagnostics
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                // Determine if permission is granted as well as service being active
                val isPermissionGranted = SPenAccessibilityService.isAccessibilityPermissionGranted(context)
                val isServiceActive = SPenAccessibilityService.isServiceRunning
                _serviceStatusRunning.value = isPermissionGranted && isServiceActive

                _lastCapturedKeycode.value = SPenAccessibilityService.lastCapturedKeycode
                _lastExecutedAction.value = SPenAccessibilityService.lastExecutedAction
                _activeForegroundApp.value = SPenAccessibilityService.activeForegroundApp
                _boundKeycode.value = SPenAccessibilityService.getBoundKeycode()
                delay(1000)
            }
        }
    }

    fun selectProfile(packageName: String) {
        _selectedProfilePackageName.value = packageName
    }

    fun toggleProfileActive(profile: AppProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertProfile(profile.copy(isActive = !profile.isActive))
        }
    }

    fun addNewProfile(packageName: String, appName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanPackage = packageName.trim().lowercase()
            val cleanName = appName.trim()
            if (cleanPackage.isNotEmpty() && cleanName.isNotEmpty()) {
                repository.insertProfile(
                    AppProfile(
                        packageName = cleanPackage,
                        appName = cleanName,
                        isActive = true,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun deleteProfile(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (packageName != AppProfile.GLOBAL_PACKAGE) {
                repository.deleteProfile(packageName)
                if (_selectedProfilePackageName.value == packageName) {
                    _selectedProfilePackageName.value = AppProfile.GLOBAL_PACKAGE
                }
            }
        }
    }

    fun updateMapping(mapping: GestureMapping) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMapping(mapping)
        }
    }

    fun resetProfileToDefaults(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetProfileToDefaults(packageName)
        }
    }

    fun setBoundKeycode(keycode: Int) {
        SPenAccessibilityService.updateBoundKeycode(keycode)
        _boundKeycode.value = keycode
    }

    fun simulateCloudBackup() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            delay(1800) // Simulate web connection delay
            _syncState.value = SyncState.Success(System.currentTimeMillis())
        }
    }

    fun simulateCloudRestore() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            delay(1800) // Simulate network recovery delay
            // Successfully simulated restore
            _syncState.value = SyncState.Success(System.currentTimeMillis())
        }
    }

    fun clearSyncState() {
        _syncState.value = SyncState.Idle
    }

    fun detectConflicts(
        selectedPackage: String,
        activeMappings: List<GestureMapping>,
        globalMappings: List<GestureMapping>
    ): List<MappingConflict> {
        val conflicts = mutableListOf<MappingConflict>()

        // Filter out NONE action types for our calculations
        val activeNonNone = activeMappings.filter { it.actionType != SPenActions.TYPE_NONE }
        val globalNonNone = globalMappings.filter { it.actionType != SPenActions.TYPE_NONE }

        // 1. Action Duplication (Redundancy Warning)
        val actionGroups = activeNonNone.groupBy { "${it.actionType}:${it.actionValue}" }
        actionGroups.forEach { (actionKey, mappingsWithSameAction) ->
            if (mappingsWithSameAction.size > 1) {
                val actionParts = actionKey.split(":")
                val actionType = actionParts.getOrNull(0) ?: ""
                val actionValue = actionParts.getOrNull(1) ?: ""
                val actionLabel = SPenActions.getActionLabel(actionType, actionValue)
                val triggers = mappingsWithSameAction.map { it.triggerType }
                val triggerLabels = triggers.map { SPenTriggers.getLabel(it) }.joinToString(", ")

                conflicts.add(
                    MappingConflict(
                        id = "redundant_${actionKey}",
                        severity = ConflictSeverity.WARNING,
                        title = "Redundant Action Mapped",
                        description = "Multiple S Pen gestures ($triggerLabels) are mapped to trigger the same action '$actionLabel'. This wastes gesture recognition slots.",
                        affectedTriggers = triggers
                    )
                )
            }
        }

        // 2. Global Mapping Overrides
        if (selectedPackage != AppProfile.GLOBAL_PACKAGE) {
            activeNonNone.forEach { activeMapping ->
                val globalMatching = globalNonNone.find { it.triggerType == activeMapping.triggerType }
                if (globalMatching != null) {
                    if (activeMapping.actionType != globalMatching.actionType || activeMapping.actionValue != globalMatching.actionValue) {
                        val activeLabel = SPenActions.getActionLabel(activeMapping.actionType, activeMapping.actionValue)
                        val globalLabel = SPenActions.getActionLabel(globalMatching.actionType, globalMatching.actionValue)
                        val triggerLabel = SPenTriggers.getLabel(activeMapping.triggerType)

                        conflicts.add(
                            MappingConflict(
                                id = "override_${activeMapping.triggerType}",
                                severity = ConflictSeverity.INFO,
                                title = "Global Action Override",
                                description = "Gesture '$triggerLabel' maps to '$activeLabel' here, but maps to '$globalLabel' globally. When this app is open, global behavior is overridden.",
                                affectedTriggers = listOf(activeMapping.triggerType)
                            )
                        )
                    }
                }
            }
        }

        // 3. Destructive/Disruptive Action Risks (Accidental Triggers)
        activeNonNone.forEach { mapping ->
            if (mapping.triggerType == SPenTriggers.SINGLE_CLICK || mapping.triggerType == SPenTriggers.DOUBLE_CLICK) {
                val actionType = mapping.actionType
                val actionValue = mapping.actionValue
                val triggerLabel = SPenTriggers.getLabel(mapping.triggerType)

                val isDisruptive = when {
                    actionType == SPenActions.TYPE_SYSTEM && actionValue == SPenActions.VAL_SYSTEM_LOCK_SCREEN -> true
                    actionType == SPenActions.TYPE_SYSTEM && actionValue == SPenActions.VAL_SYSTEM_POWER_DIALOG -> true
                    actionType == SPenActions.TYPE_LAUNCH_APP -> true
                    actionType == SPenActions.TYPE_MACRO -> true
                    else -> false
                }

                if (isDisruptive) {
                    val actionLabel = SPenActions.getActionLabel(actionType, actionValue)
                    conflicts.add(
                        MappingConflict(
                            id = "disruptive_${mapping.triggerType}",
                            severity = ConflictSeverity.HIGH,
                            title = "Accidental Trigger Risk",
                            description = "Mapping '$triggerLabel' to a high-disruption action like '$actionLabel' can cause unintended lockouts or app launches during S Pen handling.",
                            affectedTriggers = listOf(mapping.triggerType)
                        )
                    )
                }
            }
        }

        // 4. Opposing Toggle State Logic Conflict
        val activeTypesAndValues = activeNonNone.map { it.actionType to it.actionValue }

        // DND conflicts
        val hasDndToggle = activeTypesAndValues.contains(SPenActions.TYPE_DND to SPenActions.VAL_DND_TOGGLE)
        val hasDndEnable = activeTypesAndValues.contains(SPenActions.TYPE_DND to SPenActions.VAL_DND_ENABLE)
        val hasDndDisable = activeTypesAndValues.contains(SPenActions.TYPE_DND to SPenActions.VAL_DND_DISABLE)
        if (hasDndToggle && (hasDndEnable || hasDndDisable)) {
            val affected = activeNonNone.filter { it.actionType == SPenActions.TYPE_DND }.map { it.triggerType }
            conflicts.add(
                MappingConflict(
                    id = "conflict_dnd",
                    severity = ConflictSeverity.WARNING,
                    title = "Conflicting Toggle Logic",
                    description = "You have mapped both a general Toggle and a specific Enable/Disable state for 'Do Not Disturb'. This can create out-of-sync state triggers.",
                    affectedTriggers = affected
                )
            )
        }

        // Rotation conflicts
        val hasRotToggle = activeTypesAndValues.contains(SPenActions.TYPE_AUTO_ROTATE to SPenActions.VAL_ROTATION_TOGGLE)
        val hasRotEnable = activeTypesAndValues.contains(SPenActions.TYPE_AUTO_ROTATE to SPenActions.VAL_ROTATION_ENABLE)
        val hasRotDisable = activeTypesAndValues.contains(SPenActions.TYPE_AUTO_ROTATE to SPenActions.VAL_ROTATION_DISABLE)
        if (hasRotToggle && (hasRotEnable || hasRotDisable)) {
            val affected = activeNonNone.filter { it.actionType == SPenActions.TYPE_AUTO_ROTATE }.map { it.triggerType }
            conflicts.add(
                MappingConflict(
                    id = "conflict_rotation",
                    severity = ConflictSeverity.WARNING,
                    title = "Conflicting Rotation Settings",
                    description = "Mapping both Auto-Rotate Toggle and specific Enable/Disable commands in the same profile creates redundant/conflicting actions.",
                    affectedTriggers = affected
                )
            )
        }

        // Torch conflicts
        val hasTorchToggle = activeTypesAndValues.contains(SPenActions.TYPE_TORCH to SPenActions.VAL_TORCH_TOGGLE)
        val hasTorchEnable = activeTypesAndValues.contains(SPenActions.TYPE_TORCH to SPenActions.VAL_TORCH_ENABLE)
        val hasTorchDisable = activeTypesAndValues.contains(SPenActions.TYPE_TORCH to SPenActions.VAL_TORCH_DISABLE)
        if (hasTorchToggle && (hasTorchEnable || hasTorchDisable)) {
            val affected = activeNonNone.filter { it.actionType == SPenActions.TYPE_TORCH }.map { it.triggerType }
            conflicts.add(
                MappingConflict(
                    id = "conflict_torch",
                    severity = ConflictSeverity.WARNING,
                    title = "Conflicting Flashlight Controls",
                    description = "Both Flashlight Toggle and direct Flashlight Enable/Disable actions are mapped. This may lead to out-of-sync flashlight states.",
                    affectedTriggers = affected
                )
            )
        }

        return conflicts
    }
}

enum class ConflictSeverity {
    INFO,
    WARNING,
    HIGH
}

data class MappingConflict(
    val id: String,
    val severity: ConflictSeverity,
    val title: String,
    val description: String,
    val affectedTriggers: List<String>
)

class DashboardViewModelFactory(
    private val repository: SPenRepository,
    private val context: android.content.Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
