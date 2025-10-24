package com.nxoim.sample.ui

import androidx.collection.LruCache
import com.nxoim.evolpagink.core.PageEvent
import com.nxoim.evolpagink.core.anchorPages
import com.nxoim.evolpagink.core.pageable
import com.nxoim.evolpagink.core.visibilityAwarePrefetchMinimumItemAmount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.random.Random

class Model(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val source: WhateverItemSource = WhateverItemSource(),
    private val searchRepo: SearchRepository = SearchRepository()
) {
    val indexListPlaceholdersTracker = PageTracker()
    val indexListNoPlaceholdersTracker = PageTracker()
    val indexListFixedCountPlaceholdersTracker = PageTracker()
    val pagerPlaceholdersSampleTracker = PageTracker()

    val indexListPlaceholders = pageable(
        coroutineScope,
        onPage = { source.getPage(it.toString()) },
        strategy = visibilityAwarePrefetchMinimumItemAmount(
            initialPage = 0,
            minimumItemAmountSurroundingVisible = 20
        ),
        onPageEvent = indexListPlaceholdersTracker::onEvent
    )

    val indexListNoPlaceholders = pageable(
        coroutineScope,
        onPage = { source.getPageNoPlaceholders(it.toString()) },
        strategy = visibilityAwarePrefetchMinimumItemAmount(
            initialPage = 0,
            minimumItemAmountSurroundingVisible = 20
        ),
        onPageEvent = indexListNoPlaceholdersTracker::onEvent
    )

    val keyListPlaceholders = pageable(
        coroutineScope,
        onPage = { source.getPage(it) },
        strategy = visibilityAwarePrefetchMinimumItemAmount(
            initialPage = "0",
            onPreviousPage = { ((it.toInt() - 1).takeIf { it >= 0 })?.toString() },
            onNextPage = { (it.toLong() + 1).toString() },
            minimumItemAmountSurroundingVisible = 20
        ),
    )

    val indexGridPlaceholders = pageable(
        coroutineScope,
        onPage = { source.getPage(it.toString()) },
        strategy = visibilityAwarePrefetchMinimumItemAmount(
            initialPage = 0,
            minimumItemAmountSurroundingVisible = 20
        )
    )

    val indexStaggeredGridPlaceholders = pageable(
        coroutineScope,
        onPage = { source.getPage(it.toString()) },
        strategy = visibilityAwarePrefetchMinimumItemAmount(
            initialPage = 0,
            minimumItemAmountSurroundingVisible = 20
        ),
    )

    val indexListFixedCountPlaceholders = pageable(
        coroutineScope,
        onPage = { source.getPage(it.toString()) },
        onPageEvent = indexListFixedCountPlaceholdersTracker::onEvent,
        strategy = anchorPages(
            initialPage = 0,
            pageAmountSurroundingAnchor = 5
        )
    )

    val pagerPlaceholders = pageable(
        coroutineScope,
        onPage = { source.getPage(it.toString()) },
        strategy = visibilityAwarePrefetchMinimumItemAmount(
            initialPage = 0,
            minimumItemAmountSurroundingVisible = 2
        ),
        onPageEvent = pagerPlaceholdersSampleTracker::onEvent
    )

    private val _loadingFirstSearchResults = MutableStateFlow(false)
    val loadingFirstSearchResults = _loadingFirstSearchResults.asStateFlow()

    private val currentSearch = MutableStateFlow(SearchContext(""))

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchPageable = pageable(
        coroutineScope,
        context = currentSearch,
        onPage = { cursor ->
            searchRepo.search(
                session = currentSearch.value,
                cursor = cursor
            ).onEach { _loadingFirstSearchResults.update { false } }
        },
        strategy = visibilityAwarePrefetchMinimumItemAmount(
            initialPage = currentSearch.value.cursorCache.first(),
            onNextPage = { searchRepo.nextCursor(currentSearch.value, it) },
            onPreviousPage = { searchRepo.previousCursor(currentSearch.value, it) },
            minimumItemAmountSurroundingVisible = 20
        )
    )

    fun updateQuery(newQuery: String) {
        _loadingFirstSearchResults.update { true }
        currentSearch.value = SearchContext(newQuery)
    }
}


////////////////////////////////////////////////////////////////////////////////////////////

class PageTracker {
    private val _pages = MutableStateFlow<Map<Int, PageEvent<Int>>>(emptyMap())
    val pages = _pages.map { it.values.sortedBy { state -> state.key } }

    private val _lastChanged = MutableStateFlow<Pair<Int, PageEvent<Int>>?>(null)
    val lastChanged = _lastChanged.asStateFlow()

    fun onEvent(event: PageEvent<Int>) {
        _pages.update { current ->
            current.toMutableMap().apply {
                put(event.key, event)
            }
        }
        _lastChanged.value = event.key to event
    }
}

class WhateverItemSource {
    fun getPage(pageKey: String): Flow<List<ItemData>> =
        createPageFlow(pageKey, withPlaceholders = true)

    fun getPageNoPlaceholders(pageKey: String): Flow<List<ItemData.Loaded>> =
        createPageFlow(pageKey, withPlaceholders = false)
}

private fun <T : ItemData> createPageFlow(
    pageKey: String,
    withPlaceholders: Boolean
): Flow<List<T>> = flow {
    val pageIndex = pageKey.toLong()
    if (withPlaceholders) {
        @Suppress("UNCHECKED_CAST")
        emit(listOf(ItemData.Placeholder(expectedLoadedItemAmount = itemsPerPage)) as List<T>)
    }

    while (true) {
        delay(Random.nextLong(0, 1000))
        emitFakePage(pageIndex)
        delay(1000)
    }
}

private suspend fun <T : ItemData> FlowCollector<List<T>>.emitFakePage(
    pageIndex: Long
) {
    val items = List(itemsPerPage) { itemIndexInPage ->
        val randomValue = if (Random.nextBoolean()) "🔥" else "✳️"
        val globalId = "item_page${pageIndex}_item$itemIndexInPage"
        ItemData.Loaded(
            id = globalId,
            text = "Item $itemIndexInPage, Page $pageIndex, value $randomValue"
        )
    }
    @Suppress("UNCHECKED_CAST")
    emit(items as List<T>)
}


class SearchRepository(
    private val dataSource: SearchItemSource = SearchItemSource()
) {
    fun search(session: SearchContext, cursor: String): Flow<List<ItemData.Loaded>> =
        dataSource.search(session.query, cursor)
            .map { page ->
                cacheNextCursor(session, cursor, page.nextCursor)
                page.items
            }

    fun nextCursor(session: SearchContext, current: String): String? =
        session.cursorCache.indexOf(current)
            .takeIf { it >= 0 }
            ?.let { session.cursorCache.getOrNull(it + 1) }
            .also { println("search results: next key is ${it ?: "none"}") }

    fun previousCursor(session: SearchContext, current: String): String? =
        session.cursorCache.indexOf(current)
            .takeIf { it > 0 }
            ?.let { session.cursorCache.getOrNull(it - 1) }
            .also { println("search results: previous key is ${it ?: "none"}") }

    private fun cacheNextCursor(session: SearchContext, cursor: String, nextCursor: String?) {
        if (nextCursor == null || session.cursorCache.contains(nextCursor)) return
        val index = session.cursorCache.indexOf(cursor)
        if (index != -1) session.cursorCache.add(index + 1, nextCursor)
    }
}

class SearchItemSource {
    private val cursorsForQueries = LruCache<String, Map<String, Int>>(10)

    fun search(query: String, cursor: String): Flow<SearchPage> = flow {
        val cursors = getOrCreateCursors(query)
        val pageIndex = cursors[cursor] ?: return@flow
        val allItems = generateDataset().filter { it.text.contains(query, ignoreCase = true) }

        val start = pageIndex * itemsPerPage
        val pageItems = allItems.drop(start).take(itemsPerPage)
        val nextCursor = cursors.entries.elementAtOrNull(pageIndex + 1)?.key

        delay(Random.nextLong(0, 500))
        emit(SearchPage(pageItems, nextCursor))
    }

    private fun getOrCreateCursors(query: String) =
        cursorsForQueries[query] ?: newCursors().also { cursorsForQueries.put(query, it) }

    private fun newCursors() = buildMap {
        put("", 0)
        repeat(99) { index -> put(Random.nextLong().toString(), index + 1) }
    }

    private fun generateDataset(): List<ItemData.Loaded> {
        val base = listOf(
            "apple",
            "banana",
            "car",
            "dog",
            "elephant",
            "fish",
            "grape",
            "house",
            "island",
            "jacket"
        )
        return List(100) { i -> ItemData.Loaded("item_$i", "Item $i: ${base[i % base.size]}") }
    }
}

sealed interface ItemData {
    data class Loaded(val id: String, val text: String) : ItemData
    data class Placeholder(
        val expectedLoadedItemAmount: Int,
        val id: Long = Random.nextLong()
    ) : ItemData
}

class SearchContext(val query: String) {
    val cursorCache = mutableListOf("")
}

data class SearchPage(val items: List<ItemData.Loaded>, val nextCursor: String?)

private const val itemsPerPage = 5
