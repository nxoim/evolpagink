import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.Sign
import java.util.Base64
import java.util.Properties

val libraryGroup = "com.nxoim"
val libraryLicenseName = "The Apache License, Version 2.0"
val libraryLicenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"

inline fun Project.setupPublishingAndSigning(
    artifactName: String = project.name,
    description: String = "Multiplatform pagination library"
) {
    setupSigning()

    group = libraryGroup
    version = autoVersion()

    if (version != "undefined") {
        println("> Assembling artifact for publishing ${project.group}:${project.name}:${project.version}")
    }

    resolveMavenCentralCredentials()

    configure<org.danilopianini.gradle.mavencentral.PublishOnCentralExtension> {
        repoOwner.set("nxoim")
        projectDescription.set(description)
        licenseUrl.set(libraryLicenseUrl)

//        repository("https://maven.pkg.github.com/nxoim/evolpagink", "GitHub") {
//            val token = runCatching {
//                requireCredential("GITHUB_TOKEN")
//            }.getOrElse {
//                requireCredential("GITHUB_TOKEN_BASE64", allowBase64 = true)
//            }
//
//            user.set(requireCredential("GITHUB_USERNAME"))
//            password.set(token)
//        }
    }

    configure<org.gradle.api.publish.PublishingExtension> {
        publications {
            withType<MavenPublication> {
                pom {
                    name.set(artifactName)
                    this.description.set(description)
                    url.set("https://github.com/nxoim/evolpagink")
                    licenses {
                        license {
                            name.set(libraryLicenseName)
                            url.set(libraryLicenseUrl)
                        }
                    }
                    developers {
                        developer {
                            id.set("nxoim")
                            name.set("nxoim")
                            email.set("reach@nxoim.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:github.com/nxoim/evolpagink.git")
                        developerConnection.set("scm:git:ssh://github.com/nxoim/evolpagink.git")
                        url.set("https://github.com/nxoim/evolpagink")
                    }
                }
            }
        }
    }
}

@Suppress("NewApi")
fun Project.setupSigning() {
    fixSigningTaskExecution()

    configure<org.gradle.plugins.signing.SigningExtension> {
        val signingKey = runCatching {
            requireCredential("SIGNING_KEY")
        }.getOrElse {
            runCatching {
                println("SIGNING_KEY not found. Trying SIGNING_KEY_PATH")
                file(requireCredential("SIGNING_KEY_PATH")).readText()
            }.getOrElse {
                println("SIGNING_KEY_PATH not found. Trying SIGNING_KEY_BASE64")
                requireCredential("SIGNING_KEY_BASE64", allowBase64 = true)
            }
        }
        val signingPassword = runCatching {
            requireCredential("SIGNING_KEY_PASSWORD")
        }.getOrElse {
            println("SIGNING_KEY_PASSWORD not found. Trying SIGNING_KEY_PASSWORD_BASE64")
            requireCredential("SIGNING_KEY_PASSWORD_BASE64", allowBase64 = true)
        }
        val signingKeyId = getenv("SIGNING_KEY_ID") ?: projectProp("signingKeyId")

        if (signingKey.isBlank() || signingPassword.isBlank()) {
            throw GradleException("Signing credentials incomplete: both signing key and it's password are required.")
        }

        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    }
}

fun Project.fixSigningTaskExecution() {
    // stupid ass gradle bullshit omfg
    // region Fix Gradle warning about signing tasks using publishing task outputs without explicit dependencies
    // <https://youtrack.jetbrains.com/issue/KT-46466>
    tasks.withType<AbstractPublishToMaven>().configureEach {
        val signingTasks = tasks.withType<Sign>()
        mustRunAfter(signingTasks)
    }
}

fun Project.autoVersion(): String =
    runCatching {
        val p = ProcessBuilder("git", "describe", "--tags", "--always")
            .redirectErrorStream(true)
            .start()
        p.inputStream.bufferedReader().readText().trim()
    }.getOrDefault("undefined")


fun String?.trimAndNullIfEmpty(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
fun getenv(name: String): String? = System.getenv(name)?.trimAndNullIfEmpty()
fun Project.projectProp(name: String): String? = (findProperty(name) as? String).trimAndNullIfEmpty()

fun Project.loadLocalProperties(): Properties {
    val f = rootProject.file("local.properties")
    return Properties().apply {
        if (f.exists()) f.inputStream().use { load(it) }
    }
}

fun Project.requireProjectProperty(name: String) = projectProp(name)
    ?: throw GradleException("Missing required Gradle property '$name'. Add it in gradle.properties or via -P$name=value.")

/** environment → local.properties → project property */
@Suppress("NewApi")
fun Project.requireCredential(name: String, allowBase64: Boolean = false): String {
    val localProps = loadLocalProperties()
    val tried = mutableListOf<String>()

    val fromEnv = getenv(name)?.also { tried += "env:$name" }
    val fromLocal = localProps.getProperty(name)?.trimAndNullIfEmpty()?.also { tried += "local.properties:$name" }
    val fromProject = projectProp(name)?.also { tried += "project:$name" }

    val raw = fromEnv ?: fromLocal ?: fromProject
    if (raw == null) {
        throw GradleException(buildString {
            appendLine("Missing required credential '$name'.")
            appendLine("Checked: ${tried.ifEmpty { listOf("none found") }.joinToString()}")
            appendLine("Expected sources:")
            appendLine("  1. Environment variable $name")
            appendLine("  2. local.properties entry $name")
            appendLine("  3. Gradle property -P$name")
        })
    }

    return if (allowBase64) {
        runCatching { String(Base64.getDecoder().decode(raw)) }.getOrDefault(raw)
    } else raw
}

@Suppress("NewApi")
fun Project.resolveMavenCentralCredentials(): Pair<String, String> {
    val localProps = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }

    listOf("mavenCentralPortalUsername", "mavenCentralPortalPassword").forEach { key ->
        if (!project.hasProperty(key)) {
            localProps.getProperty(key)?.let { project.extensions.extraProperties.set(key, it) }
        }
    }

    val tried = mutableListOf<String>()

    // environment → gradle → legacy
    fun tryGet(vararg keys: String): String? {
        for (k in keys) {
            getenv(k)?.let { tried += "env:$k"; return it }
        }
        for (k in keys) {
            (project.findProperty(k) as? String)?.trimAndNullIfEmpty()?.let { tried += "gradle:$k"; return it }
        }
        for (k in keys) {
            localProps.getProperty(k)?.trimAndNullIfEmpty()?.let { tried += "local.properties:$k"; return it }
        }
        return null
    }

    val username = tryGet(
        "MAVEN_CENTRAL_PORTAL_USERNAME",
        "MAVEN_CENTRAL_USERNAME",
        "mavenCentralPortalUsername",
        "sonatypeUsername",
        "ossrhUsername"
    )

    val password = tryGet(
        "MAVEN_CENTRAL_PORTAL_PASSWORD",
        "MAVEN_CENTRAL_PASSWORD",
        "mavenCentralPortalPassword",
        "sonatypePassword",
        "ossrhPassword"
    )

    if (username == null || password == null) {
        throw GradleException(buildString {
            appendLine("Could not resolve Maven Central credentials for project '${project.name}'.")
            appendLine("Expected variables:")
            appendLine("  Environment:")
            appendLine("    MAVEN_CENTRAL_PORTAL_USERNAME / MAVEN_CENTRAL_PORTAL_PASSWORD")
            appendLine("  Fallback environment:")
            appendLine("    MAVEN_CENTRAL_USERNAME / MAVEN_CENTRAL_PASSWORD")
            appendLine("  Gradle properties:")
            appendLine("    mavenCentralPortalUsername / mavenCentralPortalPassword")
            appendLine("  Legacy Sonatype properties:")
            appendLine("    sonatypeUsername / sonatypePassword")
            appendLine("    ossrhUsername / ossrhPassword")
            appendLine()
            appendLine("Checked: ${tried.ifEmpty { listOf("none found") }.joinToString()}")
        })
    }

    return username to password
}