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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.nxoim.sample.ui.theme.SampleTheme

@Composable
fun YearRangePicker(
    initialRange: IntRange?,
    onClear: () -> Unit,
    onApply: (IntRange) -> Unit
) {
    var startYear by remember { mutableStateOf(initialRange?.first ?: 2000) }
    var endYear by remember { mutableStateOf(initialRange?.last ?: 2025) }

    Column(
        Modifier.padding(16.dp).width(IntrinsicSize.Max),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = spacedBy(8.dp)
    ) {
        listOf(
            "Start Year" to startYear,
            "End Year" to endYear
        ).fastForEachIndexed { index, (label, value) ->
            Text(label, style = MaterialTheme.typography.labelMedium)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (index == 0 && value > 1900) startYear--
                        else if (index == 1 && value > startYear) endYear--
                    }
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null)
                }

                Text(value.toString())

                IconButton(
                    onClick = {
                        if (index == 0 && value < endYear) startYear++
                        else endYear++
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            FilledTonalIconButton(onClear) {
                Icon(Icons.Default.Clear, contentDescription = null)
            }

            FilledTonalIconButton(onClick = { onApply(startYear..endYear) }) {
                Icon(Icons.Default.Check, contentDescription = null)
            }
        }
    }
}

@Preview
@Composable
private fun YearRangePickerPreview() {
    SampleTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            YearRangePicker(
                initialRange = 2010..2024,
                onClear = {},
                onApply = {}
            )
        }
    }
}

@Preview
@Composable
private fun YearRangePickerDefaultPreview() {
    SampleTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            YearRangePicker(
                initialRange = null,
                onClear = {},
                onApply = {}
            )
        }
    }
}
