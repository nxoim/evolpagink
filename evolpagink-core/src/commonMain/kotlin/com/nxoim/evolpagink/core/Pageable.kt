package com.nxoim.evolpagink.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(InternalPageableApi::class)
fun <Key : Any, PageItem, Event> pageable(
    coroutineScope: CoroutineScope,
    onPage: (key: Key) -> Flow<List<PageItem>?>,
    strategy: PageFetchStrategy<Key, PageItem, Event>,
    onPageEvent: ((event: PageEvent<Key>) -> Unit)? = null,
    initialItems: List<PageItem> = emptyList(),
): Pageable<Key, PageItem, Event> {
    val paginator = Paginator(
        pageStore = ObservablePageStorage(ScatterMapPageStorage(), onPageEvent),
        jobTracker = PageJobTracker(),
        fetchStrategy = strategy,
        onPage = onPage,
        initialKeys = listOf(strategy.initialPage),
        onPageEvent = onPageEvent
    )

    val itemsStateFlow = paginator
        .createFlattenedItemsFlow(coroutineScope)
        .stateIn(
            coroutineScope,
            started = WhileSubscribed(),
            initialValue = initialItems
        )

    return Pageable(
        items = itemsStateFlow,
        isFetchingPrevious = paginator.isFetchingPrevious.stateIn(
            coroutineScope,
            started = WhileSubscribed(),
            initialValue = false
        ),
        isFetchingNext = paginator.isFetchingNext.stateIn(
            coroutineScope,
            started = WhileSubscribed(),
            initialValue = false
        ),
        _onEvent = paginator::updatePagesToCache,
        getPageKeyForItem = paginator::getPageKeyForItem,
        jumpTo = { key ->
            val page = paginator.preloadAndActivate(coroutineScope, key)

            itemsStateFlow
                .map { items ->
                    if (page != null && items.contains(page.first())) page else null
                }
                .filterNotNull()
                .first()
        }
    )
}

@OptIn(InternalPageableApi::class)
class Pageable<Key : Any, PageItem, Event>(
    val items: StateFlow<List<PageItem>>,
    val isFetchingPrevious: StateFlow<Boolean>,
    val isFetchingNext: StateFlow<Boolean>,
    val getPageKeyForItem: (item: PageItem) -> Key?,
    val jumpTo: suspend (key: Key) -> List<PageItem>,
    @property:InternalPageableApi
    @Suppress("propertyName")
    val _onEvent: (event: Event) -> Unit
)