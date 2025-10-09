package com.nxoim.evolpagink

import androidx.collection.MutableScatterMap
import kotlinx.coroutines.channels.Channel

internal interface PageStorage<Key : Any, PageItem> {
    operator fun get(key: Key): List<PageItem>?
    operator fun set(key: Key, page: List<PageItem>)

    val all: Map<Key, List<PageItem>>
    fun remove(key: Key)

    fun getPageKeyForItem(item: PageItem): Key?

    fun clear() { all.keys.forEach(::remove) }
}

internal class TransactionalPageStorage<Key : Any, PageItem>(
    private val pageStorage: PageStorage<Key, PageItem>
) {
    val transactionEvents = Channel<Map<Key, List<PageItem>>>(Channel.CONFLATED)

    val all: Map<Key, List<PageItem>> get() = pageStorage.all
    operator fun get(key: Key): List<PageItem>? = pageStorage[key]
    fun getPageKeyForItem(item: PageItem): Key? = pageStorage.getPageKeyForItem(item)

    inline  fun <T> transaction(block: PageStorage<Key, PageItem>.() -> T): T {
        val result = block(pageStorage)
        transactionEvents.trySend(pageStorage.all)
        return result
    }
}

////////////////////////////////////////////////////////////////////////////////////////////

internal class ScatterMapPageStorage<Key : Any, PageItem> : PageStorage<Key, PageItem> {
    private val pageCache = MutableScatterMap<Key, List<PageItem>>(defaultAssumedCacheSize)
    private val itemToPageKeyCache = MutableScatterMap<PageItem, Key>(defaultAssumedCacheSize)

    override fun get(key: Key): List<PageItem>? = pageCache[key]

    override fun set(key: Key, page: List<PageItem>) {
        pageCache[key] = page
        page.forEach { item -> itemToPageKeyCache[item] = key }
    }

    override val all: Map<Key, List<PageItem>> = pageCache.asMap()

    override fun remove(key: Key) {
        pageCache.remove(key)?.forEach { item ->
            itemToPageKeyCache.remove(item)
        }
    }

    override fun getPageKeyForItem(item: PageItem): Key? = itemToPageKeyCache[item]
}