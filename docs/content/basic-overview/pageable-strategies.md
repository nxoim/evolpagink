---
title: Pageable Strategies
seo:
  title: evolpagink Pageable Strategies
---
## evolpagink provides a few strategies out of the box:
- Based on page amount
- Based on minimum item amount

The variant based on minimum item amount will try to load as many pages needed to reach the minimum specified amount of items.
```kotlin
prefetchMinimumItemAmount( // the default overload for indexed pages
    initialPage = 0, // the first page key to use when loading starts
    minimumItemAmount = 20
)
```

The variant based on page amount will try to load the minimum specified amount of pages.
```kotlin
prefetchPageAmount(
    initialPage = 0,
    minimumPageAmount = 2
)   
```

::tip
Should you want your own strategy - you can implement `PageFetchStrategy`. 
::

Visit the sample linked below and play with the UI scale to see how items are loaded.

::card
---
title: Sample
icon: i-lucide-eye
to: ../other/sample/index.html
target: _blank
---
Compose WasmJS evolpagink sample
::

## How UI interacts with pagination
`Pageable` exposes a method, `_onEvent`, that allows the UI to tell it what is visible. `Pageable` will then refer to the provided strategy to compose the new intended visible list of pages.

The UI can report visible items to `Pageable` in 2 ways:
- By reporting visible items. Fully dynamic, adaptive to viewport size. Recommended
- By reporting the intended centered item. Non adaptive, the amount of pages loaded will be more static. This method is **discouraged** unless you have a specific use case.

Compose Multoplatform bindings report visible items by default. They observe the list state and report to `Pageable` accordingly.

See the API Reference for full details.

::card
---
title: API Reference
icon: i-lucide-code
to: ../other/dokka/index.html
target: _blank
---
::