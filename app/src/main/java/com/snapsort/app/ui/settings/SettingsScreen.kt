package com.snapsort.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snapsort.app.SnapSortDependencies
import com.snapsort.app.core.SortDirection
import com.snapsort.app.data.settings.ThemeMode
import com.snapsort.app.ui.copy.burstThresholdLabel
import com.snapsort.app.ui.copy.gestureShortcutDescription
import com.snapsort.app.ui.copy.looseGroupThresholdLabel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            repository = SnapSortDependencies.userSettingsRepository(LocalContext.current),
            updateRepository = SnapSortDependencies.updateRepository()
        )
    )
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val maxOverscrollOffsetPx = with(LocalDensity.current) { 40.dp.toPx() }
    var burstSheetOpen by remember { mutableStateOf(false) }
    var looseGroupSheetOpen by remember { mutableStateOf(false) }
    var themeSheetOpen by remember { mutableStateOf(false) }
    var overscrollOffsetPx by remember { mutableFloatStateOf(0f) }
    var releaseAnimationJob by remember { mutableStateOf<Job?>(null) }
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
    val overscrollConnection = remember(maxOverscrollOffsetPx) {
        object : NestedScrollConnection {
            private fun stopReleaseAnimation() {
                releaseAnimationJob?.cancel()
                releaseAnimationJob = null
            }

            private fun scheduleRelease() {
                if (overscrollOffsetPx == 0f) {
                    return
                }
                stopReleaseAnimation()
                releaseAnimationJob = coroutineScope.launch {
                    animate(
                        initialValue = overscrollOffsetPx,
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.72f,
                            stiffness = 420f
                        )
                    ) { value, _ ->
                        overscrollOffsetPx = value
                    }
                }
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.Drag || overscrollOffsetPx == 0f) {
                    return Offset.Zero
                }
                val delta = available.y
                if (delta == 0f || (delta > 0f) == (overscrollOffsetPx > 0f)) {
                    return Offset.Zero
                }

                stopReleaseAnimation()
                val previous = overscrollOffsetPx
                overscrollOffsetPx = if (previous > 0f) {
                    max(0f, previous + delta)
                } else {
                    min(0f, previous + delta)
                }
                return Offset(
                    x = 0f,
                    y = overscrollOffsetPx - previous
                )
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source != NestedScrollSource.Drag || available.y == 0f) {
                    return Offset.Zero
                }

                stopReleaseAnimation()
                val stretchRatio = 1f - (abs(overscrollOffsetPx) / maxOverscrollOffsetPx).coerceIn(0f, 1f)
                val dampedDelta = available.y * (0.10f + 0.06f * stretchRatio)
                overscrollOffsetPx = (overscrollOffsetPx + dampedDelta)
                    .coerceIn(-maxOverscrollOffsetPx, maxOverscrollOffsetPx)
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                scheduleRelease()
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                scheduleRelease()
                return Velocity.Zero
            }
        }
    }

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
                .padding(paddingValues)
                .nestedScroll(overscrollConnection)
                .graphicsLayer {
                    val stretchProgress = (abs(overscrollOffsetPx) / maxOverscrollOffsetPx)
                        .coerceIn(0f, 1f)
                    transformOrigin = if (overscrollOffsetPx >= 0f) {
                        TransformOrigin(0.5f, 0f)
                    } else {
                        TransformOrigin(0.5f, 1f)
                    }
                    scaleY = 1f + stretchProgress * 0.035f
                },
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSectionCard(title = "连拍阈值") {
                    SelectionRow(
                        title = "连拍分组间隔",
                        value = burstThresholdLabel(settings.burstThresholdMillis),
                        onClick = { burstSheetOpen = true }
                    )
                }
            }

            item {
                SettingsSectionCard(title = "散片分组") {
                    ToggleRow(
                        title = "自动拆分散片组",
                        checked = settings.autoSplitLooseGroups,
                        onCheckedChange = viewModel::setAutoSplitLooseGroups
                    )
                    CardRowDivider()
                    SelectionRow(
                        title = "散片分组时间段",
                        description = if (settings.autoSplitLooseGroups) null
                            else "自动拆分已关闭，重新开启后继续使用该时间。",
                        value = looseGroupThresholdLabel(settings.looseGroupThresholdMillis),
                        enabled = settings.autoSplitLooseGroups,
                        onClick = { looseGroupSheetOpen = true }
                    )
                }
            }

            item {
                SettingsSectionCard(title = "筛选行为") {
                    ToggleRow(
                        title = if (settings.sortDirection == SortDirection.NEWEST_FIRST) "从新到旧" else "从旧到新",
                        description = "首页分组和组内照片使用同一排序。",
                        checked = settings.sortDirection == SortDirection.NEWEST_FIRST,
                        onCheckedChange = viewModel::setNewestFirst
                    )
                    CardRowDivider()
                    ToggleRow(
                        title = "完成当前组后自动进入下一组",
                        description = "关闭后会停留在当前组，由你手动返回首页。",
                        checked = settings.autoAdvanceGroup,
                        onCheckedChange = viewModel::setAutoAdvanceGroup
                    )
                    CardRowDivider()
                    ToggleRow(
                        title = "手势快捷",
                        description = gestureShortcutDescription(),
                        checked = settings.gestureShortcutEnabled,
                        onCheckedChange = viewModel::setGestureShortcutEnabled
                    )
                }
            }

            item {
                SettingsSectionCard(title = "主题") {
                    SelectionRow(
                        title = "界面外观",
                        value = themeOptions.first { it.value == settings.themeMode }.label,
                        onClick = { themeSheetOpen = true }
                    )
                }
            }

            item {
                SettingsSectionCard(title = "应用更新") {
                    UpdateCheckSection(
                        state = updateState,
                        onCheck = viewModel::checkForUpdates,
                        onOpenRelease = { releaseUrl ->
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl)))
                            } catch (_: ActivityNotFoundException) {
                                viewModel.onReleaseOpenFailed()
                            } catch (_: SecurityException) {
                                viewModel.onReleaseOpenFailed()
                            }
                        }
                    )
                }
            }
        }
    }

    if (burstSheetOpen) {
        SettingSelectionSheet(
            title = "连拍分组间隔",
            options = thresholds,
            selectedValue = settings.burstThresholdMillis,
            onDismiss = { burstSheetOpen = false },
            onSelect = { value ->
                viewModel.setBurstThresholdMillis(value)
                burstSheetOpen = false
            }
        )
    }

    if (looseGroupSheetOpen) {
        SettingSelectionSheet(
            title = "散片分组时间段",
            options = looseGroupThresholds,
            selectedValue = settings.looseGroupThresholdMillis,
            onDismiss = { looseGroupSheetOpen = false },
            onSelect = { value ->
                viewModel.setLooseGroupThresholdMillis(value)
                looseGroupSheetOpen = false
            }
        )
    }

    if (themeSheetOpen) {
        SettingSelectionSheet(
            title = "主题",
            options = themeOptions,
            selectedValue = settings.themeMode,
            onDismiss = { themeSheetOpen = false },
            onSelect = { value ->
                viewModel.setThemeMode(value)
                themeSheetOpen = false
            }
        )
    }
}

@Composable
private fun UpdateCheckSection(
    state: UpdateUiState,
    onCheck: () -> Unit,
    onOpenRelease: (String) -> Unit
) {
    SelectionRow(
        title = "检查更新",
        description = updateDescription(state),
        value = updateValue(state),
        enabled = state !is UpdateUiState.Checking,
        onClick = onCheck
    )

    if (state is UpdateUiState.Available) {
        CardRowDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { onOpenRelease(state.releaseUrl) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("前往下载")
            }
        }
    }
}

private fun updateDescription(state: UpdateUiState): String = when (state) {
    is UpdateUiState.Idle -> "当前版本 ${state.currentVersionName}"
    is UpdateUiState.Checking -> "正在连接 GitHub..."
    is UpdateUiState.UpToDate -> "当前版本 ${state.currentVersionName}"
    is UpdateUiState.Available -> "当前版本 ${state.currentVersionName}，最新版本 ${state.latestVersionName}"
    is UpdateUiState.Failed -> state.message
}

private fun updateValue(state: UpdateUiState): String = when (state) {
    is UpdateUiState.Idle -> "检查"
    is UpdateUiState.Checking -> "检查中"
    is UpdateUiState.UpToDate -> "已是最新"
    is UpdateUiState.Available -> "可更新"
    is UpdateUiState.Failed -> "重试"
}

private data class SettingOption<T>(
    val value: T,
    val label: String
)

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    val cardColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
        Color.White
    } else {
        MaterialTheme.colorScheme.surface
    }

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = cardColor,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            )
        ) {
            Column(content = { content() })
        }
    }
}

@Composable
private fun CardRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingSelectionSheet(
    title: String,
    options: List<SettingOption<T>>,
    selectedValue: T,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            options.forEach { option ->
                val selected = option.value == selectedValue
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selected,
                            onClick = { onSelect(option.value) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = null
                    )
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("取消")
            }
        }
    }
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
