package com.nxoim.sample.ui

import com.nxoim.evolpagink.PageEvent
import com.nxoim.evolpagink.anchorPages
import com.nxoim.evolpagink.pageable
import com.nxoim.evolpagink.visibilityAwarePrefetchMinimumItemAmount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.random.Random

class Model(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val source: WhateverItemSource = WhateverItemSource(),
) {
    val indexListPlaceholdersTracker = PageTracker()
    val indexListNoPlaceholdersTracker = PageTracker()
    val indexListFixedCountPlaceholdersTracker = PageTracker()

    val indexListPlaceholders = pageable(
        coroutineScope,
        onPage = { source.getPage(it.toString()) },
        strategy = visibilityAwarePrefetchMinimumItemAmount(
            initialPage = 0,
            minimumItemAmountSurroundingVisible = 20
        ),
        onPageEvent = { event -> indexListPlaceholdersTracker.onEvent(event) }
    )

    val indexListNoPlaceholders = pageable(
        coroutineScope,
        onPage = { source.getPageNoPlaceholders(it.toString()) },
        strategy = visibilityAwarePrefetchMinimumItemAmount(
            initialPage = 0,
            minimumItemAmountSurroundingVisible = 20
        ),
        onPageEvent = { event -> indexListNoPlaceholdersTracker.onEvent(event) }
    )

    val keyListPlaceholders = pageable(
        coroutineScope,
        onPage = { source.getPage(it) },
        strategy = visibilityAwarePrefetchMinimumItemAmount(
            initialPageKey = "0",
            onPreviousKey = { ((it.toInt() - 1).takeIf { it >= 0 })?.toString() },
            onNextKey = { (it.toLong() + 1).toString() },
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
        onPageEvent = { event -> indexListFixedCountPlaceholdersTracker.onEvent(event) },
        strategy = anchorPages(initialPage = 0,5)
    )
}

class WhateverItemSource {
    val itemsPerPage= 5

    fun getPage(pageKey: String): Flow<List<ItemData>> =
        getPageInternal(pageKey, withPlaceholders = true)

    fun getPageNoPlaceholders(pageKey: String): Flow<List<ItemData.Loaded>> =
        getPageInternal(pageKey, withPlaceholders = false)

    private fun <T: ItemData> getPageInternal(pageKey: String, withPlaceholders: Boolean): Flow<List<T>> = flow {
        val pageIndex = pageKey.toLong()
        if (withPlaceholders) {
            @Suppress("UNCHECKED_CAST")
            emit(listOf(ItemData.Placeholder(expectedLoadedItemAmount = itemsPerPage)) as List<T>)
        }

        while (true) {
            delay(Random.nextLong(0, 1000))
            val items = List(itemsPerPage) { itemIndexInPage ->
                val randomValue = if (Random.nextBoolean()) "🔥" else "✳️"
                val globalId = "item_page${pageIndex}_item$itemIndexInPage"
                ItemData.Loaded(id = globalId, text = "Item $itemIndexInPage, Page $pageIndex, value $randomValue")
            }
            @Suppress("UNCHECKED_CAST")
            emit(items as List<T>)
            delay(1000)
        }
    }
}

sealed interface ItemData {
    data class Loaded(val id: String, val text: String) : ItemData
    data class Placeholder(val expectedLoadedItemAmount: Int, val id: Long = Random.nextLong()) : ItemData
}

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
