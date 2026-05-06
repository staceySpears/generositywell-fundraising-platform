# 02 — Gradle Multi-Module Builds

## Why this exists

A multi-module Gradle build is how you enforce separation of concerns at the *build* level, not
just the *code* level. Without it, you can import anything from anywhere and nothing stops you.
With it, a module can only use code from modules it explicitly declares as dependencies — the
build fails otherwise.

The capstone used this structure from the start. The modules are:

| Module | What it does |
|---|---|
| `Application` | Spring Boot REST API — your controllers, services, DAOs, and config |
| `ServiceLambda` | AWS Lambda functions — currently dormant, will become the Stripe webhook handler in Phase 4 |
| `ServiceLambdaModel` | Shared DTOs across the Lambda boundary |
| `ServiceLambdaJavaClient` | HTTP client the Spring app used to call the Lambda — removed in Phase 1 |
| `Frontend` | React + Vite SPA |
| `IntegrationTests` | Testcontainers-backed integration test suite |
| `Utilities` | Shared build helpers and Jacoco config |

The key insight: **modules enforce contracts**. When `ServiceLambdaJavaClient` was a dependency
of `Application`, anything in `ServiceLambdaJavaClient` was available to your Spring app. When
we removed that dependency, the contract was severed — the compiler immediately told us every
place `LambdaServiceClient` was still referenced. The multi-module structure made a major
architectural change visible and safe.

---

## The code

### `settings.gradle` — declaring what exists

```groovy
rootProject.name = 'kenzie-capstone-project'
include(':Frontend')
include(':Application')
include(':Utilities')
include(':IntegrationTests')
include(':ServiceLambdaModel')
include(':ServiceLambdaJavaClient')
include(':ServiceLambda')
```

This file tells Gradle which directories are modules. Every included module must have its own
`build.gradle`. If a directory is not listed here, Gradle does not know it exists.

Note: `rootProject.name` is still `kenzie-capstone-project` from the original capstone. This
is a cosmetic issue — the artifact name — and does not affect runtime behavior. It can be renamed
in a later cleanup pass.

---

### `Application/build.gradle` — declaring what Application can use

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'software.amazon.awssdk:dynamodb-enhanced'
    implementation 'software.amazon.awssdk:dynamodb'
    // ...
}
```

Notice what is *not* here: `implementation project(":ServiceLambdaModel")` and
`implementation project(":ServiceLambdaJavaClient")`. These were removed in Phase 1 when
the Lambda proxy was eliminated. Before removal, Application could use any class from those
modules. After removal, any remaining references were compile errors — which is exactly how
we found and cleaned up the last Lambda dependencies.

---

### `buildSrc/` — convention plugins

The `buildSrc/` directory is Gradle's way of sharing build logic across modules. Code in
`buildSrc/` is compiled before the main build runs, and its plugins are available to all
`build.gradle` files without any import.

The capstone's `buildSrc/` contains:
- `ata-curriculum.java-conventions.gradle` — base Java config, JUnit setup, common dependencies
- `ata-curriculum.snippets-conventions.gradle` — adds Checkstyle, JaCoCo, SpotBugs on top

These convention plugins are applied by `ServiceLambda`, `ServiceLambdaModel`, `ServiceLambdaJavaClient`,
`IntegrationTests`, and `Utilities` — but *not* by `Application`. Application has its own
complete `build.gradle` because it is a Spring Boot project with different requirements.

The bug we fixed in Module 01 (`xml.enabled` → `xml.required`) was in one of these convention
plugin files. Because all the Lambda-side modules apply it, fixing it in one place fixed all
of them simultaneously. That is the point of convention plugins.

---

## What to understand

1. What is the difference between `settings.gradle` and `build.gradle`?
2. If you add a class to `ServiceLambdaModel` but `Application` doesn't declare it as a
   dependency, what happens when `Application` tries to import that class?
3. Why is `buildSrc/` compiled before the main build, and why does that matter?
4. We removed `ServiceLambdaJavaClient` from `Application`'s dependencies. What would happen
   if we had missed a reference to `LambdaServiceClient` somewhere in `Application`?

---

## Next

[03 — AWS SDK v2 Migration](03-aws-sdk-v2-migration.md)
