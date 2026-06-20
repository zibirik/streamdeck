package com.streampanel.core.database



import com.streampanel.core.model.ButtonState

import com.streampanel.core.model.ControlAction

import com.streampanel.core.model.ControlPage

import com.streampanel.core.model.ControlProfile

import com.streampanel.core.model.DashboardButton

import com.streampanel.core.model.DeckExport

import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.first

import kotlinx.coroutines.flow.map

import java.util.UUID

import javax.inject.Inject

import javax.inject.Singleton



interface ControlPanelRepository {

    fun observeActiveProfile(): Flow<ControlProfile?>

    fun observePages(profileId: String): Flow<List<ControlPage>>

    fun observeButtons(pageId: String): Flow<List<DashboardButton>>

    fun observeButton(buttonId: String): Flow<DashboardButton?>

    fun observeActions(buttonId: String): Flow<List<ControlAction>>

    suspend fun upsertButton(button: DashboardButton)

    suspend fun replaceActions(buttonId: String, actions: List<ControlAction>)

    suspend fun createPage(profileId: String, title: String, parentPageId: String? = null): ControlPage

    suspend fun renamePage(pageId: String, title: String)

    suspend fun deletePage(pageId: String)

    suspend fun createButton(pageId: String, row: Int, column: Int): DashboardButton

    suspend fun deleteButton(buttonId: String)

    suspend fun exportDeck(): DeckExport?

    suspend fun importDeck(export: DeckExport)

    suspend fun seedIfEmpty()

}



@Singleton

class RoomControlPanelRepository @Inject constructor(

    private val dao: ControlPanelDao,

) : ControlPanelRepository {

    override fun observeActiveProfile(): Flow<ControlProfile?> =

        dao.observeActiveProfile().map { it?.asExternalModel() }



    override fun observePages(profileId: String): Flow<List<ControlPage>> =

        dao.observePages(profileId).map { pages -> pages.map(PageEntity::asExternalModel) }



    override fun observeButtons(pageId: String): Flow<List<DashboardButton>> =

        dao.observeButtons(pageId).map { buttons -> buttons.map(ButtonEntity::asExternalModel) }



    override fun observeButton(buttonId: String): Flow<DashboardButton?> =

        dao.observeButton(buttonId).map { it?.asExternalModel() }



    override fun observeActions(buttonId: String): Flow<List<ControlAction>> =

        dao.observeActions(buttonId).map { actions -> actions.map(ActionEntity::asExternalModel) }



    override suspend fun upsertButton(button: DashboardButton) {

        dao.upsertButton(button.asEntity())

    }



    override suspend fun replaceActions(buttonId: String, actions: List<ControlAction>) {

        dao.replaceActions(buttonId, actions.map(ControlAction::asEntity))

    }



    override suspend fun createPage(profileId: String, title: String, parentPageId: String?): ControlPage {

        val pages = dao.getPages(profileId)

        val page = ControlPage(

            id = UUID.randomUUID().toString(),

            profileId = profileId,

            parentPageId = parentPageId,

            title = title,

            sortOrder = pages.size,

        )

        dao.upsertPages(listOf(page.asEntity()))

        return page

    }



    override suspend fun deletePage(pageId: String) {

        val buttons = dao.getButtons(pageId)

        buttons.forEach { dao.deleteActionsForButton(it.id) }

        dao.deleteButtonsForPage(pageId)

        dao.deletePage(pageId)

    }

    override suspend fun renamePage(pageId: String, title: String) {
        val page = dao.getPage(pageId) ?: return
        dao.upsertPages(listOf(page.copy(title = title)))
    }



    override suspend fun createButton(pageId: String, row: Int, column: Int): DashboardButton {

        val button = DashboardButton(

            id = UUID.randomUUID().toString(),

            pageId = pageId,

            title = "New Button",

            iconName = "bolt",

            backgroundColor = "#242836",

            row = row,

            column = column,

        )

        dao.upsertButton(button.asEntity())

        dao.replaceActions(

            button.id,

            listOf(

                ControlAction(

                    id = UUID.randomUUID().toString(),

                    buttonId = button.id,

                    type = com.streampanel.core.model.ActionType.Hotkey,

                    label = "Hotkey",

                    payload = mapOf("keys" to "CTRL+C"),

                ),

            ).map(ControlAction::asEntity),

        )

        return button

    }



    override suspend fun deleteButton(buttonId: String) {

        dao.deleteActionsForButton(buttonId)

        dao.deleteButton(buttonId)

    }



    override suspend fun exportDeck(): DeckExport? {

        val profile = dao.observeActiveProfile().first()?.asExternalModel() ?: return null

        val pages = dao.getPages(profile.id).map(PageEntity::asExternalModel)

        val buttons = pages.flatMap { dao.getButtons(it.id).map(ButtonEntity::asExternalModel) }

        val actions = if (buttons.isEmpty()) emptyList() else {

            dao.getActionsForButtons(buttons.map { it.id }).map(ActionEntity::asExternalModel)

        }

        return DeckExport(profile = profile, pages = pages, buttons = buttons, actions = actions)

    }



    override suspend fun importDeck(export: DeckExport) {

        dao.upsertProfile(export.profile.asEntity())

        dao.upsertPages(export.pages.map { it.asEntity() })

        dao.upsertButtons(export.buttons.map { it.asEntity() })

        export.buttons.forEach { dao.deleteActionsForButton(it.id) }

        dao.upsertActions(export.actions.map { it.asEntity() })

    }



    override suspend fun seedIfEmpty() {

        if (dao.profileCount() == 0) {

            dao.upsertProfile(SeedData.profile)

            dao.upsertPages(SeedData.pages)

            dao.upsertButtons(SeedData.buttons)

            dao.upsertActions(SeedData.actions)

            return

        }

        ensureAppsPage()

    }



    private suspend fun ensureAppsPage() {

        if (dao.pageCount(SeedData.APPS_PAGE_ID) > 0) return

        dao.upsertPages(SeedData.pages.filter { it.id == SeedData.APPS_PAGE_ID })

        dao.upsertButtons(

            SeedData.buttons.filter {

                it.id == "btn-apps-folder" || it.pageId == SeedData.APPS_PAGE_ID

            },

        )

        dao.upsertActions(

            SeedData.actions.filter {

                it.buttonId == "btn-chrome" || it.buttonId == "btn-discord" || it.buttonId == "btn-spotify"

            },

        )

    }

}





private fun ControlProfile.asEntity() = ProfileEntity(id, name, isActive)


