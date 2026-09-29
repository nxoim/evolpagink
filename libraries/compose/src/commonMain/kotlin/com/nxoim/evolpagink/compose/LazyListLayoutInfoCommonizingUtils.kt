package com.nxoim.evolpagink.compose

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed

internal class PageableLazyListLayoutInfo(
    private val state: LazyListState
) : PageableLayoutInfo {
    override val visibleItemCount: Int
        get() = state.layoutInfo.visibleItemsInfo.size

    override fun getVisibleItemKey(index: Int): Any =
        state.layoutInfo.visibleItemsInfo[index].key

    override fun getVisibleItemIndex(index: Int): Int =
        state.layoutInfo.visibleItemsInfo[index].index

    override fun forEachVisibleItem(action: (index: Int, key: Any) -> Unit) {
        state.layoutInfo.visibleItemsInfo.fastForEach { item ->
            action(item.index, item.key)
        }
    }
}

internal class PageableLazyGridLayoutInfo(
    private val state: LazyGridState
) : PageableLayoutInfo {
    private fun getSortedItems(): List<LazyGridItemInfo> {
        val items = state.layoutInfo.visibleItemsInfo

        items.fastForEachIndexed { index, item ->
            if (index < items.lastIndex && item.index > items[index + 1].index) {
                return items.sortedBy { it.index }
            }
        }
        return items
    }

    override val visibleItemCount: Int
        get() = state.layoutInfo.visibleItemsInfo.size

    override fun getVisibleItemKey(index: Int): Any =
        getSortedItems()[index].key

    override fun getVisibleItemIndex(index: Int): Int =
        getSortedItems()[index].index

    override fun forEachVisibleItem(action: (index: Int, key: Any) -> Unit) {
        getSortedItems().fastForEach { item ->
            action(item.index, item.key)
        }
    }
}

internal class PageableLazyStaggeredGridLayoutInfo(
    private val state: LazyStaggeredGridState
) : PageableLayoutInfo {
    private fun getSortedItems(): List<LazyStaggeredGridItemInfo> {
        val items = state.layoutInfo.visibleItemsInfo
        items.fastForEachIndexed { index, item ->
            if (index < items.lastIndex && item.index > items[index + 1].index) {
                return items.sortedBy { it.index }
            }
        }
        return items
    }

    override val visibleItemCount: Int
        get() = state.layoutInfo.visibleItemsInfo.size

    override fun getVisibleItemKey(index: Int): Any =
        getSortedItems()[index].key

    override fun getVisibleItemIndex(index: Int): Int =
        getSortedItems()[index].index

    override fun forEachVisibleItem(action: (index: Int, key: Any) -> Unit) {
        val items = getSortedItems()
        for (i in items.indices) {
            val item = items[i]
            action(item.index, item.key)
        }
    }
}

internal interface PageableLayoutInfo {
    val visibleItemCount: Int
    fun getVisibleItemKey(index: Int): Any
    fun getVisibleItemIndex(index: Int): Int
    fun forEachVisibleItem(action: (index: Int, key: Any) -> Unit)
}
