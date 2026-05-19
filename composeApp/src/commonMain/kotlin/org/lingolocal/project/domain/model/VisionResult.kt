package org.lingolocal.project.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class VisionEntity(
    val chiave: String,
    val valore: String
)

@Serializable
data class VisionGrammarRule(
    val regola: String,
    val dettaglio: String
)

@Serializable
data class VisionVocabularyWord(
    val originale: String,
    val traduzione: String,
    val pronuncia: String
)

@Serializable
data class VisionResult(
    val tipo: String,
    val lingua_originale: String,
    val argomento: String? = null,
    val testo_estratto: String,
    val traduzione: String,
    val entita: List<VisionEntity>? = null,
    @SerialName("regele_grammaticali")
    val regoleGrammaticali: List<VisionGrammarRule>? = null,
    val vocaboli_chiave: List<VisionVocabularyWord>? = null
)
