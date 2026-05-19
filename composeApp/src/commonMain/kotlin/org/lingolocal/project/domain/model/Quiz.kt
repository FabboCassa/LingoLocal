package org.lingolocal.project.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String
)

@Serializable
data class Quiz(
    val title: String,
    val questions: List<QuizQuestion>
)

enum class QuizFocus {
    GENERAL,
    GRAMMAR,
    VOCABULARY,
    WITH_EXAMPLES
}

