package com.nxoim.evolpagink

import androidx.collection.MutableScatterMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlin.jvm.JvmInline

@JvmInline
internal value class PageJobTracker<Key : Any>(
    private val runningJobs: MutableScatterMap<Key, Job> = MutableScatterMap(
        defaultAssumedCacheSize
    )
) {
    val active get() = runningJobs.asMap().keys

    fun launchIfIdle(
        key: Key,
        scope: CoroutineScope,
        block: suspend () -> Unit
    ): Job = runningJobs.getOrPut(key) {
        scope.launch {
            try {
                block()
            } finally {
                runningJobs.remove(key)
            }
        }
    }


    suspend fun cancelAndJoin(key: Key) {
        runningJobs.remove(key)?.cancelAndJoin()
    }

    fun isActive(key: Key): Boolean = runningJobs[key]?.isActive == true
    suspend fun clear() {
        runningJobs.forEach { key, value -> value.cancelAndJoin() }
        runningJobs.clear()
    }
}