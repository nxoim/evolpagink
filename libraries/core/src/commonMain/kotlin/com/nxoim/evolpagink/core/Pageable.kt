package com.nxoim.evolpagink.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Creates a [Pageable] with integer-based page keys starting from 0.
 */
@OptIn(InternalPageableApi::class)
fun <Key : Any, PageItem : Any> pageable(
    coroutineScope: CoroutineScope,
    onPage: Unit.(key: Key) -> Flow<List<PageItem>?>,
    strategy: PageFetchStrategy<Key, PageItem, Unit>,
    onPageEvent: ((event: PageEvent<Key>) -> Unit)? = null,
    resultingItemsTransform: (List<PageItem>) -> List<PageItem> = { it },
    initialItems: List<PageItem> = emptyList(),
    pageItemKey: (PageItem) -> Any = { it }
) = pageable(
    coroutineScope = coroutineScope,
    context = singleEmissionStateFlowOfUnit,
    onPage = onPage,
    strategy = strategy,
    onPageEvent = onPageEvent,
    resultingItemsTransform = resultingItemsTransform,
    initialItems = initialItems,
    pageItemKey = pageItemKey
)

/**
 * Creates a [Pageable] that can react to changes in the provided context.
 *
 * When the context emits a new value, any currently cached pages are invalidated and re-fetched.
 */
@OptIn(InternalPageableApi::class, ExperimentalAtomicApi::class, ExperimentalCoroutinesApi::class)
fun <Key : Any, PageItem : Any, Context> pageable(
    coroutineScope: CoroutineScope,
    context: StateFlow<Context>,
    onPage: Context.(key: Key) -> Flow<List<PageItem>?>,
    strategy: PageFetchStrategy<Key, PageItem, Context>,
    onPageEvent: ((event: PageEvent<Key>) -> Unit)? = null,
    resultingItemsTransform: (List<PageItem>) -> List<PageItem> = { it },
    initialItems: List<PageItem> = emptyList(),
    pageItemKey: (PageItem) -> Any = { it }
): Pageable<Key, PageItem> {
    val paginator = Paginator(
        coroutineScope,
        context,
        onPage,
        strategy,
        onPageEvent,
        pageItemKey
    )

    return Pageable(
        items = paginator
            .collectPagesAndFlattenIntoItemList()
            .map(resultingItemsTransform)
            .stateIn(coroutineScope, WhileSubscribed(), initialItems),
        isFetchingPrevious = paginator.isFetchingPrevious,
        isFetchingNext = paginator.isFetchingNext,
        getPageKeyForItem = paginator::getPageKeyForItem,
        jumpTo = { page ->
            paginator.preloadAndActivate(coroutineScope.coroutineContext, page)
        },
        _onEvent = { event -> paginator.updatePagesToCache(event) },
        pageItemKey = pageItemKey
    )
}

/**
 * Represents a paginated data source with flattened items and fetching state.
 *
 * The item list automatically updates as pages are fetched based on the configured strategy.
 *
 * @param getPageKeyForItem Maps an item back to its page key. Returns null if the item isn't associated with a page.
 * @param jumpTo Loads and activates the specified page, returning its items. Subsequent fetches will be centered around this page.
 * @param pageItemKey Provides key for an item from any loaded page. See [pageable].
 */
@OptIn(InternalPageableApi::class)
class Pageable<Key : Any, PageItem> internal constructor(
    val items: StateFlow<List<PageItem>>,
    val isFetchingPrevious: StateFlow<Boolean>,
    val isFetchingNext: StateFlow<Boolean>,
    val getPageKeyForItem: suspend (item: PageItem) -> Key?,
    val jumpTo: suspend (Key) -> List<PageItem>?,
    @property:InternalPageableApi
    @Suppress("propertyName")
    val _onEvent: suspend (PageDisplayingEvent<Key>) -> Unit,
    val pageItemKey: (PageItem) -> Any
)