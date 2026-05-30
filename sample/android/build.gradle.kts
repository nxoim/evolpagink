import convention.setupComposeAndroidApp

plugins {
    id("com.nxoim.gradle.compose-android-app-plugins")
}

android {
    setupComposeAndroidApp()
}

dependencies {
    implementation(projects.sample.shared)
}