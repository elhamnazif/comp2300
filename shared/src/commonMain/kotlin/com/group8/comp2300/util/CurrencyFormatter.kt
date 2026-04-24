package com.group8.comp2300.util

import net.sergeych.sprintf.format

fun formatCurrency(value: Double): String = "RM %.2f".format(value)
