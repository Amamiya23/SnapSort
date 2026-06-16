package com.snapsort.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.snapsort.app.BuildConfig
import com.snapsort.app.core.SortDirection
import com.snapsort.app.data.settings.ThemeMode
import com.snapsort.app.data.settings.UserSettings
import com.snapsort.app.data.settings.UserSettingsRepository
import com.snapsort.app.data.update.UpdateCheckResult
import com.snapsort.app.data.update.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: UserSettingsRepository,
    private val updateRepository: UpdateRepository
) : ViewModel() {
    val settings: StateFlow<UserSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings()
    )
    private val _updateState = MutableStateFlow<UpdateUiState>(
        UpdateUiState.Idle(currentVersionName = BuildConfig.VERSION_NAME)
    )
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

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

    fun checkForUpdates() {
        if (_updateState.value is UpdateUiState.Checking) {
            return
        }

        _updateState.value = UpdateUiState.Checking(currentVersionName = BuildConfig.VERSION_NAME)
        viewModelScope.launch {
            _updateState.value = when (val result = updateRepository.checkForUpdates(
                currentVersionName = BuildConfig.VERSION_NAME,
                currentVersionCode = BuildConfig.VERSION_CODE
            )) {
                is UpdateCheckResult.Available -> UpdateUiState.Available(
                    currentVersionName = BuildConfig.VERSION_NAME,
                    latestVersionName = result.release.tagName,
                    releaseUrl = result.release.releaseUrl
                )
                UpdateCheckResult.UpToDate -> UpdateUiState.UpToDate(
                    currentVersionName = BuildConfig.VERSION_NAME
                )
                is UpdateCheckResult.Failed -> UpdateUiState.Failed(
                    currentVersionName = BuildConfig.VERSION_NAME,
                    message = result.message
                )
            }
        }
    }

    fun onReleaseOpenFailed() {
        _updateState.value = UpdateUiState.Failed(
            currentVersionName = BuildConfig.VERSION_NAME,
            message = "无法打开浏览器，请稍后重试。"
        )
    }

    class Factory(
        private val repository: UserSettingsRepository,
        private val updateRepository: UpdateRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository, updateRepository) as T
        }
    }
}

sealed interface UpdateUiState {
    val currentVersionName: String

    data class Idle(
        override val currentVersionName: String
    ) : UpdateUiState

    data class Checking(
        override val currentVersionName: String
    ) : UpdateUiState

    data class UpToDate(
        override val currentVersionName: String
    ) : UpdateUiState

    data class Available(
        override val currentVersionName: String,
        val latestVersionName: String,
        val releaseUrl: String
    ) : UpdateUiState

    data class Failed(
        override val currentVersionName: String,
        val message: String
    ) : UpdateUiState
}
