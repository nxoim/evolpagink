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

    override val all: Map<Key, List<PageItem>> get() = buildMap {
        putAll(pageCache.asMap())
    }

    override fun remove(key: Key) {
        pageCache.remove(key)?.forEach { item ->
            itemToPageKeyCache.remove(item)
        }
    }

    override fun getPageKeyForItem(item: PageItem): Key? = itemToPageKeyCache[item]

    override fun contains(key: Key): Boolean = pageCache.contains(key)
}