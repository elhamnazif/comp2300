package com.group8.comp2300.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.ui.components.ScreenHeader

@Composable
fun GuestSignInScreen(onRequireAuth: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScreenHeader(horizontalPadding = 24.dp) {
            Spacer(Modifier.height(16.dp))
            NotLoggedInContent(onRequireAuth = onRequireAuth)
            Spacer(Modifier.height(32.dp))
        }
    }
}
