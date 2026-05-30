import convention.setupAndroidTarget
import convention.setupIosTargets
import convention.setupJvmTarget
import convention.setupWebTargets
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("com.nxoim.gradle.compose-multiplatform-plugins")
}

kotlin {
    setupJvmTarget()
    android { setupAndroidTarget(project) }
    setupWebTargets(project, isExecutable = true, moduleName = "composeApp")
    setupIosTargets(baseFrameworkName = "ComposeApp")

    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }

    sourceSets {
        all {
            languageSettings.optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
        }
        commonMain.dependencies {
            api(projects.libraries.compose)
            api(libs.compose.runtime)
            api(libs.compose.material3)
            api(libs.compose.material3Adaptive)
            api(libs.compose.materialIconsExtended)
            api(libs.kotlinx.serialization.json)
            api(libs.compose.preview)
        }
        androidMain.dependencies {
            api(libs.androidx.appcompat)
            api(libs.androidx.activityCompose)
            api(libs.compose.uitooling)
            api(libs.kotlinx.coroutines.android)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.compose.animationGraphics)
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi)
            packageName = "com.nxoim.sample.desktopApp"
            packageVersion = "1.0.0"
        }
    }
}
