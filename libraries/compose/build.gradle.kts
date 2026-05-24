plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.publishOnCentral)
    alias(libs.plugins.dokka)
}

kotlin {
    jvm()
    android {
        configureCompileAndMinSdkForLibrary(
            namespace = "$evolpaginkPackageName.compose"
        )
    }
    wasmJs() {
        browser()
        nodejs()
    }
    iosArm64()
    iosSimulatorArm64()
    macosArm64()
    js() {
        browser()
        nodejs()
    }

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

setupPublishingAndSigning()