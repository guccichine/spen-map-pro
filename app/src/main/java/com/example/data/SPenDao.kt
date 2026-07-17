package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SPenDao {

    @Query("SELECT * FROM app_profiles ORDER BY packageName = 'global' DESC, appName ASC")
    fun getAllProfiles(): Flow<List<AppProfile>>

    @Query("SELECT * FROM app_profiles WHERE packageName = :packageName LIMIT 1")
    suspend fun getProfile(packageName: String): AppProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: AppProfile)

    @Query("DELETE FROM app_profiles WHERE packageName = :packageName AND packageName != 'global'")
    suspend fun deleteProfile(packageName: String)

    @Query("SELECT * FROM gesture_mappings WHERE profilePackageName = :packageName")
    fun getMappingsForProfile(packageName: String): Flow<List<GestureMapping>>

    @Query("SELECT * FROM gesture_mappings WHERE profilePackageName = :packageName")
    suspend fun getMappingsForProfileSync(packageName: String): List<GestureMapping>

    @Query("SELECT * FROM gesture_mappings WHERE profilePackageName = :packageName AND triggerType = :triggerType LIMIT 1")
    suspend fun getMappingSync(packageName: String, triggerType: String): GestureMapping?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: GestureMapping)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMappings(mappings: List<GestureMapping>)

    @Query("DELETE FROM gesture_mappings WHERE profilePackageName = :packageName")
    suspend fun clearMappingsForProfile(packageName: String)
}
