package com.gavr123456789.github.sovazeleboba

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SovaZeleBoba",
    ) {
        App()
    }
}