@file:OptIn(ExperimentalMaterial3Api::class)

package com.nxoim.sample.ui.components

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nxoim.sample.ui.theme.SampleTheme

@Composable
fun ControlButtons(
    searchController: SearchController,
    onFilterSelected: () -> Unit,
    onSortSelected: () -> Unit,
    filterSharedElementKey: Any? = null,
    sortingSharedElementKey: Any? = null
) {
    val colors = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilledTonalIconButton(
                onClick = onFilterSelected,
                modifier = Modifier
                    .withLocalTransitionContext(filterSharedElementKey) {
                        Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(it),
                            animatedVisibilityScope = this,
                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
                        )
                    }
                    .size(54.dp)
                    .controlShadow(FilterButtonShape),
                colors = colors,
                shape = FilterButtonShape,
            ) {
                Icon(Icons.Default.FilterAlt, contentDescription = null)
            }

            FilledTonalIconButton(
                onClick = onSortSelected,
                modifier = Modifier
                    .withLocalTransitionContext(sortingSharedElementKey) {
                        Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(it),
                            animatedVisibilityScope = this,
                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
                        )
                    }
                    .size(54.dp)
                    .controlShadow(SortButtonShape),
                colors = colors,
                shape = SortButtonShape
            ) {
                Icon(Icons.Default.FilterList, contentDescription = null)
            }
        }
        SearchBar(
            searchController,
            shadowElevation = 8.dp
        )
    }
}

@Preview
@Composable
private fun ControlButtonsPreview() {
    SampleTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ControlButtons(
                searchController = object : SearchController {
                    override fun onQuery(new: String) {}
                },
                onFilterSelected = {},
                onSortSelected = {}
            )
        }
    }
}

private val FilterButtonShape = RoundedCornerShape(32.dp, 8.dp, 8.dp, 32.dp)
private val SortButtonShape = RoundedCornerShape(8.dp, 32.dp, 32.dp, 8.dp)
@Composable
private fun Modifier.controlShadow(shape: Shape, color: Color = Color.Black) =
    this.shadow(
        elevation = 8.dp,
        shape = shape,
        spotColor = color,
        ambientColor = color
    )
