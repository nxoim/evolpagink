package com.nxoim.evolpagink.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndUpdate
import kotlin.concurrent.atomics.update

@OptIn(InternalPageableApi::class)
fun <Key : Any, PageItem, Event> pageable(
    coroutineScope: CoroutineScope,
    onPage: Unit.(key: Key) -> Flow<List<PageItem>?>,
    strategy: PageFetchStrategy<Key, PageItem, Event, Unit>,
    onPageEvent: ((event: PageEvent<Key>) -> Unit)? = null,
    initialItems: List<PageItem> = emptyList(),
) = pageable(
    coroutineScope = coroutineScope,
    context = singleEmissionStateFlowOfUnit,
    onPage = onPage,
    strategy = strategy,
    onPageEvent = onPageEvent,
    initialItems = initialItems
)

@OptIn(InternalPageableApi::class, ExperimentalAtomicApi::class, ExperimentalCoroutinesApi::class)
fun <Key : Any, PageItem, Event, Context> pageable(
    coroutineScope: CoroutineScope,
    context: StateFlow<Context>,
    onPage: Context.(key: Key) -> Flow<List<PageItem>?>,
    strategy: PageFetchStrategy<Key, PageItem, Event, Context>,
    onPageEvent: ((event: PageEvent<Key>) -> Unit)? = null,
    initialItems: List<PageItem> = emptyList(),
): Pageable<Key, PageItem, Event> {
    val paginatorCache = MutableStateFlow(
        Paginator(
            pageStore = ObservablePageStorage(ScatterMapPageStorage(), onPageEvent),
            jobTracker = PageJobTracker(),
            fetchStrategy = strategy,
            onPage = onPage,
            initialKeys = listOf(strategy.initialPage),
            onPageEvent = onPageEvent,
            context = context.value,
            coroutineScope = coroutineScope
        )
    )
    val isFetchingPrevious = MutableStateFlow(false)
    val isFetchingNext = MutableStateFlow(false)
    val fetchingTrackJob = AtomicReference<Job?>(null)

    val items = context
        .flatMapLatest { newContext ->
            var initialCache = emptyMap<Key, List<PageItem>>()
            val oldPaginator = paginatorCache.value
            val oldContext = oldPaginator.context

            val newPaginator = if (oldContext != null && oldContext === newContext) {
                oldPaginator // reuse instance
            } else {
                Paginator(
                    pageStore = ObservablePageStorage(ScatterMapPageStorage(), onPageEvent),
                    jobTracker = PageJobTracker(),
                    fetchStrategy = strategy,
                    onPage = onPage,
                    // keep visible keys unless the user uses jumpTo
                    initialKeys = oldPaginator.activePageKeys.value,
                    onPageEvent = onPageEvent,
                    context = newContext,
                    coroutineScope = coroutineScope
                ).also { newOne ->
                    // swap paginator first, cancel old jobs, then cleanup
                    paginatorCache.value = newOne

                    fetchingTrackJob.update { oldJob ->
                        oldJob?.cancelAndJoin()
                        null
                    }

                    oldPaginator.let {
                        initialCache = it.pageStore.pagesSnapshot
                        it.jobTracker.clear()
                        it.pageStore.clear(emitSnapshot = false)
                    }
                }
            }

            fetchingTrackJob.update {
                coroutineScope.launch {
                    newPaginator.activePageKeys.collect { keys ->
                        isFetchingPrevious.value = keys
                            .firstOrNull()
                            ?.let { strategy.onPreviousPage(newContext, it) } != null

                        isFetchingNext.value = keys
                            .lastOrNull()
                            ?.let { strategy.onNextPage(newContext, it) } != null
                    }
                }
            }

            newPaginator.createFlattenedItemsFlowAndCollectPages(initialCache)
        }
        .onCompletion {
            fetchingTrackJob.fetchAndUpdate { oldJob ->
                paginatorCache.value.jobTracker.clear()
                oldJob?.cancelAndJoin()
                null
            }
            isFetchingPrevious.value = false
            isFetchingNext.value = false
        }
        .stateIn(coroutineScope, WhileSubscribed(), initialItems)

    return Pageable(
        items = items,
        isFetchingPrevious = isFetchingPrevious,
        isFetchingNext = isFetchingNext,
        getPageKeyForItem = { item -> paginatorCache.value.getPageKeyForItem(item) },
        jumpTo = { page ->
            paginatorCache.value.preloadAndActivate(coroutineScope.coroutineContext, page)
        },
        _onEvent = { event -> paginatorCache.value.updatePagesToCache(event) }
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