package com.nxoim.sample.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateBounds
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp

@Composable
fun ScrollBarLabelHint(label: String, isPressed: Boolean) {
    LookaheadScope {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 4.dp,
            modifier = Modifier
                .layout() { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(0, 0) {
                        placeable.placeRelative(
                            if (isPressed)
                                -(placeable.width + 8.dp.roundToPx())
                            else
                                -placeable.width / 2,
                            -placeable.height / 2
                        )
                    }
                }
                .animateBounds(this)
        ) {
            AnimatedContent(isPressed) {
                if (it) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(16.dp, 8.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                } else {
                    Spacer(Modifier.size(8.dp))
                }
            }
        }
    }
}