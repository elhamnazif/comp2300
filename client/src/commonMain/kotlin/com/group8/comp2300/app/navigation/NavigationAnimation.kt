package com.group8.comp2300.app.navigation

import androidx.compose.animation.togetherWith
import com.group8.comp2300.core.ui.motion.materialSharedAxisXIn
import com.group8.comp2300.core.ui.motion.materialSharedAxisXOut

private const val InitialOffsetFactor = 0.10f

val pushAnimation =
    materialSharedAxisXIn(
        initialOffsetX = { (it * InitialOffsetFactor).toInt() },
    ) togetherWith
        materialSharedAxisXOut(
            targetOffsetX = {
                -(it * InitialOffsetFactor).toInt()
            },
        )

val popAnimation =
    materialSharedAxisXIn(
        initialOffsetX = { -(it * InitialOffsetFactor).toInt() },
    ) togetherWith
        materialSharedAxisXOut(
            targetOffsetX = {
                (it * InitialOffsetFactor).toInt()
            },
        )
