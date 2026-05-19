package org.lingolocal.project.presentation.util

import androidx.compose.runtime.Composable

/**
 * Launcher astratto per attivare la selezione visiva sulla piattaforma di runtime.
 */
interface ImagePickerLauncher {
    /**
     * Apre il selettore per caricare un'immagine dalla galleria.
     * Gestito in modo permissionless su piattaforme moderne.
     */
    fun launchGallery()

    /**
     * Richiede il permesso della fotocamera e avvia l'acquisizione dell'immagine.
     */
    fun launchCamera()
}

/**
 * Composable expect per inizializzare il launcher dell'Image Picker.
 * Gestisce automaticamente il ciclo di vita, i callback e le richieste di permesso.
 *
 * @param onImagePicked Callback invocato quando un'immagine viene acquisita o selezionata con successo (restituisce il ByteArray grezzo) o null se annullato.
 * @param onPermissionDenied Callback invocato quando l'utente nega esplicitamente un permesso necessario (es. fotocamera).
 */
@Composable
expect fun rememberImagePicker(
    onImagePicked: (ByteArray?) -> Unit,
    onPermissionDenied: (String) -> Unit
): ImagePickerLauncher
