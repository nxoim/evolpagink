import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

// thanks to https://github.com/tunjid/Tiler/blob/develop/benchmarks/microbenchmark/src/androidTest/java/com/example/microbenchmark/AllocationBenchmark.kt
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Benchmarks {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun a_paging3Scroll() = benchmarkRule.measureRepeated {
        runBlocking {
            Paging3Benchmarked(
                pageToScrollTo = PAGE_TO_SCROLL_TO,
                pagesToInvalidate = emptyPages
            ).benchmark()
        }
    }

    @Test
    fun b_ownScroll() = benchmarkRule.measureRepeated {
        runBlocking {
            EvolpaginkBenchmarked(
                pageToScrollTo = PAGE_TO_SCROLL_TO,
                pagesToInvalidate = emptyPages
            ).benchmark()
        }
    }

    @Test
    fun c_paging3InvalidationOffScreen() = benchmarkRule.measureRepeated {
        runBlocking {
            Paging3Benchmarked(
                pageToScrollTo = PAGE_TO_SCROLL_TO,
                pagesToInvalidate = offScreenPages
            ).benchmark()
        }
    }

    @Test
    fun d_ownInvalidationOffScreen() = benchmarkRule.measureRepeated {
        runBlocking {
            EvolpaginkBenchmarked(
                pageToScrollTo = PAGE_TO_SCROLL_TO,
                pagesToInvalidate = offScreenPages
            ).benchmark()
        }
    }

    @Test
    fun e_paging3InvalidationOnScreen() = benchmarkRule.measureRepeated {
        runBlocking {
            Paging3Benchmarked(
                pageToScrollTo = PAGE_TO_SCROLL_TO,
                pagesToInvalidate = onScreenPages
            ).benchmark()
        }
    }

    @Test
    fun f_ownInvalidationOnScreen() = benchmarkRule.measureRepeated {
        runBlocking {
            EvolpaginkBenchmarked(
                pageToScrollTo = PAGE_TO_SCROLL_TO,
                pagesToInvalidate = onScreenPages
            ).benchmark()
        }
    }
}