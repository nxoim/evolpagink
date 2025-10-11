package com.nxoim.evolpagink.core

import kotlin.jvm.JvmInline

fun <PageItem> visibilityAwarePrefetchMinimumItemAmount(
    initialPage: Int = 0,
    minimumItemAmountSurroundingVisible: Int = 20
): PageFetchStrategy<Int, PageItem, VisibleItemsUpdated<Int>> = visibilityAwarePrefetchMinimumItemAmount(
    initialPage = initialPage,
    onNextPage = { it + 1 },
    onPreviousPage = { if (it > 0) it - 1 else null },
    minimumItemAmountSurroundingVisible = minimumItemAmountSurroundingVisible
)

fun <Key : Any, PageItem> visibilityAwarePrefetchMinimumItemAmount(
    initialPage: Key,
    onNextPage: (key: Key) -> Key?,
    onPreviousPage: (key: Key) -> Key?,
    minimumItemAmountSurroundingVisible: Int = 20
): PageFetchStrategy<Key, PageItem, VisibleItemsUpdated<Key>> = PageFetchStrategy(
    initialPage = initialPage,
    onNextPage = onNextPage,
    onPreviousPage = onPreviousPage
) { context ->
    val halvedAmount = minimumItemAmountSurroundingVisible / 2
    val pages = context.event.value.toMutableList()

    prefetchForwardByItemCount(pages, onNextPage, context.pageCache, halvedAmount)
    prefetchBackwardByItemCount(pages, onPreviousPage, context.pageCache, halvedAmount)
    pages
}


fun <PageItem> visibilityAwarePrefetchPageAmount(
    initialPage: Int = 0,
    pageAmountSurroundingVisible: Int = 2
): PageFetchStrategy<Int, PageItem, VisibleItemsUpdated<Int>> = visibilityAwarePrefetchPageAmount(
    initialPage = initialPage,
    onNextPage = { it + 1 },
    onPreviousPage = { if (it > 0) it - 1 else null },
    pageAmountSurroundingVisible = pageAmountSurroundingVisible
)

fun <Key : Any, PageItem> visibilityAwarePrefetchPageAmount(
    initialPage: Key,
    onNextPage: (key: Key) -> Key?,
    onPreviousPage: (key: Key) -> Key?,
    pageAmountSurroundingVisible: Int = 2
): PageFetchStrategy<Key, PageItem, VisibleItemsUpdated<Key>> = PageFetchStrategy(
    initialPage = initialPage,
    onNextPage = onNextPage,
    onPreviousPage = onPreviousPage,
    onPageCalculation = { context ->
        val halvedAmount = pageAmountSurroundingVisible / 2
        val pages = context.event.value.toMutableList()

        prefetchForwardByPageCount(pages, onNextPage, halvedAmount)
        prefetchBackwardByPageCount(pages, onPreviousPage, halvedAmount)
        pages
    }
)

fun <PageItem> anchorPages(
    initialPage: Int,
    pageAmountSurroundingAnchor: Int = 5
): PageFetchStrategy<Int, PageItem, PageAnchorChanged<Int>> = anchorPages(
    initialPage = initialPage,
    onNextPage = { it + 1 },
    onPreviousPage = { if (it > 0) it - 1 else null },
    pageAmountSurroundingAnchor = pageAmountSurroundingAnchor
)

fun <Key : Any, PageItem> anchorPages(
    initialPage: Key,
    onNextPage: (key: Key) -> Key?,
    onPreviousPage: (key: Key) -> Key?,
    pageAmountSurroundingAnchor: Int = 5
): PageFetchStrategy<Key, PageItem, PageAnchorChanged<Key>> = PageFetchStrategy(
    initialPage = initialPage,
    onNextPage = onNextPage,
    onPreviousPage = onPreviousPage,
    onPageCalculation = { context ->
        val halvedAmount = pageAmountSurroundingAnchor / 2
        val pages = mutableListOf(context.event.anchor)

        prefetchForwardByPageCount(pages, onNextPage, halvedAmount)
        prefetchBackwardByPageCount(pages, onPreviousPage, halvedAmount)
        pages
    }
)

////////////////////////////////////////////////////////////////////////////////////////////

class PageFetchStrategy<Key : Any, PageItem, Event>(
    val initialPage: Key,
    val onNextPage: (key: Key) -> Key?,
    val onPreviousPage: (key: Key) -> Key?,
    private val onPageCalculation: PageFetchStrategy<Key, PageItem, Event>.(
        context: PageFetchContext<Key, PageItem, Event>
    ) -> List<Key>
) {
    // this is made for easy constructor property access
    // since the class is small
    fun calculatePages(context: PageFetchContext<Key, PageItem, Event>): List<Key> =
        onPageCalculation(context)
}

class PageFetchContext<Key : Any, PageItem, Event> internal constructor(
    val event: Event,
    val pageCache: Map<Key, List<PageItem>>
)


@JvmInline
value class VisibleItemsUpdated<Key : Any>(val value: List<Key>)
typealias VisibilityAwarePageable<Key, PageItem> = Pageable<Key, PageItem, VisibleItemsUpdated<Key>>

@JvmInline
value class PageAnchorChanged<Key : Any>(val anchor: Key)
typealias AnchoredPageable<Key, PageItem> = Pageable<Key, PageItem, PageAnchorChanged<Key>>

////////////////////////////////////////////////////////////////////////////////////////////

private inline fun <Key : Any, PageItem> prefetchForwardByItemCount(
    pages: MutableList<Key>,
    onNextKey: (Key) -> Key?,
    pageCache: Map<Key, List<PageItem>>,
    count: Int
) {
    var prefetchedItems = 0
    var nextKey = onNextKey(pages.lastOrNull() ?: return)

    while (nextKey != null && prefetchedItems < count) {
        if (pages.contains(nextKey)) break
        pages.add(nextKey)

        val pageSize = pageCache[nextKey]?.size ?: 0
        if (pageSize == 0) break

        prefetchedItems += pageSize
        nextKey = onNextKey(nextKey)
    }
}

private inline fun <Key : Any, PageItem> prefetchBackwardByItemCount(
    pages: MutableList<Key>,
    onPreviousKey: (Key) -> Key?,
    pageCache: Map<Key, List<PageItem>>,
    count: Int
) {
    var prefetchedItems = 0
    var prevKey = onPreviousKey(pages.firstOrNull() ?: return)

    while (prevKey != null && prefetchedItems < count) {
        if (pages.contains(prevKey)) break
        pages.add(0, prevKey)

        val pageSize = pageCache[prevKey]?.size ?: 0
        if (pageSize == 0) break

        prefetchedItems += pageSize
        prevKey = onPreviousKey(prevKey)
    }
}

private inline fun <Key : Any> prefetchForwardByPageCount(
    pages: MutableList<Key>,
    onNextKey: (Key) -> Key?,
    count: Int
) {
    var nextKey = onNextKey(pages.lastOrNull() ?: return)
    repeat(count) {
        if (nextKey == null || pages.contains(nextKey)) return
        pages.add(nextKey)
        nextKey = onNextKey(nextKey)
    }
}

private inline fun <Key : Any> prefetchBackwardByPageCount(
    pages: MutableList<Key>,
    onPreviousKey: (Key) -> Key?,
    count: Int
) {
    var prevKey = onPreviousKey(pages.firstOrNull() ?: return)
    repeat(count) {
        if (prevKey == null || pages.contains(prevKey)) return
        pages.add(0, prevKey)
        prevKey = onPreviousKey(prevKey)
    }
}