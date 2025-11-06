package com.nxoim.evolpagink.compose

import androidx.annotation.FloatRange
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nxoim.evolpagink.core.InternalPageableApi
import com.nxoim.evolpagink.core.Pageable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.JvmName

@OptIn(ExperimentalCoroutinesApi::class, InternalPageableApi::class)
@Composable
@JvmName("toPagerStateVisible")
fun <Key : Any, PageItem> Pageable<Key, PageItem>.toPagerState(
    key: (PageItem) -> Any,
    initialPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) initialPageOffsetFraction: Float = 0f,
    coroutineContext: CoroutineContext = EmptyCoroutineContext
): PageablePagerComposeState<PageItem> {
    val currentItemsState = items.collectAsStateWithLifecycle()

    return collectPagerStateIntoPageable(
        state = rememberPagerState(
            initialPage,
            initialPageOffsetFraction,
            pageCount = { currentItemsState.value.size }
        ),
        currentItemsState = currentItemsState,
        key = key,
        coroutineContext = coroutineContext,
        anchored = false
    )
}

@OptIn(ExperimentalCoroutinesApi::class, InternalPageableApi::class)
@Composable
@JvmName("toPagerStateVisibleDeprecated")
@Deprecated(anchoredParamDeprecationNote, replaceWith = ReplaceWith("toPagerState(state, key)"))
fun <Key : Any, PageItem> Pageable<Key, PageItem>.toPagerState(
    key: (PageItem) -> Any,
    anchored: Boolean,
    initialPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) initialPageOffsetFraction: Float = 0f,
    coroutineContext: CoroutineContext = EmptyCoroutineContext
): PageablePagerComposeState<PageItem> {
    val currentItemsState = items.collectAsStateWithLifecycle()

    return collectPagerStateIntoPageable(
        state = rememberPagerState(
            initialPage,
            initialPageOffsetFraction,
            pageCount = { currentItemsState.value.size }
        ),
        currentItemsState = currentItemsState,
        key = key,
        coroutineContext = coroutineContext,
        anchored = anchored
    )
}