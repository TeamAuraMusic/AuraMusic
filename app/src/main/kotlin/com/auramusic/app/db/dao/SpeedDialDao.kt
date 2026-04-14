/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.auramusic.app.db.entities.SpeedDialItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedDialDao {
    @Query("SELECT * FROM speed_dial_item")
    fun getAll(): Flow<List<SpeedDialItem>>

    @Query("SELECT * FROM speed_dial_item WHERE id = :id")
    suspend fun getById(id: String): SpeedDialItem?

    @Query("SELECT EXISTS(SELECT 1 FROM speed_dial_item WHERE id = :id)")
    fun isPinned(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SpeedDialItem)

    @Query("DELETE FROM speed_dial_item WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM speed_dial_item")
    suspend fun deleteAll()
}