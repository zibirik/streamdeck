package com.streampanel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streampanel.core.datastore.PreferencesDataSource
import com.streampanel.core.model.AppearanceSettings
import com.streampanel.core.model.AppLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource,
) : ViewModel() {
    val appearance = preferencesDataSource.appearance.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppearanceSettings(),
    )

    val appLanguage = preferencesDataSource.appLanguage.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppLanguage.Russian,
    )

    fun resetLocalSettings() {
        viewModelScope.launch {
            preferencesDataSource.clearAll()
        }
    }
}
