package com.nxoim.evolpagink.compose

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.State
import kotlin.jvm.JvmInline

class PageableComposeState<T> internal constructor(
    private val _items: State<List<T>>,
    keyer: PageItemKeyProvider<T>
) : PageItemKeyProvider<T> by keyer {
    val items get() = _items.value
}

class PageablePagerComposeState<T> internal constructor(
    private val _items: State<List<T>>,
    val key: (index: Int) -> Any,
    val pagerState: PagerState
) {
    val items get() = _items.value
}

// exists to centralize compose list key management
// between paging and displaying lists
sealed interface PageItemKeyProvider<T> {
    fun key(item: T): Any
    fun key(index: Int, item: T): Any
}

@JvmInline
internal value class PageItemKeyProviderImpl<PageItem>(
    private val keyProvider: (PageItem) -> Any,
) : PageItemKeyProvider<PageItem> {
    override fun key(item: PageItem): Any = keyProvider(item)
    override fun key(index: Int, item: PageItem): Any = keyProvider(item)
}
