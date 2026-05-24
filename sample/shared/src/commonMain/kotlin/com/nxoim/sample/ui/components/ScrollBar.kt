package com.nxoim.sample.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.nxoim.sample.ui.theme.SampleTheme
import kotlin.math.roundToInt

@Composable
fun ScrollBar(
    currentFraction: () -> Float,
    onFractionChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    indicator: (@Composable (fraction: Float, isDragging: Boolean) -> Unit)? = null,
) {
    val currentFraction = currentFraction()

    var isDragging by remember { mutableStateOf(false) }
    var internalFraction by remember { mutableStateOf(currentFraction) }
    val stabilizedFraction by animateFloatAsState(internalFraction)

    LaunchedEffect(currentFraction) {
        if (!isDragging) internalFraction = currentFraction
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onVerticalDrag = { change, _ ->
                        val pixelFraction = (change.position.y / size.height).coerceIn(0f, 1f)
                        internalFraction = pixelFraction
                        onFractionChanged(pixelFraction)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val pixelFraction = (offset.y / size.height).coerceIn(0f, 1f)
                    internalFraction = pixelFraction
                    onFractionChanged(pixelFraction)
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (isDragging)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
        )

        if (indicator != null) Box(
            modifier = Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(Constraints())
                layout(0, 0) {
                    placeable.placeRelative(
                        -placeable.width / 2,
                        (constraints.maxHeight * stabilizedFraction).roundToInt()
                    )
                }
            }
        ) {
            indicator(internalFraction, isDragging)
        }
    }
}

@Preview
@Composable
private fun ScrollBarPreview() {
    SampleTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ScrollBar(
                currentFraction = { 0.75f },
                onFractionChanged = {},
                indicator = { fraction, isDragging ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            )
        }
    }
}
