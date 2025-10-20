import org.danilopianini.gradle.mavencentral.PublishOnCentralExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import java.util.Base64
import java.util.Properties

inline fun Project.setupPublishingAndSigning(
    artifactName: String = project.name,
    description: String = "Multiplatform pagination library"
) {
    val signingWasSetUp = setupSigning()
    if (signingWasSetUp) {
        loadMavenCredentialsForPublishingOnCentralPlugin()
    } else {
        println("Signing was not setup, therefore will not set up maven central credentials for publishing")
    }

    group = "com.nxoim.evolpagink"
    version = projectProp("version") ?: autoVersionFromGit()

    if (version != "undefined") {
        println("> Assembling artifact for publishing ${project.group}:${project.name}:${project.version}")
    }

    configure<PublishOnCentralExtension> {
        repoOwner.set("nxoim")
        projectLongName.set(artifactName)
        projectDescription.set(description)
        licenseName.set("The Apache License, Version 2.0")
        licenseUrl.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
        projectUrl.set("https://github.com/nxoim/evolpagink")
        scmConnection.set("scm:git:https://github.com/nxoim/evolpagink.git")
    }

    configure<PublishingExtension> {
        publications {
            withType<MavenPublication> {
                pom {
                    developers {
                        developer {
                            id.set("nxoim")
                            name.set("nxoim")
                            email.set("reach@nxoim.com")
                        }
                    }
                }
            }
        }
    }
}

/**
 * sets up signing explicitly from properties or environment.
 * returns false when failed to
 */
fun Project.setupSigning(): Boolean {
    val signingKey = getAnyCredentialOf(
        "SIGNING_KEY",
        "SIGNING_KEY_PATH",
        "SIGNING_KEY_BASE64"
    ) ?: return false

    val signingPassword = getAnyCredentialOf(
        "SIGNING_KEY_PASSWORD",
        "SIGNING_KEY_PASSWORD_BASE64"
    ) ?: return false

    if (signingKey.isBlank() || signingPassword.isBlank()) {
        println("Signing credentials incomplete: both signing key and its password are required.")
        return false
    }

    fixSigningTaskExecution()

    configure<SigningExtension> {
        useInMemoryPgpKeys(
            /* defaultKeyId = */ getenv("SIGNING_KEY_ID") ?: projectProp("signingKeyId"),
            /* defaultSecretKey = */ signingKey,
            /* defaultPassword = */ signingPassword
        )
    }

    return true
}

// Utils
////////////////////////////////////////////////////////////////////////////////////////////

fun Project.autoVersionFromGit(): String = runCatching {
    ProcessBuilder("git", "describe", "--tags", "--always")
        .redirectErrorStream(true)
        .start()
        .run { inputStream.bufferedReader().readText().trim() }
}.getOrDefault("undefined")

var _localProps: Properties? = null
val Project.localProps get() = if (_localProps != null)
    _localProps!!
else {
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }
        .also { _localProps = it }
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

/** environment → local.properties → project property */
@Suppress("NewApi")
fun Project.getAnyCredentialOf(vararg names: String): String? {
    for (name in names) {
        val raw = getenv(name)
            ?: localProps.getProperty(name)?.trimAndNullIfEmpty()
            ?: projectProp(name)

        if (raw == null) continue

        val resolved = runCatching {
            when {
                name.contains("_PATH") -> rootProject
                    .file(raw)
                    .takeIf { it.exists() }
                    ?.readText()

                name.contains("_BASE64") -> Base64
                    .getDecoder()
                    .decode(raw)
                    .decodeToString()

                else -> raw
            }
        }.getOrNull()?.trimAndNullIfEmpty()

        if (!resolved.isNullOrEmpty()) return resolved
    }

    println(
        buildString {
            appendLine("Could not resolve any of the credentials: ${names.joinToString(", ")} for project '${project.name}'.")
            appendLine("Searched sources in order for each name:")
            appendLine("  1. Environment variable <NAME>")
            appendLine("  2. local.properties entry <NAME>")
            appendLine("  3. Gradle property -P<NAME>")
        }
    )

    return null
}

// looks for maven credentials in properties and sets them
// up for publish on central plugin because by default it only
// looks for them in the environment
fun Project.loadMavenCredentialsForPublishingOnCentralPlugin(): Pair<String, String>? {
    listOf("mavenCentralPortalUsername", "mavenCentralPortalPassword").forEach { key ->
        if (!project.hasProperty(key)) {
            localProps.getProperty(key)?.let { project.extensions.extraProperties.set(key, it) }
        }
    }

    val username = getAnyCredentialOf(
        "MAVEN_CENTRAL_PORTAL_USERNAME",
        "MAVEN_CENTRAL_USERNAME",
        "mavenCentralPortalUsername",
        "sonatypeUsername",
        "ossrhUsername"
    )

    val password = getAnyCredentialOf(
        "MAVEN_CENTRAL_PORTAL_PASSWORD",
        "MAVEN_CENTRAL_PASSWORD",
        "mavenCentralPortalPassword",
        "sonatypePassword",
        "ossrhPassword"
    )

    if (username == null || password == null) {
        println(
            buildString {
                appendLine("Could not resolve Maven Central credentials for project '${project.name}'.")
                appendLine("Expected variables or properties (any one of the alternatives):")
                appendLine("  Environment:")
                appendLine("    MAVEN_CENTRAL_PORTAL_USERNAME / MAVEN_CENTRAL_PORTAL_PASSWORD")
                appendLine("  Fallback environment:")
                appendLine("    MAVEN_CENTRAL_USERNAME / MAVEN_CENTRAL_PASSWORD")
                appendLine("  Gradle properties:")
                appendLine("    mavenCentralPortalUsername / mavenCentralPortalPassword")
                appendLine("  Legacy Sonatype properties:")
                appendLine("    sonatypeUsername / sonatypePassword")
                appendLine("    ossrhUsername / ossrhPassword")
            }
        )
        return null
    }

    return username to password
}

fun String?.trimAndNullIfEmpty(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
fun getenv(name: String): String? = System.getenv(name)?.trimAndNullIfEmpty()
fun Project.projectProp(name: String): String? = (findProperty(name) as? String).trimAndNullIfEmpty()