import com.android.build.api.dsl.AndroidSourceSet
import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.NamedDomainObjectContainer

object AndroidBuildStuff {
    const val compileSdk = 37
    const val minSdk = 24
}

inline fun ApplicationExtension.configureCompileAndMinSdkForApp(
    applicationId: String,
    versionCode: Int,
    versionName: String
) {
    compileSdk = AndroidBuildStuff.compileSdk

    defaultConfig {
        minSdk = AndroidBuildStuff.minSdk
        targetSdk = AndroidBuildStuff.compileSdk

        this.applicationId = applicationId
        this.versionCode = versionCode
        this.versionName = versionName
    }
}


inline fun ApplicationExtension.getDebugSigningConfig() = signingConfigs.getByName("debug")

inline fun NamedDomainObjectContainer<ApplicationBuildType>.configureReleaseBuild(signingConfig: ApkSigningConfig?) {
    getByName("release") {
        isMinifyEnabled = true
        isShrinkResources = true
        this.signingConfig = signingConfig
    }
}


inline fun KotlinMultiplatformAndroidLibraryTarget.configureCompileAndMinSdkForLibrary(
    namespace: String
) {
    this.namespace = namespace
    compileSdk { version = release(AndroidBuildStuff.compileSdk) }
    minSdk { version = release(AndroidBuildStuff.minSdk) }
}

inline fun LibraryExtension.configureBenchmark() {
    compileSdk = AndroidBuildStuff.compileSdk

    defaultConfig {
        minSdk = AndroidBuildStuff.minSdk
        defaultConfig.testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
        testInstrumentationRunnerArguments["androidx.benchmark.profiling.mode"] = "StackSampling"
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] =
            "EMULATOR,LOW-BATTERY"
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

