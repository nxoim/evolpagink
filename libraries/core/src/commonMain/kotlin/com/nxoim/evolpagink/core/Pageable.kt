package com.nxoim.evolpagink.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Creates an empty-context [Pageable].
 *
 * @param coroutineScope Scope that will be used for all the work.
 * @param onPage Factory of page flows. A page that is empty is treated as valid, a page that is null is treated as loading.
 * @param strategy Strategy for compositing a list of pages to load.
 * @param initialItems Items that will be displayed before any page data is collected.
 * @param pageItemKey Key factory for tracking items in storage and UI.
 */
@OptIn(InternalPageableApi::class)
fun <Key : Any, PageItem : Any> pageable(
    coroutineScope: CoroutineScope,
    onPage: Unit.(key: Key) -> Flow<List<PageItem>?>,
    strategy: PageFetchStrategy<Key, PageItem, Unit>,
    initialItems: List<PageItem> = emptyList(),
    pageItemKey: (PageItem) -> Any = { it }
) = pageable(
    coroutineScope = coroutineScope,
    context = singleEmissionStateFlowOfUnit,
    onPage = onPage,
    strategy = strategy,
    initialItems = initialItems,
    pageItemKey = pageItemKey
)

/**
 * Creates a [Pageable] that can react to changes in the provided context.
 *
 * When the context emits a new value, any currently cached pages are invalidated and re-fetched.
 *
 * @param coroutineScope Scope that will be used for all the work.
 * @param context Context for page updates and strategy calculations. Context changes trigger a reload.
 * @param onPage Factory of page flows. A page that is empty is treated as valid, a page that is null is treated as loading.
 * @param strategy Strategy for compositing a list of pages to load.
 * @param initialItems Items that will be displayed before any page data is collected.
 * @param pageItemKey Key factory for tracking items in storage and UI.
 */
@OptIn(InternalPageableApi::class, ExperimentalAtomicApi::class, ExperimentalCoroutinesApi::class)
fun <Key : Any, PageItem : Any, Context> pageable(
    coroutineScope: CoroutineScope,
    context: StateFlow<Context>,
    onPage: Context.(key: Key) -> Flow<List<PageItem>?>,
    strategy: PageFetchStrategy<Key, PageItem, Context>,
    initialItems: List<PageItem> = emptyList(),
    pageItemKey: (PageItem) -> Any = { it }
): Pageable<Key, PageItem> {
    val paginator = Paginator(
        coroutineScope,
        context,
        onPage,
        strategy,
        pageItemKey
    )

    return object : Pageable<Key, PageItem> {
        override val items =  paginator
            .collectPagesAndFlattenIntoItemList()
            .stateIn(coroutineScope, WhileSubscribed(), initialItems)
        override val isFetchingPrevious = paginator.isFetchingPrevious
        override val isFetchingNext = paginator.isFetchingNext

        override suspend fun getPageKeyForItem(
            item: PageItem
        ): Key? = paginator.getPageKeyForItem(item)

        override suspend fun jumpTo(
            page: Key
        ): List<PageItem>? = paginator.preloadAndActivate(page)

        override suspend fun onVisibilityEvent(event: PageDisplayingEvent<Key>) {
            paginator.updatePagesToCache(event)
        }

        override fun pageItemKey(item: PageItem): Any = pageItemKey(item)
    }
}

/**
 * Represents a paginated data source with flattened items and fetching state.
 *
 * The item list automatically updates as pages are fetched based on the configured strategy.
 */
interface Pageable<Key : Any, PageItem> {
    val items: StateFlow<List<PageItem>>
    val isFetchingPrevious: StateFlow<Boolean>
    val isFetchingNext: StateFlow<Boolean>

    /**
     * Maps an item back to its page key. Returns null if the item isn't associated with a page.
     */
    suspend fun getPageKeyForItem(item: PageItem): Key?

    /**
     *  Loads and activates the specified page, returning its items. Subsequent fetches will be centered around this page.
     */
    suspend fun jumpTo(page: Key): List<PageItem>?

    /**
     * Reports visible pages to the paginator, triggers the declared
     * [PageFetchStrategy] to calculate a list of pages that are
     * expected to be loaded by the paginator.
     */
    suspend fun onVisibilityEvent(event: PageDisplayingEvent<Key>)

    /**
     * Provides key for an item from any loaded page. See [pageable].
     */
    fun pageItemKey(item: PageItem): Any
}