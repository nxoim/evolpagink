---
title: What does the Pageable actually do?
description: 
seo:
  title: evolpagink What does the Pageable actually do?
---

## The pageable represents a paged data source
It provides you with:
- A list of relevant items from your pages
- The loading states in each direction
- The tools to notify the engine about visible pages
- To activate a specific page (usually referred to as "jumping")
- etc.

Internally it creates the paginator engine, which takes care of:
- Storage 
- Lifecycle of pages
- Page loading decisions
- Concurrent page updates

::card
---
title: Pageable in API Reference
icon: i-lucide-code
to: ../other/dokka/libraries/core/com.nxoim.evolpagink.core/-pageable/index.html
target: _blank
---
::

::card
---
title: Pageable Strategies Overview
icon: i-lucide-eye
to: pageable-strategies
---
Basic overview of the predefined strategies and how UI interacts with them
::


## The data flow looks like this:

0. UI appears, the collection of pageable items starts
1. Load initial page
2. UI tells paginator whats visible
3. Paginator uses the specified strategy to calculate pages to serve
4. The pages are requested and loaded, and the storage has their items deduplicated using the specified page item key factory
5. Repeat from point 2 unless the UI state is idle

When unsubscribed and resubscribed to the pageable, which may be the case during navigation, the page items stay cached, but all the work stops.

## Page loading may be contextual
The context may be a search query, or a filter, or something else entirely, stored as a StateFlow. When it emits a new value, the fetching restarts from initial page, and after the new page data is loaded all previously cached pages are cleared. This makes search, filters, and sorting seamless.

```kotlin
val songRetrievalContext = MutableStateFlow(SongRetrievalContext())

val pageable = pageable(
    coroutineScope = coroutineScope,
    context = songRetrievalContext,
    onPage = { page -> // context accessible via `this`
        songSource.getSongs(
            index = page,
            context = this
        )
    },
    strategy = prefetchMinimumItemAmount(),
    pageItemKey = { item -> item.id }
)

data class SongRetrievalContext(
    val searchQuery: String = "",
    val filters: Filters = Filters(),
    val sorting: Sorting = Sorting()
)
```

See the following code for the full example.

::card
---
title: Sample App Code
icon: i-lucide-code
to: https://github.com/nxoim/evolpagink/blob/main/sample/shared/src/commonMain/kotlin/com/nxoim/sample/ui/Model.kt
target: _blank
---
Example usage within a screen's model
::

You can also try out the sample app.
::card
---
title: Sample App
icon: i-lucide-layout-grid
to: ../other/sample/index.html
target: _blank
---
Sample App in Compose Web (WasmJS)
::
