@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package com.nxoim.sample.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
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
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.window.core.layout.WindowSizeClass
import com.nxoim.evolpagink.compose.PageableComposeState
import com.nxoim.evolpagink.compose.itemsIndexed
import com.nxoim.evolpagink.compose.toState
import com.nxoim.sample.ui.components.ControlsBottomBar
import com.nxoim.sample.ui.components.ControlsPane
import com.nxoim.sample.ui.components.ScrollBar
import com.nxoim.sample.ui.components.SongItem
import com.nxoim.sample.ui.components.SongItemPlaceholder
import com.nxoim.sample.ui.components.SongItemShape
import com.nxoim.sample.ui.components.TopAppBar
import com.nxoim.sample.ui.theme.SampleTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val scope = rememberCoroutineScope()
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
                    val listState = rememberLazyListState()
                    val pagedItems = model.pageable.toState(listState, key = { it.id })

                    Box {
                        LazyColumn(
                            state = listState,
                            contentPadding = scaffoldPadding,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            itemsIndexed(pagedItems) { index, item ->
                                val modifier = Modifier
                                    .animateItem()
                                    .widthIn(max = WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND.dp)
                                    .padding(horizontal = 16.dp, vertical = 2.dp)

                                when (item) {
                                    is ItemData.Loaded -> {
                                        SwipeToDismissBox(
                                            rememberSwipeToDismissBoxState { it },
                                            backgroundContent = { },
                                            onDismiss = {
                                                model.remove(item.value)
                                            },
                                            modifier = modifier
                                        ) {
                                            // FIX: updated to use renamed private vals
                                            SongItem(
                                                item.value,
                                                shape = when (index) {
                                                    0 -> SongItemShape.First
                                                    pagedItems.items.value.lastIndex -> SongItemShape.Last
                                                    else -> SongItemShape.Middle
                                                }
                                            )
                                        }
                                    }

                                    is ItemData.Placeholder -> SongItemPlaceholder(modifier)
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
            }
        )
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
                currentMiddleItemIndex = pagedItems.items.value.getOrNull(middleVisible.index)?.index
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
                val activeSection = sections.lastOrNull { targetIndex >= it.startIndex } ?: sections.first()

                ScrollBarHint(activeSection.label, isPressed)
            }
        }
    )
}

@Composable
private fun ScrollBarHint(label: String, isPressed: Boolean) {
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TwoPane(
    modifier: Modifier = Modifier,
    rightPane: @Composable (isWide: Boolean) -> Unit,
    leftPane: @Composable (isWide: Boolean) -> Unit,
) {
    val windowClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isWide =
        windowClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    Row(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            isWide,
            transitionSpec = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right) togetherWith
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left)
            }
        ) {
            if (it) Box(modifier = Modifier.fillMaxWidth(0.35f).zIndex(1f)) {
                leftPane(isWide)
            } else {
                // compensation for internal container resize animation.
                // without this the content will be clipped on the
                // vertical axis during animation
                Spacer(Modifier.fillMaxHeight())
            }
        }

        Box(Modifier.weight(1f)) {
            rightPane(isWide)
        }
    }
}