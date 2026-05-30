import convention.setupAndroidTarget
import convention.setupIosTargets
import convention.setupJvmTarget
import convention.setupTvosTargets
import convention.setupWatchosTargets
import convention.setupWebTargets
import evolpagink.convention.setupPublishing

plugins {
    id("com.nxoim.gradle.kotlin-multiplatform-plugins")
}

setupPublishing(artifactId = "evolpagink-core")

kotlin {
    setupJvmTarget()
    android { setupAndroidTarget(project) }
    setupWebTargets(project, isExecutable = false)
    setupIosTargets()
    setupTvosTargets()
    setupWatchosTargets()
    linuxArm64()
    linuxX64()
    mingwX64()

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
    }
}