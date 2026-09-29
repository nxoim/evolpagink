package com.nxoim.evolpagink.compose

import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMapNotNull
import com.nxoim.evolpagink.core.PageDisplayingEvent
import com.nxoim.evolpagink.core.Pageable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

@Composable
internal fun <Key : Any, PageItem> Pageable<Key, PageItem>.collectPagerStateIntoPageable(
    state: PagerState,
    currentItemsState: State<List<PageItem>>,
    coroutineContext: CoroutineContext,
    anchored: Boolean,
    key: (PageItem) -> Any = ::pageItemKey
): PageablePagerComposeState<PageItem> {
    val pageable = this
    val keyer = remember(key, pageable) {
        PageItemKeyProviderImpl(key)
    }

    LaunchedEffect(pageable, state, currentItemsState, anchored, keyer) {
        withContext(coroutineContext) {
            var cachedItems: List<PageItem>? = null
            var cachedMap: MutableScatterMap<Any, PageItem>? = null

            val resolvedItems = snapshotFlow {
                val currentPage = if (anchored) state.currentPage else null
                val visiblePages = state.layoutInfo.visiblePagesInfo
                val items = currentItemsState.value
                val pages = visiblePages.fastMapNotNull { page ->
                    if (currentPage == null || page.index == currentPage) {
                        PagerPageSnapshot(page.index, page.key)
                    } else {
                        null
                    }
                }
                PagerItemsSnapshot(pages, items)
            }
                .map { snapshot ->
                    val items = snapshot.items
                    if (cachedItems !== items) {
                        cachedItems = items
                        cachedMap = null
                    }
                    if (items.isEmpty()) return@map emptyList<PageItem>()

                    snapshot.pages.fastMapNotNull { page ->
                        val direct = items.getOrNull(page.index)
                        // items can change before pager remeasures, leaving indices from the old list
                        if (direct != null && keyer.key(direct) == page.key) {
                            direct
                        } else {
                            val itemMap = cachedMap ?: MutableScatterMap<Any, PageItem>(items.size)
                                .also { map ->
                                    items.fastForEach { item -> map[keyer.key(item)] = item }
                                    cachedMap = map
                                }
                            itemMap[page.key]
                        }
                    }
                }

            // deduplicating equal items would hide page-owner changes after a list update.
            if (anchored) {
                resolvedItems
                    .mapNotNull { items -> items.firstOrNull()?.let { pageable.getPageKeyForItem(it) } }
                    .distinctUntilChanged()
                    .collect { pageable.onVisibilityEvent(PageDisplayingEvent.PageAnchorChanged(it)) }
            } else {
                resolvedItems
                    .map { visiblePagedItems ->
                        if (visiblePagedItems.isEmpty()) return@map emptyList()

                        val keys = ArrayList<Key>(visiblePagedItems.size)
                        val seenKeys = MutableScatterSet<Key>(visiblePagedItems.size)
                        var lastKey: Key? = null
                        visiblePagedItems.fastForEach {
                            val pageKey = pageable.getPageKeyForItem(it) ?: return@fastForEach
                            if (pageKey != lastKey && seenKeys.add(pageKey)) {
                                keys.add(pageKey)
                                lastKey = pageKey
                            }
                        }
                        keys
                    }
                    .distinctUntilChanged()
                    .collect { pageable.onVisibilityEvent(PageDisplayingEvent.VisibleItemsUpdated(it)) }
            }
        }
    }

    return remember(pageable, currentItemsState, keyer, state) {
        PageablePagerComposeState(
            items = currentItemsState,
            key = { keyer.key(currentItemsState.value.getOrNull(it)!!) },
            pagerState = state
        )
    }
}

private data class PagerPageSnapshot(val index: Int, val key: Any)

private class PagerItemsSnapshot<T>(
    val pages: List<PagerPageSnapshot>,
    val items: List<T>
) {
    override fun equals(other: Any?): Boolean =
        other is PagerItemsSnapshot<*> && pages == other.pages && items === other.items

    override fun hashCode(): Int = pages.hashCode()
}
