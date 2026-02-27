package com.group8.comp2300.mock

data class OnboardingQuestion(val id: Int, val text: String, val options: List<String>)

val sampleOnboardingQuestions =
    listOf(
        OnboardingQuestion(
            id = 1,
            text = "In the last 3 months, how many sexual partners have you had?",
            options = listOf("0", "1", "2-5", "5+"),
        ),
        OnboardingQuestion(
            id = 2,
            text = "How often do you use protection (condoms/PrEP)?",
            options = listOf("Always", "Sometimes", "Never"),
        ),
    )
