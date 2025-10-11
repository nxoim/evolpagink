package com.nxoim.evolpagink.core

import androidx.collection.MutableScatterSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

internal class Paginator<Key : Any, PageItem, Event>(
    private val onPage: (key: Key) -> Flow<List<PageItem>?>,
    private val fetchStrategy: PageFetchStrategy<Key, PageItem, Event>,
    private val onPageEvent: ((PageEvent<Key>) -> Unit)?
) {
    private val initialKeyList = listOf(fetchStrategy.initialPage) // not to recreate it many times
    private val transactionalPageStorage = TransactionalPageStorage(
        ScatterMapPageStorage<Key, PageItem>()
    )
    private val _activePageKeys = MutableStateFlow(initialKeyList)
    val activePageKeys = _activePageKeys.asStateFlow()
    private val pageCollectionJobTracker = PageJobTracker<Key>()

    fun createFlattenedItemsStateFlow(
        coroutineScope: CoroutineScope,
        initialItems: List<PageItem>
    ): StateFlow<List<PageItem>> = activePageKeys
        .onEach { pages ->
            val current = pageCollectionJobTracker.active
            val toCancel = current - pages

            (pages - current).forEach { page ->
                getOrLaunchPageCollection(page, coroutineScope)
            }

            toCancel.forEach { pageCollectionJobTracker.cancelAndJoin(it) }
        }
        .combine(
            transactionalPageStorage.transactionEvents.receiveAsFlow(),
            ::createFlattenedItemList
        )
        .stateIn(
            coroutineScope,
            started = WhileSubscribed(),
            initialValue = initialItems
        )

    private fun getOrLaunchPageCollection(page: Key, coroutineScope: CoroutineScope) =
        pageCollectionJobTracker.launchIfIdle(page, coroutineScope) {
            onPage(page)
                .cancellable()
                .onStart { onPageEvent?.invoke(PageEvent.Loading(page)) }
                .onCompletion {
                    transactionalPageStorage.transaction {
                        remove(page)
                        onPageEvent?.invoke(PageEvent.Unloaded(page))
                    }
                }
                .collect {  items ->
                    // if items is null = not loaded
                    // if items is empty = loaded as empty page
                    items?.let {
                        transactionalPageStorage.transaction {
                            set(page, items)
                            onPageEvent?.invoke(PageEvent.Loaded(page))
                        }
                    }
                }
        }

    private fun createFlattenedItemList(
        pages: List<Key>,
        cache: Map<Key, List<PageItem>>
    ): List<PageItem> {
        val capacity = pages.sumOf { key -> cache[key]?.size ?: 0 }
        val flatMappedItemList = ArrayList<PageItem>(capacity)

        for (index in pages.indices) {
            val key = pages[index]
            cache[key]?.let(flatMappedItemList::addAll)
        }
        return flatMappedItemList
    }

    fun updatePagesToCache(event: Event) {
        val targetPages = fetchStrategy.calculatePages(
            PageFetchContext(
                event = event,
                pageCache = transactionalPageStorage.all
            )
        )
        setActivePages(targetPages.ifEmpty { _activePageKeys.value })
    }

    fun getPageKeyForItem(item: PageItem): Key? = transactionalPageStorage
        .getPageKeyForItem(item)

    context(scope: CoroutineScope)
    suspend fun jumpToAndGetAccessLambda(
        key: Key,
        alsoPrevious: Boolean = false,
        alsoNext: Boolean = true,
    ): () -> List<PageItem>? {
        val keys = buildList {
            if (alsoPrevious) fetchStrategy.onPreviousPage(key)?.let { add(it) }
            add(key)
            if (alsoNext) fetchStrategy.onNextPage(key)?.let { add(it) }
        }

        transactionalPageStorage.transaction {
            keys
                .map {
                    scope.launch {
                        if (!pageCollectionJobTracker.isActive(it)) {
                            prefetchFirstPageValue(it)
                        }
                    }
                }
                .joinAll()
        }
        // upon visibility change updatePagesToCache may
        // get called from ui with no data but thats ok
        // because it falls back to latest active keys
        // in that case
        setActivePages(keys)

        return { transactionalPageStorage[key] }
    }

    private suspend fun PageStorage<Key, PageItem>.prefetchFirstPageValue(
        key: Key
    ) {
        onPageEvent?.invoke(PageEvent.Loading(key))

        onPage(key)
            .firstOrNull()
            .let { items ->
                items?.let {
                    set(key, it)
                    onPageEvent?.invoke(PageEvent.Loaded(key))
                }
            }
    }

    private fun setActivePages(targetPages: List<Key>) {
        // there may be cases where fast scrolling
        // in slow environment would cause a page or two to
        // be unloaded. this mitigates it by collecting keys that sit
        // between adjacent visible pages
        val bridged = MutableScatterSet<Key>()
        for (targetPageIndex in 0 until targetPages.lastIndex) {
            var kpageKey = fetchStrategy.onNextPage(targetPages[targetPageIndex])
            while (kpageKey != null && kpageKey != targetPages[targetPageIndex + 1]) {
                if (transactionalPageStorage[kpageKey] != null || pageCollectionJobTracker.isActive(kpageKey)) {
                    bridged.add(kpageKey)
                }
                kpageKey = fetchStrategy.onNextPage(kpageKey)
            }
        }

        val merged = ArrayList<Key>(targetPages.size + bridged.size)
        for (targetPageIndex in targetPages.indices) {
            merged.add(targetPages[targetPageIndex])
            if (targetPageIndex < targetPages.lastIndex) {
                var pageKey = fetchStrategy.onNextPage(targetPages[targetPageIndex])
                while (pageKey != null && pageKey != targetPages[targetPageIndex + 1]) {
                    if (pageKey in bridged) merged.add(pageKey)
                    pageKey = fetchStrategy.onNextPage(pageKey)
                }
            }
        }

        _activePageKeys.update { merged.distinct() }
    }
}