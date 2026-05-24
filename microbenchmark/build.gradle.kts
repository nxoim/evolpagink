plugins {
    alias(libs.plugins.android.library2)
    alias(libs.plugins.androidx.benchmark)
}

android {
    namespace = "$evolpaginkPackageName.microbenchmark"

    configureBenchmark()
}


dependencies {
    androidTestImplementation(projects.libraries.core)
    androidTestImplementation(libs.androidx.paging)
    androidTestImplementation(libs.androidx.paging.compose)
    androidTestImplementation(libs.androidx.benchmark.junit)
    androidTestImplementation(libs.androidx.benchmark.macro)
    androidTestImplementation(libs.kotlinx.coroutines.core)

    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
}
