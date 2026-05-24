@file:OptIn(ExperimentalMaterial3Api::class)

package com.nxoim.sample.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarColors
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nxoim.sample.ui.theme.SampleTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    state: SearchController,
    modifier: Modifier = Modifier,
    colors: SearchBarColors = SearchBarDefaults.colors(),
    shadowElevation: Dp = SearchBarDefaults.ShadowElevation
) {
    var current by remember { mutableStateOf("") }

    Surface(
        modifier = modifier,
        color = colors.containerColor,
        shadowElevation = shadowElevation,
        shape = CircleShape
    ) {
        SearchBarDefaults.InputField(
            query = current,
            onQueryChange = {
                state.onQuery(it)
                current = it
            },
            onSearch = { },
            expanded = false,
            onExpandedChange = { },
            placeholder = { Text("Search") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                AnimatedContent(current.isEmpty()) { empty ->
                    if (!empty) {
                        IconButton(onClick = { current = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                }
            }
        )
    }
}

interface SearchController {
    fun onQuery(new: String)
}

private object NoOpSearchController : SearchController {
    override fun onQuery(new: String) {}
}

@Preview
@Composable
private fun SearchBarPreview() {
    SampleTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SearchBar(state = NoOpSearchController)
        }
    }
}

@Preview
@Composable
private fun SearchBarWithQueryPreview() {
    SampleTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SearchBar(state = NoOpSearchController)
        }
    }
}
