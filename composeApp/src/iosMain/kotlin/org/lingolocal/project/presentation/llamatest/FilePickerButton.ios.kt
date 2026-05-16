package org.lingolocal.project.presentation.llamatest

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun FilePickerButton(onPathSelected: (String) -> Unit, modifier: Modifier) {
    Button(onClick = {}, enabled = false, modifier = modifier) {
        Text("Sfoglia (iOS — Task 1.4b)")
    }
}
