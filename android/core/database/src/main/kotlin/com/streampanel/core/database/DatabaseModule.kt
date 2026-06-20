package com.streampanel.core.database

import android.content.Context
import android.util.Log
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DB_NAME = "stream-panel-v2.db"
private const val TAG = "StreamPanelDB"

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseBindingModule {
    @Binds
    @Singleton
    abstract fun bindControlPanelRepository(
        repository: RoomControlPanelRepository,
    ): ControlPanelRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StreamPanelDatabase {
        fun build() = Room.databaseBuilder(
            context,
            StreamPanelDatabase::class.java,
            DB_NAME,
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()

        return try {
            build()
        } catch (error: Exception) {
            Log.e(TAG, "Database open failed, recreating", error)
            context.deleteDatabase(DB_NAME)
            context.deleteDatabase("stream-panel.db")
            build()
        }
    }

    @Provides
    fun provideControlPanelDao(database: StreamPanelDatabase): ControlPanelDao =
        database.controlPanelDao()
}
