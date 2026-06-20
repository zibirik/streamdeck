package com.streampanel.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.streampanel.core.model.ActionType
import com.streampanel.core.model.ActionWhen
import com.streampanel.core.model.ButtonState
import com.streampanel.core.model.ControlAction
import com.streampanel.core.model.ControlPage
import com.streampanel.core.model.ControlProfile
import com.streampanel.core.model.DashboardButton

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isActive: Boolean,
)

@Entity(tableName = "pages")
data class PageEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val parentPageId: String?,
    val title: String,
    val sortOrder: Int,
)

@Entity(tableName = "buttons")
data class ButtonEntity(
    @PrimaryKey val id: String,
    val pageId: String,
    val title: String,
    val subtitle: String?,
    val iconName: String?,
    val imageUri: String?,
    val gifUri: String?,
    val backgroundColor: String,
    val gradientEndColor: String?,
    val row: Int,
    val column: Int,
    val rowSpan: Int,
    val columnSpan: Int,
    val state: ButtonState,
    val targetPageId: String?,
    val isFolder: Boolean,
    val isToggle: Boolean = false,
    val activeIconName: String? = null,
    val activeBackgroundColor: String? = null,
)

@Entity(tableName = "actions")
data class ActionEntity(
    @PrimaryKey val id: String,
    val buttonId: String,
    val type: ActionType,
    val label: String,
    val payload: Map<String, String>,
    val sortOrder: Int,
    val whenState: ActionWhen = ActionWhen.Always,
)

fun ProfileEntity.asExternalModel() = ControlProfile(id, name, isActive)

fun PageEntity.asExternalModel() = ControlPage(
    id = id,
    profileId = profileId,
    parentPageId = parentPageId,
    title = title,
    sortOrder = sortOrder,
)

fun ButtonEntity.asExternalModel() = DashboardButton(
    id = id,
    pageId = pageId,
    title = title,
    subtitle = subtitle,
    iconName = iconName,
    imageUri = imageUri,
    gifUri = gifUri,
    backgroundColor = backgroundColor,
    gradientEndColor = gradientEndColor,
    row = row,
    column = column,
    rowSpan = rowSpan,
    columnSpan = columnSpan,
    state = state,
    targetPageId = targetPageId,
    isFolder = isFolder,
    isToggle = isToggle,
    activeIconName = activeIconName,
    activeBackgroundColor = activeBackgroundColor,
)

fun ActionEntity.asExternalModel() = ControlAction(
    id = id,
    buttonId = buttonId,
    type = type,
    label = label,
    payload = payload,
    sortOrder = sortOrder,
    whenState = whenState,
)

fun DashboardButton.asEntity() = ButtonEntity(
    id = id,
    pageId = pageId,
    title = title,
    subtitle = subtitle,
    iconName = iconName,
    imageUri = imageUri,
    gifUri = gifUri,
    backgroundColor = backgroundColor,
    gradientEndColor = gradientEndColor,
    row = row,
    column = column,
    rowSpan = rowSpan,
    columnSpan = columnSpan,
    state = state,
    targetPageId = targetPageId,
    isFolder = isFolder,
    isToggle = isToggle,
    activeIconName = activeIconName,
    activeBackgroundColor = activeBackgroundColor,
)

fun ControlPage.asEntity() = PageEntity(id, profileId, parentPageId, title, sortOrder)

fun ControlAction.asEntity() = ActionEntity(id, buttonId, type, label, payload, sortOrder, whenState)
