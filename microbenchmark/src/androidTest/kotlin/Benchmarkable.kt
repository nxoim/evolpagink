
const val PAGE_TO_SCROLL_TO = 20
const val ITEMS_PER_PAGE = 100

internal const val NUM_PAGES_IN_MEMORY = 3
internal const val MAX_LOAD_SIZE = NUM_PAGES_IN_MEMORY * ITEMS_PER_PAGE

const val ITEM_LOAD_DELAY_MS = 100L

@Suppress("EmptyRange")
val emptyPages = 0 until 0
val onScreenPages = (PAGE_TO_SCROLL_TO - NUM_PAGES_IN_MEMORY)..PAGE_TO_SCROLL_TO
val offScreenPages = (onScreenPages.first - NUM_PAGES_IN_MEMORY) until onScreenPages.first

sealed interface Benchmarkable {
    suspend fun benchmark()
}

data class Item(
  val  index: Int,
  val  lastInvalidatedPage: Int = Int.MIN_VALUE
)

object ItemDataSource {
    fun loadItems(page: Int, loadSize: Int, lastInvalidatedPage: Int): List<Item> {
        val pageCount = loadSize / ITEMS_PER_PAGE
        return rangeFor(page, numberOfPages = pageCount).map { index ->
            Item(index = index, lastInvalidatedPage = lastInvalidatedPage)
        }
    }
}

fun rangeFor(startPage: Int, numberOfPages: Int = 1): IntRange {
    val offset = startPage * ITEMS_PER_PAGE
    val next = offset + (ITEMS_PER_PAGE * numberOfPages)

    return offset until next
}

fun pageFor(item: Item): Int {
    val diff = item.index % ITEMS_PER_PAGE
    val firstItemIndex = item.index - diff
    return firstItemIndex / ITEMS_PER_PAGE
}