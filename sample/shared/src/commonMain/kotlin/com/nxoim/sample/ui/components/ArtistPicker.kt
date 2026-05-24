package com.nxoim.sample.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction.Companion.Done
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nxoim.sample.ui.theme.SampleTheme

@Composable
fun ArtistPicker(
    initialArtist: String?,
    onClear: () -> Unit,
    onApply: (String?) -> Unit
) {
    var artistInput by remember { mutableStateOf(initialArtist ?: "") }

    Column(
        Modifier.padding(16.dp).width(IntrinsicSize.Max),
        verticalArrangement = spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = artistInput,
            onValueChange = { artistInput = it },
            label = { Text("Artist") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = Done),
            keyboardActions = KeyboardActions(
                onDone = { onApply(artistInput.ifBlank { null }) }
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            FilledTonalIconButton(onClear) {
                Icon(Icons.Default.Clear, contentDescription = null)
            }

            FilledTonalIconButton(onClick = { onApply(artistInput.ifBlank { null }) }) {
                Icon(Icons.Default.Check, contentDescription = null)
            }
        }
    }
}

@Preview
@Composable
private fun ArtistPickerPreview() {
    SampleTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ArtistPicker(
                initialArtist = null,
                onClear = {},
                onApply = {}
            )
        }
    }
}

@Preview
@Composable
private fun ArtistPickerWithInputPreview() {
    SampleTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ArtistPicker(
                initialArtist = "Daft Punk",
                onClear = {},
                onApply = {}
            )
        }
    }
}
