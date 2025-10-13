package com.nxoim.evolpagink.core

import androidx.collection.MutableScatterSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class Paginator<Key : Any, PageItem, Event, Context>(
    val pageStore: ObservablePageStorage<Key, PageItem>,
    val jobTracker: PageJobTracker<Key>,
    private val fetchStrategy: PageFetchStrategy<Key, PageItem, Event, Context>,
    private val onPage: Context.(key: Key) -> Flow<List<PageItem>?>,
    private val initialKeys: List<Key>,
    private val onPageEvent: ((event: PageEvent<Key>) -> Unit)? = null,
    val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val preloadMutex = Mutex()
    private val _activePageKeys = MutableStateFlow(initialKeys)
    val activePageKeys = _activePageKeys.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun createFlattenedItemsFlowAndCollectPages(
        initial: Map<Key, List<PageItem>> = pageStore.pagesSnapshot
    ): Flow<List<PageItem>> = activePageKeys.flatMapLatest { pages ->
        pageStore.pageSnapshots
            .onStart { emit(initial) }
            .map { cache ->
                val old = jobTracker.active
                val toCancel = old - pages

                pages.forEach { key ->
                    jobTracker.launchIfIdle(key, coroutineScope) {
                        context.onPage(key)
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

                pages.flatMap { cache[it].orEmpty() }
            }
    }

    fun updatePagesToCache(event: Event) {
        // keep pages if suddenly paged items arent visible
        // fallback to initial keys just in case
        val pages = fetchStrategy
            .calculatePages(
                PageFetchContext(event, pageStore.pagesSnapshot, context)
            )
            .ifEmpty { activePageKeys.value }
            .ifEmpty { initialKeys }

        _activePageKeys.update { mergeBridgedKeys(pages) }
    }

    fun getPageKeyForItem(item: PageItem): Key? = pageStore.getPageKeyForItem(item)

    suspend fun preloadAndActivate(
        prefetchContext: CoroutineContext,
        key: Key,
    ): List<PageItem>? = withContext(prefetchContext) {
        preloadMutex.withLock {
            val snapshot = pageStore.pagesSnapshot
            val pageContents = snapshot[key] ?: context.onPage(key)
                .firstOrNull()
                .also {
                    if (it != null)
                        pageStore.updatePage(key, it, true)
                    else
                        pageStore.removePage(key, true)
                }


            _activePageKeys.update { mergeBridgedKeys(listOf(key)) }
            pageContents
        }
    }

    private fun mergeBridgedKeys(targetPages: List<Key>): List<Key> {
        val bridged = MutableScatterSet<Key>()
        for (i in 0 until targetPages.lastIndex) {
            var k = fetchStrategy.onNextPage(context, targetPages[i])
            while (k != null && k != targetPages[i + 1]) {
                if (pageStore.contains(k) || jobTracker.isActive(k)) bridged.add(k)
                k = fetchStrategy.onNextPage(context, k)
            }
        }

        val merged = ArrayList<Key>(targetPages.size + bridged.size)
        for (i in targetPages.indices) {
            merged.add(targetPages[i])
            if (i < targetPages.lastIndex) {
                var k = fetchStrategy.onNextPage(context, targetPages[i])
                while (k != null && k != targetPages[i + 1]) {
                    if (k in bridged) merged.add(k)
                    k = fetchStrategy.onNextPage(context, k)
                }
            }
        }
        return merged.distinct()
    }

}
