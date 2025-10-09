import com.android.build.api.dsl.CompileOptions
import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

object JvmStuff {
    val javaVersion = JavaVersion.VERSION_21
}

inline fun CompileOptions.configureJava() {
    sourceCompatibility = JvmStuff.javaVersion
    targetCompatibility = JvmStuff.javaVersion
}

inline fun KotlinMultiplatformExtension.jvmToolchain() =
    jvmToolchain(JavaVersion.VERSION_21.majorVersion.toInt())