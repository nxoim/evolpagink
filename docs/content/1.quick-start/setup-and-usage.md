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
data class YourData( // public ui data structure
    val id: String,
    val data: Something
)
```
::

### Define your source, design its requirements
::collapsible
```kotlin
// this is a source that only allows to retrieve pages by index
interface YourSource {
    fun getPage(pageIndex: Int): Flow<List<YourData>>
}
```
::

### Use the source definition in `pageable` in your view model
::collapsible
```kotlin
// the source will be implemented by some part of your code that
// knows about the actual data source
class YourModel(source: YourSource, coroutineScope: CoroutineScope) {  
    val yourPageableageable = pageable(
        coroutineScope,
        onPage = { index -> source.getPage(index) },
        strategy = prefetchPageAmount( // one of the default strategies
            initialPage = 0, 
            pageAmountSurroundingVisible = 2
        )
    )
}
```
::


### Convert the pageable to usable state in your UI
::collapsible
```kotlin
val lazyListState = rememberLazyListState()
val pageableState = yourModel.pageable.toState(
    lazyListState,
    key = { item -> item.id }
)
```
::

### Optionally configure the strategy for `pageable`. See [Pageable Strategies](/basic-overview/pageable-strategies)

</steps>

## Example Structure

::code-tree{default-value="yourFeature/YourModel.kt"}

```kotlin [yourFeature/YourSource.kt]
data class YourData(val id: String, val data: Something)

interface YourSource {
    fun getPage(pageIndex: Int): Flow<List<YourData>>
}
```

```kotlin [yourFeature/YourModel.kt]
class YourModel(source: YourSource, coroutineScope: CoroutineScope) {
    // the default startegies use Int as page key/index,
    // but any page key type you want is possible   
    val yourPageableageable = pageable(
        coroutineScope,
        onPage = { index -> source.getPage(index) },
        strategy = prefetchPageAmount( // one of the default strategies
            initialPage = 0, 
            pageAmountSurroundingVisible = 2
        )
    )
}
```

```kotlin [yourFeature/YourScreen.kt]
@Composable
fun YourScreen(model: PaginationModel) {
    val lazyListState = rememberLazyListState()
    val pageableState = yourModel.pageable.toState(
        lazyListState, // will be observed for automatic pageable state updates
        key = { item -> item.id }
    )

    LazyColumn(lazyListState) { // dont forget to use the state
        // this is an overload that automatically uses the key lambda from `.toState` above
        items(pageableState) { item ->
            YourItem(item)
        }
    }
}
```
::

