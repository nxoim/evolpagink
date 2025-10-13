package com.nxoim.evolpagink.core

import androidx.collection.MutableScatterMap


internal interface PageStorage<Key : Any, PageItem> {
    operator fun get(key: Key): List<PageItem>?
    operator fun set(key: Key, page: List<PageItem>)

    val all: Map<Key, List<PageItem>>
    fun remove(key: Key)

    fun getPageKeyForItem(item: PageItem): Key?

    fun clear() { all.keys.forEach(::remove) }

    operator fun contains(key: Key): Boolean

    fun putAll(pages: Map<Key, List<PageItem>>)
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

    // create non backed snapshot. read .asMap() docs
    override val all: Map<Key, List<PageItem>> get() = MutableScatterMap<Key, List<PageItem>>(pageCache.capacity)
        .apply { putAll(pageCache) }
        .asMap()

    override fun remove(key: Key) {
        try {
            pageCache.remove(key)?.forEach { item ->
                itemToPageKeyCache.remove(item)
            }
        } catch (_: NullPointerException) {
            // swallow "Parameter specified as non-null is null"
        }
    }

    override fun getPageKeyForItem(item: PageItem): Key? = itemToPageKeyCache[item]

    override fun contains(key: Key): Boolean = pageCache.contains(key)

    override fun putAll(pages: Map<Key, List<PageItem>>) {
        pageCache.putAll(pages)
        pages.forEach { (key, page) ->
            page.forEach { item -> itemToPageKeyCache[item] = key }
        }
    }
}