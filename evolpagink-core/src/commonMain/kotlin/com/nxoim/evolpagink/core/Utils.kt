package com.nxoim.evolpagink.core

internal const val defaultAssumedCacheSize = 20

@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    message = "This API is for internal or test use only.",
    level = RequiresOptIn.Level.ERROR
)
annotation class InternalPageableApi