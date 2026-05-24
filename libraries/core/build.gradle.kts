import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.publishOnCentral)
    alias(libs.plugins.dokka)
}

kotlin {
    jvm()
    android {
        configureCompileAndMinSdkForLibrary(
            namespace = "$evolpaginkPackageName.core"
        )
    }
    wasmJs() {
        browser()
        nodejs()
    }
    macosArm64()
    iosArm64()
    iosSimulatorArm64()
    tvosArm64()
    tvosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()
    linuxArm64()
    linuxX64()
    mingwX64()
    js() {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.collections)
            implementation(libs.concurrentCollections)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        iosMain.dependencies {

        }

        jvmMain.dependencies {

        }
    }
}

setupPublishingAndSigning()