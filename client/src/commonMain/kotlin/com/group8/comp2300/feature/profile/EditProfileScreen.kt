package com.group8.comp2300.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun EditProfileScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileEditorScreen(
        mode = ProfileEditorMode.Edit,
        onSaved = onSaved,
        onBack = onBack,
        modifier = modifier,
    )
}
