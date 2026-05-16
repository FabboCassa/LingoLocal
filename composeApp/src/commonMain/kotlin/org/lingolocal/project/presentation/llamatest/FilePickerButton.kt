package org.lingolocal.project.presentation.llamatest

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Pulsante per aprire il file explorer del device e selezionare un file GGUF.
 * Quando la selezione è completata, [onPathSelected] viene chiamato con il
 * percorso assoluto del file (già copiato nella directory interna dell'app
 * se necessario per la piattaforma).
 */
@Composable
expect fun FilePickerButton(
    onPathSelected: (String) -> Unit,
    modifier: Modifier = Modifier
)
