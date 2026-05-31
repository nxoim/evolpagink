@file:OptIn(ExperimentalAtomicApi::class)

package com.nxoim.evolpagink.core

import androidx.collection.MutableScatterSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ensureActive
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
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
internal class Paginator<Key : Any, PageItem, Context>(
    private val scope: CoroutineScope,
    private val contextFlow: StateFlow<Context>,
    private val onPage: Context.(Key) -> Flow<List<PageItem>?>,
    private val strategy: PageFetchStrategy<Key, PageItem, Context>,
    private val pageItemKey: (PageItem) -> Any
) {
    private val storage = ObservablePageStorage<Key, PageItem>(
        ScatterMapPageStorage(pageItemKey)
    )
    private val pageCollectionJobTracker = PageJobTracker<Key>()
    private var jumpJob: Job? = null
    private val _activePageKeys = MutableStateFlow(listOf(strategy.initialPage(contextFlow.value)))
    val activePageKeys = _activePageKeys.asStateFlow()

    private val _isFetchingPrevious = MutableStateFlow(false)
    val isFetchingPrevious = _isFetchingPrevious.asStateFlow()
    private val _isFetchingNext = MutableStateFlow(false)
    val isFetchingNext = _isFetchingNext.asStateFlow()

    // only set in collectPagesAndFlattenIntoItemList
    private var currentContext = contextFlow.value

    fun collectPagesAndFlattenIntoItemList(): Flow<List<PageItem>> = contextFlow
        .flatMapLatest { newContext ->
            // make sure to always reuse the cache if context
            // didn't change since last subscription
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

            newContext.collectAndFlattenPages()
        }
        .onCompletion { stopPageCollections() }

    suspend fun preloadAndActivate(
        prefetchContext: CoroutineContext,
        key: Key
    ) = preloadAndActivate(prefetchContext, key, storage.getSnapshot())

    private suspend fun preloadAndActivate(
        prefetchContext: CoroutineContext,
        key: Key,
        pagesSnapshot: Map<Key, List<PageItem>>
    ): List<PageItem>? {
        jumpJob?.cancelAndJoin()

        val deferred = scope.async(prefetchContext) {
            val pageContents = pagesSnapshot[key] ?: onPage(currentContext, key)
                .firstOrNull()
                .also {
                    ensureActive()
                    if (it != null) storage.updatePage(key, it, true)
                    else storage.removePage(key, true)
                }

            ensureActive()
            // call with mocked event to trigger user defined
            // preload strategies as well
            updatePagesToCache(PageDisplayingEvent.PageAnchorChanged(key))
            pageContents
        }

        jumpJob = deferred

        return try {
            deferred.await()
        } catch (e: CancellationException) {
            null // superseded by a newer jump
        }
    }

    suspend fun updatePagesToCache(event: PageDisplayingEvent<Key>) {
        val context = currentContext
        val pagesSnapshot = storage.getSnapshot()

        val newPages = strategy
            .calculatePages(
                PageFetchContext(
                    event = event,
                    pageCache = pagesSnapshot,
                    externalContext = context
                )
            )
            .ifEmpty { activePageKeys.value }
            .ifEmpty { listOf(strategy.initialPage(context)) }

        _activePageKeys.update {
            mergeBridgedKeys(newPages, pagesSnapshot, context)
        }
    }

    suspend fun getPageKeyForItem(item: PageItem): Key? = storage.getPageKeyForItem(item)

    private fun Context.collectAndFlattenPages(): Flow<List<PageItem>> =
        activePageKeys.flatMapLatest { newPageKeys ->
            val currentlyActiveKeys = pageCollectionJobTracker.active
            val toCancel = currentlyActiveKeys - newPageKeys.toSet()

            toCancel.forEach { key ->
                pageCollectionJobTracker.cancelAndJoin(key)
                storage.removePage(key, false)
            }

            storage.pageSnapshots
                .onStart { emit(storage.getSnapshot()) }
                .map { pagesSnapshot ->
                    newPageKeys.flatMap { pageKey ->
                        launchPageCollectionIfNeeded(pageKey, pagesSnapshot)

                        pagesSnapshot[pageKey].orEmpty()
                    }
                }
        }

    private fun Context.launchPageCollectionIfNeeded(
        key: Key,
        pagesSnapshot: Map<Key, List<PageItem>>
    ) {
        pageCollectionJobTracker.launchIfIdle(key, scope) {
            fun isFirstItem() = (_activePageKeys.value.firstOrNull() == key)
            fun isLastItem() = (_activePageKeys.value.lastOrNull() == key)

            onPage(key)
                .cancellable()
                .onStart {
                    if (!pagesSnapshot.contains(key)) {
                        if (isFirstItem() && isPreviousPageExpected(key)) {
                            _isFetchingPrevious.value = true
                        }
                        if (isLastItem() && isNextPageExpected(key)) {
                            _isFetchingNext.value = true
                        }
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
        pagesSnapshot: Map<Key, List<PageItem>>,
        context: Context
    ): List<Key> {
        val bridged = MutableScatterSet<Key>()

        for (index in 0 until target.lastIndex) {
            var key = strategy.onNextPage(context, target[index])

            while (key != null && key != target[index + 1]) {
                if (pagesSnapshot.contains(key) || pageCollectionJobTracker.isActive(key)) {
                    bridged.add(key)
                }
                key = strategy.onNextPage(context, key)
            }
        }

        val merged = ArrayList<Key>(target.size + bridged.size)

        for (index in target.indices) {
            merged.add(target[index])

            if (index < target.lastIndex) {
                var nextKey = strategy.onNextPage(context, target[index])

                while (nextKey != null && nextKey != target[index + 1]) {
                    if (nextKey in bridged) merged.add(nextKey)
                    nextKey = strategy.onNextPage(context, nextKey)
                }
            }
        }

        return merged
    }

    private fun Context.isNextPageExpected(key: Key): Boolean =
        strategy.onNextPage(this, key) != null

    private fun Context.isPreviousPageExpected(key: Key): Boolean =
        strategy.onPreviousPage(this, key) != null

    private suspend fun stopPageCollections() {
        pageCollectionJobTracker.clear()
        _isFetchingPrevious.value = false
        _isFetchingNext.value = false
    }
}