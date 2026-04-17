package com.group8.comp2300.data.repository

import com.group8.comp2300.domain.model.education.*
import com.group8.comp2300.domain.repository.QuizRepository
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.data.QuizEnt

class QuizRepositoryImpl(
    private val database: ServerDatabase
) : QuizRepository {

    private val quizQueries = database.quizQueries

    override fun getQuizById(id: String): Quiz? {
        return quizQueries.getQuizById(id).executeAsOneOrNull()?.let { mapToDomain(it) }
    }

    override fun getQuizByArticleId(articleId: String): Quiz? {
        return quizQueries.getQuizByArticleId(articleId).executeAsOneOrNull()?.let { mapToDomain(it) }
    }

    override fun getAllQuizzes(): List<Quiz> {
        return quizQueries.getAllQuizzes().executeAsList().map { mapToDomain(it) }
    }

    override fun upsertQuiz(quiz: Quiz) {
        database.transaction {
            quizQueries.upsertQuiz(
                id = quiz.id,
                articleId = quiz.articleId,
                title = quiz.title
            )

            // Wipe existing Questions/Options to avoid duplicates
            database.quizQuestionQueries.deleteQuestionsByQuizId(quiz.id)

            // Insert Questions and nested Options
            quiz.questions.forEachIndexed { qIndex, question ->
                database.quizQuestionQueries.insertQuestion(
                    id = question.id,
                    quizId = quiz.id,
                    title = question.title,
                    explanation = question.explanation,
                    questionOrder = qIndex.toLong()
                )

                question.options.forEachIndexed { oIndex, option ->
                    database.quizOptionQueries.insertOption(
                        id = option.id,
                        questionId = question.id,
                        answerText = option.text,
                        isCorrect = if (option.isCorrect) 1L else 0L,
                        optionOrder = oIndex.toLong()
                    )
                }
            }
        }
    }

    override fun deleteQuiz(id: String) {
        quizQueries.deleteQuiz(id)
    }

    private fun mapToDomain(quizEnt: QuizEnt): Quiz {
        val questionsEnt = database.quizQuestionQueries.getAllQuestionsForQuiz(quizEnt.id).executeAsList()

        val questions = questionsEnt.map { qEnt ->
            val optionsEnt = database.quizOptionQueries.getAllOptionsForQuestion(qEnt.id).executeAsList()

            QuizQuestion(
                id = qEnt.id,
                title = qEnt.title,
                explanation = qEnt.explanation,
                options = optionsEnt.map { oEnt ->
                    QuizOption(
                        id = oEnt.id,
                        text = oEnt.answer_text,
                        isCorrect = oEnt.is_correct == 1L
                    )
                }
            )
        }

        return Quiz(
            id = quizEnt.id,
            articleId = quizEnt.article_id ?: "",
            title = quizEnt.title,
            questions = questions
        )
    }
}
