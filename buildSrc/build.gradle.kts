plugins {
    `kotlin-dsl`
    signing
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.publishOnCentralPlugin)
}