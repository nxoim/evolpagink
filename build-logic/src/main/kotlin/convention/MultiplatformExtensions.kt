package convention

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import evolpagink.convention.AndroidBuildStuff
import evolpagink.convention.Constants
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import java.io.File

fun KotlinMultiplatformExtension.setupJvmTarget(): KotlinJvmTarget = jvm()


fun KotlinMultiplatformAndroidLibraryTarget.setupAndroidTarget(
    project: Project,
    namespace: String = "${Constants.packageRootName}.${project.name}"
) {
    this.namespace = namespace

    compileSdk { version = release(AndroidBuildStuff.compileSdk) }
    minSdk { version = release(AndroidBuildStuff.minSdk) }

    androidResources {
        this.enable = true
    }

    this.optimization {
        this.consumerKeepRules.files.add(File("consumer-proguard-rules.pro"))
    }
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
fun KotlinMultiplatformExtension.setupWebTargets(
    project: Project,
    isExecutable: Boolean = false,
    moduleName: String = project.name
) {
    wasmJs("wasmJs") {
        outputModuleName.set(project.provider { moduleName })

        browser {
            commonWebpackConfig { outputFileName = "$moduleName.js" }
        }

        if (isExecutable) binaries.executable() else nodejs()
    }

    js("js") {
        outputModuleName.set(project.provider { moduleName })

        browser {
            commonWebpackConfig { outputFileName = "$moduleName.js" }
        }

        if (isExecutable) binaries.executable() else nodejs()
    }
}

fun KotlinMultiplatformExtension.setupIosTargets(
    baseFrameworkName: String? = null
) {
    val targets = listOf(iosArm64(), iosSimulatorArm64())

    if (baseFrameworkName != null) {
        targets.forEach { target ->
            target.binaries.framework {
                baseName = baseFrameworkName
                isStatic = true
            }
        }
    }
}

fun KotlinMultiplatformExtension.setupTvosTargets(
    baseFrameworkName: String? = null
) {
    val targets = listOf(tvosArm64(), tvosSimulatorArm64())

    if (baseFrameworkName != null) {
        targets.forEach { target ->
            target.binaries.framework {
                baseName = baseFrameworkName
                isStatic = true
            }
        }
    }
}

fun KotlinMultiplatformExtension.setupWatchosTargets(
    baseFrameworkName: String? = null
) {
    val targets = listOf(
        watchosArm32(),
        watchosArm64(),
        watchosDeviceArm64(),
        watchosSimulatorArm64()
    )

    if (baseFrameworkName != null) {
        targets.forEach { target ->
            target.binaries.framework {
                baseName = baseFrameworkName
                isStatic = true
            }
        }
    }
}