package com.streampanel.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ControlPanelDao {
    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun profileCount(): Int

    @Query("SELECT COUNT(*) FROM pages WHERE id = :pageId")
    suspend fun pageCount(pageId: String): Int

    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    fun observeActiveProfile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM pages WHERE profileId = :profileId ORDER BY sortOrder ASC")
    fun observePages(profileId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM buttons WHERE pageId = :pageId ORDER BY row ASC, column ASC")
    fun observeButtons(pageId: String): Flow<List<ButtonEntity>>

    @Query("SELECT * FROM buttons WHERE id = :buttonId LIMIT 1")
    fun observeButton(buttonId: String): Flow<ButtonEntity?>

    @Query("SELECT * FROM actions WHERE buttonId = :buttonId ORDER BY sortOrder ASC")
    fun observeActions(buttonId: String): Flow<List<ActionEntity>>

    @Upsert
    suspend fun upsertProfile(profile: ProfileEntity)

    @Upsert
    suspend fun upsertPages(pages: List<PageEntity>)

    @Upsert
    suspend fun upsertButtons(buttons: List<ButtonEntity>)

    @Upsert
    suspend fun upsertButton(button: ButtonEntity)

    @Upsert
    suspend fun upsertActions(actions: List<ActionEntity>)

    @Query("DELETE FROM actions WHERE buttonId = :buttonId")
    suspend fun deleteActionsForButton(buttonId: String)

    @Transaction
    suspend fun replaceActions(buttonId: String, actions: List<ActionEntity>) {
        deleteActionsForButton(buttonId)
        upsertActions(actions)
    }

    @Query("DELETE FROM pages WHERE id = :pageId")
    suspend fun deletePage(pageId: String)

    @Query("DELETE FROM buttons WHERE pageId = :pageId")
    suspend fun deleteButtonsForPage(pageId: String)

    @Query("DELETE FROM buttons WHERE id = :buttonId")
    suspend fun deleteButton(buttonId: String)

    @Query("SELECT * FROM pages WHERE profileId = :profileId ORDER BY sortOrder ASC")
    suspend fun getPages(profileId: String): List<PageEntity>

    @Query("SELECT * FROM pages WHERE id = :pageId LIMIT 1")
    suspend fun getPage(pageId: String): PageEntity?

    @Query("SELECT * FROM buttons WHERE pageId = :pageId")
    suspend fun getButtons(pageId: String): List<ButtonEntity>

    @Query("SELECT * FROM actions WHERE buttonId IN (:buttonIds)")
    suspend fun getActionsForButtons(buttonIds: List<String>): List<ActionEntity>
}
