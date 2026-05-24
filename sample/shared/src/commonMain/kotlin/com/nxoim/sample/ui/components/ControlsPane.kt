@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.nxoim.sample.ui.components

import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nxoim.sample.ui.SongRetrievalContext
import com.nxoim.sample.ui.theme.SampleTheme
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlsPane(
    filterController: FilterController,
    sortingController: SortingController,
    searchController: SearchController,
    modifier: Modifier = Modifier
) {
    val filters by filterController.filters.collectAsState()
    val sorting by sortingController.sorting.collectAsState()

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .systemBarsPadding()
            .padding(16.dp),
        verticalArrangement = spacedBy(4.dp)
    ) {
        SearchBar(
            searchController,
            modifier = Modifier.fillMaxWidth(),
            colors = SearchBarDefaults.colors(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
        )

        EmbeddedPanelHeader(
            title = "Filtering",
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            FilledTonalButton(
                onClick = { filterController.resetFilters() },
                enabled = filters != SongRetrievalContext.Default.filters
            ) { Text("Reset") }
        }

        FilterPanelContent(controller = filterController)

        EmbeddedPanelHeader(
            title = "Sorting",
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        ) {
            FilledTonalButton(
                onClick = { sortingController.resetSorting() },
                enabled = sorting != SongRetrievalContext.Default.sorting
            ) { Text("Reset") }
        }

        SortingPanelContent(controller = sortingController)
    }
}

@Preview
@Composable
private fun ControlsPanePreview() {
    SampleTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ControlsPane(
                filterController = object : FilterController {
                    override val filters = MutableStateFlow(SongRetrievalContext.Filters())
                    override fun setGenre(new: String?) {}
                    override fun setYearRange(new: IntRange?) {}
                    override fun setExplicit(new: Boolean?) {}
                    override fun setArtist(new: String?) {}
                    override fun resetFilters() {}
                },
                sortingController = object : SortingController {
                    override val sorting = MutableStateFlow(SongRetrievalContext.Sorting())
                    override fun setSortBy(new: SongRetrievalContext.Sorting.By) {}
                    override fun setSortOrder(new: SongRetrievalContext.Sorting.Order) {}
                    override fun resetSorting() {}
                },
                searchController = object : SearchController {
                    override fun onQuery(new: String) {}
                }
            )
        }
    }
}
