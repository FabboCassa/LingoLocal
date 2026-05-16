package org.lingolocal.project.presentation.llamatest

/**
 * Stato immutabile della UI di test per l'inferenza LLM.
 */
data class LlamaTestUiState(
    val phase: Phase = Phase.Idle,
    val modelPathInput: String = "",
    val promptInput: String = "",
    val generatedText: String = "",
    val errorMessage: String? = null
) {
    enum class Phase { Idle, Initializing, Loading, Loaded, Generating, LoadFailed, Error }

    val isLoadEnabled: Boolean
        get() = phase == Phase.Idle || phase == Phase.LoadFailed || phase == Phase.Loaded || phase == Phase.Error

    val isGenerateEnabled: Boolean
        get() = phase == Phase.Loaded

    val isUnloadEnabled: Boolean
        get() = phase == Phase.Loaded || phase == Phase.Error
}
