package com.example.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM crosshair_presets ORDER BY createdAt DESC")
    fun getAllPresets(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM crosshair_presets ORDER BY createdAt DESC")
    suspend fun getAllPresetsSnapshot(): List<PresetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: PresetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<PresetEntity>)

    @Query("SELECT * FROM crosshair_presets WHERE id = :id LIMIT 1")
    suspend fun getPresetById(id: String): PresetEntity?

    @Query("UPDATE crosshair_presets SET name = :name WHERE id = :id")
    suspend fun updatePresetName(id: String, name: String)

    @Query("DELETE FROM crosshair_presets WHERE id = :id")
    suspend fun deletePresetById(id: String)

    @Query("SELECT COUNT(*) FROM crosshair_presets")
    suspend fun getCount(): Int
}
