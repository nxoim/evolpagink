package com.nxoim.evolpagink.compose

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import kotlin.jvm.JvmInline

@JvmInline
internal value class PageableLazyListItemInfo(private val info: LazyListItemInfo) :
    PageableItemLayoutInfo {
    override val index: Int get() = info.index
    override val key: Any get() = info.key
}

@JvmInline
internal value class PageableLazyGridItemInfo(private val info: LazyGridItemInfo) :
    PageableItemLayoutInfo {
    override val index: Int get() = info.index
    override val key: Any get() = info.key
}

@JvmInline
internal value class PageableLazyStaggeredGridItemInfo(private val info: LazyStaggeredGridItemInfo) :
    PageableItemLayoutInfo {
    override val index: Int get() = info.index
    override val key: Any get() = info.key
}

@JvmInline
internal value class PageableLazyListLayoutInfo(
    private val state: LazyListState
) : PageableLayoutInfo {
    override val visibleItemsInfo: List<PageableItemLayoutInfo>
        get() = state.layoutInfo.visibleItemsInfo.map(::PageableLazyListItemInfo)

    override val totalItemsCount: Int
        get() = state.layoutInfo.totalItemsCount

    override val lastScrolledForward: Boolean
        get() = state.lastScrolledForward
}

@JvmInline
internal value class PageableLazyGridLayoutInfo(private val state: LazyGridState) :
    PageableLayoutInfo {
    override val visibleItemsInfo: List<PageableItemLayoutInfo>
        // Grids can have out of order visible com.nxoim.TODORENAMElibrary.items
        get() = state.layoutInfo.visibleItemsInfo.sortedBy { it.index }
            .map(::PageableLazyGridItemInfo)

    override val totalItemsCount: Int
        get() = state.layoutInfo.totalItemsCount

    override val lastScrolledForward: Boolean
        get() = state.lastScrolledForward
}

@JvmInline
internal value class PageableLazyStaggeredGridLayoutInfo(
    private val state: LazyStaggeredGridState
) : PageableLayoutInfo {
    override val visibleItemsInfo: List<PageableItemLayoutInfo>
        // Grids can have out of order visible items
        get() = state.layoutInfo.visibleItemsInfo.sortedBy { it.index }
            .map(::PageableLazyStaggeredGridItemInfo)

    override val totalItemsCount: Int
        get() = state.layoutInfo.totalItemsCount

    override val lastScrolledForward: Boolean
        get() = state.lastScrolledForward
}

internal interface PageableItemLayoutInfo {
    val index: Int
    val key: Any
}

internal interface PageableLayoutInfo {
    val visibleItemsInfo: List<PageableItemLayoutInfo>
    val totalItemsCount: Int
    val lastScrolledForward: Boolean
}
