package com.nxoim.evolpagink.core

import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet


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

    // will also deduplicate
    override fun set(key: Key, page: List<PageItem>) {
        var stolenByPage: MutableScatterMap<Key, MutableScatterSet<PageItem>>? = null

        pageCache[key]?.forEach { oldItem ->
            if (itemToPageKeyCache[oldItem] == key && oldItem !in page) {
                itemToPageKeyCache.remove(oldItem)
            }
        }

        page.forEach { item ->
            val currentOwner = itemToPageKeyCache[item]
            if (currentOwner != null && currentOwner != key) {
                val map = stolenByPage
                    ?: MutableScatterMap<Key, MutableScatterSet<PageItem>>().also {
                        stolenByPage = it
                    }
                map
                    .getOrPut(currentOwner) { MutableScatterSet() }
                    .add(item)
            }
            itemToPageKeyCache[item] = key
        }

        stolenByPage?.forEach { ownerKey, stolen ->
            val existing = pageCache[ownerKey] ?: return@forEach
            val trimmed = existing.filter { it !in stolen }
            if (trimmed.isEmpty()) pageCache.remove(ownerKey) else pageCache[ownerKey] = trimmed
        }

        pageCache[key] = page
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