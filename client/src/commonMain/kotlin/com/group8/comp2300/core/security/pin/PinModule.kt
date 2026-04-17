package com.group8.comp2300.core.security.pin

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val pinModule = module {
    viewModelOf(::PinLockViewModel)
}
