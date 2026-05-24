@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.nxoim.sample.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.nxoim.sample.ui.SongRetrievalContext
import com.nxoim.sample.ui.theme.SampleTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SortingPanel(
    controller: SortingController,
    onClose: () -> Unit,
    sharedElementKey: Any? = null
) {
    val sorting by controller.sorting.collectAsState()
    val defaultSorting = SongRetrievalContext.Default.sorting

    PanelContainer(sharedElementKey) {
        PanelHeader(title = "Sorting", onClose = onClose, Modifier.padding(bottom = 8.dp)) {
            FilledTonalButton(
                onClick = { controller.resetSorting() },
                enabled = sorting != defaultSorting
            ) { Text("Reset") }
        }

        SortingPanelContent(controller = controller)
    }
}

@Composable
fun SortingPanelContent(
    controller: SortingController,
    itemColors: ListItemColors = panelItemColors()
) {
    val sorting by controller.sorting.collectAsState()

    var sortByDropDownVisible by remember { mutableStateOf(false) }
    var sortOrderDropDownVisible by remember { mutableStateOf(false) }

    ListItem(
        content = { Text("Sort By") },
        colors = itemColors,
        shapes = ListItemDefaults.segmentedShapes(0, 2),
        checked = sorting.by != SongRetrievalContext.Default.sorting.by,
        onCheckedChange = { sortByDropDownVisible = true },
        trailingContent = {
            Text(sorting.by.name)

            DropdownMenuPopup(
                sortByDropDownVisible,
                onDismissRequest = { sortByDropDownVisible = false }
            ) {
                DropdownMenuGroup(MenuDefaults.groupShapes()) {
                    SongRetrievalContext.Sorting.By.entries.fastForEach { by ->
                        SelectableDropdownMenuItem(
                            text = by.toString(),
                            checked = by == sorting.by,
                            onCheckedChange = {
                                controller.setSortBy(by)
                                sortByDropDownVisible = false
                            }
                        )
                    }
                }
            }
        }
    )

    ListItem(
        content = { Text("Order") },
        colors = itemColors,
        shapes = ListItemDefaults.segmentedShapes(1, 2),
        checked = sorting.order != SongRetrievalContext.Default.sorting.order,
        onCheckedChange = { sortOrderDropDownVisible = true },
        trailingContent = {
            Text(sorting.order.name)

            DropdownMenuPopup(
                sortOrderDropDownVisible,
                onDismissRequest = { sortOrderDropDownVisible = false }
            ) {
                DropdownMenuGroup(MenuDefaults.groupShapes()) {
                    SongRetrievalContext.Sorting.Order.entries.fastForEach { order ->
                        SelectableDropdownMenuItem(
                            text = order.name,
                            checked = order == sorting.order,
                            onCheckedChange = {
                                controller.setSortOrder(order)
                                sortOrderDropDownVisible = false
                            }
                        )
                    }
                }
            }
        }
    )
}

interface SortingController {
    val sorting: StateFlow<SongRetrievalContext.Sorting>
    fun setSortBy(new: SongRetrievalContext.Sorting.By)
    fun setSortOrder(new: SongRetrievalContext.Sorting.Order)
    fun resetSorting()
}

private object PreviewSortingController : SortingController {
    override val sorting = MutableStateFlow(SongRetrievalContext.Sorting())
    override fun setSortBy(new: SongRetrievalContext.Sorting.By) {}
    override fun setSortOrder(new: SongRetrievalContext.Sorting.Order) {}
    override fun resetSorting() {}
}

@Preview
@Composable
private fun SortingPanelPreview() {
    SampleTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SortingPanel(
                controller = PreviewSortingController,
                onClose = {}
            )
        }
    }
}
