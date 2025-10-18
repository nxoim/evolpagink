plugins {
    `kotlin-dsl`version "6.4.1"
    signing
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.publishOnCentralPlugin)
}