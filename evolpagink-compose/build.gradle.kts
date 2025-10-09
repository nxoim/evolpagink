plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.library)
}

kotlin {
    jvm()
    androidTarget()
    wasmJs() {
        browser()
        nodejs()
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()
    js() {
        browser()
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.evolpaginkCore)
            implementation(compose.runtime)
            implementation(compose.foundation)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            // Add android specific dependencies here
        }
    }
}

android {
    namespace = "$evolpaginkPackageName.compose"

    configureCompileAndMinSdkForLibrary()

    compileOptions {
        configureJava()
    }

    buildFeatures {
        compose = true
    }
}
