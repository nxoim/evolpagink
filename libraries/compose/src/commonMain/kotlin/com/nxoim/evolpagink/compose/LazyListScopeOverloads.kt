@file:OptIn(com.nxoim.evolpagink.core.InternalPageableApi::class)

package com.nxoim.evolpagink.compose

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.runtime.Composable

inline fun <T> LazyStaggeredGridScope.items(
    state: PageableComposeState<T>,
    crossinline contentType: (T) -> Any? = { null },
    noinline span: ((T) -> StaggeredGridItemSpan)? = null,
    crossinline itemContent: @Composable LazyStaggeredGridItemScope.(T) -> Unit
) = items<T>(
    items = state.items,
    key = state::key,
    span = span,
    contentType = contentType,
    itemContent = itemContent
)

inline fun <T> LazyStaggeredGridScope.itemsIndexed(
    state: PageableComposeState<T>,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    noinline span: ((index: Int, item: T) -> StaggeredGridItemSpan)? = null,
    crossinline itemContent: @Composable LazyStaggeredGridItemScope.(index: Int, item: T) -> Unit
) = itemsIndexed<T>(
    items = state.items,
    key = state::key,
    span = span,
    contentType = contentType,
    itemContent = itemContent
)

inline fun <T> LazyGridScope.items(
    state: PageableComposeState<T>,
    noinline contentType: (T) -> Any? = { null },
    crossinline itemContent: @Composable LazyGridItemScope.(T) -> Unit
) = items<T>(
    items = state.items,
    key = state::key,
    contentType = contentType,
    itemContent = itemContent
)

inline fun <T> LazyGridScope.itemsIndexed(
    state: PageableComposeState<T>,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable LazyGridItemScope.(index: Int, item: T) -> Unit
) = itemsIndexed<T>(
    items = state.items,
    key = state::key,
    contentType = contentType,
    itemContent = itemContent
)

inline fun <T> LazyListScope.items(
    state: PageableComposeState<T>,
    noinline contentType: (T) -> Any? = { null },
    crossinline itemContent: @Composable LazyItemScope.(T) -> Unit
) = items<T>(
    items = state.items,
    key = state::key,
    contentType = contentType,
    itemContent = itemContent
)

inline fun <T> LazyListScope.itemsIndexed(
    state: PageableComposeState<T>,
    crossinline contentType: (Int, T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) = itemsIndexed<T>(
    items = state.items,
    key = state::key,
    contentType = contentType,
    itemContent = itemContent
)
