package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [AppProfile::class, GestureMapping::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sPenDao(): SPenDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spen_command_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        prepopulateDatabase(database.sPenDao())
                    }
                }
            }

            private suspend fun prepopulateDatabase(dao: SPenDao) {
                // 1. Create global profile
                val globalProfile = AppProfile(
                    packageName = AppProfile.GLOBAL_PACKAGE,
                    appName = "Global Default",
                    isActive = true
                )
                dao.insertProfile(globalProfile)

                // 2. Create global default mappings
                val globalMappings = listOf(
                    GestureMapping(
                        profilePackageName = AppProfile.GLOBAL_PACKAGE,
                        triggerType = SPenTriggers.SINGLE_CLICK,
                        actionType = SPenActions.TYPE_MEDIA,
                        actionValue = SPenActions.VAL_MEDIA_PLAY_PAUSE,
                        vibrationPattern = "TICK",
                        vibrationIntensity = 40
                    ),
                    GestureMapping(
                        profilePackageName = AppProfile.GLOBAL_PACKAGE,
                        triggerType = SPenTriggers.DOUBLE_CLICK,
                        actionType = SPenActions.TYPE_SYSTEM,
                        actionValue = SPenActions.VAL_SYSTEM_BACK,
                        vibrationPattern = "DOUBLE_TICK",
                        vibrationIntensity = 60
                    ),
                    GestureMapping(
                        profilePackageName = AppProfile.GLOBAL_PACKAGE,
                        triggerType = SPenTriggers.TRIPLE_CLICK,
                        actionType = SPenActions.TYPE_SYSTEM,
                        actionValue = SPenActions.VAL_SYSTEM_RECENTS,
                        vibrationPattern = "PULSE",
                        vibrationIntensity = 70
                    ),
                    GestureMapping(
                        profilePackageName = AppProfile.GLOBAL_PACKAGE,
                        triggerType = SPenTriggers.QUADRUPLE_CLICK,
                        actionType = SPenActions.TYPE_SYSTEM,
                        actionValue = SPenActions.VAL_SYSTEM_NOTIFICATIONS,
                        vibrationPattern = "PULSE",
                        vibrationIntensity = 80
                    ),
                    GestureMapping(
                        profilePackageName = AppProfile.GLOBAL_PACKAGE,
                        triggerType = SPenTriggers.LONG_PRESS,
                        actionType = SPenActions.TYPE_SYSTEM,
                        actionValue = SPenActions.VAL_SYSTEM_HOME,
                        vibrationPattern = "HEAVY_CLICK",
                        vibrationIntensity = 90
                    ),
                    GestureMapping(
                        profilePackageName = AppProfile.GLOBAL_PACKAGE,
                        triggerType = SPenTriggers.FLICK_UP,
                        actionType = SPenActions.TYPE_MEDIA,
                        actionValue = SPenActions.VAL_MEDIA_VOLUME_UP,
                        vibrationPattern = "TICK",
                        vibrationIntensity = 30
                    ),
                    GestureMapping(
                        profilePackageName = AppProfile.GLOBAL_PACKAGE,
                        triggerType = SPenTriggers.FLICK_DOWN,
                        actionType = SPenActions.TYPE_MEDIA,
                        actionValue = SPenActions.VAL_MEDIA_VOLUME_DOWN,
                        vibrationPattern = "TICK",
                        vibrationIntensity = 30
                    ),
                    GestureMapping(
                        profilePackageName = AppProfile.GLOBAL_PACKAGE,
                        triggerType = SPenTriggers.FLICK_LEFT,
                        actionType = SPenActions.TYPE_MEDIA,
                        actionValue = SPenActions.VAL_MEDIA_PREVIOUS,
                        vibrationPattern = "DOUBLE_TICK",
                        vibrationIntensity = 40
                    ),
                    GestureMapping(
                        profilePackageName = AppProfile.GLOBAL_PACKAGE,
                        triggerType = SPenTriggers.FLICK_RIGHT,
                        actionType = SPenActions.TYPE_MEDIA,
                        actionValue = SPenActions.VAL_MEDIA_NEXT,
                        vibrationPattern = "DOUBLE_TICK",
                        vibrationIntensity = 40
                    ),
                    GestureMapping(
                        profilePackageName = AppProfile.GLOBAL_PACKAGE,
                        triggerType = SPenTriggers.CIRCLE_CW,
                        actionType = SPenActions.TYPE_SYSTEM,
                        actionValue = SPenActions.VAL_SYSTEM_QUICK_SETTINGS,
                        vibrationPattern = "HEAVY_CLICK",
                        vibrationIntensity = 70
                    ),
                    GestureMapping(
                        profilePackageName = AppProfile.GLOBAL_PACKAGE,
                        triggerType = SPenTriggers.CIRCLE_CCW,
                        actionType = SPenActions.TYPE_SYSTEM,
                        actionValue = SPenActions.VAL_SYSTEM_SPLIT_SCREEN,
                        vibrationPattern = "HEAVY_CLICK",
                        vibrationIntensity = 75
                    )
                )
                dao.insertMappings(globalMappings)

                // 3. Create YouTube Profile
                val ytPackage = "com.google.android.youtube"
                dao.insertProfile(AppProfile(packageName = ytPackage, appName = "YouTube", isActive = true))
                dao.insertMappings(listOf(
                    GestureMapping(profilePackageName = ytPackage, triggerType = SPenTriggers.SINGLE_CLICK, actionType = SPenActions.TYPE_MEDIA, actionValue = SPenActions.VAL_MEDIA_PLAY_PAUSE, vibrationPattern = "TICK", vibrationIntensity = 50),
                    GestureMapping(profilePackageName = ytPackage, triggerType = SPenTriggers.DOUBLE_CLICK, actionType = SPenActions.TYPE_MEDIA, actionValue = SPenActions.VAL_MEDIA_NEXT, vibrationPattern = "DOUBLE_TICK", vibrationIntensity = 60),
                    GestureMapping(profilePackageName = ytPackage, triggerType = SPenTriggers.LONG_PRESS, actionType = SPenActions.TYPE_GESTURE, actionValue = SPenActions.VAL_GESTURE_DOUBLE_TAP, vibrationPattern = "HEAVY_CLICK", vibrationIntensity = 80),
                    GestureMapping(profilePackageName = ytPackage, triggerType = SPenTriggers.FLICK_LEFT, actionType = SPenActions.TYPE_GESTURE, actionValue = SPenActions.VAL_GESTURE_SWIPE_LEFT, vibrationPattern = "TICK", vibrationIntensity = 40),
                    GestureMapping(profilePackageName = ytPackage, triggerType = SPenTriggers.FLICK_RIGHT, actionType = SPenActions.TYPE_GESTURE, actionValue = SPenActions.VAL_GESTURE_SWIPE_RIGHT, vibrationPattern = "TICK", vibrationIntensity = 40)
                ))

                // 4. Create Chrome Profile
                val chromePackage = "com.android.chrome"
                dao.insertProfile(AppProfile(packageName = chromePackage, appName = "Google Chrome", isActive = true))
                dao.insertMappings(listOf(
                    GestureMapping(profilePackageName = chromePackage, triggerType = SPenTriggers.SINGLE_CLICK, actionType = SPenActions.TYPE_GESTURE, actionValue = SPenActions.VAL_GESTURE_SWIPE_DOWN, vibrationPattern = "TICK", vibrationIntensity = 45),
                    GestureMapping(profilePackageName = chromePackage, triggerType = SPenTriggers.DOUBLE_CLICK, actionType = SPenActions.TYPE_GESTURE, actionValue = SPenActions.VAL_GESTURE_SWIPE_UP, vibrationPattern = "DOUBLE_TICK", vibrationIntensity = 55),
                    GestureMapping(profilePackageName = chromePackage, triggerType = SPenTriggers.LONG_PRESS, actionType = SPenActions.TYPE_SYSTEM, actionValue = SPenActions.VAL_SYSTEM_BACK, vibrationPattern = "HEAVY_CLICK", vibrationIntensity = 75)
                ))

                // 5. Create Spotify Profile
                val spotifyPackage = "com.spotify.music"
                dao.insertProfile(AppProfile(packageName = spotifyPackage, appName = "Spotify", isActive = true))
                dao.insertMappings(listOf(
                    GestureMapping(profilePackageName = spotifyPackage, triggerType = SPenTriggers.SINGLE_CLICK, actionType = SPenActions.TYPE_MEDIA, actionValue = SPenActions.VAL_MEDIA_PLAY_PAUSE, vibrationPattern = "TICK", vibrationIntensity = 50),
                    GestureMapping(profilePackageName = spotifyPackage, triggerType = SPenTriggers.DOUBLE_CLICK, actionType = SPenActions.TYPE_MEDIA, actionValue = SPenActions.VAL_MEDIA_NEXT, vibrationPattern = "DOUBLE_TICK", vibrationIntensity = 60),
                    GestureMapping(profilePackageName = spotifyPackage, triggerType = SPenTriggers.LONG_PRESS, actionType = SPenActions.TYPE_MEDIA, actionValue = SPenActions.VAL_MEDIA_PREVIOUS, vibrationPattern = "HEAVY_CLICK", vibrationIntensity = 80)
                ))

                // 6. Create Home Screen Profile (com.android.launcher)
                val launcherPackage = "com.android.launcher"
                dao.insertProfile(AppProfile(packageName = launcherPackage, appName = "Home Screen", isActive = true))
                dao.insertMappings(listOf(
                    GestureMapping(profilePackageName = launcherPackage, triggerType = SPenTriggers.SINGLE_CLICK, actionType = SPenActions.TYPE_SYSTEM, actionValue = SPenActions.VAL_SYSTEM_NOTIFICATIONS, vibrationPattern = "TICK", vibrationIntensity = 50),
                    GestureMapping(profilePackageName = launcherPackage, triggerType = SPenTriggers.DOUBLE_CLICK, actionType = SPenActions.TYPE_SYSTEM, actionValue = SPenActions.VAL_SYSTEM_RECENTS, vibrationPattern = "DOUBLE_TICK", vibrationIntensity = 60),
                    GestureMapping(profilePackageName = launcherPackage, triggerType = SPenTriggers.LONG_PRESS, actionType = SPenActions.TYPE_SYSTEM, actionValue = SPenActions.VAL_SYSTEM_HOME, vibrationPattern = "HEAVY_CLICK", vibrationIntensity = 85),
                    GestureMapping(profilePackageName = launcherPackage, triggerType = SPenTriggers.FLICK_LEFT, actionType = SPenActions.TYPE_GESTURE, actionValue = SPenActions.VAL_GESTURE_SWIPE_LEFT, vibrationPattern = "TICK", vibrationIntensity = 40),
                    GestureMapping(profilePackageName = launcherPackage, triggerType = SPenTriggers.FLICK_RIGHT, actionType = SPenActions.TYPE_GESTURE, actionValue = SPenActions.VAL_GESTURE_SWIPE_RIGHT, vibrationPattern = "TICK", vibrationIntensity = 40),
                    GestureMapping(profilePackageName = launcherPackage, triggerType = SPenTriggers.FLICK_UP, actionType = SPenActions.TYPE_SYSTEM, actionValue = SPenActions.VAL_SYSTEM_QUICK_SETTINGS, vibrationPattern = "TICK", vibrationIntensity = 50),
                    GestureMapping(profilePackageName = launcherPackage, triggerType = SPenTriggers.FLICK_DOWN, actionType = SPenActions.TYPE_GESTURE, actionValue = SPenActions.VAL_GESTURE_SWIPE_DOWN, vibrationPattern = "TICK", vibrationIntensity = 50)
                ))

                // 7. Create System Settings Profile (com.android.settings)
                val settingsPackage = "com.android.settings"
                dao.insertProfile(AppProfile(packageName = settingsPackage, appName = "System Settings", isActive = true))
                dao.insertMappings(listOf(
                    GestureMapping(profilePackageName = settingsPackage, triggerType = SPenTriggers.SINGLE_CLICK, actionType = SPenActions.TYPE_SYSTEM, actionValue = SPenActions.VAL_SYSTEM_BACK, vibrationPattern = "TICK", vibrationIntensity = 50),
                    GestureMapping(profilePackageName = settingsPackage, triggerType = SPenTriggers.DOUBLE_CLICK, actionType = SPenActions.TYPE_SYSTEM, actionValue = SPenActions.VAL_SYSTEM_HOME, vibrationPattern = "DOUBLE_TICK", vibrationIntensity = 60),
                    GestureMapping(profilePackageName = settingsPackage, triggerType = SPenTriggers.LONG_PRESS, actionType = SPenActions.TYPE_SYSTEM, actionValue = SPenActions.VAL_SYSTEM_RECENTS, vibrationPattern = "HEAVY_CLICK", vibrationIntensity = 80),
                    GestureMapping(profilePackageName = settingsPackage, triggerType = SPenTriggers.FLICK_LEFT, actionType = SPenActions.TYPE_SYSTEM, actionValue = SPenActions.VAL_SYSTEM_BACK, vibrationPattern = "TICK", vibrationIntensity = 40),
                    GestureMapping(profilePackageName = settingsPackage, triggerType = SPenTriggers.FLICK_RIGHT, actionType = SPenActions.TYPE_SYSTEM, actionValue = SPenActions.VAL_SYSTEM_BACK, vibrationPattern = "TICK", vibrationIntensity = 40)
                ))
            }
        }
    }
}
