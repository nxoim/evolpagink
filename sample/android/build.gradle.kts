plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(projects.sample.shared)
}


android {
    namespace = "com.nxoim.sample"

    configureCompileAndMinSdkForApp(
        applicationId = "com.nxoim.sample.androidApp",
        versionCode = 1,
        versionName = "1.0.0"
    )

    compileOptions {
        configureJava()
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        configureReleaseBuild(getDebugSigningConfig())
    }
}