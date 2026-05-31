@file:OptIn(ExperimentalAtomicApi::class)

package com.nxoim.sample.ui

import com.nxoim.evolpagink.core.pageable
import com.nxoim.evolpagink.core.prefetchMinimumItemAmount
import com.nxoim.sample.ui.components.FilterController
import com.nxoim.sample.ui.components.SearchController
import com.nxoim.sample.ui.components.SortingController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndUpdate

class Model(
    private val songSource: SongSource
) : FilterController, SortingController, SearchController {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val _songRetrievalContext = MutableStateFlow(SongRetrievalContext.Default)

    override val filters = _songRetrievalContext
        .map { it.filters }
        .stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(),
            _songRetrievalContext.value.filters
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val songCount = _songRetrievalContext
        .flatMapLatest(songSource::songCount)
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val sections = _songRetrievalContext
        .flatMapLatest { songSource.getSections(it) }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

    val pageable = pageable(
        coroutineScope = coroutineScope,
        context = _songRetrievalContext,
        onPage = { page ->
            val start = page * pageSize
            val end = start + pageSize
            songSource.getSongs(
                amount = start..end,
                context = this
            )
        },
        strategy = prefetchMinimumItemAmount(),
        pageItemKey = { it.id }
    )

    private val lastJump = AtomicInt(-1)

    suspend fun jumpToSongAtIndex(index: Int) {
        // getAndSet returns the previous value; if it equals index, skip (already jumped here)
        if (lastJump.fetchAndUpdate { index } != index) {
            val song = songSource.getSongAtIndex(index, _songRetrievalContext.value).first()
            if (song != null) {
                val pageKey = index / pageSize
                pageable.jumpTo(pageKey)
            }
        }
    }

    fun remove(item: Track) {
        coroutineScope.launch {
            songSource.remove(item)
        }
    }

    override fun setGenre(new: String?) = _songRetrievalContext.update {
        it.copy(filters = it.filters.copy(genreId = new))
    }

    override fun setYearRange(new: IntRange?) = _songRetrievalContext.update {
        it.copy(filters = it.filters.copy(yearRange = new))
    }

    override fun setExplicit(new: Boolean?) = _songRetrievalContext.update {
        it.copy(filters = it.filters.copy(isExplicit = new))
    }

    override fun setArtist(new: String?) = _songRetrievalContext.update {
        it.copy(filters = it.filters.copy(artist = new))
    }

    override fun resetFilters() = _songRetrievalContext.update {
        it.copy(filters = SongRetrievalContext.Filters())
    }

    override fun onQuery(new: String) {
        _songRetrievalContext.update { it.copy(searchQuery = new) }
    }

    override val sorting = _songRetrievalContext
        .map { it.sorting }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), SongRetrievalContext.Sorting())

    override fun setSortBy(new: SongRetrievalContext.Sorting.By) = _songRetrievalContext.update {
        it.copy(sorting = it.sorting.copy(by = new))
    }

    override fun setSortOrder(new: SongRetrievalContext.Sorting.Order) =
        _songRetrievalContext.update {
            it.copy(sorting = it.sorting.copy(order = new))
        }

    override fun resetSorting() = _songRetrievalContext.update {
        it.copy(sorting = SongRetrievalContext.Sorting())
    }
}

data class SongRetrievalContext(
    val searchQuery: String = "",
    val filters: Filters = Filters(),
    val sorting: Sorting = Sorting()
) {
    companion object {
        val Default = SongRetrievalContext()
    }

    data class Filters(
        val genreId: String? = null,
        val yearRange: IntRange? = null,
        val isExplicit: Boolean? = null,
        val artist: String? = null
    )

    data class Sorting(
        val by: By = By.TITLE,
        val order: Order = Order.ASCENDING
    ) {
        enum class By { TITLE, ARTIST, ALBUM, YEAR, PLAY_COUNT, DURATION }
        enum class Order { ASCENDING, DESCENDING }
    }
}

sealed interface ItemData {
    val id: String
    val index: Int

    data class Loaded(val value: Track, override val index: Int) : ItemData {
        override val id = value.id
    }
    data class Placeholder(
        override val id: String,
        override val index: Int
    ) : ItemData
}

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val genreId: String,
    val durationSeconds: Int,
    val year: Int,
    val playCount: Int,
    val isExplicit: Boolean,
)

interface SongSource {
    fun getSongs(
        amount: IntRange,
        context: SongRetrievalContext
    ): Flow<List<ItemData>>

    fun songCount(
        context: SongRetrievalContext
    ): Flow<Int>

    fun getSongAtIndex(index: Int, context: SongRetrievalContext): Flow<Track?>

    fun getSections(context: SongRetrievalContext): Flow<List<SectionInfo>>

    fun remove(item: Track)

    data class SectionInfo(val label: String, val startIndex: Int)

    companion object {
        val genres =
            listOf("pop", "rock", "hiphop", "jazz", "classical", "electronic", "country", "rnb")
    }
}

private const val pageSize = 5