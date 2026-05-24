import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.android.library)
}

kotlin {
    jvm()
    android {
        configureCompileAndMinSdkForLibrary(
            namespace = "com.nxoim.sample.composeApp"
        )
    }
    wasmJs {
        outputModuleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }

    sourceSets {
        all {
            languageSettings {
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
            }
        }
        commonMain.dependencies {
                    api(libs.compose.runtime)
                    api(libs.compose.material3)
                    api(libs.compose.material3Adaptive)
                    api(libs.compose.materialIconsExtended)
                    api(libs.compose.foundation)
                    api(libs.compose.preview)
                    api(libs.compose.animation)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                    api(libs.compose.components.resources)
                    api(libs.kotlinx.coroutines.core)
                    api(libs.kotlinx.serialization.json)
                    api(libs.androidx.collections)
                    api(libs.concurrentCollections)
                    api(projects.libraries.compose)
                }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            api(libs.androidx.appcompat)
            api(libs.androidx.activityCompose)
            api(libs.compose.uitooling)
            api(libs.kotlinx.coroutines.android)
        }

        iosMain.dependencies {

        }

        jvmMain.dependencies {
            implementation(compose.desktop.common)
            implementation(compose.desktop.currentOs)
            implementation(libs.compose.animationGraphics)
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "com.nxoim.sample.desktopApp"
            packageVersion = "1.0.0"
        }
    }
}
