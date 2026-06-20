package com.streampanel.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE buttons ADD COLUMN isToggle INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE buttons ADD COLUMN activeIconName TEXT")
        db.execSQL("ALTER TABLE buttons ADD COLUMN activeBackgroundColor TEXT")
        db.execSQL("ALTER TABLE actions ADD COLUMN whenState TEXT NOT NULL DEFAULT 'Always'")
    }
}

/** Fixes databases that received a broken v2 migration with NULL whenState values. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE actions SET whenState = 'Always' WHERE whenState IS NULL")
    }
}
