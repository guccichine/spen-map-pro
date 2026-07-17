package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PenMap Pro", appName)
  }

  @Test
  fun `test S Pen conflict detection engine`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dummyDao = object : com.example.data.SPenDao {
        override fun getAllProfiles(): kotlinx.coroutines.flow.Flow<List<com.example.data.AppProfile>> {
            return kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.AppProfile>())
        }
        override suspend fun getProfile(packageName: String): com.example.data.AppProfile? = null
        override suspend fun insertProfile(profile: com.example.data.AppProfile) {}
        override suspend fun deleteProfile(packageName: String) {}
        override fun getMappingsForProfile(packageName: String): kotlinx.coroutines.flow.Flow<List<com.example.data.GestureMapping>> {
            return kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.GestureMapping>())
        }
        override suspend fun getMappingsForProfileSync(packageName: String): List<com.example.data.GestureMapping> {
            return emptyList<com.example.data.GestureMapping>()
        }
        override suspend fun getMappingSync(packageName: String, triggerType: String): com.example.data.GestureMapping? = null
        override suspend fun insertMapping(mapping: com.example.data.GestureMapping) {}
        override suspend fun insertMappings(mappings: List<com.example.data.GestureMapping>) {}
        override suspend fun clearMappingsForProfile(packageName: String) {}
    }
    val repository = com.example.data.SPenRepository(dummyDao)
    val viewModel = com.example.viewmodel.DashboardViewModel(repository, context)

    // Scenario 1: Redundant action mapping (Duplicate Actions)
    val activeMappingsWithDuplication = listOf(
        com.example.data.GestureMapping(
            profilePackageName = "global",
            triggerType = com.example.data.SPenTriggers.FLICK_UP,
            actionType = com.example.data.SPenActions.TYPE_MEDIA,
            actionValue = com.example.data.SPenActions.VAL_MEDIA_VOLUME_UP
        ),
        com.example.data.GestureMapping(
            profilePackageName = "global",
            triggerType = com.example.data.SPenTriggers.CIRCLE_CW,
            actionType = com.example.data.SPenActions.TYPE_MEDIA,
            actionValue = com.example.data.SPenActions.VAL_MEDIA_VOLUME_UP // Conflicting / duplicate action!
        )
    )

    val conflictsDuplication = viewModel.detectConflicts("global", activeMappingsWithDuplication, emptyList())
    assertTrue(
        "Should detect redundant mapping warning",
        conflictsDuplication.any { it.severity == com.example.viewmodel.ConflictSeverity.WARNING && it.id.contains("redundant") }
    )

    // Scenario 2: High accidental trigger risk (Disruptive action mapped to single click)
    val activeMappingsWithDisruptive = listOf(
        com.example.data.GestureMapping(
            profilePackageName = "global",
            triggerType = com.example.data.SPenTriggers.SINGLE_CLICK,
            actionType = com.example.data.SPenActions.TYPE_SYSTEM,
            actionValue = com.example.data.SPenActions.VAL_SYSTEM_LOCK_SCREEN // Lock screen is highly disruptive!
        )
    )

    val conflictsDisruptive = viewModel.detectConflicts("global", activeMappingsWithDisruptive, emptyList())
    assertTrue(
        "Should detect high risk warning for Single Click to Lock Screen",
        conflictsDisruptive.any { it.severity == com.example.viewmodel.ConflictSeverity.HIGH && it.id.contains("disruptive") }
    )

    // Scenario 3: Global Action Override (YouTube profile overrides Global profile mapping)
    val globalMappings = listOf(
        com.example.data.GestureMapping(
            profilePackageName = "global",
            triggerType = com.example.data.SPenTriggers.FLICK_UP,
            actionType = com.example.data.SPenActions.TYPE_MEDIA,
            actionValue = com.example.data.SPenActions.VAL_MEDIA_VOLUME_UP
        )
    )
    val appMappings = listOf(
        com.example.data.GestureMapping(
            profilePackageName = "com.google.android.youtube",
            triggerType = com.example.data.SPenTriggers.FLICK_UP,
            actionType = com.example.data.SPenActions.TYPE_MEDIA,
            actionValue = com.example.data.SPenActions.VAL_MEDIA_PLAY_PAUSE // Overridden value!
        )
    )

    val conflictsOverride = viewModel.detectConflicts("com.google.android.youtube", appMappings, globalMappings)
    assertTrue(
        "Should detect global override info",
        conflictsOverride.any { it.severity == com.example.viewmodel.ConflictSeverity.INFO && it.id.contains("override") }
    )

    // Scenario 4: Conflicting Toggle state logic (DND enable and DND toggle in same profile)
    val activeMappingsWithToggleConflict = listOf(
        com.example.data.GestureMapping(
            profilePackageName = "global",
            triggerType = com.example.data.SPenTriggers.FLICK_UP,
            actionType = com.example.data.SPenActions.TYPE_DND,
            actionValue = com.example.data.SPenActions.VAL_DND_TOGGLE
        ),
        com.example.data.GestureMapping(
            profilePackageName = "global",
            triggerType = com.example.data.SPenTriggers.FLICK_DOWN,
            actionType = com.example.data.SPenActions.TYPE_DND,
            actionValue = com.example.data.SPenActions.VAL_DND_ENABLE
        )
    )

    val conflictsToggle = viewModel.detectConflicts("global", activeMappingsWithToggleConflict, emptyList())
    assertTrue(
        "Should detect conflicting toggle logic warning",
        conflictsToggle.any { it.severity == com.example.viewmodel.ConflictSeverity.WARNING && it.id.contains("conflict_dnd") }
    )
  }
}
