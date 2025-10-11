@file:OptIn(com.nxoim.evolpagink.core.InternalPageableApi::class)

package com.nxoim.evolpagink.compose

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExplicitGroupsComposable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastMapNotNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nxoim.evolpagink.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmInline
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

class PageableComposeState<T> internal constructor(
    private val _items: State<List<T>>,
    keyer: PageItemKeyProvider<T>
) : PageItemKeyProvider<T> by keyer {
    val items get() = _items.value
}

// exists to centralize compose list key management
// between paging and displaying
sealed interface PageItemKeyProvider<T> {
    fun key(item: T): Any
    fun key(index: Int, item: T): Any
}

@JvmInline
private value class PageItemKeyProviderImpl<PageItem>(
    private val keyProvider: (PageItem) -> Any,
) : PageItemKeyProvider<PageItem> {
    override fun key(item: PageItem): Any = keyProvider(item)
    override fun key(index: Int, item: PageItem): Any = keyProvider(item)
}

////////////////////////////////////////////////////////////////////////////////////////////

@OptIn(InternalPageableApi::class)
@Composable
@JvmName("collectListStateIntoPageableAnchored")
private fun <Key : Any, PageItem> AnchoredPageable<Key, PageItem>.collectListStateIntoPageable(
    layoutInfo: PageableLayoutInfo,
    key: (PageItem) -> Any,
    coroutineContext: CoroutineContext
): PageableComposeState<PageItem> {
    val pageable = this
    val currentItemsState = pageable.items.collectAsStateWithLifecycle(context = coroutineContext)
    val keyer = remember(key, pageable) {
        PageItemKeyProviderImpl(key)
    }

    LaunchedEffect(layoutInfo, pageable, currentItemsState) {
        withContext(coroutineContext) {
            snapshotFlow { layoutInfo.visibleItemsInfo }
                .map {
                    val items = currentItemsState.value
                    val itemMap = items.associateBy(keyer::key)
                    val visibleItemsInfo = layoutInfo.visibleItemsInfo

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
private fun <Key : Any, PageItem> VisibilityAwarePageable<Key, PageItem>.collectListStateIntoPageable(
    layoutInfo: PageableLayoutInfo,
    key: (PageItem) -> Any,
    coroutineContext: CoroutineContext
): PageableComposeState<PageItem> {
    val pageable = this
    val currentItemsState = pageable.items.collectAsStateWithLifecycle(context = coroutineContext)
    val keyer = remember(key, pageable) {
        PageItemKeyProviderImpl(key)
    }

    LaunchedEffect(layoutInfo, pageable, currentItemsState) {
        withContext(coroutineContext) {
            snapshotFlow { layoutInfo.visibleItemsInfo }
                // bind emissions to updates in pageable
                .map {
                    val items = currentItemsState.value
                    val layoutInfo = layoutInfo
                    val itemMap = items.associateBy(keyer::key)

                    val visibleItemsInfo = layoutInfo.visibleItemsInfo

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
                .collect() {
                    pageable._onEvent(VisibleItemsUpdated(it))
                }
        }
    }

    return remember(pageable, currentItemsState, keyer) {
        PageableComposeState(currentItemsState, keyer)
    }
}


@JvmInline
private value class PageableLazyListItemInfo(private val info: LazyListItemInfo) :
    PageableItemLayoutInfo {
    override val index: Int get() = info.index
    override val key: Any get() = info.key
}

@JvmInline
private value class PageableLazyGridItemInfo(private val info: LazyGridItemInfo) :
    PageableItemLayoutInfo {
    override val index: Int get() = info.index
    override val key: Any get() = info.key
}

@JvmInline
private value class PageableLazyStaggeredGridItemInfo(private val info: LazyStaggeredGridItemInfo) :
    PageableItemLayoutInfo {
    override val index: Int get() = info.index
    override val key: Any get() = info.key
}

@JvmInline
private value class PageableLazyListLayoutInfo(
    private val state: LazyListState
) : PageableLayoutInfo {
    override val visibleItemsInfo: List<PageableItemLayoutInfo>
        get() = state.layoutInfo.visibleItemsInfo.map(::PageableLazyListItemInfo)

    override val totalItemsCount: Int
        get() = state.layoutInfo.totalItemsCount

    override val lastScrolledForward: Boolean
        get() = state.lastScrolledForward
}

@JvmInline
private value class PageableLazyGridLayoutInfo(private val state: LazyGridState) :
    PageableLayoutInfo {
    override val visibleItemsInfo: List<PageableItemLayoutInfo>
        // Grids can have out of order visible com.nxoim.TODORENAMElibrary.items
        get() = state.layoutInfo.visibleItemsInfo.sortedBy { it.index }
            .map(::PageableLazyGridItemInfo)

    override val totalItemsCount: Int
        get() = state.layoutInfo.totalItemsCount

    override val lastScrolledForward: Boolean
        get() = state.lastScrolledForward
}

@JvmInline
private value class PageableLazyStaggeredGridLayoutInfo(
    private val state: LazyStaggeredGridState
) : PageableLayoutInfo {
    override val visibleItemsInfo: List<PageableItemLayoutInfo>
        // Grids can have out of order visible com.nxoim.TODORENAMElibrary.items
        get() = state.layoutInfo.visibleItemsInfo.sortedBy { it.index }
            .map(::PageableLazyStaggeredGridItemInfo)

    override val totalItemsCount: Int
        get() = state.layoutInfo.totalItemsCount

    override val lastScrolledForward: Boolean
        get() = state.lastScrolledForward
}

private interface PageableItemLayoutInfo {
    val index: Int
    val key: Any
}

private interface PageableLayoutInfo {
    val visibleItemsInfo: List<PageableItemLayoutInfo>
    val totalItemsCount: Int
    val lastScrolledForward: Boolean
}