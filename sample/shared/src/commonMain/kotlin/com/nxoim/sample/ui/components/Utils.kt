@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.nxoim.sample.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Unified context for shared element transitions.
 * Wraps SharedTransitionScope and AnimatedVisibilityScope for use with CompositionLocal.
 */
@Immutable
class SharedTransitionContext(
    val sharedScope: SharedTransitionScope,
    val animatedScope: AnimatedVisibilityScope
) : SharedTransitionScope by sharedScope, AnimatedVisibilityScope by animatedScope

val LocalSharedTransitionContext =
    staticCompositionLocalOf<SharedTransitionContext?> { null }

/**
 * Provides the shared transition context if present. Does nothing if not.
 * Also does nothing if key is null, simplifying shared element declaration.
 * ```kotlin
 * .withLocalTransitionContext(key) {
 *     Modifier.sharedBounds(
 *         rememberSharedContentState(it), // use key
 *         animatedVisibilityScope = this // use the SharedTransitionContext
 * }
 * ```
 */
@Composable
inline fun <T : Any> Modifier.withLocalTransitionContext(
    key: T?,
    block: SharedTransitionContext.(key: T) -> Modifier
): Modifier {
    val context = LocalSharedTransitionContext.current
    return then(
        if (context != null && key != null)
            context.block(key)
        else
            Modifier
    )
}

/**
 * Provides SharedTransitionContext via CompositionLocal, then executes the block.
 * Must be called where both SharedTransitionScope and AnimatedVisibilityScope are in context.
 */
context(sharedScope: SharedTransitionScope, animatedScope: AnimatedVisibilityScope)
@Composable
inline fun ProvideSharedTransitionContext(
    crossinline block: @Composable SharedTransitionContext.() -> Unit
) {
    val context = remember(sharedScope, animatedScope) {
        SharedTransitionContext(sharedScope, animatedScope)
    }

    CompositionLocalProvider(
        LocalSharedTransitionContext provides context
    ) {
        context.block()
    }
}

@Composable
fun PanelContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier.widthIn(max = 520.dp),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content
        )
    }
}

@Composable
fun panelItemColors() = ListItemDefaults.segmentedColors(
    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
)

@Composable
fun PanelHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.titleMedium)

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null)
            }

            action()
        }
    }
}

@Composable
fun EmbeddedPanelHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit
) {
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            action()
        }
    }
}

@Composable
fun SelectableDropdownMenuItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    uncheckedLeadingIcon: (@Composable () -> Unit)? = null
) {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    DropdownMenuItem(
        checked = checked,
        text = { Text(text) },
        onCheckedChange = onCheckedChange,
        shapes = MenuDefaults.itemShapes(),
        colors = MenuDefaults.selectableItemColors(),
        leadingIcon = {
            AnimatedVisibility(checked) {
                Icon(Icons.Default.Check, contentDescription = null)
            }
            if (!checked) uncheckedLeadingIcon?.invoke()
        }
    )
}

@Composable
fun ClearDropdownMenuGroup(onClick: () -> Unit) {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    DropdownMenuGroup(MenuDefaults.groupShapes()) {
        DropdownMenuItem(
            text = { Text("Clear") },
            onClick = onClick,
            leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) }
        )
    }
}

@Composable
fun Modifier.surfaceFadeGradient(fadeDown: Boolean): Modifier {
    val surface = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)

    val colors = remember(fadeDown, surface) {
        if (fadeDown) {
            listOf(surface, Color.Transparent)
        } else {
            listOf(Color.Transparent, surface)
        }
    }
    return background(
        brush = Brush.verticalGradient(colors),
        shape = RoundedCornerShape(16.dp)
    )
}