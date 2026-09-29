package com.nxoim.evolpagink.compose

import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nxoim.evolpagink.core.InternalPageableApi
import com.nxoim.evolpagink.core.PageDisplayingEvent
import com.nxoim.evolpagink.core.Pageable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

@OptIn(InternalPageableApi::class)
@Composable
internal fun <Key : Any, PageItem> Pageable<Key, PageItem>.collectListStateIntoPageable(
    layoutInfo: PageableLayoutInfo,
    key: (PageItem) -> Any,
    coroutineContext: CoroutineContext,
    anchored: Boolean = false
): PageableComposeState<PageItem> {
    val pageable = this
    val currentItemsState = pageable.items.collectAsStateWithLifecycle()
    val keyer = remember(key, pageable) {
        PageItemKeyProviderImpl(key)
    }

    LaunchedEffect(
        layoutInfo,
        pageable,
        currentItemsState,
        anchored,
        keyer
    ) {
        withContext(coroutineContext) {
            if (anchored) {
                var cachedItems: List<PageItem>? = null
                var cachedMap: MutableScatterMap<Any, PageItem>? = null

                snapshotFlow {
                    val count = layoutInfo.visibleItemCount
                    val items = currentItemsState.value
                    if (count == 0 || items.isEmpty()) return@snapshotFlow null

                    val middleIndex = count / 2
                    val middleKey = layoutInfo.getVisibleItemKey(middleIndex)
                    val middleLayoutIndex = layoutInfo.getVisibleItemIndex(middleIndex)

                    MiddleItemSnapshot(middleKey, middleLayoutIndex, items)
                }
                    .mapNotNull { snapshot ->
                        if (snapshot == null) return@mapNotNull null

                        val items = snapshot.items
                        val directCandidate = items.getOrNull(snapshot.layoutIndex)
                        val middleItem = if (directCandidate != null && keyer.key(directCandidate) == snapshot.key) {
                            directCandidate
                        } else {
                            if (cachedItems !== items) {
                                cachedItems = items
                                val map = MutableScatterMap<Any, PageItem>(items.size)
                                for (i in items.indices) {
                                    val item = items[i]
                                    map[keyer.key(item)] = item
                                }
                                cachedMap = map
                            }
                            cachedMap?.get(snapshot.key)
                        }

                        middleItem?.let { pageable.getPageKeyForItem(it) }
                    }
                    .distinctUntilChanged()
                    .collect { pageable.onVisibilityEvent(PageDisplayingEvent.PageAnchorChanged(it)) }
            } else {
                var cachedItems: List<PageItem>? = null
                var cachedMap: MutableScatterMap<Any, PageItem>? = null

                snapshotFlow {
                    val count = layoutInfo.visibleItemCount
                    val items = currentItemsState.value
                    if (count == 0 || items.isEmpty()) return@snapshotFlow null

                    val keys = ArrayList<Any>(count)
                    val indices = IntArray(count)
                    var i = 0
                    layoutInfo.forEachVisibleItem { index, key ->
                        keys.add(key)
                        indices[i++] = index
                    }
                    VisibleItemsSnapshot(keys, indices, items)
                }
                    .map { snapshot ->
                        if (snapshot == null) return@map emptyList()

                        val items = snapshot.items
                        val keys = snapshot.keys
                        val indices = snapshot.indices

                        if (cachedItems !== items) {
                            cachedItems = items
                            val map = MutableScatterMap<Any, PageItem>(items.size)
                            items.fastForEach { item ->
                                map[keyer.key(item)] = item
                            }

                            cachedMap = map
                        }

                        val resolvedItems = ArrayList<PageItem>(keys.size)
                        keys.fastForEachIndexed { index, itemKey ->
                            val layoutIndex = indices[index]
                            val direct = items.getOrNull(layoutIndex)

                            if (direct != null && keyer.key(direct) == itemKey) {
                                resolvedItems.add(direct)
                            } else {
                                cachedMap
                                    ?.get(itemKey)
                                    ?.let(resolvedItems::add)
                            }
                        }

                        if (resolvedItems.isEmpty()) return@map emptyList()

                        val pageKeys = ArrayList<Key>(resolvedItems.size)
                        val seenKeys = MutableScatterSet<Key>(resolvedItems.size)
                        var lastKey: Key? = null
                        resolvedItems.fastForEach { item ->
                            val pageKey = pageable.getPageKeyForItem(item) ?: return@fastForEach
                            if (pageKey != lastKey && seenKeys.add(pageKey)) {
                                pageKeys.add(pageKey)
                                lastKey = pageKey
                            }
                        }
                        pageKeys
                    }
                    .distinctUntilChanged()
                    .collect {
                        pageable.onVisibilityEvent(PageDisplayingEvent.VisibleItemsUpdated(it))
                    }
            }
        }
    }

    return remember(pageable, currentItemsState, keyer) {
        PageableComposeState(currentItemsState, keyer)
    }
}

private class MiddleItemSnapshot<T>(
    val key: Any,
    val layoutIndex: Int,
    val items: List<T>
) {
    override fun equals(other: Any?): Boolean =
        other is MiddleItemSnapshot<*> &&
            key == other.key &&
            items === other.items

    override fun hashCode(): Int = key.hashCode()
}

private class VisibleItemsSnapshot<T>(
    val keys: List<Any>,
    val indices: IntArray,
    val items: List<T>
) {
    override fun equals(other: Any?): Boolean =
        other is VisibleItemsSnapshot<*> &&
            keys == other.keys &&
            items === other.items

    override fun hashCode(): Int = keys.hashCode()
}