import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.publishOnCentral)
}

kotlin {
    jvm()
    androidTarget()
    wasmJs() {
        browser()
        nodejs()
    }
    macosX64()
    macosArm64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    tvosArm64()
    tvosSimulatorArm64()
    tvosX64()
    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()
    watchosX64()
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

        androidMain.dependencies {

        }

        jvmMain.dependencies {

        }
    }
}

android {
    namespace = "$evolpaginkPackageName.core"

    configureCompileAndMinSdkForLibrary()

    compileOptions {
        configureJava()
    }
}

setupPublishingAndSigning()