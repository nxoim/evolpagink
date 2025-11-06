import com.nxoim.evolpagink.core.InternalPageableApi
import com.nxoim.evolpagink.core.PageDisplayingEvent
import com.nxoim.evolpagink.core.pageable
import com.nxoim.evolpagink.core.prefetchMinimumItemAmount
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.test.runTest
import java.util.concurrent.ConcurrentHashMap

class EvolpaginkBenchmarked(
    private val pageToScrollTo: Int,
    private val pagesToInvalidate: IntRange
) : Benchmarkable {

    private var lastInvalidatedPage = pagesToInvalidate.first + 1
    private val pageFlows = ConcurrentHashMap<Int, MutableStateFlow<List<Item>?>>() // page -> latest items or null

    @OptIn(InternalPageableApi::class)
    override suspend fun benchmark() = runTest {
        val pageable = pageable(
            coroutineScope = this.backgroundScope,
            onPage = { page ->
                // capture snapshot to avoid races with concurrent invalidation
                val snapshotInvalidated = lastInvalidatedPage
                pageFlows.getOrPut(page) {
                    val loadSize = if (page == 0) MAX_LOAD_SIZE else ITEMS_PER_PAGE
                    val items = ItemDataSource.loadItems(page, loadSize, snapshotInvalidated)
                    MutableStateFlow(items)
                }
            },
            strategy = prefetchMinimumItemAmount(
                initialPage = 0,
                minimumItemAmountSurroundingVisible = ITEMS_PER_PAGE * 2
            )
        )

        pageable.items
            // explicitly drop empty emissions
            .filter { it.isNotEmpty() && it.size % ITEMS_PER_PAGE == 0 }
            .takeWhile { items ->
                val lastItem = items.last()
                val lastPage = pageFor(lastItem)
                val invalidatedItem = items.lastInvalidatedItem()

                val isFinished =
                    (invalidatedItem != null &&
                            invalidatedItem.lastInvalidatedPage >= pagesToInvalidate.last) ||
                            (pagesToInvalidate.isEmpty() && lastPage >= pageToScrollTo)

                !isFinished
            }
            .collect { items ->
                require(items.isNotEmpty()) { "Items were empty" }

                val lastPageKey = pageable.getPageKeyForItem(items.last())
                require(lastPageKey != null) { "Could not infer page key from item" }
                check(lastPageKey <= pageToScrollTo) {
                    "Invalid page key $lastPageKey. Expected $pageToScrollTo"
                }

                val invalidatedItem = items.lastInvalidatedItem()
                val canInvalidate =
                    invalidatedItem == null ||
                            invalidatedItem.lastInvalidatedPage == lastInvalidatedPage

                if (canInvalidate && ++lastInvalidatedPage <= pagesToInvalidate.last) {
                    // replace each cached page with freshly loaded items using the new lastInvalidatedPage
                    val snapshotInvalidated = lastInvalidatedPage
                    pageFlows.forEach { (page, flow) ->
                        val loadSize = if (page == 0) MAX_LOAD_SIZE else ITEMS_PER_PAGE
                        val newItems = ItemDataSource.loadItems(page, loadSize, snapshotInvalidated)
                        flow.value = newItems
                    }

                    // tell pageable which pages are visible so it can re-evaluate (forces reload semantics)
                    pageable.getPageKeyForItem(items.last())?.let { visibleKey ->
                        pageable._onEvent(PageDisplayingEvent.VisibleItemsUpdated(listOf(visibleKey)))
                    }

                    delay(ITEM_LOAD_DELAY_MS) // preserve rebuild cost parity with Paging3
                }
            }
    }

    private fun List<Item>.lastInvalidatedItem(): Item? =
        firstOrNull { it.lastInvalidatedPage == lastInvalidatedPage }
}