package com.nxoim.evolpagink.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val defaultAssumedCacheSize = 20

@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    message = "This API is for internal or test use only.",
    level = RequiresOptIn.Level.ERROR
)
annotation class InternalPageableApi

internal val singleEmissionStateFlowOfUnit = MutableStateFlow(Unit).asStateFlow()