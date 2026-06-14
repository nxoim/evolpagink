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

/**
 * Binds a [Pageable] to a Compose Pager (e.g., [HorizontalPager] or [VerticalPager]).
 *
 * Each item in the pageable becomes a single pager page. Fetches are triggered as pages become visible.
 */
@OptIn(ExperimentalCoroutinesApi::class, InternalPageableApi::class)
@Composable
@JvmName("toPagerStateVisible")
fun <Key : Any, PageItem> Pageable<Key, PageItem>.toPagerState(
    initialPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) initialPageOffsetFraction: Float = 0f,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    key: (PageItem) -> Any = ::pageItemKey
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