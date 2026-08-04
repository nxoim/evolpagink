package com.nxoim.evolpagink.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ObservablePageStorage<Key : Any, PageItem>(
    private val storage: PageStorage<Key, PageItem>,
) {
    private val mutex = Mutex()
    private val _pageSnapshots = MutableSharedFlow<Map<Key, List<PageItem>>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val pageSnapshots = _pageSnapshots.asSharedFlow()

    suspend fun getSnapshot() = mutex.withLock { storage.snapshot }

    suspend fun getPageKeyForItem(item: PageItem): Key? =
        mutex.withLock { storage.getPageKeyForItem(item) }

    suspend fun updatePage(key: Key, items: List<PageItem>, emitSnapshot: Boolean) =
        mutex.withLock {
            storage.replacePage(key, items)
            if (emitSnapshot) emitSnapshot()
        }

    suspend fun removePage(key: Key, emitSnapshot: Boolean) = mutex.withLock {
        storage.removePage(key)
        if (emitSnapshot) emitSnapshot()
    }

    suspend fun clear(emitSnapshot: Boolean) = mutex.withLock {
        storage.clear()
        if (emitSnapshot) emitSnapshot()
    }

    private suspend fun emitSnapshot() {
        _pageSnapshots.tryEmit(storage.snapshot)
    }
}
