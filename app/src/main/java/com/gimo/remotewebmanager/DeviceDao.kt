package com.gimo.remotewebmanager

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY favorite DESC, lastOpenedAt DESC, createdAt DESC")
    fun observeAll(): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE id=:id LIMIT 1")
    suspend fun get(id: Long): Device?

    @Insert suspend fun insert(device: Device): Long
    @Update suspend fun update(device: Device)
    @Delete suspend fun delete(device: Device)

    @Query("UPDATE devices SET lastOpenedAt=:time WHERE id=:id")
    suspend fun touch(id: Long, time: Long)
}
