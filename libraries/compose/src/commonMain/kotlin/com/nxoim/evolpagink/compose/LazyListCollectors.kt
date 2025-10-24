package com.nxoim.evolpagink.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastMapNotNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nxoim.evolpagink.core.AnchoredPageable
import com.nxoim.evolpagink.core.InternalPageableApi
import com.nxoim.evolpagink.core.PageAnchorChanged
import com.nxoim.evolpagink.core.VisibilityAwarePageable
import com.nxoim.evolpagink.core.VisibleItemsUpdated
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmName

@OptIn(InternalPageableApi::class)
@Composable
@JvmName("collectListStateIntoPageableAnchored")
internal fun <Key : Any, PageItem> AnchoredPageable<Key, PageItem>.collectListStateIntoPageable(
    layoutInfo: PageableLayoutInfo,
    key: (PageItem) -> Any,
    coroutineContext: CoroutineContext
): PageableComposeState<PageItem> {
    val pageable = this
    val currentItemsState = pageable.items.collectAsStateWithLifecycle()
    val keyer = remember(key, pageable) {
        PageItemKeyProviderImpl(key)
    }

    LaunchedEffect(layoutInfo, pageable, currentItemsState) {
        withContext(coroutineContext) {
            snapshotFlow { layoutInfo.visibleItemsInfo }
                .map { visibleItemsInfo ->
                    val items = currentItemsState.value
                    val itemMap = items.associateBy(keyer::key)

                    if (visibleItemsInfo.isEmpty() || items.isEmpty()) {
                        return@map null
                    }

                    val visiblePagedItems = visibleItemsInfo
                        .fastMapNotNull { itemInfo -> itemMap[itemInfo.key] }

                    if (visiblePagedItems.isEmpty()) return@map null

                    val middleItem = visiblePagedItems.getOrNull(visiblePagedItems.size / 2)
                    middleItem?.let(pageable.getPageKeyForItem)
                }
                .distinctUntilChanged()
                .collect() {
                    it?.let { pageable._onEvent(PageAnchorChanged(it)) }
                }
        }
    }

    return remember(pageable, currentItemsState, keyer) {
        PageableComposeState(currentItemsState, keyer)
    }
}

@OptIn(InternalPageableApi::class)
@Composable
@JvmName("collectListStateIntoPageableVisibility")
internal fun <Key : Any, PageItem> VisibilityAwarePageable<Key, PageItem>.collectListStateIntoPageable(
    layoutInfo: PageableLayoutInfo,
    key: (PageItem) -> Any,
    coroutineContext: CoroutineContext
): PageableComposeState<PageItem> {
    val pageable = this
    val currentItemsState = pageable.items.collectAsStateWithLifecycle()
    val keyer = remember(key, pageable) {
        PageItemKeyProviderImpl(key)
    }

    LaunchedEffect(layoutInfo, pageable, currentItemsState) {
        withContext(coroutineContext) {
            snapshotFlow { layoutInfo.visibleItemsInfo }
                // bind emissions to updates in pageable
                .map { visibleItemsInfo ->
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
                .collect() {
                    pageable._onEvent(VisibleItemsUpdated(it))
                }
        }
    }

    return remember(pageable, currentItemsState, keyer) {
        PageableComposeState(currentItemsState, keyer)
    }
}