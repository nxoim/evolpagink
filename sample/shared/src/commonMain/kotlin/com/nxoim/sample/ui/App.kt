@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package com.nxoim.sample.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.nxoim.evolpagink.compose.PageableComposeState
import com.nxoim.evolpagink.compose.itemsIndexed
import com.nxoim.evolpagink.compose.toState
import com.nxoim.sample.ui.components.ControlsBottomBar
import com.nxoim.sample.ui.components.ControlsPane
import com.nxoim.sample.ui.components.ScrollBar
import com.nxoim.sample.ui.components.ScrollBarLabelHint
import com.nxoim.sample.ui.components.SongItem
import com.nxoim.sample.ui.components.SongItemPlaceholder
import com.nxoim.sample.ui.components.SongItemShape
import com.nxoim.sample.ui.components.TopAppBar
import com.nxoim.sample.ui.components.TwoPane
import com.nxoim.sample.ui.theme.SampleTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val model = remember { Model(FakeSongSource(emitPlaceholders = true)) }
    val songCount by model.songCount.collectAsState()

    SampleTheme {
        TwoPane(
            leftPane = {
                ControlsPane(
                    filterController = model,
                    sortingController = model,
                    searchController = model
                )
            },
            rightPane = { isWide ->
                Scaffold(
                    topBar = { TopAppBar(songCount) },
                    bottomBar = {
                        AnimatedVisibility(
                            !isWide,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            ControlsBottomBar(
                                filterController = model,
                                sortingController = model,
                                searchController = model
                            )
                        }
                    }
                ) { scaffoldPadding ->
                    SongListContent(model, scaffoldPadding, songCount)
                }
            }
        )
    }
}

@Composable
private fun SongListContent(
    model: Model,
    scaffoldPadding: PaddingValues,
    songCount: Int?,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val pagedItems = model.pageable.toState(listState, key = { it.id })
    val isLoadingPrevious by model.pageable.isFetchingPrevious.collectAsState()
    val isLoadingNext by model.pageable.isFetchingNext.collectAsState()

    Box {
        LazyColumn(
            state = listState,
            contentPadding = scaffoldPadding,
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(pagedItems) { index, item ->
                // should you use a loading indicator - make
                // sure to add it alongside the item ui, like this.
                // having separate lazy items
                // for loading indicator will cause scrolling
                // issues due to how lazy lists work
                Column(
                    Modifier
                        .animateItem()
                        .widthIn(max = WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND.dp)
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    ListLoadingIndicator(visible = isLoadingPrevious && index == 0)

                    when (item) {
                        is ItemData.Loaded -> {
                            SwipeToDismissBox(
                                rememberSwipeToDismissBoxState { it },
                                backgroundContent = { },
                                onDismiss = {
                                    model.remove(item.value)
                                }
                            ) {
                                SongItem(
                                    item.value,
                                    shape = SongItemShape.auto(index, pagedItems.items.value.size)
                                )
                            }
                        }

                        is ItemData.Placeholder -> SongItemPlaceholder()
                    }

                    ListLoadingIndicator(visible = isLoadingNext && index == pagedItems.items.value.lastIndex)
                }
            }
        }

        ScrollBar(
            songCount = songCount,
            pagedItems = pagedItems,
            listState = listState,
            model = model,
            scaffoldPadding = scaffoldPadding,
            scope = scope
        )
    }
}

@Composable
private fun ColumnScope.ListLoadingIndicator(
    visible: Boolean
) {
    AnimatedVisibility(
        visible,
        Modifier.padding(vertical = 16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            CircularWavyProgressIndicator()
        }
    }
}

@Composable
private fun BoxScope.ScrollBar(
    songCount: Int?,
    pagedItems: PageableComposeState<ItemData>,
    listState: LazyListState,
    model: Model,
    scaffoldPadding: PaddingValues,
    scope: CoroutineScope
) {
    val sections by model.sections.collectAsState()
    var currentMiddleItemIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }.collect { visibleItems ->
            if (visibleItems.isNotEmpty()) {
                val middleVisible = visibleItems[visibleItems.size / 2]
                currentMiddleItemIndex =
                    pagedItems.items.value.getOrNull(middleVisible.index)?.index
            }
        }
    }

    val count = songCount?.takeIf { it > 0 } ?: return

    ScrollBar(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .width(12.dp)
            .padding(vertical = 16.dp)
            .padding(scaffoldPadding),
        currentFraction = { currentMiddleItemIndex?.toFloat()?.div(count) ?: 0f },
        onFractionChanged = { newFraction ->
            scope.launch {
                model.jumpToSongAtIndex(
                    (newFraction * (count - 1)).roundToInt()
                )
            }
        },
        indicator = { activeFraction, isPressed ->
            if (sections.isNotEmpty()) {
                val targetIndex = (activeFraction * (count - 1)).roundToInt()
                val activeSection =
                    sections.lastOrNull { targetIndex >= it.startIndex } ?: sections.first()

                ScrollBarLabelHint(activeSection.label, isPressed)
            }
        }
    )
}