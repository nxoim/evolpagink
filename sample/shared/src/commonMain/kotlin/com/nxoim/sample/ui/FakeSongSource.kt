@file:OptIn(ExperimentalUuidApi::class)

package com.nxoim.sample.ui

import com.nxoim.sample.ui.SongSource.Companion.genres
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class FakeSongSource(
    val emitPlaceholders: Boolean = false
) : SongSource {
    private val db = MutableStateFlow(generateFakeTracks(500))

    override fun getSongs(amount: IntRange, context: SongRetrievalContext): Flow<List<ItemData>> {
        var isFirstEmission = true
        return db
            .map { tracks ->
                tracks.filter(context).sort(context.sorting).let { sorted ->
                    if (sorted.isEmpty()) emptyList()
                    else sorted.subList(
                        amount.first.coerceIn(0, sorted.size),
                        amount.last.coerceIn(0, sorted.size)
                    )
                }
            }
            .transform { tracks ->
                if (emitPlaceholders && isFirstEmission) {
                    isFirstEmission = false
                    val result = List(tracks.size) { index ->
                        ItemData.Placeholder(id = Uuid.random().toHexString(), amount.first + index)
                    }
                        .toMutableList<ItemData>()

                    emit(result.toList())
                    tracks.forEachIndexed { index, track ->
                        delay(fakeRandom.nextLong(10, 300).milliseconds)
                        result[index] = ItemData.Loaded(track, index = amount.first + index)
                        emit(result.toList())
                    }
                } else {
                    emit(tracks.mapIndexed { index, track -> ItemData.Loaded(track, amount.first + index) })
                }
            }
    }

    override fun songCount(context: SongRetrievalContext): Flow<Int> =
        db.map { tracks -> tracks.filter(context).size }

    override fun getSongAtIndex(index: Int, context: SongRetrievalContext): Flow<Track?> =
        db.map { tracks -> tracks.filter(context).sort(context.sorting).getOrNull(index) }

    override fun getSections(context: SongRetrievalContext): Flow<List<SongSource.SectionInfo>> =
        db.map { tracks ->
            val (sections, _) = tracks
                .filter(context)
                .sort(context.sorting)
                .foldIndexed(
                    mutableListOf<SongSource.SectionInfo>() to mutableSetOf<String>()
                ) { index, (list, seen), track ->
                    val label = sectionLabel(track, index, context.sorting.by)
                    if (seen.add(label)) list += SongSource.SectionInfo(label, index)
                    list to seen
                }
            sections
        }

    override fun remove(item: Track) {
        db.update { tracks -> tracks.filter { it.id != item.id } }
    }

    private fun sectionLabel(track: Track, index: Int, by: SongRetrievalContext.Sorting.By): String =
        when (by) {
            SongRetrievalContext.Sorting.By.TITLE -> (index / titleSectionSize * titleSectionSize).toString()
            SongRetrievalContext.Sorting.By.ARTIST -> track.artist.first().uppercaseChar().toString()
            SongRetrievalContext.Sorting.By.ALBUM -> track.album.first().uppercaseChar().toString()
            SongRetrievalContext.Sorting.By.YEAR -> track.year.toString()
            SongRetrievalContext.Sorting.By.PLAY_COUNT -> "${track.playCount / 10000 * 10}k"
            SongRetrievalContext.Sorting.By.DURATION -> "${track.durationSeconds / 60}m"
        }

    private fun List<Track>.filter(context: SongRetrievalContext): List<Track> =
        filter { track ->
            val q = context.searchQuery.lowercase()
            val matchesQuery = q.isEmpty() || track.title.lowercase().contains(q)
                    || track.artist.lowercase().contains(q)
                    || track.album.lowercase().contains(q)
            val matchesGenre = context.filters.genreId?.let { it == track.genreId } ?: true
            val matchesYear = context.filters.yearRange?.let { track.year in it } ?: true
            val matchesExplicit = context.filters.isExplicit?.let { it == track.isExplicit } ?: true
            val matchesArtist =
                context.filters.artist?.let { track.artist.lowercase().contains(it.lowercase()) }
                    ?: true
            matchesQuery && matchesGenre && matchesYear && matchesExplicit && matchesArtist
        }

    private fun List<Track>.sort(sorting: SongRetrievalContext.Sorting): List<Track> {
        val comparator = when (sorting.by) {
            SongRetrievalContext.Sorting.By.TITLE -> byTitle
            SongRetrievalContext.Sorting.By.ARTIST -> byArtist
            SongRetrievalContext.Sorting.By.ALBUM -> byAlbym
            SongRetrievalContext.Sorting.By.YEAR -> byYear
            SongRetrievalContext.Sorting.By.PLAY_COUNT -> byPlayCount
            SongRetrievalContext.Sorting.By.DURATION -> byDuration
        }
        return sortedWith(if (sorting.order == SongRetrievalContext.Sorting.Order.ASCENDING) comparator else comparator.reversed())
    }

    companion object {
        private val byTitle = compareBy<Track> {
            it.title.lowercase().replace(Regex("(\\d+)")) { m -> m.value.padStart(10, '0') }
        }
        private val byArtist = compareBy<Track> { it.artist.lowercase() }
        private val byAlbym = compareBy<Track> { it.album.lowercase() }
        private val byYear = compareBy<Track> { it.year }
        private val byPlayCount = compareBy<Track> { it.playCount }
        private val byDuration = compareBy<Track> { it.durationSeconds }
    }
}

val artists = listOf(
    "Arctic Monkeys",
    "Kendrick Lamar",
    "Taylor Swift",
    "Radiohead",
    "Billie Eilish",
    "The Weeknd",
    "Frank Ocean",
    "Tame Impala"
)

private val albums = listOf(
    "Currents",
    "DAMN.",
    "Folklore",
    "OK Computer",
    "Happier Than Ever",
    "After Hours",
    "Blonde",
    "AM"
)

private val fakeRandom = Random(seed = 42)

@OptIn(ExperimentalUuidApi::class)
private fun generateFakeTracks(count: Int): List<Track> = (1..count).map { index ->
    Track(
        id = "track_$index",
        title = "Track $index",
        artist = artists.random(fakeRandom),
        album = albums.random(fakeRandom),
        genreId = genres.random(fakeRandom),
        durationSeconds = (120..360).random(fakeRandom),
        year = (2000..2026).random(fakeRandom),
        playCount = (0..100000).random(fakeRandom),
        isExplicit = fakeRandom.nextBoolean()
    )
}

private const val titleSectionSize = 20