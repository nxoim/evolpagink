package com.nxoim.evolpagink.core

import kotlin.jvm.JvmInline

/**
 * Represents the loading state of a page identified by [key].
 */
sealed interface PageEvent<Key> {
    val key: Key

    @JvmInline
    value class Loading<Key>(override val key: Key) : PageEvent<Key>

    @JvmInline
    value class Loaded<Key>(override val key: Key) : PageEvent<Key>

    @JvmInline
    value class Unloaded<Key>(override val key: Key) : PageEvent<Key>
}