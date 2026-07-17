package com.example.data

import kotlinx.coroutines.flow.Flow

class SPenRepository(private val sPenDao: SPenDao) {

    val allProfiles: Flow<List<AppProfile>> = sPenDao.getAllProfiles()

    fun getMappingsForProfile(packageName: String): Flow<List<GestureMapping>> {
        return sPenDao.getMappingsForProfile(packageName)
    }

    suspend fun getProfile(packageName: String): AppProfile? {
        return sPenDao.getProfile(packageName)
    }

    suspend fun getMappingsForProfileSync(packageName: String): List<GestureMapping> {
        return sPenDao.getMappingsForProfileSync(packageName)
    }

    suspend fun getMappingSync(packageName: String, triggerType: String): GestureMapping? {
        return sPenDao.getMappingSync(packageName, triggerType)
    }

    suspend fun insertProfile(profile: AppProfile) {
        sPenDao.insertProfile(profile)
        // Ensure default blank mappings exist if none are defined
        val existing = sPenDao.getMappingsForProfileSync(profile.packageName)
        if (existing.isEmpty()) {
            val defaults = SPenTriggers.ALL.map { trigger ->
                GestureMapping(
                    profilePackageName = profile.packageName,
                    triggerType = trigger,
                    actionType = SPenActions.TYPE_NONE,
                    actionValue = "",
                    vibrationPattern = "NONE",
                    vibrationIntensity = 0
                )
            }
            sPenDao.insertMappings(defaults)
        }
    }

    suspend fun deleteProfile(packageName: String) {
        sPenDao.deleteProfile(packageName)
    }

    suspend fun insertMapping(mapping: GestureMapping) {
        sPenDao.insertMapping(mapping)
    }

    suspend fun insertMappings(mappings: List<GestureMapping>) {
        sPenDao.insertMappings(mappings)
    }

    private data class DefaultMapping(
        val actionType: String,
        val actionValue: String,
        val vibPattern: String,
        val intensity: Int
    )

    suspend fun resetProfileToDefaults(packageName: String) {
        sPenDao.clearMappingsForProfile(packageName)
        val defaults = SPenTriggers.ALL.map { trigger ->
            val defaultMapping = when (packageName) {
                AppProfile.GLOBAL_PACKAGE -> when (trigger) {
                    SPenTriggers.SINGLE_CLICK -> DefaultMapping(SPenActions.TYPE_MEDIA, SPenActions.VAL_MEDIA_PLAY_PAUSE, "TICK", 40)
                    SPenTriggers.DOUBLE_CLICK -> DefaultMapping(SPenActions.TYPE_SYSTEM, SPenActions.VAL_SYSTEM_BACK, "DOUBLE_TICK", 60)
                    SPenTriggers.TRIPLE_CLICK -> DefaultMapping(SPenActions.TYPE_SYSTEM, SPenActions.VAL_SYSTEM_RECENTS, "PULSE", 70)
                    SPenTriggers.QUADRUPLE_CLICK -> DefaultMapping(SPenActions.TYPE_SYSTEM, SPenActions.VAL_SYSTEM_NOTIFICATIONS, "PULSE", 80)
                    SPenTriggers.LONG_PRESS -> DefaultMapping(SPenActions.TYPE_SYSTEM, SPenActions.VAL_SYSTEM_HOME, "HEAVY_CLICK", 90)
                    SPenTriggers.FLICK_UP -> DefaultMapping(SPenActions.TYPE_MEDIA, SPenActions.VAL_MEDIA_VOLUME_UP, "TICK", 30)
                    SPenTriggers.FLICK_DOWN -> DefaultMapping(SPenActions.TYPE_MEDIA, SPenActions.VAL_MEDIA_VOLUME_DOWN, "TICK", 30)
                    SPenTriggers.FLICK_LEFT -> DefaultMapping(SPenActions.TYPE_MEDIA, SPenActions.VAL_MEDIA_PREVIOUS, "DOUBLE_TICK", 40)
                    SPenTriggers.FLICK_RIGHT -> DefaultMapping(SPenActions.TYPE_MEDIA, SPenActions.VAL_MEDIA_NEXT, "DOUBLE_TICK", 40)
                    SPenTriggers.CIRCLE_CW -> DefaultMapping(SPenActions.TYPE_SYSTEM, SPenActions.VAL_SYSTEM_QUICK_SETTINGS, "HEAVY_CLICK", 70)
                    SPenTriggers.CIRCLE_CCW -> DefaultMapping(SPenActions.TYPE_SYSTEM, SPenActions.VAL_SYSTEM_SPLIT_SCREEN, "HEAVY_CLICK", 75)
                    else -> DefaultMapping(SPenActions.TYPE_NONE, "", "NONE", 0)
                }
                else -> DefaultMapping(SPenActions.TYPE_NONE, "", "NONE", 0)
            }
            GestureMapping(
                profilePackageName = packageName,
                triggerType = trigger,
                actionType = defaultMapping.actionType,
                actionValue = defaultMapping.actionValue,
                vibrationPattern = defaultMapping.vibPattern,
                vibrationIntensity = defaultMapping.intensity
            )
        }
        sPenDao.insertMappings(defaults)
    }
}
