package com.gimo.remotewebmanager

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class Device(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = 0,
    val favorite: Boolean = false
)
