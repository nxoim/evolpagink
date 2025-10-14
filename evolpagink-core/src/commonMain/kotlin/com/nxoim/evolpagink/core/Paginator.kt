@file:OptIn(ExperimentalAtomicApi::class)

package com.nxoim.evolpagink.core

import androidx.collection.MutableScatterSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
internal class Paginator<Key : Any, PageItem, Event, Context>(
    private val scope: CoroutineScope,
    private val contextFlow: StateFlow<Context>,
    private val onPage: Context.(Key) -> Flow<List<PageItem>?>,
    private val strategy: PageFetchStrategy<Key, PageItem, Event, Context>,
    private val onPageEvent: ((PageEvent<Key>) -> Unit)?,
) {
    private val storage = ObservablePageStorage<Key, PageItem>(
        ScatterMapPageStorage(),
        onPageEvent
    )
    private val pageCollectionJobTracker = PageJobTracker<Key>()
    private val preloadMutex = Mutex()
    private val _activePageKeys = MutableStateFlow(listOf(strategy.initialPage(contextFlow.value)))
    val activePageKeys = _activePageKeys.asStateFlow()

    private val _isFetchingPrevious = MutableStateFlow(false)
    val isFetchingPrevious = _isFetchingPrevious.asStateFlow()
    private val _isFetchingNext = MutableStateFlow(false)
    val isFetchingNext = _isFetchingNext.asStateFlow()

    private var currentContext = contextFlow.value

    fun collectPagesAndFlattenIntoItemList(): Flow<List<PageItem>> = contextFlow
        .flatMapLatest { newContext ->
            val oldPagesSnapshot = storage.pagesSnapshot

            if (newContext !== currentContext) {
                currentContext = newContext
                pageCollectionJobTracker.clear()
                storage.clear(emitSnapshot = false)

                preloadAndActivate(
                    prefetchContext = scope.coroutineContext,
                    key = strategy.initialPage(newContext),
                    pagesSnapshot = emptyMap() // always reload
                )
            } // else reuse current

            collectAndFlattenPages(pagesSnapshotToBeginEmissionsFrom = oldPagesSnapshot)
        }
        .onCompletion { stopPageCollections() }

    suspend fun preloadAndActivate(
        prefetchContext: CoroutineContext,
        key: Key,
        pagesSnapshot: Map<Key, List<PageItem>> = storage.pagesSnapshot
    ): List<PageItem>? = withContext(prefetchContext) {
        preloadMutex.withLock {
            val pageContents = pagesSnapshot[key] ?: onPage(currentContext, key)
                .firstOrNull()
                .also {
                    if (it != null)
                        storage.updatePage(key, it, true)
                    else
                        storage.removePage(key, true)
                }

            _activePageKeys.update { mergeBridgedKeys(listOf(key), pagesSnapshot) }
            pageContents
        }
    }

    fun updatePagesToCache(event: Event) {
        val pagesSnapshot = storage.pagesSnapshot

        val newPages = strategy
            .calculatePages(
                PageFetchContext(
                    event = event,
                    pageCache = pagesSnapshot,
                    externalContext = currentContext
                )
            )
            .ifEmpty { activePageKeys.value }
            .ifEmpty { listOf(strategy.initialPage(currentContext)) }

        _activePageKeys.update { mergeBridgedKeys(newPages, pagesSnapshot) }
    }

    fun getPageKeyForItem(item: PageItem): Key? = storage.getPageKeyForItem(item)

    private fun collectAndFlattenPages(
        pagesSnapshotToBeginEmissionsFrom: Map<Key, List<PageItem>>
    ): Flow<List<PageItem>> = activePageKeys.flatMapLatest { pageKeys ->
        val active = pageCollectionJobTracker.active
        val toCancel = active - pageKeys

        pageKeys.forEach { launchPageCollectionIfNeeded(it, storage.pagesSnapshot) }

        toCancel.forEach { key ->
            pageCollectionJobTracker.cancelAndJoin(key)
            storage.removePage(key, false)
        }

        storage.pageSnapshots
            .onStart { emit(pagesSnapshotToBeginEmissionsFrom) }
            .map { pagesSnapshot ->
                pageKeys.flatMap { pagesSnapshot[it].orEmpty() }
            }
    }

    private fun launchPageCollectionIfNeeded(
        key: Key,
        pagesSnapshot: Map<Key, List<PageItem>>
    ) {
        pageCollectionJobTracker.launchIfIdle(key, scope) {
            val isFirstItem = { _activePageKeys.value.firstOrNull() == key }
            val isLastItem = { _activePageKeys.value.lastOrNull() == key }

            currentContext.onPage(key)
                .cancellable()
                .onStart {
                    if (!pagesSnapshot.contains(key)) {
                        onPageEvent?.invoke(PageEvent.Loading(key))
                        if (isFirstItem()) _isFetchingPrevious.value = true
                        if (isLastItem()) _isFetchingNext.value = true
                    }
                }
                .collect { items ->
                    if (items != null)
                        storage.updatePage(key, items, true)
                    else
                        storage.removePage(key, true)

                    if (isFirstItem()) _isFetchingPrevious.value = false
                    if (isLastItem()) _isFetchingNext.value = false
                }
        }
    }

    private fun mergeBridgedKeys(
        target: List<Key>,
        pagesSnapshot: Map<Key, List<PageItem>>
    ): List<Key> {
        val bridged = MutableScatterSet<Key>()
        for (i in 0 until target.lastIndex) {
            var k = strategy.onNextPage(currentContext, target[i])
            while (k != null && k != target[i + 1]) {
                if (pagesSnapshot.contains(k) || pageCollectionJobTracker.isActive(k)) bridged.add(k)
                k = strategy.onNextPage(currentContext, k)
            }
        }
        val merged = ArrayList<Key>(target.size + bridged.size)
        for (i in target.indices) {
            merged.add(target[i])
            if (i < target.lastIndex) {
                var k = strategy.onNextPage(currentContext, target[i])
                while (k != null && k != target[i + 1]) {
                    if (k in bridged) merged.add(k)
                    k = strategy.onNextPage(currentContext, k)
                }
            }
        }
        return merged.distinct()
    }

    private suspend fun stopPageCollections() {
        pageCollectionJobTracker.clear()
        _isFetchingPrevious.value = false
        _isFetchingNext.value = false
    }
}