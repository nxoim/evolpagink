package com.nxoim.sample.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.window.core.layout.WindowSizeClass

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TwoPane(
    modifier: Modifier = Modifier,
    rightPane: @Composable (isWide: Boolean) -> Unit,
    leftPane: @Composable (isWide: Boolean) -> Unit,
) {
    val windowClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isWide =
        windowClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    Row(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            isWide,
            transitionSpec = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right) togetherWith
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left)
            }
        ) {
            if (it) Box(modifier = Modifier.fillMaxWidth(0.35f).zIndex(1f)) {
                leftPane(isWide)
            } else {
                // compensation for internal container resize animation.
                // without this the content will be clipped on the
                // vertical axis during animation
                Spacer(Modifier.fillMaxHeight())
            }
        }

        Box(Modifier.weight(1f)) {
            rightPane(isWide)
        }
    }
}