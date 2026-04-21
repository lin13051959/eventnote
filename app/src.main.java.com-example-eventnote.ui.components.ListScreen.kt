package com.example.eventnote.ui.components

import androidx.compose.material.*
import androidx.compose.runtime.Composable

@Composable
fun ListScreen() {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarData: String) -> it.text
            .color
        )
    }
}
