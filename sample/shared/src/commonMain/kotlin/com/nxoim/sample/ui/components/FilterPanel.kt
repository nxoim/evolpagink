@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.nxoim.sample.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.nxoim.sample.ui.SongSource.Companion.genres
import com.nxoim.sample.ui.theme.SampleTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun FilterPanel(
    controller: FilterController,
    onClose: () -> Unit,
    sharedElementKey: Any? = null
) {
    val filters by controller.filters.collectAsState()
    val defaultFilters = SongRetrievalContext.Default.filters

    PanelContainer(sharedElementKey) {
        PanelHeader(title = "Filtering", onClose = onClose, Modifier.padding(bottom = 8.dp)) {
            FilledTonalButton(
                onClick = { controller.resetFilters() },
                enabled = filters != defaultFilters
            ) { Text("Reset") }
        }

        FilterPanelContent(controller = controller)
    }
}

@Composable
fun FilterPanelContent(
    controller: FilterController,
    itemColors: ListItemColors = panelItemColors()
) {
    val filters by controller.filters.collectAsState()
    val defaultFilters = SongRetrievalContext.Default.filters

    var genreDropDownVisible by remember { mutableStateOf(false) }
    var yearPickerVisible by remember { mutableStateOf(false) }
    var explicitDropDownVisible by remember { mutableStateOf(false) }
    var artistDropDownVisible by remember { mutableStateOf(false) }

    ListItem(
        content = { Text("Genre") },
        colors = itemColors,
        shapes = ListItemDefaults.segmentedShapes(0, 4),
        checked = filters.genreId != defaultFilters.genreId,
        onCheckedChange = { genreDropDownVisible = true },
        trailingContent = {
            Text(filters.genreId ?: "None")

            DropdownMenuPopup(
                genreDropDownVisible,
                onDismissRequest = { genreDropDownVisible = false }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DropdownMenuGroup(MenuDefaults.groupShapes()) {
                        genres.fastForEach { genre ->
                            SelectableDropdownMenuItem(
                                text = genre,
                                checked = genre == filters.genreId,
                                onCheckedChange = {
                                    controller.setGenre(genre)
                                    genreDropDownVisible = false
                                }
                            )
                        }
                    }
                    ClearDropdownMenuGroup(
                        onClick = {
                            controller.setGenre(null)
                            genreDropDownVisible = false
                        }
                    )
                }
            }
        }
    )

    ListItem(
        content = { Text("Year Range") },
        colors = itemColors,
        shapes = ListItemDefaults.segmentedShapes(1, 4),
        checked = filters.yearRange != defaultFilters.yearRange,
        onCheckedChange = { yearPickerVisible = true },
        trailingContent = {
            Text(filters.yearRange?.toString() ?: "None")

            DropdownMenuPopup(
                yearPickerVisible,
                onDismissRequest = { yearPickerVisible = false }
            ) {
                DropdownMenuGroup(MenuDefaults.groupShapes()) {
                    YearRangePicker(
                        initialRange = filters.yearRange,
                        onClear = {
                            controller.setYearRange(null)
                            yearPickerVisible = false
                        },
                        onApply = { range ->
                            controller.setYearRange(range)
                            yearPickerVisible = false
                        }
                    )
                }
            }
        }
    )

    ListItem(
        content = { Text("Explicit") },
        colors = itemColors,
        shapes = ListItemDefaults.segmentedShapes(2, 4),
        checked = filters.isExplicit != defaultFilters.isExplicit,
        onCheckedChange = { explicitDropDownVisible = true },
        trailingContent = {
            Text(
                when (filters.isExplicit) {
                    true -> "Yes"
                    false -> "No"
                    null -> "None"
                }
            )

            DropdownMenuPopup(
                explicitDropDownVisible,
                onDismissRequest = { explicitDropDownVisible = false }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DropdownMenuGroup(MenuDefaults.groupShapes()) {
                        listOf(true to "Yes", false to "No").forEach { (value, label) ->
                            SelectableDropdownMenuItem(
                                text = label,
                                checked = filters.isExplicit == value,
                                onCheckedChange = {
                                    controller.setExplicit(value)
                                    explicitDropDownVisible = false
                                }
                            )
                        }
                    }

                    ClearDropdownMenuGroup(
                        onClick = {
                            controller.setExplicit(null)
                            explicitDropDownVisible = false
                        }
                    )
                }
            }
        }
    )

    ListItem(
        content = { Text("Artist") },
        colors = itemColors,
        shapes = ListItemDefaults.segmentedShapes(3, 4),
        checked = filters.artist != defaultFilters.artist,
        onCheckedChange = { artistDropDownVisible = true },
        trailingContent = {
            Text(filters.artist ?: "None")

            DropdownMenuPopup(
                artistDropDownVisible,
                onDismissRequest = { artistDropDownVisible = false }
            ) {
                DropdownMenuGroup(MenuDefaults.groupShapes()) {
                    ArtistPicker(
                        initialArtist = filters.artist,
                        onClear = {
                            controller.setArtist(null)
                            artistDropDownVisible = false
                        },
                        onApply = { artist ->
                            controller.setArtist(artist)
                            artistDropDownVisible = false
                        }
                    )
                }
            }
        }
    )
}

interface FilterController {
    val filters: StateFlow<SongRetrievalContext.Filters>
    fun setGenre(new: String?)
    fun setYearRange(new: IntRange?)
    fun setExplicit(new: Boolean?)
    fun setArtist(new: String?)
    fun resetFilters()
}

private object PreviewFilterController : FilterController {
    override val filters = MutableStateFlow(SongRetrievalContext.Filters())
    override fun setGenre(new: String?) {}
    override fun setYearRange(new: IntRange?) {}
    override fun setExplicit(new: Boolean?) {}
    override fun setArtist(new: String?) {}
    override fun resetFilters() {}
}

@Preview
@Composable
private fun FilterPanelPreview() {
    SampleTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            FilterPanel(
                controller = PreviewFilterController,
                onClose = {}
            )
        }
    }
}
