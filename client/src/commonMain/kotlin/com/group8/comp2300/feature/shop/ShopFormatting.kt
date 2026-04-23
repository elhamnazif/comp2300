package com.group8.comp2300.feature.shop

import net.sergeych.sprintf.format

internal fun formatShopCurrency(value: Double): String = "$%.2f".format(value)
