plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.androidx.benchmark)
    alias(libs.plugins.multiplatform)
}

kotlin {
    jvmToolchain(17)
    androidTarget()
}
android {
    namespace = "com.example.microbenchmark"
    compileSdk = 36

    sourceSets["main"].manifest.srcFile("src/main/AndroidManifest.xml")


    defaultConfig {
        minSdk = 24
        android.defaultConfig.testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
        testInstrumentationRunnerArguments["androidx.benchmark.profiling.mode"] = "StackSampling"
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR,LOW-BATTERY"
    }

    testBuildType = "release"

    buildTypes {
        getByName("release") {
            // The androidx.benchmark plugin configures release buildType with proper settings, such as:
            // - disables code coverage
            // - adds CPU clock locking task
            // - signs release buildType with debug signing config
            // - copies benchmark results into build/outputs/connected_android_test_additional_output folder
        }
    }
}

dependencies {
    androidTestImplementation(projects.library)
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
