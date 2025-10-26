package com.nxoim.evolpagink.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(InternalPageableApi::class)
fun <Key : Any, PageItem, Event> pageable(
    coroutineScope: CoroutineScope,
    onPage: Unit.(key: Key) -> Flow<List<PageItem>?>,
    strategy: PageFetchStrategy<Key, PageItem, Event, Unit>,
    onPageEvent: ((event: PageEvent<Key>) -> Unit)? = null,
    resultingItemsTransform: (List<PageItem>) -> List<PageItem> = { it },
    initialItems: List<PageItem> = emptyList(),
) = pageable(
    coroutineScope = coroutineScope,
    context = singleEmissionStateFlowOfUnit,
    onPage = onPage,
    strategy = strategy,
    onPageEvent = onPageEvent,
    resultingItemsTransform = resultingItemsTransform,
    initialItems = initialItems
)

@OptIn(InternalPageableApi::class, ExperimentalAtomicApi::class, ExperimentalCoroutinesApi::class)
fun <Key : Any, PageItem, Event, Context> pageable(
    coroutineScope: CoroutineScope,
    context: StateFlow<Context>,
    onPage: Context.(key: Key) -> Flow<List<PageItem>?>,
    strategy: PageFetchStrategy<Key, PageItem, Event, Context>,
    onPageEvent: ((event: PageEvent<Key>) -> Unit)? = null,
    resultingItemsTransform: (List<PageItem>) -> List<PageItem> = { it },
    initialItems: List<PageItem> = emptyList(),
): Pageable<Key, PageItem, Event> {
    val paginator = Paginator(
        coroutineScope,
        context,
        onPage,
        strategy,
        onPageEvent
    )

    return Pageable(
        items = paginator
            .collectPagesAndFlattenIntoItemList()
            .map(resultingItemsTransform)
            .stateIn(coroutineScope, WhileSubscribed(), initialItems),
        isFetchingPrevious = paginator.isFetchingPrevious,
        isFetchingNext = paginator.isFetchingNext,
        getPageKeyForItem = { item -> paginator.getPageKeyForItem(item) },
        jumpTo = { page ->
            paginator.preloadAndActivate(coroutineScope.coroutineContext, page)
        },
        _onEvent = { event -> paginator.updatePagesToCache(event) }
    )
}

@OptIn(InternalPageableApi::class)
class Pageable<Key : Any, PageItem, Event> internal constructor(
    val items: StateFlow<List<PageItem>>,
    val isFetchingPrevious: StateFlow<Boolean>,
    val isFetchingNext: StateFlow<Boolean>,
    val getPageKeyForItem: (item: PageItem) -> Key?,
    val jumpTo: suspend (key: Key) -> List<PageItem>?,
    @property:InternalPageableApi
    @Suppress("propertyName")
    val _onEvent: (event: Event) -> Unit
)