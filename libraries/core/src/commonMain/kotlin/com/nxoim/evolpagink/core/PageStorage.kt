package com.nxoim.evolpagink.core

import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet


internal interface PageStorage<Key : Any, PageItem> {
    fun replacePage(key: Key, items: List<PageItem>)
    fun removePage(key: Key)

    val snapshot: Map<Key, List<PageItem>>
    fun getPageKeyForItem(item: PageItem): Key?

    fun clear()
}

internal class ScatterMapPageStorage<Key : Any, PageItem>(
    private val pageItemKey: (PageItem) -> Any
) : PageStorage<Key, PageItem> {
    private val pagesByKey = MutableScatterMap<Key, List<PageItem>>(defaultAssumedCacheSize)
    private val ownerByItemId = MutableScatterMap<Any, Key>(defaultAssumedCacheSize)

    override val snapshot: Map<Key, List<PageItem>>
        get() = MutableScatterMap<Key, List<PageItem>>(pagesByKey.capacity)
            .apply { putAll(pagesByKey) }
            .asMap()

    override fun replacePage(key: Key, items: List<PageItem>) {
        var displacedPageKeys: MutableScatterSet<Key>? = null
        val incomingIds = MutableScatterSet<Any>(items.size)
        val deduplicatedItems = ArrayList<PageItem>(items.size)

        for (index in items.indices) {
            val item = items[index]

            val itemId = pageItemKey(item)
            if (!incomingIds.add(itemId)) continue
            deduplicatedItems.add(item)

            val currentOwner = ownerByItemId[itemId]
            if (currentOwner != null && currentOwner != key) {
                val keys = displacedPageKeys
                    ?: MutableScatterSet<Key>().also { displacedPageKeys = it }
                keys.add(currentOwner)
            }
            ownerByItemId[itemId] = key
        }

        pagesByKey[key]?.forEach { oldItem ->
            val oldItemId = pageItemKey(oldItem)
            // if it's not in the new page, and no other page stole it in the meantime - drop it
            if (oldItemId !in incomingIds && ownerByItemId[oldItemId] == key) {
                ownerByItemId.remove(oldItemId)
            }
        }

        displacedPageKeys?.forEach { ownerKey ->
            val existing = pagesByKey[ownerKey] ?: return@forEach
            val remaining = ArrayList<PageItem>(existing.size)

            for (index in existing.indices) {
                val item = existing[index]
                if (ownerByItemId[pageItemKey(item)] == ownerKey) {
                    remaining.add(item)
                }
            }

            if (remaining.isEmpty()) {
                pagesByKey.remove(ownerKey)
            } else {
                pagesByKey[ownerKey] = remaining
            }
        }

        pagesByKey[key] = deduplicatedItems
    }

    override fun getPageKeyForItem(item: PageItem): Key? =
        ownerByItemId[pageItemKey(item)]

    override fun removePage(key: Key) {
        val list = pagesByKey.remove(key) ?: return

        for (index in list.indices) {
            val item = list[index]

            val itemId = pageItemKey(item)
            if (ownerByItemId[itemId] == key) {
                ownerByItemId.remove(itemId)
            }
        }
    }

    override fun clear() {
        pagesByKey.clear()
        ownerByItemId.clear()
    }
}
