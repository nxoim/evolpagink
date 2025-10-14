package com.nxoim.evolpagink.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class ObservablePageStorage<Key : Any, PageItem>(
    private val storage: PageStorage<Key, PageItem>,
    private val onPageEvent: ((PageEvent<Key>) -> Unit)? = null
) {
    private val _pageSnapshots = MutableSharedFlow<Map<Key, List<PageItem>>>(replay = 1)
    val pageSnapshots = _pageSnapshots.asSharedFlow()

    var pagesSnapshot: Map<Key, List<PageItem>> = storage.all
        private set

    operator fun get(key: Key): List<PageItem>? = storage[key]
    fun contains(key: Key): Boolean = storage.contains(key)

    // bypass snapshot
    fun getPageKeyForItem(item: PageItem): Key? = storage.getPageKeyForItem(item)

    suspend fun updatePage(key: Key, items: List<PageItem>, emitSnapshot: Boolean) {
        storage[key] = items
        if (emitSnapshot) emitSnapshot()
        onPageEvent?.invoke(PageEvent.Loaded(key))
    }

    suspend fun removePage(key: Key, emitSnapshot: Boolean) {
        storage.remove(key)
        if (emitSnapshot) emitSnapshot()
        onPageEvent?.invoke(PageEvent.Unloaded(key))
    }

    suspend fun clear(emitSnapshot: Boolean) {
        val snap = storage.all
        storage.clear()
        if (emitSnapshot) emitSnapshot()
        snap.keys.forEach { onPageEvent?.invoke(PageEvent.Unloaded(it)) }
    }

    private suspend fun emitSnapshot() {
        val snap = storage.all
        pagesSnapshot = snap
        _pageSnapshots.emit(snap)
    }
}
