package com.streampanel.core.database

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DB_NAME = "stream-panel-v2.db"

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
        // Every released schema version (1..3) has a migration, so user decks are never wiped on upgrade.
        // Room only opens the file lazily, so a try/catch around build() could never detect corruption anyway.
        // Only a downgrade (installing an older APK over a newer one) is allowed to reset the data.
        return Room.databaseBuilder(
            context,
            StreamPanelDatabase::class.java,
            DB_NAME,
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideControlPanelDao(database: StreamPanelDatabase): ControlPanelDao =
        database.controlPanelDao()
}
