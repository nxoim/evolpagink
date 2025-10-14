package com.nxoim.sample.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.nxoim.evolpagink.compose.itemsIndexed
import com.nxoim.evolpagink.compose.toState
import com.nxoim.evolpagink.core.AnchoredPageable
import com.nxoim.evolpagink.core.PageEvent
import com.nxoim.evolpagink.core.VisibilityAwarePageable
import com.nxoim.sample.ui.theme.rememberNotCupertinoOverscrollFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val model = remember { Model() }
    val pagerState = rememberPagerState(pageCount = { 6 })
    val coroutineScope = rememberCoroutineScope()

    var scale by remember { mutableFloatStateOf(1f) }
    var showNonPaginatedItems by remember { mutableStateOf(false) }

    CompositionLocalProvider(
        LocalOverscrollFactory provides rememberNotCupertinoOverscrollFactory()
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Text(
                "SIze scale",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Slider(
                value = scale,
                onValueChange = { scale = it },
                valueRange = 0.1f..3f,
                steps = 28,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Show non-paginated items",
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = showNonPaginatedItems,
                    onCheckedChange = { showNonPaginatedItems = it }
                )
            }

            PrimaryScrollableTabRow(selectedTabIndex = pagerState.currentPage, edgePadding = 16.dp) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("Index, List") }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("Index, List, Placeholders") }
                )
                Tab(
                    selected = pagerState.currentPage == 2,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                    text = { Text("Key, List, Placeholders") }
                )
                Tab(
                    selected = pagerState.currentPage == 3,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(3) } },
                    text = { Text("Index, Grid, Placeholders") }
                )
                Tab(
                    selected = pagerState.currentPage == 4,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(4) } },
                    text = { Text("Index, Staggered, Placeholders") }
                )
                Tab(
                    selected = pagerState.currentPage == 5,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(5) } },
                    text = { Text("Index, Fixed count, Placeholders") }
                )
            }

            CompositionLocalProvider(
                LocalDensity provides LocalDensity.current.let {
                    Density(
                        it.density * scale,
                        it.fontScale
                    )
                }
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    key(page) {
                        when (page) {
                            0 -> Row {
                                PageTrackerBar(tracker = model.indexListNoPlaceholdersTracker)

                                NonPlaceholderIndexBasedPaginatedList(
                                    pageable = model.indexListNoPlaceholders,
                                    showNonPaginatedItems = showNonPaginatedItems
                                )
                            }

                            1 -> {
                                PageTrackerBar(tracker = model.indexListPlaceholdersTracker)

                                IndexBasedPaginatedList(
                                    pageable = model.indexListPlaceholders,
                                    showNonPaginatedItems = showNonPaginatedItems
                                )
                            }

                            2 -> KeyBasedPaginatedList(
                                pageable = model.keyListPlaceholders,
                                showNonPaginatedItems = showNonPaginatedItems
                            )

                            3 -> IndexBasedPaginatedGrid(
                                pageable = model.indexGridPlaceholders,
                                showNonPaginatedItems = showNonPaginatedItems
                            )

                            4 -> IndexBasedPaginatedStaggeredGrid(
                                pageable = model.indexStaggeredGridPlaceholders,
                                showNonPaginatedItems = showNonPaginatedItems
                            )

                            5 -> {
                                PageTrackerBar(tracker = model.indexListFixedCountPlaceholdersTracker)

                                FixedCountPaginatedList(
                                    pageable = model.indexListFixedCountPlaceholders,
                                    showNonPaginatedItems = showNonPaginatedItems
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NonPlaceholderIndexBasedPaginatedList(
    pageable: VisibilityAwarePageable<Int, ItemData.Loaded>,
    showNonPaginatedItems: Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val pageableState = pageable.toState(
        state = listState,
        key = { it.id }
    )

    val isFetchingPrevious by pageable.isFetchingPrevious.collectAsState()
    val isFetchingNext by pageable.isFetchingNext.collectAsState()

    Column {
        Button(onClick = {
            coroutineScope.launch {
                val pageWeJumpTo = pageable.jumpTo(10)

                pageWeJumpTo
                    ?.firstOrNull()
                    ?.let { firstItemOfTenthPage ->
                        val index = pageableState.items.indexOf(firstItemOfTenthPage)

                        if (index != -1) {
                            listState.animateScrollToItem(index)
                        }
                    }
            }
        }) {
            Text("Jump to page 10")
        }

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (showNonPaginatedItems) {
                repeat(40) {
                    item("repeated$it") {
                        NonPagedItem()
                    }
                }
            }

            itemsIndexed(pageableState) { index, item ->
                AnimatedVisibility(index == 0 && isFetchingPrevious) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                SampleListItem(item, Modifier.animateItem())

                AnimatedVisibility(index == pageableState.items.lastIndex && isFetchingNext) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun IndexBasedPaginatedList(
    pageable: VisibilityAwarePageable<Int, ItemData>,
    showNonPaginatedItems: Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val pageableState = pageable.toState(
        state = listState,
        key = ItemData::toComposeLazyListKey
    )

    val isFetchingPrevious by pageable.isFetchingPrevious.collectAsState()
    val isFetchingNext by pageable.isFetchingNext.collectAsState()

    Column {
        Button(onClick = {
            coroutineScope.launch {
                val pageWeJumpTo = pageable.jumpTo(10)

                pageWeJumpTo
                    ?.firstOrNull()
                    ?.let { firstItemOfTenthPage ->
                        val index = pageableState.items.indexOf(firstItemOfTenthPage)

                        if (index != -1) {
                            listState.animateScrollToItem(index)
                        }
                    }
            }
        }) {
            Text("Jump to page 10")
        }

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (showNonPaginatedItems) {
                repeat(40) {
                    item("repeated$it") {
                        NonPagedItem()
                    }
                }
            }

            itemsIndexed(pageableState) { index, item ->
                AnimatedVisibility(index == 0 && isFetchingPrevious) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                SampleListItem(item, Modifier.animateItem())

                AnimatedVisibility(index == pageableState.items.lastIndex && isFetchingNext) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun FixedCountPaginatedList(
    pageable: AnchoredPageable<Int, ItemData>,
    showNonPaginatedItems: Boolean
) {
    val listState = rememberLazyListState()
    val pageableState = pageable.toState(
        state = listState,
        key = ItemData::toComposeLazyListKey
    )

    val isFetchingPrevious by pageable.isFetchingPrevious.collectAsState()
    val isFetchingNext by pageable.isFetchingNext.collectAsState()

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (showNonPaginatedItems) {
            repeat(40) {
                item("repeated$it") {
                    NonPagedItem()
                }
            }
        }

        itemsIndexed(pageableState) { index, item ->
            AnimatedVisibility(index == 0 && isFetchingPrevious) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            SampleListItem(item, Modifier.animateItem())

            AnimatedVisibility(index == pageableState.items.lastIndex && isFetchingNext) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun KeyBasedPaginatedList(
    pageable: VisibilityAwarePageable<String, ItemData>,
    showNonPaginatedItems: Boolean
) {
    val listState = rememberLazyListState()
    val pageableState = pageable.toState(
        state = listState,
        key = ItemData::toComposeLazyListKey
    )

    val isFetchingPrevious by pageable.isFetchingPrevious.collectAsState()
    val isFetchingNext by pageable.isFetchingNext.collectAsState()

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (showNonPaginatedItems) {
            repeat(40) {
                item("repeated$it") {
                    NonPagedItem()
                }
            }
        }

        itemsIndexed(pageableState) { index, item ->
            AnimatedVisibility(index == 0 && isFetchingPrevious) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            SampleListItem(item, Modifier.animateItem())

            AnimatedVisibility(index == pageableState.items.lastIndex && isFetchingNext) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IndexBasedPaginatedGrid(
    pageable: VisibilityAwarePageable<Int, ItemData>,
    showNonPaginatedItems: Boolean
) {
    val gridState = rememberLazyGridState()
    val pageableState = pageable.toState(
        state = gridState,
        key = ItemData::toComposeLazyListKey
    )

    val isFetchingPrevious by pageable.isFetchingPrevious.collectAsState()
    val isFetchingNext by pageable.isFetchingNext.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(180.dp),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        if (showNonPaginatedItems) {
            repeat(40) {
                item("repeated$it") {
                    NonPagedItem()
                }
            }
        }

        itemsIndexed(pageableState) { index, item ->
            AnimatedVisibility(index == 0 && isFetchingPrevious) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            SampleListItem(item, Modifier.animateItem())

            AnimatedVisibility(index == pageableState.items.lastIndex && isFetchingNext) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IndexBasedPaginatedStaggeredGrid(
    pageable: VisibilityAwarePageable<Int, ItemData>,
    showNonPaginatedItems: Boolean
) {
    val staggeredGridState = rememberLazyStaggeredGridState()
    val pageableState = pageable.toState(
        state = staggeredGridState,
        key = ItemData::toComposeLazyListKey
    )

    val isFetchingPrevious by pageable.isFetchingPrevious.collectAsState()
    val isFetchingNext by pageable.isFetchingNext.collectAsState()

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(180.dp),
        state = staggeredGridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        if (showNonPaginatedItems) {
            repeat(40) {
                item("repeated$it") {
                    NonPagedItem()
                }
            }
        }
        itemsIndexed(pageableState) { index, item ->
            AnimatedVisibility(index == 0 && isFetchingPrevious) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            SampleListItem(item, Modifier.animateItem())

            AnimatedVisibility(index == pageableState.items.lastIndex && isFetchingNext) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun NonPagedItem() {
    Text(
        "Non paginated item",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun PageTrackerBar(tracker: PageTracker) {
    val pages by tracker.pages.collectAsState(emptyList())
    val lastChanged by tracker.lastChanged.collectAsState()
    val listState = rememberLazyListState()

    // React to changes in both lastChanged and pages
    LaunchedEffect(lastChanged, pages) {
        lastChanged?.let { (key, _) ->
            val idx = pages.indexOfFirst { it.key == key }
            if (idx >= 0) {
                val viewport = listState.layoutInfo.viewportEndOffset
                listState.animateScrollToItem(idx, scrollOffset = -viewport / 2)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxHeight()
            .width(8.dp)
    ) {
        items(pages, key = { it.key }) { state ->
            val color = when (state) {
                is PageEvent.Loading -> Color.Yellow
                is PageEvent.Loaded -> Color.Green
                is PageEvent.Unloaded -> Color.Gray
            }
            Box(
                modifier = Modifier
                    .padding(1.dp)
                    .size(width = 4.dp, height = 2.dp)
                    .background(color)
            )
        }
    }
}

@Composable
fun SampleListItem(item: ItemData, modifier: Modifier) {
    when (item) {
        is ItemData.Loaded -> ListItem(
            headlineContent = { Text(item.text) },
            supportingContent = { Text("ID: ${"$"}{item.id}") },
            modifier = modifier
        )

        is ItemData.Placeholder -> Column {
            repeat(item.expectedLoadedItemAmount) {
                ListItem(
                    headlineContent = { Text("Placeholder") },
                    supportingContent = { Text("") },
                )
            }
        }
    }
}


private fun ItemData.toComposeLazyListKey() = when (this) {
    is ItemData.Loaded -> id
    is ItemData.Placeholder -> id
}
