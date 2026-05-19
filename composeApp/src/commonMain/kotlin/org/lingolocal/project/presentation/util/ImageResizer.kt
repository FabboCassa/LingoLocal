package org.lingolocal.project.presentation.util

/**
 * Ridimensiona e comprime un'immagine rappresentata come ByteArray.
 * Preserva l'aspect ratio originale e riduce la dimensione massima al valore indicato.
 *
 * @param imageBytes I byte dell'immagine originale.
 * @param maxDimension La dimensione massima consentita per larghezza o altezza.
 * @param compressionQuality La qualità della compressione JPEG (da 1 a 100).
 * @return I byte dell'immagine compressa e ridimensionata in formato JPEG.
 */
expect fun resizeAndCompressImage(
    imageBytes: ByteArray,
    maxDimension: Int = 1024,
    compressionQuality: Int = 80
): ByteArray
