package com.snapsort.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.snapsort.app.ui.navigation.SnapSortApp
import com.snapsort.app.ui.theme.SnapSortTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SnapSortTheme {
                SnapSortApp()
            }
        }
    }
}
