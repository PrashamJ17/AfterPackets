package com.packethunter.mobile.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FilterPresetDao {
    @Query("SELECT * FROM filter_presets ORDER BY lastUsed DESC")
    fun getAllPresets(): Flow<List<FilterPreset>>
    
    @Query("SELECT * FROM filter_presets WHERE id = :id")
    suspend fun getPresetById(id: Long): FilterPreset?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: FilterPreset): Long
    
    @Update
    suspend fun updatePreset(preset: FilterPreset)
    
    @Delete
    suspend fun deletePreset(preset: FilterPreset)
    
    @Query("UPDATE filter_presets SET lastUsed = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Long, timestamp: Long)
}

