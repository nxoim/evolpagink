@file:OptIn(com.nxoim.evolpagink.core.InternalPageableApi::class)

package com.nxoim.evolpagink.compose

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExplicitGroupsComposable
import androidx.compose.runtime.remember
import com.nxoim.evolpagink.core.AnchoredPageable
import com.nxoim.evolpagink.core.VisibilityAwarePageable
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmName

@Composable
@ExplicitGroupsComposable
@JvmName("toStateAnchoredList")
fun <Key : Any, PageItem> AnchoredPageable<Key, PageItem>.toState(
    state: LazyListState,
    key: (PageItem) -> Any,
    coroutineContext: CoroutineContext = Dispatchers.Default
): PageableComposeState<PageItem> = collectListStateIntoPageable(
    layoutInfo = remember(state) { PageableLazyListLayoutInfo(state) },
    key = key,
    coroutineContext = coroutineContext
)

@Composable
@ExplicitGroupsComposable
@JvmName("toStateAnchoredGrid")
fun <Key : Any, PageItem> AnchoredPageable<Key, PageItem>.toState(
    state: LazyGridState,
    key: (PageItem) -> Any,
    coroutineContext: CoroutineContext = Dispatchers.Default
): PageableComposeState<PageItem> = collectListStateIntoPageable(
    layoutInfo = remember(state) { PageableLazyGridLayoutInfo(state) },
    key = key,
    coroutineContext = coroutineContext
)

@Composable
@ExplicitGroupsComposable
@JvmName("toStateAnchoredStaggeredGrid")
fun <Key : Any, PageItem> AnchoredPageable<Key, PageItem>.toState(
    state: LazyStaggeredGridState,
    key: (PageItem) -> Any,
    coroutineContext: CoroutineContext = Dispatchers.Default
): PageableComposeState<PageItem> = collectListStateIntoPageable(
    layoutInfo = remember(state) { PageableLazyStaggeredGridLayoutInfo(state) },
    key = key,
    coroutineContext = coroutineContext
)

@Composable
@ExplicitGroupsComposable
@JvmName("toStateVisibleList")
fun <Key : Any, PageItem> VisibilityAwarePageable<Key, PageItem>.toState(
    state: LazyListState,
    key: (PageItem) -> Any,
    coroutineContext: CoroutineContext = Dispatchers.Default
): PageableComposeState<PageItem> = collectListStateIntoPageable(
    layoutInfo = remember(state) { PageableLazyListLayoutInfo(state) },
    key = key,
    coroutineContext = coroutineContext
)


@Composable
@ExplicitGroupsComposable
@JvmName("toStateVisibleGrid")
fun <Key : Any, PageItem> VisibilityAwarePageable<Key, PageItem>.toState(
    state: LazyStaggeredGridState,
    key: (PageItem) -> Any,
    coroutineContext: CoroutineContext = Dispatchers.Default
): PageableComposeState<PageItem> = collectListStateIntoPageable(
    layoutInfo = remember(state) {
        PageableLazyStaggeredGridLayoutInfo(state)
    },
    key = key,
    coroutineContext = coroutineContext
)

@Composable
@ExplicitGroupsComposable
@JvmName("toStateVisibleStaggeredGrid")
fun <Key : Any, PageItem> VisibilityAwarePageable<Key, PageItem>.toState(
    state: LazyGridState,
    key: (PageItem) -> Any,
    coroutineContext: CoroutineContext = Dispatchers.Default
): PageableComposeState<PageItem> = collectListStateIntoPageable(
    layoutInfo = remember(state) { PageableLazyGridLayoutInfo(state) },
    key = key,
    coroutineContext = coroutineContext
)

