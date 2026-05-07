---
title: Setup and Usage
navigation:
  icon: i-lucide-folder-tree
seo:
  title: evolpagink Setup & Usage
---
## Basically
<steps>


### Define your UI data
::collapsible
::tip
All items should have an identificator
::
```kotlin
data class SongData( // public ui data structure
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long
)
```
::


### Define your source, design its requirements
::collapsible
```kotlin
// this is a source that only allows to retrieve pages by index
interface SongSource {
    fun getPage(pageIndex: Int): Flow<List<SongData>>
}
::


### Use the source definition in `pageable` in your view model
::collapsible
```kotlin
// the source will be implemented by some part of your code that
// knows about the actual data source
class SongListModel(source: SongSource, coroutineScope: CoroutineScope) {  
    val songPageable = pageable(
        coroutineScope,
        onPage = { index -> source.getPage(index) },
        strategy = prefetchPageAmount( // one of the default strategies
            initialPage = 0, 
            minimumPageAmount = 2
        )
    )
}
```
::



### Convert the pageable to usable state in your UI
::collapsible
```kotlin
val lazyListState = rememberLazyListState()
val pageableState = songListModel.songPageable.toState(
    lazyListState,
    key = { item -> item.id }
)
```
::


### Optionally configure the strategy for `pageable`. See [Pageable Strategies](/basic-overview/pageable-strategies)


</steps>


## Example Structure


::code-tree{default-value="songs/SongListModel.kt"}


```kotlin [songs/SongData.kt]
// UI model
data class SongData(
    val id: String, 
    val title: String, 
    val artist: String, 
    val durationMs: Long
)
```


```kotlin [songs/SongSource.kt]
interface SongSource {
    fun getPage(pageIndex: Int): Flow<List<SongData>>
}
```


```kotlin [songs/SongListModel.kt]
class SongListModel(
    private val source: SongSource, 
    coroutineScope: CoroutineScope
) {
    // the default strategies use Int as page key/index,
    // but any page key type you want is possible   
    val songPageable = pageable(
        coroutineScope,
        onPage = { index -> source.getPage(index) },
        strategy = prefetchPageAmount( // one of the default strategies
            initialPage = 0, 
            minimumPageAmount = 2
        )
    )
}
```


```kotlin [songs/SongListScreen.kt]
@Composable
fun SongListScreen(model: SongListModel) {
    val lazyListState = rememberLazyListState()
    val pageableState = model.songPageable.toState(
        lazyListState, // will be observed for automatic pageable state updates
        key = { item -> item.id }
    )

    LazyColumn(lazyListState) { // dont forget to use the state
        // this is an overload that automatically 
        // uses the key lambda from .toState above
        items(pageableState) { song ->
            SongItem(song)
        }
    }
}
```
::