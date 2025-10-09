package com.nxoim.evolpagink

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
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
        onPage = onPage,
        fetchStrategy = strategy,
        onPageEvent = onPageEvent
    )
    
    val itemsStateFlow = paginator.createFlattenedItemsStateFlow(coroutineScope, initialItems)

    return PageableImpl(
        items = itemsStateFlow,
        isFetchingPrevious = paginator.activePageKeys
            .map { keys -> keys.firstOrNull()?.let(strategy.onPreviousKey) != null }
            .stateIn(
                coroutineScope,
                started = WhileSubscribed(),
                initialValue = paginator.activePageKeys.value
                    .firstOrNull()
                    ?.let(strategy.onPreviousKey) != null
            ),
        isFetchingNext = paginator.activePageKeys
            .map { keys -> keys.lastOrNull()?.let(strategy.onNextKey) != null }
            .stateIn(
                coroutineScope,
                started = WhileSubscribed(),
                initialValue = paginator.activePageKeys.value
                    .lastOrNull()
                    ?.let(strategy.onNextKey) != null
            ),
        _onEvent = paginator::updatePagesToCache,
        getPageKeyForItem = paginator::getPageKeyForItem,
        jumpTo = { key ->
            // launch in inner scope to avoid ui thread
            val pageReference =
                coroutineScope.async { paginator.jumpToAndGetAccessLambda(key) }.await()

            // wait until transactional storage contains the page
            itemsStateFlow
                .map { items ->
                    val page = pageReference.invoke()
                    if (page != null && items.contains(page.first())) {
                        page
                    } else {
                        null
                    }
                }
                .filterNotNull()
                .first()
        }
    )
}


sealed interface Pageable<Key : Any, PageItem, Event> {
    val items: StateFlow<List<PageItem>>
    val isFetchingPrevious: StateFlow<Boolean>
    val isFetchingNext: StateFlow<Boolean>
    val getPageKeyForItem: (item: PageItem) -> Key?
    val jumpTo: suspend (key: Key) -> List<PageItem>
    @InternalPageableApi
    @Suppress("propertyName")
    val _onEvent: (event: Event) -> Unit
}

@OptIn(InternalPageableApi::class)
internal class PageableImpl<Key : Any, PageItem, Event>(
    override val items: StateFlow<List<PageItem>>,
    override val isFetchingPrevious: StateFlow<Boolean>,
    override val isFetchingNext: StateFlow<Boolean>,
    override val getPageKeyForItem: (item: PageItem) -> Key?,
    override val jumpTo: suspend (key: Key) -> List<PageItem>,
    override val _onEvent: (event: Event) -> Unit
) : Pageable<Key, PageItem, Event>