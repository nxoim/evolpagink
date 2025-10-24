package com.nxoim.evolpagink.compose

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastMapNotNull
import com.nxoim.evolpagink.core.AnchoredPageable
import com.nxoim.evolpagink.core.InternalPageableApi
import com.nxoim.evolpagink.core.PageAnchorChanged
import com.nxoim.evolpagink.core.VisibilityAwarePageable
import com.nxoim.evolpagink.core.VisibleItemsUpdated
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmName

@OptIn(InternalPageableApi::class, ExperimentalCoroutinesApi::class)
@Composable
@JvmName("collectPagerStateIntoPageableAnchored")
internal fun <Key : Any, PageItem> AnchoredPageable<Key, PageItem>.collectPagerStateIntoPageable(
    state: PagerState,
    currentItemsState: State<List<PageItem>>,
    key: (PageItem) -> Any,
    coroutineContext: CoroutineContext
): PageablePagerComposeState<PageItem> {
    val pageable = this
    val keyer = remember(key, pageable) {
        PageItemKeyProviderImpl(key)
    }

    LaunchedEffect(pageable, currentItemsState) {
        withContext(coroutineContext) {
            snapshotFlow { state.currentPage }
                .flatMapLatest { currentPage ->
                    val items = currentItemsState.value

                    snapshotFlow { items.getOrNull(currentPage) }.mapNotNull { currentItem ->
                        if (currentItem == null)
                            null
                        else {
                            pageable.getPageKeyForItem(currentItem)
                        }
                    }
                }
                .distinctUntilChanged()
                .collect { pageable._onEvent(PageAnchorChanged(it)) }
        }
    }

    return remember(pageable, currentItemsState, keyer, state) {
        PageablePagerComposeState(
            _items = currentItemsState,
            key = { keyer.key(currentItemsState.value.getOrNull(it)!!) },
            pagerState = state
        )
    }
}

@OptIn(InternalPageableApi::class, ExperimentalCoroutinesApi::class)
@Composable
@JvmName("collectPagerStateIntoPageableVisibilityAware")
internal fun <Key : Any, PageItem> VisibilityAwarePageable<Key, PageItem>.collectPagerStateIntoPageable(
    state: PagerState,
    currentItemsState: State<List<PageItem>>,
    key: (PageItem) -> Any,
    coroutineContext: CoroutineContext
): PageablePagerComposeState<PageItem> {
    val pageable = this
    val keyer = remember(key, pageable) {
        PageItemKeyProviderImpl(key)
    }

    LaunchedEffect(pageable, currentItemsState) {
        withContext(coroutineContext) {
            snapshotFlow { state.layoutInfo }
                .map { layoutInfo ->
                    val visibleItemsInfo = layoutInfo.visiblePagesInfo
                    val items = currentItemsState.value
                    val itemMap = items.associateBy(keyer::key)

                    if (visibleItemsInfo.isEmpty() || items.isEmpty()) {
                        return@map emptyList()
                    }

                    val visiblePagedItems = visibleItemsInfo
                        .fastMapNotNull { itemInfo -> itemMap[itemInfo.key] }

                    if (visiblePagedItems.isEmpty()) return@map emptyList()

                    val visiblePageKeys = visiblePagedItems
                        .fastMapNotNull(pageable.getPageKeyForItem)
                        .toSet()
                        .toList()

//                    val firstVisibleIsFirstLoaded = currentItems.isNotEmpty() &&
//                            visiblePagedItems.first() == currentItems.first()
//
//                    val lastVisibleIsLastLoaded = currentItems.isNotEmpty() &&
//                            visiblePagedItems.last() == currentItems.last()

//                    val viewportIsConsideredFull = layoutInfo.totalItemsCount > 0 &&
//                            (firstVisibleIsFirstLoaded || lastVisibleIsLastLoaded)

                    visiblePageKeys
                }
                .distinctUntilChanged()
                .collect { pageable._onEvent(VisibleItemsUpdated(it)) }
        }
    }

    return remember(pageable, currentItemsState, keyer, state) {
        PageablePagerComposeState(
            _items = currentItemsState,
            key = { keyer.key(currentItemsState.value.getOrNull(it)!!) },
            pagerState = state
        )
    }
}