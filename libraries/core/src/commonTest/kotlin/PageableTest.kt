@file:OptIn(InternalPageableApi::class, ExperimentalCoroutinesApi::class)

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test

import com.nxoim.evolpagink.core.InternalPageableApi
import com.nxoim.evolpagink.core.PageDisplayingEvent
import com.nxoim.evolpagink.core.Pageable
import com.nxoim.evolpagink.core.pageable
import com.nxoim.evolpagink.core.prefetchMinimumItemAmount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class PageableTest {
    @Test
    fun initialLoad_firstPageFetchedAutomatically() = runTest {
        val source = FakeSource()
        source.load(0, pageItems(0))

        val pageable = backgroundScope.makePageable(source)

        pageable.items.test {
            val items = awaitNonEmpty()
            assertEquals(pageItems(0), items)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun contextChange_restartsFromPageZero_withFreshData() = runTest {
        val context = MutableStateFlow("query_a")
        val sourceA = FakeSource().also { it.load(0, pageItems(0)) }
        val sourceB = FakeSource().also { it.load(0, pageItems(10)) }

        val pageable = backgroundScope.makeContextPageable(
            context = context,
            onPage = { key ->
                if (this == "query_a") sourceA.page(key) else sourceB.page(key)
            },
        )

        pageable.items.test {
            awaitNonEmpty()

            context.value = "query_b"

            val itemsAfterReset = awaitNonEmpty()
            assertTrue(
                itemsAfterReset.all { it.startsWith("p10") },
                "Expected query_b items, got: $itemsAfterReset",
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun contextChange_noStaleItemsLeakBeforeNewPageLoads() = runTest {
        val context = MutableStateFlow("a")
        val sourceA = FakeSource().also { it.load(0, pageItems(0)) }
        val sourceB = FakeSource()

        val pageable = backgroundScope.makeContextPageable(
            context = context,
            onPage = { key ->
                if (this == "a") sourceA.page(key) else sourceB.page(key)
            },
        )

        pageable.items.test {
            awaitNonEmpty()

            context.value = "b"

            val itemsAfterReset = awaitItem()
            assertTrue(
                itemsAfterReset.isEmpty() || itemsAfterReset.none { it.startsWith("p0i") },
                "Stale items from previous context leaked: $itemsAfterReset",
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun scrollForward_prefetchesNextPage() = runTest {
        val source = FakeSource()
        source.load(0, pageItems(0))
        source.load(1, pageItems(1))

        val pageable = backgroundScope.makePageable(source)

        pageable.items.test {
            awaitNonEmpty()

            pageable.onVisibilityEvent(PageDisplayingEvent.VisibleItemsUpdated(listOf(0)))

            val withPrefetchedPage = awaitNonEmpty()
            assertTrue(
                withPrefetchedPage.containsAll(pageItems(1)),
                "Forward prefetch did not load page 1: $withPrefetchedPage",
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun scrollBackward_prefetchesPreviousPage() = runTest {
        val source = FakeSource()
        source.load(0, pageItems(0))
        source.load(1, pageItems(1))
        source.load(2, pageItems(2))

        val pageable = backgroundScope.makePageable(source)

        pageable.items.test {
            awaitNonEmpty()

            pageable.onVisibilityEvent(PageDisplayingEvent.PageAnchorChanged(2))
            advanceUntilIdle()

            pageable.onVisibilityEvent(PageDisplayingEvent.VisibleItemsUpdated(listOf(2)))

            val withBackPrefetchedPage = awaitNonEmpty()
            assertTrue(
                withBackPrefetchedPage.containsAll(pageItems(1)),
                "Backward prefetch did not load page 1: $withBackPrefetchedPage",
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun isFetchingNext_pageAppearsInItemsOnceFetchCompletes() = runTest {
        val page1Gate = MutableSharedFlow<List<String>?>(replay = 1)
        page1Gate.tryEmit(pageItems(1)) // pre-load before any collector exists

        val pageable = pageable(
            coroutineScope = backgroundScope,
            onPage = { key ->
                when (key) {
                    0 -> flowOf(pageItems(0))
                    else -> page1Gate
                }
            },
            strategy = prefetchMinimumItemAmount(minimumItemAmount = prefetchAmount),
            pageItemKey = { it },
        )

        pageable.items.test {
            val page0Items = awaitNonEmpty()
            assertTrue(
                page0Items.none { it.startsWith("p1") },
                "Page 1 should not appear before strategy requests it",
            )

            pageable.onVisibilityEvent(PageDisplayingEvent.VisibleItemsUpdated(listOf(0)))

            val withPage1Loaded = awaitItemUntil { it.containsAll(pageItems(1)) }
            assertTrue(
                withPage1Loaded.containsAll(pageItems(1)),
                "Page 1 should appear after strategy requests it",
            )

            assertFalse(pageable.isFetchingNext.value)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun isFetchingPrevious_falseAfterLoad_notStuck() = runTest {
        val source = FakeSource()
        source.load(0, pageItems(0))

        val pageable = backgroundScope.makePageable(source)
        pageable.items.first { it.isNotEmpty() }

        assertFalse(
            pageable.isFetchingPrevious.value,
            "isFetchingPrevious should be false after initial load",
        )
    }

    @Test
    fun jumpTo_loadsAndReturnsExactPageItems() = runTest {
        val source = FakeSource()
        source.load(0, pageItems(0))
        source.load(7, pageItems(7))

        val pageable = backgroundScope.makePageable(source)
        pageable.items.first { it.isNotEmpty() }

        val result = pageable.jumpTo(7)

        assertEquals(pageItems(7), result, "jumpTo(7) should return page 7 items")
    }

    @Test
    fun concurrentJumps_latestJumpWins_noInterleaving() = runTest {
        val source = FakeSource()
        (0..10).forEach { source.load(it, pageItems(it)) }

        val pageable = backgroundScope.makePageable(source)
        pageable.items.first { it.isNotEmpty() }

        val jobs = (1..5).map { page ->
            async { pageable.jumpTo(page) }
        }
        val results = jobs.awaitAll()

        assertNotNull(results.last(), "Last concurrent jump must succeed")
    }

    @Test
    fun deduplication_sameItemKeyAcrossPagesAppearsOnce() = runTest {
        val source = FakeSource()
        val duplicateKey = "DUPE"
        source.load(0, listOf(duplicateKey, "p0_1", "p0_2", "p0_3", "p0_4"))
        source.load(1, listOf(duplicateKey, "p1_1", "p1_2", "p1_3", "p1_4"))

        val pageable = pageable(
            coroutineScope = backgroundScope,
            onPage = { key -> source.page(key) },
            strategy = prefetchMinimumItemAmount(minimumItemAmount = pageSize + 1),
            pageItemKey = { it },
        )

        pageable.items.test {
            awaitNonEmpty()

            pageable.onVisibilityEvent(PageDisplayingEvent.PageAnchorChanged(0))

            val withBothPagesLoaded = awaitItemUntil { items ->
                items.containsAll(listOf("p1_1", "p1_2", "p1_3", "p1_4"))
            }

            val duplicateCount = withBothPagesLoaded.count { it == duplicateKey }
            assertEquals(1, duplicateCount, "Duplicate key should appear only once: $withBothPagesLoaded")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun transientDeduplication_itemNotDoubledDuringCrossPageOverlap() = runTest {
        val source = FakeSource()
        val sharedItem = pageItems(0).first()
        source.load(0, pageItems(0))
        source.load(1, pageItems(1))

        val pageable = backgroundScope.makePageable(source)

        pageable.items.test {
            awaitNonEmpty()

            pageable.onVisibilityEvent(PageDisplayingEvent.VisibleItemsUpdated(listOf(0)))

            awaitItemUntil { it.containsAll(pageItems(1)) }

            source.load(1, listOf(sharedItem) + pageItems(1).drop(1))

            val items = awaitItem()
            assertEquals(
                1,
                items.count { it == sharedItem },
                "Item $sharedItem should appear exactly once during transient overlap: $items",
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun rapidContextChanges_onlyLatestContextReflected() = runTest {
        val context = MutableStateFlow(0)
        val sources = (0..5).associateWith { generation ->
            FakeSource().also { fakeSource ->
                fakeSource.load(0, (0 until pageSize).map { "gen${generation}_$it" })
            }
        }

        val pageable = backgroundScope.makeContextPageable(
            context = context,
            onPage = { key -> sources[this]!!.page(key) },
        )

        pageable.items.first { it.isNotEmpty() }

        (1..5).forEach { context.value = it }

        val settled = pageable.items.first { items ->
            items.isNotEmpty() && items.all { it.startsWith("gen5_") }
        }
        assertTrue(
            settled.all { it.startsWith("gen5_") },
            "Expected only gen5 items, got: $settled",
        )
    }

    @Test
    fun placeholderEmission_initialItemsShownBeforeFirstPageLoads() = runTest {
        val placeholder = "PLACEHOLDER"
        val source = FakeSource()

        val pageable = backgroundScope.makePageable(
            source = source,
            initialItems = listOf(placeholder),
        )

        pageable.items.test {
            assertEquals(
                listOf(placeholder),
                awaitItem(),
                "initialItems should be visible before first page arrives",
            )

            source.load(0, pageItems(0))

            val loaded = awaitNonEmpty()
            assertEquals(pageItems(0), loaded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun pageReEmission_updatesOnlyThatPageSlice_adjacentPageIntact() = runTest {
        val source = FakeSource()
        source.load(0, pageItems(0))
        source.load(1, pageItems(1))

        val pageable = backgroundScope.makePageable(source)

        pageable.items.test {
            awaitNonEmpty()
            pageable.onVisibilityEvent(PageDisplayingEvent.VisibleItemsUpdated(listOf(0)))
            awaitNonEmpty()

            val updatedPage0 = (0 until pageSize).map { "upd_$it" }
            source.load(0, updatedPage0)

            val final = awaitNonEmpty()
            assertTrue(final.containsAll(updatedPage0), "Updated page 0 missing: $final")
            assertTrue(final.containsAll(pageItems(1)), "Page 1 corrupted after page 0 re-emit: $final")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun nullPage_producesNoItems_andResolvesWhenNonNullArrives() = runTest {
        val source = FakeSource()

        val pageable = backgroundScope.makePageable(source)

        pageable.items.test {
            assertTrue(awaitItem().isEmpty(), "Null page must produce no items")

            source.load(0, pageItems(0))

            assertEquals(pageItems(0), awaitNonEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyPage_doesNotBlockAdjacentPageFetching() = runTest {
        val source = FakeSource()
        source.load(0, emptyList())
        source.load(1, pageItems(1))

        val pageable = backgroundScope.makePageable(source)

        pageable.items.test {
            awaitItem()

            pageable.onVisibilityEvent(PageDisplayingEvent.VisibleItemsUpdated(listOf(0)))
            advanceUntilIdle()

            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(
            pageable.isFetchingNext.value,
            "isFetchingNext stuck true after empty page",
        )
    }

    @Test
    fun cachePersistedAfterCollectorUnsubscribes() = runTest {
        val source = FakeSource()
        source.load(0, pageItems(0))

        val pageable = backgroundScope.makePageable(source)

        pageable.items.first { it.isNotEmpty() }

        source.load(0, null)

        assertEquals(
            pageItems(0),
            pageable.items.value,
            "Cached page should survive collector unsubscription",
        )
    }

    @Test
    fun itemOrderIntegrity_pagesAssembledInCorrectOrder() = runTest {
        val source = FakeSource()
        (0..2).forEach { source.load(it, pageItems(it)) }

        val pageable = backgroundScope.makePageable(source)

        pageable.onVisibilityEvent(PageDisplayingEvent.PageAnchorChanged(2))
        advanceUntilIdle()

        val items = pageable.items.value

        fun firstIdx(page: Int) = items.indexOfFirst { it == pageItems(page).first() }

        val index0 = firstIdx(0)
        val index1 = firstIdx(1)
        val index2 = firstIdx(2)

        if (index0 != -1 && index1 != -1) assertTrue(index0 < index1, "Page 0 must precede page 1 in $items")
        if (index1 != -1 && index2 != -1) assertTrue(index1 < index2, "Page 1 must precede page 2 in $items")
    }

    @Test
    fun removalPropagation_removedItemDisappearsAfterSourceReEmits() = runTest {
        val source = FakeSource()
        val original = pageItems(0)
        source.load(0, original)

        val pageable = backgroundScope.makePageable(source)
        pageable.items.first { it.isNotEmpty() }

        val removedItemKey = original.first()
        val itemsAfterRemoval = original.drop(1)
        source.load(0, itemsAfterRemoval)

        val updated = pageable.items.first { it == itemsAfterRemoval }
        assertFalse(updated.contains(removedItemKey), "Removed item $removedItemKey must not appear")
    }

    @Test
    fun partialLastPage_emitsExactlyAvailableItems_noPaddingOrCrash() = runTest {
        val source = FakeSource()
        source.load(0, pageItems(0, count = 3))

        val pageable = backgroundScope.makePageable(source)

        val items = pageable.items.first { it.isNotEmpty() }
        assertEquals(3, items.size, "Should emit only the 3 available items")
        assertEquals(pageItems(0, count = 3), items)
    }

    @Test
    fun pagesBeyondBounds_noInfiniteLoopOrCrash() = runTest {
        val source = FakeSource()
        source.load(0, pageItems(0))

        val pageable = backgroundScope.makePageable(source)
        pageable.items.first { it.isNotEmpty() }

        withTimeoutOrNull(5.seconds) {
            pageable.jumpTo(999)
        }

        assertFalse(
            pageable.isFetchingNext.value,
            "isFetchingNext must not remain stuck after out-of-bounds jump",
        )
    }
}

private suspend fun <T> ReceiveTurbine<T>.awaitItemUntil(predicate: (T) -> Boolean): T {
    var item = awaitItem()
    while (!predicate(item)) item = awaitItem()
    return item
}

private fun pageItems(page: Int, count: Int = pageSize): List<String> =
    (0 until count).map { "p${page}i${it}" }

private class FakeSource {
    private val flows = HashMap<Int, MutableStateFlow<List<String>?>>()

    private fun stateFlowOf(key: Int) =
        flows.getOrPut(key) { MutableStateFlow(null) }

    fun page(key: Int): Flow<List<String>?> = stateFlowOf(key)

    fun load(key: Int, items: List<String>?) {
        stateFlowOf(key).value = items
    }
}

private fun CoroutineScope.makePageable(
    source: FakeSource,
    pageItemKey: (String) -> Any = { it },
    initialItems: List<String> = emptyList(),
): Pageable<Int, String> = pageable(
    coroutineScope = this,
    onPage = { key -> source.page(key) },
    strategy = prefetchMinimumItemAmount(minimumItemAmount = prefetchAmount),
    initialItems = initialItems,
    pageItemKey = pageItemKey,
)

private fun <C> CoroutineScope.makeContextPageable(
    context: StateFlow<C>,
    onPage: C.(Int) -> Flow<List<String>?>,
    pageItemKey: (String) -> Any = { it },
): Pageable<Int, String> = pageable(
    coroutineScope = this,
    context = context,
    onPage = onPage,
    strategy = prefetchMinimumItemAmount(minimumItemAmount = prefetchAmount),
    pageItemKey = pageItemKey,
)

private suspend fun ReceiveTurbine<List<String>>.awaitNonEmpty(): List<String> {
    var item = awaitItem()
    while (item.isEmpty()) item = awaitItem()
    return item
}

private const val pageSize = 5
private const val prefetchAmount = pageSize * 2