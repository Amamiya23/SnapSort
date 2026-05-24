package com.snapsort.app.ui.scan

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.snapsort.app.data.repository.TaskRepository
import com.snapsort.app.data.scanner.FolderScanner
import com.snapsort.app.data.scanner.ScanEvent
import com.snapsort.app.data.scanner.ScanSettings
import com.snapsort.app.data.settings.UserSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class ScanProgressUiState(
    val currentStage: String = "准备扫描",
    val currentProgress: Int = 0,
    val totalItems: Int = 0,
    val currentFileName: String = "",
    val isComplete: Boolean = false,
    val errorMessage: String? = null
)

class ScanProgressViewModel(
    private val scanner: FolderScanner,
    private val taskRepository: TaskRepository,
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScanProgressUiState())
    val uiState: StateFlow<ScanProgressUiState> = _uiState.asStateFlow()
    private var scanJob: Job? = null

    fun scanFolder(folderUri: Uri) {
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launch {
            val settings = userSettingsRepository.settings.first()
            val scanSettings = ScanSettings(
                burstThresholdMillis = settings.burstThresholdMillis,
                looseGroupThresholdMillis = if (settings.autoSplitLooseGroups) {
                    settings.looseGroupThresholdMillis
                } else {
                    Long.MAX_VALUE
                },
                sortDirection = settings.sortDirection
            )
            scanner.scan(folderUri, scanSettings).collect { event ->
                when (event) {
                    is ScanEvent.Progress -> _uiState.value = ScanProgressUiState(
                        currentStage = event.stage,
                        currentProgress = event.processed,
                        totalItems = event.total,
                        currentFileName = event.currentFileName.orEmpty()
                    )
                    is ScanEvent.Complete -> {
                        taskRepository.saveRecentTask(
                            folderUri = folderUri.toString(),
                            folderName = event.folderName,
                            scannedAtMillis = System.currentTimeMillis(),
                            burstThresholdMillis = settings.burstThresholdMillis,
                            sortDirection = settings.sortDirection,
                            groups = event.groups
                        )
                        _uiState.update { it.copy(isComplete = true) }
                    }
                    is ScanEvent.Failed -> _uiState.update {
                        it.copy(errorMessage = event.message)
                    }
                }
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
    }

    class Factory(
        private val scanner: FolderScanner,
        private val taskRepository: TaskRepository,
        private val userSettingsRepository: UserSettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScanProgressViewModel(scanner, taskRepository, userSettingsRepository) as T
        }
    }
}
