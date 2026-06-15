package com.nxoim.sample.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nxoim.sample.ui.SongRetrievalContext
import com.nxoim.sample.ui.theme.SampleTheme
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun ControlsBottomBar(
    filterController: FilterController,
    sortingController: SortingController,
    searchController: SearchController
) {
    var selected by remember { mutableStateOf<Selected?>(null) }

    SharedTransitionLayout {
        Row(
            Modifier
                .surfaceFadeGradient(fadeDown = false)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                selected,
                contentAlignment = Alignment.BottomCenter
            ) { current ->
                ProvideSharedTransitionContext {
                    when (current) {
                        Selected.Filters -> FilterPanel(
                            modifier = Modifier.sharedBounds(
                                rememberSharedContentState("filter_panel"),
                                this
                            ),
                            controller = filterController,
                            onClose = { selected = null }
                        )

                        Selected.Sorting -> SortingPanel(
                            modifier = Modifier.sharedBounds(
                                rememberSharedContentState("sorting_panel"),
                                this
                            ),
                            controller = sortingController,
                            onClose = { selected = null }
                        )

                        null -> ControlButtons(
                            filterSharedElementKey = "filter_panel",
                            sortingSharedElementKey = "sorting_panel",
                            searchController = searchController,
                            onFilterSelected = { selected = Selected.Filters },
                            onSortSelected = { selected = Selected.Sorting }
                        )
                    }
                }
            }
        }
    }
}

sealed interface Selected {
    data object Filters : Selected
    data object Sorting : Selected
}

@Preview
@Composable
private fun ControlsBottomBarPreview() {
    SampleTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ControlsBottomBar(
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
