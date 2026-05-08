package com.nxoim.evolpagink.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ObservablePageStorage<Key : Any, PageItem>(
    private val storage: PageStorage<Key, PageItem>,
    private val onPageEvent: ((PageEvent<Key>) -> Unit)? = null
) {
    private val mutex = Mutex()
    private val _pageSnapshots = MutableSharedFlow<Map<Key, List<PageItem>>>(replay = 1)
    val pageSnapshots = _pageSnapshots.asSharedFlow()

    suspend fun getSnapshot() = mutex.withLock { storage.all }

    suspend fun getPageKeyForItem(item: PageItem): Key? =
        mutex.withLock { storage.getPageKeyForItem(item) }

    suspend fun updatePage(key: Key, items: List<PageItem>, emitSnapshot: Boolean) =
        mutex.withLock {
            storage[key] = items
            if (emitSnapshot) emitSnapshot()
            onPageEvent?.invoke(PageEvent.Loaded(key))
        }

    suspend fun removePage(key: Key, emitSnapshot: Boolean) = mutex.withLock {
        storage.remove(key)
        if (emitSnapshot) emitSnapshot()
        onPageEvent?.invoke(PageEvent.Unloaded(key))
    }

    suspend fun clear(emitSnapshot: Boolean) = mutex.withLock {
        val snap = storage.all
        storage.clear()
        if (emitSnapshot) emitSnapshot()
        snap.keys.forEach { onPageEvent?.invoke(PageEvent.Unloaded(it)) }
    }

    private suspend fun emitSnapshot() {
        _pageSnapshots.emit(storage.all)
    }
}
