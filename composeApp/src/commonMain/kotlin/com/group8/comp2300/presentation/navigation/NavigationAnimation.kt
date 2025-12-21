package com.group8.comp2300.presentation.navigation

import androidx.compose.animation.togetherWith
import com.group8.comp2300.presentation.motion.materialSharedAxisXIn
import com.group8.comp2300.presentation.motion.materialSharedAxisXOut

private const val INITIAL_OFFSET_FACTOR = 0.10f

val pushAnimation =
    materialSharedAxisXIn(
        initialOffsetX = { (it * INITIAL_OFFSET_FACTOR).toInt() },
    ) togetherWith
        materialSharedAxisXOut(
            targetOffsetX = {
                -(it * INITIAL_OFFSET_FACTOR).toInt()
            },
        )

val popAnimation =
    materialSharedAxisXIn(
        initialOffsetX = { -(it * INITIAL_OFFSET_FACTOR).toInt() },
    ) togetherWith
        materialSharedAxisXOut(
            targetOffsetX = {
                (it * INITIAL_OFFSET_FACTOR).toInt()
            },
        )
