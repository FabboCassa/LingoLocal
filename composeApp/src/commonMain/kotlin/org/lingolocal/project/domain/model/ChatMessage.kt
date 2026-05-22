package org.lingolocal.project.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val generatedQuiz: Quiz? = null,
    val isError: Boolean = false,
    val isGenerating: Boolean = false
)
