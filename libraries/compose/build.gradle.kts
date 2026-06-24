import convention.setupAndroidTarget
import convention.setupIosTargets
import convention.setupJvmTarget
import convention.setupWebTargets
import evolpagink.convention.setupPublishing

plugins {
    id("com.nxoim.gradle.compose-multiplatform-plugins")
}

setupPublishing(artifactId = "compose")

kotlin {
    setupJvmTarget()
    android { setupAndroidTarget(project) }
    setupWebTargets(project, isExecutable = false)
    setupIosTargets()

    sourceSets {
        commonMain.dependencies {
            api(projects.libraries.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.lifecycle)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}