package com.nxoim.sample.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nxoim.sample.ui.theme.SampleTheme

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TopAppBar(songCount: Int?) {
    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Songs")
                Crossfade(songCount) {
                    if (it == null) {
                        LinearProgressIndicator()
                    } else {
                        Text(
                            "found $songCount songs",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors().copy(containerColor = Color.Transparent),
        modifier = Modifier.surfaceFadeGradient(fadeDown = true)
    )
}

@Preview
@Composable
private fun TopAppBarLoadingPreview() {
    SampleTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TopAppBar(songCount = null)
        }
    }
}

@Preview
@Composable
private fun TopAppBarLoadedPreview() {
    SampleTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TopAppBar(songCount = 420)
        }
    }
}
