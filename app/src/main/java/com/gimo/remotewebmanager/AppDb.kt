package com.gimo.remotewebmanager

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Device::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    companion object {
        @Volatile private var INSTANCE: AppDb? = null
        fun get(context: Context): AppDb = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDb::class.java, "remote_devices.db").build().also { INSTANCE = it }
        }
    }
}
