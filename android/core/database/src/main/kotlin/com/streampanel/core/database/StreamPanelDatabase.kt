package com.streampanel.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ProfileEntity::class,
        PageEntity::class,
        ButtonEntity::class,
        ActionEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class StreamPanelDatabase : RoomDatabase() {
    abstract fun controlPanelDao(): ControlPanelDao
}
