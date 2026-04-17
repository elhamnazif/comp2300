@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.group8.comp2300.platform.biometrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics

@Composable
actual fun isBiometricAvailable(): Boolean = remember {
    memScoped {
        val errorPointer = alloc<ObjCObjectVar<NSError?>>()
        LAContext().canEvaluatePolicy(
            policy = LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            error = errorPointer.ptr,
        )
    }
}
