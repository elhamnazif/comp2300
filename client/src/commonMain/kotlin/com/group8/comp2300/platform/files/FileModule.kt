package com.group8.comp2300.platform.files

import org.koin.dsl.module

val fileModule = module {
    single { MedicalRecordFileOpener() }
}
