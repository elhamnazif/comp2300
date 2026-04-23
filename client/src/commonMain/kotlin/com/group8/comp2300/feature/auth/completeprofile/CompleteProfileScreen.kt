package com.group8.comp2300.feature.auth.completeprofile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.group8.comp2300.feature.profile.ProfileEditorMode
import com.group8.comp2300.feature.profile.ProfileEditorScreen

@Composable
fun CompleteProfileScreen(
    email: String,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    email
    ProfileEditorScreen(
        mode = ProfileEditorMode.Complete,
        onSaved = onComplete,
        onBack = onBack,
        modifier = modifier,
    )
}
