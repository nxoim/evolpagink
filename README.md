![evolpagink](https://img.shields.io/maven-central/v/com.nxoim.evolpagink/core?label=evolpagink)

![badge][badge-ios]
![badge][badge-js]
![badge][badge-jvm]
![badge][badge-linux]
![badge][badge-windows]
![badge][badge-mac]
![badge][badge-tvos]
![badge][badge-watchos]
# evolpagink
Pagination made truly small, truly easy to use. The evil, unknown counterpart of... uhm.. some *other* commonly used pagination library. 

## how small?
First of all - the amount of source code is small.

But regarding the amount of boilerplate - you define your pageable source:
```kotlin
val pageable = pageable(
	coroutineScope,
	onPage = { index ->
		yourSource.getPage(index) 
		// getPage is Flow<List<YourItem>>
	}, 
	strategy = visibilityAwarePrefetchPageAmount( 
		// this strategy will use your ui to fetch 
		// items to fill the viewport + prefetch 
		// specified amount beyond viewport
		initialPage = 0,
		pageAmountSurroundingVisible = 2
	)
)
```

Aaaaaand then you use it. For example in compose it looks like: 
```kotlin
val lazyListState = rememberLazyListState()
val pageableState = yourModel.pageable.toState(
	lazyListState,
	key = { item -> item.maybeSomeId }
)

LazyColumn(state) {
	// this is an overload that automatically
	// uses the key lambda from above
	items(pageableState) { item ->
		YourItem(item)
	}
	// by the way the overload prevents the
	// import fights of items(count: Int) 
	// vs items(items: List<T>)!!
}
```

## fast?
Theres a microbenchmark in the repository. Clone the repo and run it. If you find the benchmark unsatisfactory - i'd very much appreciate a discussion in an open issue!

## customizable?
Yes. If you are unsatisfied with any of the stategies for fetching and prefetching items - you can easily create your own by implementing 'PageFetchStrategy'. You can tailor the behavior precisely. 

## what else?
- evolpagink is Compose Multiplatform first, but the **core** logic being **platform agnostic** leaves room for compatibility with other UI frameworks.
- If the library becomes unmaintained - forking and maintaining it yourself should be easy due to the small size and code being mostly self documenting.

## Contributions
To contribute:
- First open an issue and describe your contribution so it can be discussed
- Then link the branch of your fork, containing the contribution, in the issue
- And then the contribution may be merged

## Credits
Kudos to the [Tiler](https://github.com/tunjid/Tiler) project for being an inspiration and for the benchmark

> [!NOTE]
> The library is experimental and in early stage of development, though intended to
> eventually reach stability

[badge-android]: http://img.shields.io/badge/-android-6EDB8D.svg?style=flat

[badge-jvm]: http://img.shields.io/badge/-jvm-DB413D.svg?style=flat

[badge-js]: http://img.shields.io/badge/-js-F8DB5D.svg?style=flat

[badge-js-ir]: https://img.shields.io/badge/support-[IR]-AAC4E0.svg?style=flat

[badge-nodejs]: https://img.shields.io/badge/-nodejs-68a063.svg?style=flat

[badge-linux]: http://img.shields.io/badge/-linux-2D3F6C.svg?style=flat

[badge-windows]: http://img.shields.io/badge/-windows-4D76CD.svg?style=flat

[badge-wasm]: https://img.shields.io/badge/-wasm-624FE8.svg?style=flat

[badge-apple-silicon]: http://img.shields.io/badge/support-[AppleSilicon]-43BBFF.svg?style=flat

[badge-ios]: http://img.shields.io/badge/-ios-CDCDCD.svg?style=flat

[badge-mac]: http://img.shields.io/badge/-macos-111111.svg?style=flat

[badge-watchos]: http://img.shields.io/badge/-watchos-C0C0C0.svg?style=flat

[badge-tvos]: http://img.shields.io/badge/-tvos-808080.svg?style=flat
