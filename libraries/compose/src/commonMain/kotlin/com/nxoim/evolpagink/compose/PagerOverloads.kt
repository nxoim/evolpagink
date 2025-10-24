package com.nxoim.evolpagink.compose

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun <PageItem> VerticalPager(
    state: PageablePagerComposeState<PageItem>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    pageSpacing: Dp = 0.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    flingBehavior: TargetedFlingBehavior = PagerDefaults.flingBehavior(state = state.pagerState),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    pageNestedScrollConnection: NestedScrollConnection =
        PagerDefaults.pageNestedScrollConnection(state.pagerState, Orientation.Vertical),
    snapPosition: SnapPosition = SnapPosition.Start,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    pageContent: @Composable PagerScope.(PageItem) -> Unit,
) = androidx.compose.foundation.pager.VerticalPager(
    state = state.pagerState,
    modifier = modifier,
    contentPadding = contentPadding,
    pageSize = pageSize,
    beyondViewportPageCount = beyondViewportPageCount,
    pageSpacing = pageSpacing,
    horizontalAlignment = horizontalAlignment,
    flingBehavior = flingBehavior,
    userScrollEnabled = userScrollEnabled,
    reverseLayout = reverseLayout,
    key = state.key,
    pageNestedScrollConnection = pageNestedScrollConnection,
    snapPosition = snapPosition,
    overscrollEffect = overscrollEffect,
    pageContent = { pageContent(state.items[it]!!) }
)

@Composable
fun <PageItem> HorizontalPager(
    state: PageablePagerComposeState<PageItem>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    pageSpacing: Dp = 0.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    flingBehavior: TargetedFlingBehavior = PagerDefaults.flingBehavior(state = state.pagerState),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    pageNestedScrollConnection: NestedScrollConnection =
        PagerDefaults.pageNestedScrollConnection(state.pagerState, Orientation.Vertical),
    snapPosition: SnapPosition = SnapPosition.Start,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    pageContent: @Composable PagerScope.(PageItem) -> Unit,
) = androidx.compose.foundation.pager.HorizontalPager(
    state = state.pagerState,
    modifier = modifier,
    contentPadding = contentPadding,
    pageSize = pageSize,
    beyondViewportPageCount = beyondViewportPageCount,
    pageSpacing = pageSpacing,
    verticalAlignment = verticalAlignment,
    flingBehavior = flingBehavior,
    userScrollEnabled = userScrollEnabled,
    reverseLayout = reverseLayout,
    key = state.key,
    pageNestedScrollConnection = pageNestedScrollConnection,
    snapPosition = snapPosition,
    overscrollEffect = overscrollEffect,
    pageContent = { pageContent(state.items[it]!!)}
)