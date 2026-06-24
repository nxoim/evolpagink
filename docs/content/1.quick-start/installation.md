---
title: Installation
description: ""

navigation:
  icon: i-lucide-download
seo:
  title: evolpagink Installation
---
**Platforms:** <u-badge label="Android" variant="outline" color="neutral"></u-badge> <u-badge label="iOS" variant="outline" color="neutral"></u-badge> <u-badge label="macOS" variant="outline" color="neutral"></u-badge> <u-badge label="JVM" variant="outline" color="neutral"></u-badge> <u-badge label="JS" variant="outline" color="neutral"></u-badge> <u-badge label="WasmJS" variant="outline" color="neutral"></u-badge>


::code-group

```toml [libs.versions.toml]
[versions]
evolpagink = "0.13.0"

[libraries]
evolpaginkCompose = { module = "com.nxoim.evolpagink:compose", version.ref = "evolpagink" }
evolpaginkCore = { module = "com.nxoim.evolpagink:core", version.ref = "evolpagink" }
```

```kotlin [build.gradle.kts]
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Compose Multiplatform / Jetpack Compose — includes core automatically
            implementation(libs.evolpagink.compose) 

            // DIY bindings — core is framework-agnostic, no UI dependency
            implementation(libs.evolpagink.core) 
        }
    }
}
```
::

## Choosing the right module

`core` - The pagination engine itself. No UI framework dependency. Use this if you're integrating with a non-Compose UI system or writing custom bindings.

`compose` - Compose-specific extensions that provide `toState()` and `items()` overloads. Includes core automatically, so you only need this dependency if you're using Compose.
