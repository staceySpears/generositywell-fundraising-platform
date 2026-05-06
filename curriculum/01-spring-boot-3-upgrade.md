# 01 — Spring Boot 3 Upgrade: Java 21, Gradle 8, and the javax→jakarta Rename

## Why this exists

Spring Boot 2 reached end-of-life in November 2023. The capstone was built on `2.6.3`, which
means no security patches, no bug fixes, and increasing incompatibility with modern libraries.
Staying on it was not an option for a production-quality portfolio project.

Spring Boot 3 requires Java 17 minimum. We went to Java 21 because it is the current LTS
(Long-Term Support) release — the version that will be supported in production environments
for years. Java 21 also brings virtual threads (Project Loom), records, pattern matching, and
sealed classes. We are not using all of those yet, but we are on the platform that supports them.

Gradle 7 had compatibility issues with Java 21 and the newer plugin ecosystem. Gradle 8.7 is
the stable release that supports everything we need.

The most disruptive change in Spring Boot 3 is the `javax` → `jakarta` namespace rename.
When Jakarta EE 9 was released, the entire `javax.*` package namespace was renamed to `jakarta.*`.
Spring Boot 3 is built on Jakarta EE 9+, so every annotation you used before — `@NotNull`,
`@NotEmpty`, `@Valid`, persistence annotations — changed package. Code that compiled fine on
Spring Boot 2 breaks immediately on Spring Boot 3 with `cannot find symbol` errors pointing
at `javax.*` imports.

---

## The code

### `Application/build.gradle` — the three plugin changes

```groovy
plugins {
    id 'java'                                               // (1)
    id 'org.springframework.boot' version '3.2.5'          // (2)
    id 'io.spring.dependency-management' version '1.1.4'   // (3)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
```

**(1)** `id 'java'` — In a standard single-module Spring Boot project, the Spring Boot plugin
applies the Java plugin automatically. In our Gradle multi-module setup, Gradle evaluates all
subprojects during a build, and the Java plugin was not being applied to `Application` explicitly.
Without it, the `implementation` dependency configuration does not exist, and every line in the
`dependencies` block fails with `Could not find method implementation()`. Adding it explicitly
is the safe, unambiguous fix.

**(2)** `3.2.5` — A specific Spring Boot version pinned explicitly so the build is reproducible.
The `io.spring.dependency-management` plugin uses this to manage all Spring library versions as
a BOM (Bill of Materials), meaning you do not specify versions for most Spring dependencies.

**(3)** `1.1.4` — the dependency management plugin version. This was also updated from the
capstone version to match Spring Boot 3's expectations.

---

### The `javax` → `jakarta` fix

Every DTO and model class in the capstone that used validation annotations had this import:

```java
// Before — Spring Boot 2 / javax
import javax.validation.constraints.NotEmpty;
```

After the Spring Boot 3 upgrade, this import resolves to nothing. The package was renamed:

```java
// After — Spring Boot 3 / jakarta
import jakarta.validation.constraints.NotEmpty;
```

This affected five files: `CreateEventRequest`, `EventUpdateRequest`, `CreateUserRequest`,
`UserUpdateRequest`, and `Customer`. The compiler error was:

```
error: package javax.validation.constraints does not exist
error: cannot find symbol — @NotEmpty
```

The fix is mechanical — every `javax.` import becomes `jakarta.` — but you need to know *why*
it happened to explain it in an interview without hesitation.

---

### The Gradle 8 `Report` API break

The capstone's `buildSrc/src/main/groovy/ata-curriculum.snippets-conventions.gradle` file was
written for Gradle 7. It configures JaCoCo and SpotBugs reporting using an API that was removed
in Gradle 8:

```groovy
// Before — Gradle 7 API (removed in Gradle 8)
reports {
    xml.enabled true
    html.enabled true
}
```

In Gradle 8, `Report.enabled` was replaced with `Report.required`:

```groovy
// After — Gradle 8 API
reports {
    xml.required = true
    html.required = true
}
```

This affected two blocks in the plugin file: the JaCoCo report configuration and the SpotBugs
report configuration. The error was:

```
Could not find method enabled() for arguments [true] on Report xml
```

---

## What to understand

Before moving on, you should be able to answer:

1. Why does Spring Boot 3 require `jakarta.*` instead of `javax.*`?
2. What does the `io.spring.dependency-management` plugin do, and why does it mean you don't
   specify versions for Spring Boot starters?
3. What is a LTS Java release and why does it matter for production software?
4. Why did `id 'java'` need to be added explicitly to `Application/build.gradle` when it was
   not needed before?

---

## Next

[02 — Gradle Multi-Module Builds](02-gradle-multimodule.md)
