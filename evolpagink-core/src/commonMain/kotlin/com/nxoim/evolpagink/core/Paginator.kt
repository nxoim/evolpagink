package com.nxoim.evolpagink.core

import androidx.collection.MutableScatterSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class Paginator<Key : Any, PageItem, Event>(
    private val pageStore: ObservablePageStorage<Key, PageItem>,
    private val jobTracker: PageJobTracker<Key>,
    private val fetchStrategy: PageFetchStrategy<Key, PageItem, Event>,
    private val onPage: (Key) -> Flow<List<PageItem>?>,
    private val initialKeys: List<Key>,
    private val onPageEvent: ((event: PageEvent<Key>) -> Unit)? = null
) {
    private val preloadMutex = Mutex()
    private val _activePageKeys = MutableStateFlow(initialKeys)
    val activePageKeys = _activePageKeys.asStateFlow()
    val isFetchingPrevious = _activePageKeys.map { keys ->
        keys.firstOrNull()?.let(fetchStrategy.onPreviousPage) != null
    }
    val isFetchingNext = _activePageKeys.map { keys ->
        keys.lastOrNull()?.let(fetchStrategy.onNextPage) != null
    }

    fun createFlattenedItemsFlow(coroutineScope: CoroutineScope): Flow<List<PageItem>> =
        pageStore.pageSnapshots.combine(
            _activePageKeys.collectPages(coroutineScope),
            ::grabAndFlattenPages
        )

    fun updatePagesToCache(event: Event) {
        // keep pages if suddenly paged items arent visible
        // fallback to initial keys just in case
        val pages = fetchStrategy
            .calculatePages(
                PageFetchContext(event, pageStore.pagesSnapshot)
            )
            .ifEmpty { activePageKeys.value }
            .ifEmpty { initialKeys }

        _activePageKeys.update { mergeBridgedKeys(pages) }
    }

    suspend fun collectPageOnce(key: Key, emitSnapshot: Boolean): List<PageItem>? =
        onPage(key)
            .firstOrNull()
            .also {
                if (it != null)
                    pageStore.updatePage(key, it, emitSnapshot)
                else
                    pageStore.removePage(key, emitSnapshot)
            }


    fun Flow<List<Key>>.collectPages(coroutineScope: CoroutineScope): Flow<List<Key>> =
        onEach { pages ->
            val old = jobTracker.active
            val toCancel = old - pages

            pages.forEach { key ->
                jobTracker.launchIfIdle(key, coroutineScope) {
                    onPage(key)
                        .cancellable()
                        .onStart {
                            if (!pageStore.contains(key)) onPageEvent?.invoke(
                                PageEvent.Loading(key)
                            )
                        }
                        .collect { items ->
                            if (items != null)
                                pageStore.updatePage(key, items, true)
                            else
                                pageStore.removePage(key, true)
                        }
                }
            }

            toCancel.forEach { key ->
                jobTracker.cancelAndJoin(key)
                pageStore.removePage(key, false)
            }
        }

    fun getPageKeyForItem(item: PageItem): Key? = pageStore.getPageKeyForItem(item)

    suspend fun preloadAndActivate(
        scope: CoroutineScope,
        key: Key,
        alsoPrevious: Boolean = false,
        alsoNext: Boolean = true
    ): List<PageItem>? = preloadMutex.withLock {
        // lock outside internal scope, process inside.
        // this forcefully avoids ui thread (if scope isn't on it ofc)
        scope.async {
            val keys = buildList {
                if (alsoPrevious) fetchStrategy.onPreviousPage(key)?.let(::add)
                add(key)
                if (alsoNext) fetchStrategy.onNextPage(key)?.let(::add)
            }
            var pageContents: List<PageItem>? = null

            keys.mapIndexed { index, pageToPrefetch ->
                launch {
                    collectPageOnce(pageToPrefetch, emitSnapshot = index == keys.lastIndex)
                        .also { if (key == pageToPrefetch) pageContents = it }
                }
            }.joinAll()

            _activePageKeys.update { mergeBridgedKeys(keys) }
            pageContents
        }
            .await()
    }

    private fun mergeBridgedKeys(targetPages: List<Key>): List<Key> {
        val bridged = MutableScatterSet<Key>()
        for (i in 0 until targetPages.lastIndex) {
            var k = fetchStrategy.onNextPage(targetPages[i])
            while (k != null && k != targetPages[i + 1]) {
                if (pageStore.contains(k) || jobTracker.isActive(k)) bridged.add(k)
                k = fetchStrategy.onNextPage(k)
            }
        }

        val merged = ArrayList<Key>(targetPages.size + bridged.size)
        for (i in targetPages.indices) {
            merged.add(targetPages[i])
            if (i < targetPages.lastIndex) {
                var k = fetchStrategy.onNextPage(targetPages[i])
                while (k != null && k != targetPages[i + 1]) {
                    if (k in bridged) merged.add(k)
                    k = fetchStrategy.onNextPage(k)
                }
            }
        }
        return merged.distinct()
    }

    private fun grabAndFlattenPages(
        cacheSnapshot: Map<Key, List<PageItem>>,
        pages: List<Key>
    ): List<PageItem> {
        val size = pages.sumOf { cacheSnapshot[it]?.size ?: 0 }
        val buffer = ArrayList<PageItem>(size)
        for (key in pages) {
            cacheSnapshot[key]?.let(buffer::addAll)
        }
        return buffer
    }
}
