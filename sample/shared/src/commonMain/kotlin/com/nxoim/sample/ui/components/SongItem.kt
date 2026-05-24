package com.nxoim.sample.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nxoim.sample.ui.Track
import com.nxoim.sample.ui.theme.SampleTheme
import kotlin.time.Duration.Companion.seconds

@Composable
fun SongItem(
    track: Track,
    modifier: Modifier = Modifier,
    shape: Shape = SongItemShape.Middle
) {
    val duration = remember(track.durationSeconds) { track.formattedDuration }

    ListItem(
        modifier = modifier.clip(shape),
        headlineContent = { Text(track.title, maxLines = 1) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        leadingContent = {
            Spacer(
                Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.secondary)
            )
        },
        supportingContent = { Text("${track.artist} | ${track.album}", maxLines = 1) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(duration)

//                IconButton(onClick = { }) {
//                    Icon(Icons.Default.MoreVert, contentDescription = null)
//                }
            }
        }
    )
}

@Composable
fun SongItemPlaceholder(
    modifier: Modifier = Modifier,
    shape: Shape = SongItemShape.Middle
) {
    ListItem(
        modifier = modifier.clip(shape),
        headlineContent = {
            Box(
                Modifier
                    .fillMaxWidth(0.5f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        leadingContent = {
            Spacer(
                Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
            )
        },
        supportingContent = {
            Box(
                Modifier
                    .fillMaxWidth(0.35f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .width(32.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                )
//                IconButton(onClick = {}, enabled = false) {
//                    Icon(
//                        Icons.Default.MoreVert,
//                        contentDescription = null,
//                        tint = LocalContentColor.current.copy(alpha = 0.12f)
//                    )
//                }
            }
        }
    )
}

private val Track.formattedDuration
    get() = durationSeconds
        .seconds
        .toComponents { minutes, seconds, _ -> "$minutes:$seconds" }

object SongItemShape {
    val First = RoundedCornerShape(18.dp, 18.dp, 8.dp, 8.dp)
    val Middle = RoundedCornerShape(8.dp)
    val Last = RoundedCornerShape(8.dp, 8.dp, 18.dp, 18.dp)
}

@Preview
@Composable
private fun SongItemPreview() {
    SampleTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SongItem(
                track = Track(
                    id = "preview-1",
                    title = "Midnight City",
                    artist = "M83",
                    album = "Hurry Up, We're Dreaming",
                    genreId = "electronic",
                    durationSeconds = 243,
                    year = 2011,
                    playCount = 42,
                    isExplicit = false
                )
            )
        }
    }
}

@Preview
@Composable
private fun SongItemFirstShapePreview() {
    SampleTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SongItem(
                track = Track(
                    id = "preview-2",
                    title = "Blinding Lights",
                    artist = "The Weeknd",
                    album = "After Hours",
                    genreId = "pop",
                    durationSeconds = 200,
                    year = 2019,
                    playCount = 150,
                    isExplicit = false
                ),
                shape = SongItemShape.First
            )
        }
    }
}

@Preview
@Composable
private fun SongItemPlaceholderPreview() {
    SampleTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SongItemPlaceholder()
        }
    }
}
