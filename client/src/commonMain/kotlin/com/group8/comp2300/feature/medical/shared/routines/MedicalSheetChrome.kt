package com.group8.comp2300.feature.medical.shared.routines

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.group8.comp2300.platform.notifications.NotificationPermissionResult
import com.group8.comp2300.platform.notifications.rememberNotificationPermissionRequester
import kotlinx.coroutines.CoroutineScope

@Stable
internal class MedicalSheetChrome(
    val snackbarHostState: SnackbarHostState,
    val coroutineScope: CoroutineScope,
    val requestNotificationPermission: suspend () -> NotificationPermissionResult,
    val sheetState: SheetState,
) {
    var showSheet by mutableStateOf(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberMedicalSheetChrome(): MedicalSheetChrome {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val requestNotificationPermission = rememberNotificationPermissionRequester()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    return remember(snackbarHostState, coroutineScope, requestNotificationPermission, sheetState) {
        MedicalSheetChrome(
            snackbarHostState = snackbarHostState,
            coroutineScope = coroutineScope,
            requestNotificationPermission = requestNotificationPermission,
            sheetState = sheetState,
        )
    }
}
