@file:OptIn(InternalPageableApi::class)

package com.nxoim.evolpagink.compose

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.State
import com.nxoim.evolpagink.core.InternalPageableApi
import com.nxoim.evolpagink.core.Pageable
import kotlin.jvm.JvmInline

/**
 * Compose representation of [Pageable].
 */
class PageableComposeState<T> internal constructor(
    val items: State<List<T>>,
    keyer: PageItemKeyProvider<T>
) : PageItemKeyProvider<T> by keyer

/**
 * Compose representation of [Pageable] specific to standard pager UI components.
 *
 * @see HorizontalPager
 * @see VerticalPager
 */
class PageablePagerComposeState<T> internal constructor(
    val items: State<List<T>>,
    val key: (index: Int) -> Any,
    val pagerState: PagerState
)

// exists to centralize compose list key management
// between paging and displaying lists
@InternalPageableApi
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
