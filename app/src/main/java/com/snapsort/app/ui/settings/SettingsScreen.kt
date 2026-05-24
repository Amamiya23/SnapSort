package com.snapsort.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snapsort.app.SnapSortDependencies
import com.snapsort.app.core.SortDirection
import com.snapsort.app.data.settings.ThemeMode
import com.snapsort.app.ui.copy.burstThresholdLabel
import com.snapsort.app.ui.copy.gestureShortcutDescription
import com.snapsort.app.ui.copy.looseGroupThresholdLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            SnapSortDependencies.userSettingsRepository(LocalContext.current)
        )
    )
) {
    val settings by viewModel.settings.collectAsState()
    var burstDialogOpen by remember { mutableStateOf(false) }
    var looseGroupDialogOpen by remember { mutableStateOf(false) }
    var themeDialogOpen by remember { mutableStateOf(false) }
    val thresholds = listOf(500L, 1_000L, 2_000L, 3_000L, 5_000L)
        .map { SettingOption(it, burstThresholdLabel(it)) }
    val looseGroupThresholds = listOf(
        30 * 60 * 1_000L,
        60 * 60 * 1_000L,
        2 * 60 * 60 * 1_000L,
        4 * 60 * 60 * 1_000L
    ).map { SettingOption(it, looseGroupThresholdLabel(it)) }
    val themeOptions = listOf(
        SettingOption(ThemeMode.SYSTEM, "跟随系统"),
        SettingOption(ThemeMode.LIGHT, "浅色"),
        SettingOption(ThemeMode.DARK, "深色"),
        SettingOption(ThemeMode.DYNAMIC, "动态")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                SectionHeader(title = "连拍阈值")
            }
            item {
                SelectionRow(
                    title = "连拍分组间隔",
                    value = burstThresholdLabel(settings.burstThresholdMillis),
                    onClick = { burstDialogOpen = true }
                )
            }

            item { SectionDivider() }

            item {
                SectionHeader(title = "散片分组")
            }
            item {
                ToggleRow(
                    title = "自动拆分散片组",
                    checked = settings.autoSplitLooseGroups,
                    onCheckedChange = viewModel::setAutoSplitLooseGroups
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }
            item {
                SelectionRow(
                    title = "散片分组时间段",
                    description = if (settings.autoSplitLooseGroups) null
                        else "自动拆分已关闭，重新开启后继续使用该时间。",
                    value = looseGroupThresholdLabel(settings.looseGroupThresholdMillis),
                    enabled = settings.autoSplitLooseGroups,
                    onClick = { looseGroupDialogOpen = true }
                )
            }

            item { SectionDivider() }

            item {
                SectionHeader(title = "筛选行为")
            }
            item {
                ToggleRow(
                    title = if (settings.sortDirection == SortDirection.NEWEST_FIRST) "从新到旧" else "从旧到新",
                    description = "首页分组和组内照片使用同一排序。",
                    checked = settings.sortDirection == SortDirection.NEWEST_FIRST,
                    onCheckedChange = viewModel::setNewestFirst
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }
            item {
                ToggleRow(
                    title = "完成当前组后自动进入下一组",
                    description = "关闭后会停留在当前组，由你手动返回首页。",
                    checked = settings.autoAdvanceGroup,
                    onCheckedChange = viewModel::setAutoAdvanceGroup
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }
            item {
                ToggleRow(
                    title = "手势快捷",
                    description = gestureShortcutDescription(),
                    checked = settings.gestureShortcutEnabled,
                    onCheckedChange = viewModel::setGestureShortcutEnabled
                )
            }

            item { SectionDivider() }

            item {
                SectionHeader(title = "主题")
            }
            item {
                SelectionRow(
                    title = "界面外观",
                    value = themeOptions.first { it.value == settings.themeMode }.label,
                    onClick = { themeDialogOpen = true }
                )
            }
        }
    }

    if (burstDialogOpen) {
        SettingSelectionDialog(
            title = "连拍分组间隔",
            options = thresholds,
            selectedValue = settings.burstThresholdMillis,
            onDismiss = { burstDialogOpen = false },
            onSelect = { value ->
                viewModel.setBurstThresholdMillis(value)
                burstDialogOpen = false
            }
        )
    }

    if (looseGroupDialogOpen) {
        SettingSelectionDialog(
            title = "散片分组时间段",
            options = looseGroupThresholds,
            selectedValue = settings.looseGroupThresholdMillis,
            onDismiss = { looseGroupDialogOpen = false },
            onSelect = { value ->
                viewModel.setLooseGroupThresholdMillis(value)
                looseGroupDialogOpen = false
            }
        )
    }

    if (themeDialogOpen) {
        SettingSelectionDialog(
            title = "主题",
            options = themeOptions,
            selectedValue = settings.themeMode,
            onDismiss = { themeDialogOpen = false },
            onSelect = { value ->
                viewModel.setThemeMode(value)
                themeDialogOpen = false
            }
        )
    }
}

private data class SettingOption<T>(
    val value: T,
    val label: String
)

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
    )
}

@Composable
private fun SectionDivider() {
    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SelectionRow(
    title: String,
    description: String? = null,
    value: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val titleColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val valueColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val contentAlpha = if (enabled) 1f else 0.5f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).alpha(contentAlpha)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            modifier = Modifier.alpha(contentAlpha)
        )
    }
}

@Composable
private fun <T> SettingSelectionDialog(
    title: String,
    options: List<SettingOption<T>>,
    selectedValue: T,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option.value) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option.value == selectedValue,
                            onClick = { onSelect(option.value) }
                        )
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ToggleRow(
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
