package com.nxoim.evolpagink.compose

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastMapNotNull
import com.nxoim.evolpagink.core.InternalPageableApi
import com.nxoim.evolpagink.core.PageDisplayingEvent
import com.nxoim.evolpagink.core.Pageable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

@OptIn(InternalPageableApi::class, ExperimentalCoroutinesApi::class)
@Composable
internal fun <Key : Any, PageItem> Pageable<Key, PageItem>.collectPagerStateIntoPageable(
    state: PagerState,
    currentItemsState: State<List<PageItem>>,
    key: (PageItem) -> Any,
    coroutineContext: CoroutineContext,
    anchored: Boolean
): PageablePagerComposeState<PageItem> {
    val pageable = this
    val keyer = remember(key, pageable) {
        PageItemKeyProviderImpl(key)
    }

    LaunchedEffect(pageable, currentItemsState) {
        withContext(coroutineContext) {
            if (anchored) {
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
                    .collect { pageable._onEvent(PageDisplayingEvent.PageAnchorChanged(it)) }
            } else {
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

                        visiblePageKeys
                    }
                    .distinctUntilChanged()
                    .collect { pageable._onEvent(PageDisplayingEvent.VisibleItemsUpdated(it)) }
            }
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