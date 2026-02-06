package com.WangWangPhone.ui

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.WangWangPhone.core.WallpaperDbHelper
import com.WangWangPhone.core.WallpaperType

@Composable
fun MainContainer() {
    var isLocked by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val wallpaperDbHelper = remember { WallpaperDbHelper(context) }
    val lockWallpaperPath = remember {
        wallpaperDbHelper.getWallpaperFilePath(WallpaperType.LOCK)
    }

    Crossfade(targetState = isLocked, label = "ScreenTransition") { locked ->
        if (locked) {
            LockScreen(
                onUnlock = { isLocked = false },
                lockWallpaperPath = lockWallpaperPath
            )
        } else {
            HomeScreen()
        }
    }
}