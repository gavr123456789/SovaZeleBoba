package com.gavr123456789.github.greeklearncompose

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "GreekLearnCompose",
    ) {
        App()
    }
}