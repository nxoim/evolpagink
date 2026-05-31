@file:OptIn(com.nxoim.evolpagink.core.InternalPageableApi::class)

package com.nxoim.evolpagink.compose

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExplicitGroupsComposable
import androidx.compose.runtime.remember
import com.nxoim.evolpagink.core.Pageable
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmName

/**
 * Binds a [Pageable] to a [LazyListState], enabling automatic page fetching as items become visible.
 */
@Composable
@ExplicitGroupsComposable
@JvmName("toStateList")
fun <Key : Any, PageItem> Pageable<Key, PageItem>.toState(
    state: LazyListState,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    key: (PageItem) -> Any = pageItemKey
): PageableComposeState<PageItem> = collectListStateIntoPageable(
    layoutInfo = remember(state) { PageableLazyListLayoutInfo(state) },
    key = key,
    coroutineContext = coroutineContext,
    anchored = false
)

/**
 * Binds a [Pageable] to a [LazyGridState], enabling automatic page fetching as items become visible.
 */
@Composable
@ExplicitGroupsComposable
@JvmName("toStateGrid")
fun <Key : Any, PageItem> Pageable<Key, PageItem>.toState(
    state: LazyGridState,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    key: (PageItem) -> Any = pageItemKey
): PageableComposeState<PageItem> = collectListStateIntoPageable(
    layoutInfo = remember(state) { PageableLazyGridLayoutInfo(state) },
    key = key,
    coroutineContext = coroutineContext,
    anchored = false
)

/**
 * Binds a [Pageable] to a [LazyStaggeredGridState], enabling automatic page fetching as items become visible.
 */
@Composable
@ExplicitGroupsComposable
@JvmName("toStateStaggeredGrid")
fun <Key : Any, PageItem> Pageable<Key, PageItem>.toState(
    state: LazyStaggeredGridState,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    key: (PageItem) -> Any = pageItemKey
): PageableComposeState<PageItem> = collectListStateIntoPageable(
    layoutInfo = remember(state) { PageableLazyStaggeredGridLayoutInfo(state) },
    key = key,
    coroutineContext = coroutineContext,
    anchored = false
)

@Composable
@ExplicitGroupsComposable
@JvmName("toStateListDeprecated")
@Deprecated(anchoredParamDeprecationNote, replaceWith = ReplaceWith("toState(state, key)"))
fun <Key : Any, PageItem> Pageable<Key, PageItem>.toState(
    state: LazyListState,
    anchored: Boolean,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    key: (PageItem) -> Any = pageItemKey
): PageableComposeState<PageItem> = collectListStateIntoPageable(
    layoutInfo = remember(state) { PageableLazyListLayoutInfo(state) },
    key = key,
    coroutineContext = coroutineContext,
    anchored = anchored
)

@Composable
@ExplicitGroupsComposable
@JvmName("toStateGridDeprecated")
@Deprecated(anchoredParamDeprecationNote, replaceWith = ReplaceWith("toState(state, key)"))
fun <Key : Any, PageItem> Pageable<Key, PageItem>.toState(
    state: LazyGridState,
    anchored: Boolean,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    key: (PageItem) -> Any = pageItemKey
): PageableComposeState<PageItem> = collectListStateIntoPageable(
    layoutInfo = remember(state) { PageableLazyGridLayoutInfo(state) },
    key = key,
    coroutineContext = coroutineContext,
    anchored = anchored
)

@Composable
@ExplicitGroupsComposable
@JvmName("toStateStaggeredGridDeprecated")
@Deprecated(anchoredParamDeprecationNote, replaceWith = ReplaceWith("toState(state, key)"))
fun <Key : Any, PageItem> Pageable<Key, PageItem>.toState(
    state: LazyStaggeredGridState,
    anchored: Boolean,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    key: (PageItem) -> Any = pageItemKey
): PageableComposeState<PageItem> = collectListStateIntoPageable(
    layoutInfo = remember(state) { PageableLazyStaggeredGridLayoutInfo(state) },
    key = key,
    coroutineContext = coroutineContext,
    anchored = anchored
)

