package com.snapsort.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.snapsort.app.core.SortDirection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userSettingsDataStore by preferencesDataStore(name = "user_settings")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK, DYNAMIC
}

data class UserSettings(
    val burstThresholdMillis: Long = 1_000L,
    val sortDirection: SortDirection = SortDirection.NEWEST_FIRST,
    val autoAdvanceGroup: Boolean = true,
    val gestureShortcutEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

class UserSettingsRepository(
    private val context: Context
) {
    val settings: Flow<UserSettings> = context.userSettingsDataStore.data.map { preferences ->
        UserSettings(
            burstThresholdMillis = preferences[BURST_THRESHOLD_MILLIS] ?: 1_000L,
            sortDirection = preferences[SORT_DIRECTION]?.let { SortDirection.valueOf(it) }
                ?: SortDirection.NEWEST_FIRST,
            autoAdvanceGroup = preferences[AUTO_ADVANCE_GROUP] ?: true,
            gestureShortcutEnabled = preferences[GESTURE_SHORTCUT_ENABLED] ?: true,
            themeMode = preferences[THEME_MODE]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM
        )
    }

    suspend fun setBurstThresholdMillis(value: Long) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[BURST_THRESHOLD_MILLIS] = value
        }
    }

    suspend fun setSortDirection(value: SortDirection) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[SORT_DIRECTION] = value.name
        }
    }

    suspend fun setAutoAdvanceGroup(value: Boolean) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[AUTO_ADVANCE_GROUP] = value
        }
    }

    suspend fun setGestureShortcutEnabled(value: Boolean) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[GESTURE_SHORTCUT_ENABLED] = value
        }
    }

    suspend fun setThemeMode(value: ThemeMode) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[THEME_MODE] = value.name
        }
    }

    companion object {
        private val BURST_THRESHOLD_MILLIS = longPreferencesKey("burst_threshold_millis")
        private val SORT_DIRECTION = stringPreferencesKey("sort_direction")
        private val AUTO_ADVANCE_GROUP = booleanPreferencesKey("auto_advance_group")
        private val GESTURE_SHORTCUT_ENABLED = booleanPreferencesKey("gesture_shortcut_enabled")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
