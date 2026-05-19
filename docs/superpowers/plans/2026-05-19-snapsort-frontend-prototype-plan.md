# SnapSort Frontend Prototype Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the frontend UI prototype for SnapSort in Android Jetpack Compose based on the 2026-05-19 design spec, covering empty states, scanning progress, grouping list, photo selection, and delete confirmation UI. Mock data and mock ViewModels will be used since other models will implement the core business logic later.

**Architecture:** Single Activity, Jetpack Compose Navigation, MVVM pattern (with mock ViewModels).

**Tech Stack:** Android, Kotlin, Jetpack Compose, Material Design 3, Navigation Compose, Coil (mocked/placeholders).

---

### Task 1: Initialize Android Compose Project Scaffold

**Files:**
- Create: `build.gradle.kts` (Project and Module)
- Create: `settings.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/snapsort/app/MainActivity.kt`
- Create: `app/src/main/java/com/snapsort/app/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/snapsort/app/ui/theme/Color.kt`
- Create: `app/src/main/java/com/snapsort/app/ui/theme/Type.kt`

- [ ] **Step 1: Set up project build configurations**
Create the minimal Gradle setup for a Jetpack Compose Android app.

```kotlin
// settings.gradle.kts
rootProject.name = "SnapSort"
include(":app")
```

- [ ] **Step 2: Create AndroidManifest and MainActivity**
```xml
<!-- app/src/main/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.snapsort.app">
    <application
        android:label="SnapSort"
        android:theme="@style/Theme.SnapSort">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

```kotlin
// app/src/main/java/com/snapsort/app/MainActivity.kt
package com.snapsort.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.snapsort.app.ui.theme.SnapSortTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SnapSortTheme {
                // MainApp() will go here
            }
        }
    }
}
```

- [ ] **Step 3: Define basic Material 3 Theme**
Setup restrained colors following the spec (neutral colors with accent for progress/danger).

- [ ] **Step 4: Commit**
```bash
git init
git add .
git commit -m "chore: init compose project scaffold"
```

### Task 2: Define Navigation and Route Structure

**Files:**
- Create: `app/src/main/java/com/snapsort/app/ui/navigation/SnapSortNavigation.kt`

- [ ] **Step 1: Create Navigation Graph**
```kotlin
// app/src/main/java/com/snapsort/app/ui/navigation/SnapSortNavigation.kt
package com.snapsort.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun SnapSortApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { /* HomeScreen() */ }
        composable("scan_progress") { /* ScanProgressScreen() */ }
        composable("group_selection/{groupId}") { /* PhotoSelectionScreen() */ }
        composable("settings") { /* SettingsScreen() */ }
    }
}
```

- [ ] **Step 2: Commit**
```bash
git add app/src/main/java/com/snapsort/app/ui/navigation
git commit -m "feat: setup navigation graph"
```

### Task 3: Build Home Screen (Empty State & Task Dashboard)

**Files:**
- Create: `app/src/main/java/com/snapsort/app/ui/home/HomeScreen.kt`
- Create: `app/src/main/java/com/snapsort/app/ui/home/HomeViewModel.kt` (Mock)

- [ ] **Step 1: Create Mock ViewModel for Home**
Include mock data for the 2 states: Empty and Active Task.

- [ ] **Step 2: Implement HomeScreen UI**
Build the top bar (Status, Folder Name, Actions) and a LazyColumn for the group list. Use placeholder colored boxes for image thumbnails.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/snapsort/app/ui/home
git commit -m "feat: implement home screen UI prototype"
```

### Task 4: Build Photo Selection Screen

**Files:**
- Create: `app/src/main/java/com/snapsort/app/ui/selection/PhotoSelectionScreen.kt`

- [ ] **Step 1: Implement Pager UI**
Use HorizontalPager to allow swiping between photos.

- [ ] **Step 2: Add Top & Bottom Bars**
Top bar: Group info, index, RAW status.
Bottom bar: Prev, Next, Mark Delete, Done.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/snapsort/app/ui/selection
git commit -m "feat: implement photo selection pager screen"
```

### Task 5: Build Delete Confirmation & Progress Screens

**Files:**
- Create: `app/src/main/java/com/snapsort/app/ui/components/DeleteConfirmationSheet.kt`
- Create: `app/src/main/java/com/snapsort/app/ui/scan/ScanProgressScreen.kt`

- [ ] **Step 1: Implement ModalBottomSheet for Delete Confirmation**
Show summary (JPGs, RAWs) and expandable file list.

- [ ] **Step 2: Implement Scan Progress UI**
Show stage, count, and cancel button.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/snapsort/app/ui/components app/src/main/java/com/snapsort/app/ui/scan
git commit -m "feat: implement delete confirmation and scan progress UI"
```
