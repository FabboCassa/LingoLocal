package org.lingolocal.project.data.llama

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Stub iOS per LlamaEngine.
 *
 * Task 1.4 (Android first): l'integrazione iOS arriverà in Task 1.4b
 * via cinterop verso `llama.xcframework` (Metal). Per ora questa actual
 * mantiene la compilazione verde sul target iOS senza fornire inferenza.
 */
actual class LlamaEngine actual constructor() {

    private var loaded = false

    actual suspend fun init() {
        // no-op: backend non disponibile in questo stub
    }

    actual suspend fun loadModel(modelPath: String, contextSize: Int, threads: Int): Boolean {
        loaded = false
        return false
    }

    actual fun generate(prompt: String, imageBytes: ByteArray?, maxTokens: Int): Flow<String> = flow {
        if (imageBytes != null) {
            val simulatedJson = getSimulatedVisionResponse(prompt)
            for (char in simulatedJson) {
                emit(char.toString())
                kotlinx.coroutines.delay(10)
            }
        } else {
            emit("[iOS LlamaEngine non ancora implementato — Task 1.4b]")
        }
    }

    actual fun generateChat(systemPrompt: String, userMessage: String, maxTokens: Int): Flow<String> = flow {
        emit("[iOS LlamaEngine non ancora implementato — Task 1.4b]")
    }

    private fun getSimulatedVisionResponse(prompt: String): String {
        val promptLower = prompt.lowercase()
        return if (promptLower.contains("ricevuta") || promptLower.contains("scontrino") || promptLower.contains("receipt")) {
            """{
  "tipo": "Ricevuta/Scontrino",
  "lingua_originale": "it",
  "testo_estratto": "CONAD SUPERMERCATO\nVia Roma 12, Milano\n\n1x PANE INTEGRALE - 1.80€\n2x LATTE PARMALAT - 2.40€\n1x PASTA BARILLA - 0.99€\n\nTOTALE: 5.19€\nGRAZIE E ARRIVEDERCI!",
  "traduzione": "CONAD SUPERMARKET\nVia Roma 12, Milan\n\n1x WHOLEMEAL BREAD - 1.80€\n2x PARMALAT MILK - 2.40€\n1x BARILLA PASTA - 0.99€\n\nTOTAL: 5.19€\nTHANK YOU AND GOODBYE!",
  "entita": [
    {"chiave": "Negozio", "valore": "CONAD SUPERMERCATO"},
    {"chiave": "Totale", "valore": "5.19€"},
    {"chiave": "Data", "valore": "19/05/2026"}
  ],
  "vocaboli_chiave": [
    {"originale": "pane", "traduzione": "bread", "pronuncia": "/'pane/"},
    {"originale": "latte", "traduzione": "milk", "pronuncia": "/'latte/"},
    {"originale": "pasta", "traduzione": "pasta", "pronuncia": "/'pasta/"}
  ]
}"""
        } else if (promptLower.contains("grammatica") || promptLower.contains("grammar")) {
            """{
  "tipo": "Pagina di Grammatica",
  "lingua_originale": "it",
  "argomento": "Il Condizionale Presente",
  "testo_estratto": "Il condizionale presente si usa per esprimere un desiderio, un dubbio o un'opinione personale.\nEsempi:\n- Vorrei un caffè (desiderio).\n- Potresti aiutarmi? (cortesia).",
  "traduzione": "The present conditional is used to express a desire, a doubt, or a personal opinion.\nExamples:\n- I would like a coffee (desire).\n- Could you help me? (politeness).",
  "regole_grammaticali": [
    {"regola": "Condizionale del verbo volere", "dettaglio": "vorrei, vorresti, vorrebbe, vorremmo, vorreste, vorrebbero"},
    {"regola": "Uso di cortesia", "dettaglio": "Si usa per attenuare una richiesta (es. potresti, vorresti)."}
  ],
  "vocaboli_chiave": [
    {"originale": "vorrei", "traduzione": "I would like", "pronuncia": "/vor'rɛi/"},
    {"originale": "potresti", "traduzione": "you could", "pronuncia": "/po'trɛsti/"}
  ]
}"""
        } else {
            """{
  "tipo": "Analisi Immagine",
  "lingua_originale": "it",
  "testo_estratto": "Benvenuto in LingoLocal! Questa è una dimostrazione del motore di acquisizione visiva locale.",
  "traduzione": "Welcome to LingoLocal! This is a demonstration of the local visual acquisition engine.",
  "entita": [
    {"chiave": "App", "valore": "LingoLocal"},
    {"chiave": "Stato", "valore": "Dimostrazione"}
  ],
  "vocaboli_chiave": [
    {"originale": "benvenuto", "traduzione": "welcome", "pronuncia": "/benve'nuto/"}
  ]
}"""
        }
    }

    actual suspend fun unloadModel() {
        loaded = false
    }

    actual fun isModelLoaded(): Boolean = loaded

    actual suspend fun embed(text: String): FloatArray? = null
}
