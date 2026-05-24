package com.snapsort.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.snapsort.app.core.SortDirection
import com.snapsort.app.data.settings.ThemeMode
import com.snapsort.app.data.settings.UserSettings
import com.snapsort.app.data.settings.UserSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: UserSettingsRepository
) : ViewModel() {
    val settings: StateFlow<UserSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings()
    )

    fun setBurstThresholdMillis(value: Long) {
        viewModelScope.launch { repository.setBurstThresholdMillis(value) }
    }

    fun setLooseGroupThresholdMillis(value: Long) {
        viewModelScope.launch { repository.setLooseGroupThresholdMillis(value) }
    }

    fun setAutoSplitLooseGroups(value: Boolean) {
        viewModelScope.launch { repository.setAutoSplitLooseGroups(value) }
    }

    fun setNewestFirst(value: Boolean) {
        viewModelScope.launch {
            repository.setSortDirection(if (value) SortDirection.NEWEST_FIRST else SortDirection.OLDEST_FIRST)
        }
    }

    fun setAutoAdvanceGroup(value: Boolean) {
        viewModelScope.launch { repository.setAutoAdvanceGroup(value) }
    }

    fun setGestureShortcutEnabled(value: Boolean) {
        viewModelScope.launch { repository.setGestureShortcutEnabled(value) }
    }

    fun setThemeMode(value: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(value) }
    }

    class Factory(
        private val repository: UserSettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository) as T
        }
    }
}
