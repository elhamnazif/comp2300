package com.group8.comp2300.feature.shop

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val shopModule = module {
    viewModelOf(::ShopViewModel)
}
