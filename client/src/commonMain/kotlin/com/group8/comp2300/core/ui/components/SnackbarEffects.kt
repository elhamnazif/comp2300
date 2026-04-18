package com.group8.comp2300.core.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun ConsumeSnackbarMessage(message: String?, snackbarHostState: SnackbarHostState, onConsumed: () -> Unit) {
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            onConsumed()
        }
    }
}
