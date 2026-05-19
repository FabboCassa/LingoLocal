package org.lingolocal.project.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lingolocal.project.presentation.util.resizeAndCompressImage

/**
 * Use Case per il pre-processamento e l'ottimizzazione off-thread delle immagini.
 * Ridimensiona e comprime le immagini per rispettare i limiti del modello AI mobile.
 */
class PreprocessImageUseCase {
    
    suspend operator fun invoke(
        imageBytes: ByteArray,
        maxDimension: Int = 1024,
        compressionQuality: Int = 80
    ): ByteArray = withContext(Dispatchers.Default) {
        resizeAndCompressImage(imageBytes, maxDimension, compressionQuality)
    }
}
