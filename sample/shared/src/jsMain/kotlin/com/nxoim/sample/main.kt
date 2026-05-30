package com.nxoim.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import com.nxoim.sample.ui.App

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        Box(Modifier.fillMaxSize()) {
            App()
        }
    }
}