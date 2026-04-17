package com.group8.comp2300.platform.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION

@Composable
fun rememberNotificationPermissionRequester(): suspend () -> NotificationPermissionResult {
    val factory = rememberPermissionsControllerFactory()
    val controller: PermissionsController = remember(factory) { factory.createPermissionsController() }
    BindEffect(controller)

    return remember(controller) {
        suspend {
            try {
                controller.providePermission(Permission.REMOTE_NOTIFICATION)
                NotificationPermissionResult.GRANTED
            } catch (_: DeniedAlwaysException) {
                NotificationPermissionResult.DENIED_ALWAYS
            } catch (_: DeniedException) {
                NotificationPermissionResult.DENIED
            }
        }
    }
}
