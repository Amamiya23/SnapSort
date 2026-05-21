package com.snapsort.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.snapsort.app.data.settings.ThemeMode
import com.snapsort.app.data.settings.UserSettings
import com.snapsort.app.ui.navigation.SnapSortApp
import com.snapsort.app.ui.theme.SnapSortTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val userSettingsRepository = remember { SnapSortDependencies.userSettingsRepository(context) }
            val settings by userSettingsRepository.settings.collectAsState(initial = UserSettings())

            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM, ThemeMode.DYNAMIC -> isSystemDark
            }
            val useDynamicColor = settings.themeMode == ThemeMode.DYNAMIC

            SnapSortTheme(darkTheme = isDarkTheme, dynamicColor = useDynamicColor) {
                SnapSortApp()
            }
        }
    }
}
