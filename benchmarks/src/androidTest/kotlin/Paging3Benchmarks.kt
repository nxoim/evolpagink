
import android.annotation.SuppressLint
import androidx.paging.CombinedLoadStates
import androidx.paging.DifferCallback
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.NullPaddedList
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingDataDiffer
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

@SuppressLint("RestrictedApi")
private val pagingConfig = PagingConfig(
    pageSize = ITEMS_PER_PAGE,
    prefetchDistance = ITEMS_PER_PAGE,
    initialLoadSize = MAX_LOAD_SIZE,
    maxSize = Int.MAX_VALUE,
)

class Paging3Benchmarked(
    private val pageToScrollTo: Int,
    private val pagesToInvalidate: IntRange
) : Benchmarkable {

    private var lastInvalidatedPage = pagesToInvalidate.first + 1

    @SuppressLint("RestrictedApi")
    override suspend fun benchmark() = runTest {
        val differ = pagingDataDiffer()
        val collectJob = backgroundScope.launch {
            Pager(
                config = pagingConfig,
                pagingSourceFactory = { itemPagingSource(lastInvalidatedPage) }
            ).flow.collectLatest(differ::collectFrom)
        }

        differ.onPagesUpdatedFlow
            .filter { differ.snapshot().items.size % ITEMS_PER_PAGE == 0 }
            .transformWhile {
                if (!differ.loadStateFlow.value.isIdle()) return@transformWhile true

                val latestItems = differ.snapshot().items
                val lastItem = latestItems.lastOrNull() ?: return@transformWhile true
                val lastPage = pageFor(lastItem)
                val invalidatedItem = latestItems.lastInvalidatedItem()

                val isFinished =
                    (invalidatedItem != null &&
                            invalidatedItem.lastInvalidatedPage >= pagesToInvalidate.last) ||
                            (pagesToInvalidate.isEmpty() && lastPage >= pageToScrollTo)

                emit(latestItems)
                !isFinished
            }
            .collect { items ->
                require(items.isNotEmpty()) { "Items were empty" }
                val lastPageKey = pageFor(items.last())
                check(lastPageKey <= pageToScrollTo) {
                    "Invalid page key $lastPageKey. Expected $pageToScrollTo"
                }

                val invalidatedItem = items.lastInvalidatedItem()
                val canInvalidate =
                    invalidatedItem == null ||
                            invalidatedItem.lastInvalidatedPage == lastInvalidatedPage

                if (canInvalidate && ++lastInvalidatedPage <= pagesToInvalidate.last) {
                    differ.refresh()
                    delay(ITEM_LOAD_DELAY_MS) // simulate rebuild cost
                }
            }

        collectJob.cancelAndJoin()
    }

    private fun List<Item>.lastInvalidatedItem(): Item? =
        firstOrNull { it.lastInvalidatedPage == lastInvalidatedPage }
}

@SuppressLint("RestrictedApi")
private fun pagingDataDiffer() = object : PagingDataDiffer<Item>(
    differCallback = object : DifferCallback {
        override fun onChanged(position: Int, count: Int) = Unit
        override fun onInserted(position: Int, count: Int) = Unit
        override fun onRemoved(position: Int, count: Int) = Unit
    }
) {
    override suspend fun presentNewList(
        previousList: NullPaddedList<Item>,
        newList: NullPaddedList<Item>,
        lastAccessedIndex: Int,
        onListPresentable: () -> Unit
    ): Int {
        onListPresentable()
        return lastAccessedIndex
    }
}

private fun itemPagingSource(lastInvalidatedPage: Int) = object : PagingSource<Int, Item>() {
    override fun getRefreshKey(state: PagingState<Int, Item>): Int =
        state.pages.firstOrNull()?.data?.firstOrNull()?.let(::pageFor) ?: 0

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Item> {
        val page = params.key ?: 0
        val items = ItemDataSource.loadItems(page, params.loadSize, lastInvalidatedPage)
        return LoadResult.Page(
            data = items,
            nextKey = if (items.isEmpty()) null else pageFor(items.last()) + 1,
            prevKey = if (page <= 0) null else page - 1
        )
    }
}

private fun CombinedLoadStates?.isIdle(): Boolean {
    if (this == null) return false
    return source.isIdle() && (mediator?.isIdle() ?: true)
}

private fun LoadStates.isIdle(): Boolean =
    refresh is LoadState.NotLoading &&
            append is LoadState.NotLoading &&
            prepend is LoadState.NotLoading