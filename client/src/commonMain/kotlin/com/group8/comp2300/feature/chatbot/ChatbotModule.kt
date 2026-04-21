package com.group8.comp2300.feature.chatbot

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val chatbotModule = module {
    viewModelOf(::ChatbotViewModel)
}
