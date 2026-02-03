package com.WangWangPhone.ui

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.*

@Composable
fun MainContainer() {
    var isLocked by remember { mutableStateOf(true) }

    Crossfade(targetState = isLocked, label = "ScreenTransition") { locked ->
        if (locked) {
            LockScreen(onUnlock = { isLocked = false })
        } else {
            HomeScreen()
        }
    }
}