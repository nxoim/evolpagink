package com.nxoim.evolpagink.core

import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet


internal interface PageStorage<Key : Any, PageItem> {
    operator fun set(key: Key, page: List<PageItem>)

    val all: Map<Key, List<PageItem>>
    fun remove(key: Key)

    fun getPageKeyForItem(item: PageItem): Key?

    fun clear() { all.keys.forEach(::remove) }
}

////////////////////////////////////////////////////////////////////////////////////////////

internal class ScatterMapPageStorage<Key : Any, PageItem>(
    private val pageItemKey: (PageItem) -> Any
) : PageStorage<Key, PageItem> {
    private val pageCache = MutableScatterMap<Key, List<PageItem>>(defaultAssumedCacheSize)
    private val itemIdToPageKeyCache = MutableScatterMap<Any, Key>(defaultAssumedCacheSize)

    override val all: Map<Key, List<PageItem>>
        get() = MutableScatterMap<Key, List<PageItem>>(pageCache.capacity)
            .apply { putAll(pageCache) }
            .asMap()

    override fun set(key: Key, page: List<PageItem>) {
        var stolenByPage: MutableScatterMap<Key, MutableScatterSet<Any>>? = null
        val incomingIds = MutableScatterSet<Any>(page.size)

        page.forEach { item ->
            val id = pageItemKey(item)
            incomingIds.add(id)

            val currentOwner = itemIdToPageKeyCache[id]
            if (currentOwner != null && currentOwner != key) {
                val map = stolenByPage
                    ?: MutableScatterMap<Key, MutableScatterSet<Any>>().also { stolenByPage = it }

                map.getOrPut(currentOwner) { MutableScatterSet() }.add(id)
            }
            itemIdToPageKeyCache[id] = key
        }

        pageCache[key]?.forEach { oldItem ->
            val oldId = pageItemKey(oldItem)
            // if it's not in the new page, and no other page stole it in the meantime - drop it
            if (oldId !in incomingIds && itemIdToPageKeyCache[oldId] == key) {
                itemIdToPageKeyCache.remove(oldId)
            }
        }

        stolenByPage?.forEach { ownerKey, stolenIds ->
            val existing = pageCache[ownerKey] ?: return@forEach
            val trimmed = ArrayList<PageItem>(existing.size)

            existing.forEach { item ->
                if (pageItemKey(item) !in stolenIds) {
                    trimmed.add(item)
                }
            }

            if (trimmed.isEmpty()) {
                pageCache.remove(ownerKey)
            } else {
                pageCache[ownerKey] = trimmed
            }
        }

        pageCache[key] = page
    }

    override fun getPageKeyForItem(item: PageItem): Key? =
        itemIdToPageKeyCache[pageItemKey(item)]

    override fun remove(key: Key) {
        pageCache.remove(key)?.forEach { item ->
            itemIdToPageKeyCache.remove(pageItemKey(item))
        }
    }
}