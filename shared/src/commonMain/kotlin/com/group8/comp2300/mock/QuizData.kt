package com.group8.comp2300.mock

import com.group8.comp2300.domain.model.education.ContentCategory
import com.group8.comp2300.domain.model.education.Quiz
import com.group8.comp2300.domain.model.education.QuizQuestion

val allQuizzes =
    listOf(
        Quiz(
            id = "conception-basics",
            title = "Conception Basics",
            description = "Test your knowledge about fertility and conception",
            category = ContentCategory.SEXUAL_HEALTH,
            questions =
            listOf(
                QuizQuestion(
                    question =
                    "When is a person most likely to conceive?",
                    options =
                    listOf(
                        "During menstruation",
                        "5-7 days before ovulation and day of ovulation",
                        "Immediately after menstruation",
                        "Any time during the cycle",
                    ),
                    correctAnswerIndex = 1,
                    explanation =
                    "Sperm can survive 5-7 days in the reproductive tract, and ovulation is " +
                        "when an egg is released. The fertile window is the days when sperm can " +
                        "meet an egg.",
                ),
                QuizQuestion(
                    question =
                    "How long does a typical menstrual cycle last?",
                    options =
                    listOf(
                        "21 days",
                        "28 days",
                        "35 days",
                        "It varies greatly for everyone",
                    ),
                    correctAnswerIndex = 3,
                    explanation =
                    "While 28 days is often cited as average, normal menstrual cycles can range " +
                        "from 21-35 days. Every person's cycle is unique and can vary month to month.",
                ),
                QuizQuestion(
                    question =
                    "Which contraception method is most effective at preventing pregnancy?",
                    options =
                    listOf(
                        "Birth control pill",
                        "Condoms",
                        "IUD or implant",
                        "Withdrawal method",
                    ),
                    correctAnswerIndex = 2,
                    explanation =
                    "Long-acting reversible contraception (LARC) like IUDs and implants are over " +
                        "99% effective. Pills are 91% effective with typical use, condoms 85%, " +
                        "and withdrawal only 78%.",
                ),
                QuizQuestion(
                    question = "What is ovulation?",
                    options =
                    listOf(
                        "The monthly period",
                        "When an egg is released from the ovary",
                        "When the uterine lining sheds",
                        "The first day of menstruation",
                    ),
                    correctAnswerIndex = 1,
                    explanation =
                    "Ovulation is when a mature egg is released from the ovary and travels down " +
                        "the fallopian tube, where it can potentially be fertilized by sperm.",
                ),
                QuizQuestion(
                    question =
                    "Can you get pregnant from pre-ejaculate (pre-cum)?",
                    options =
                    listOf(
                        "No, it contains no sperm",
                        "Yes, it can contain sperm",
                        "Only if ejaculation has occurred recently",
                        "Only during ovulation",
                    ),
                    correctAnswerIndex = 1,
                    explanation =
                    "Yes, pre-ejaculate can contain sperm, especially if there has been a recent " +
                        "ejaculation. This is why withdrawal is not a reliable contraception method.",
                ),
                QuizQuestion(
                    question =
                    "How soon can pregnancy be detected after conception?",
                    options =
                    listOf(
                        "Immediately after sex",
                        "Within 24 hours",
                        "About 6-12 days (when implantation occurs)",
                        "Only after a missed period",
                    ),
                    correctAnswerIndex = 2,
                    explanation =
                    "Pregnancy tests detect hCG hormone, which is produced after the fertilized " +
                        "egg implants in the uterus, typically 6-12 days after conception. However, " +
                        "tests are most accurate after a missed period.",
                ),
                QuizQuestion(
                    question =
                    "Which factor does NOT directly affect fertility?",
                    options =
                    listOf(
                        "Age",
                        "Stress levels",
                        "Hair color",
                        "Smoking",
                    ),
                    correctAnswerIndex = 2,
                    explanation =
                    "Age, stress, and smoking can all impact fertility. Hair color has no known " +
                        "effect on fertility. Other factors include weight, certain medications, " +
                        "and underlying health conditions.",
                ),
            ),
        ),
    )

fun getQuizById(id: String): Quiz? = allQuizzes.find { it.id == id }
