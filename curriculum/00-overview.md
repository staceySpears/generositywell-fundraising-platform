# 00 — Curriculum Overview

Read this file first, every time you start a new session.

---

## What this curriculum is

This is a guided refactor of a real Java codebase — your own academic capstone — into a
production-quality, cloud-native application. Every module covers one concept, walks through
actual code from this repo, and ends with a hands-on task or exercise.

The goal is not just to have a working app at the end. The goal is that you can sit in a
technical interview, point at any file in this repo, and explain exactly why it is written
the way it is, what tradeoff it represents, and what you would change if the requirements shifted.

That is what separates a portfolio project from homework.

---

## What you are building

**GenerosityWell** is a hyperlocal, community-first fundraising platform for neighborhood groups,
local schools, and grassroots nonprofits. It has three differentiating features that no existing
SaaS tool handles in one place:

1. **Unified volunteer + donor coordination** — contributors can give time or money through the same platform
2. **Campaign transparency** — organizers post impact updates; donors see exactly what their contribution did
3. **Enterprise back-office via Salesforce** — rather than rebuilding a CRM, the platform syncs into Salesforce so organizers get a 360° view of community engagement; Agentforce drafts impact updates automatically

The technical architecture reflects the product split:
- **Public interface** — Spring Boot 3 / Java 21 REST API + React + Vite SPA, hosted on AWS
- **System of record** — Salesforce, synced via REST API
- **AI transparency layer** — Salesforce Data Cloud + Agentforce (Phase 4)

---

## Where this codebase came from

This project started as a team academic capstone at Kenzie Academy (2022). It worked, but it had
real problems: SDK v1 dependencies that were already end-of-life, a Lambda proxy layer that added
latency without adding value, broken repository interfaces that imported a library not declared in
the build, known bugs in `EventService`, and capstone scaffolding files that were never cleaned up.

The refactor treats those problems honestly. We do not pretend the original code was production-ready.
We document what was wrong, why it was wrong, and how we fixed it. That is a stronger portfolio
story than if it had been clean to begin with.

---

## How to use the curriculum

Each module covers one concept. The structure within every module is the same:

1. **Why this exists** — the problem being solved and why this solution was chosen over alternatives
2. **The code** — annotated walkthrough of actual files in this repo
3. **What to understand** — the questions you should be able to answer before moving on
4. **Exercise or next step** — either a link to `curriculum/exercises/` or the next module

Work through them in order. Do not skip the "Why this exists" section, even if the code looks
familiar. The code is the easy part. The reasoning is what gets tested in interviews.

---

## The full arc

| Module | Topic | Phase | Status |
|---|---|---|---|
| 01 | Spring Boot 3 upgrade, Java 21, Gradle 8 | 1 | ✅ Complete |
| 02 | Gradle multi-module builds | 1 | ✅ Complete |
| 03 | AWS SDK v2 migration | 1 | ✅ Complete |
| 04 | Removing the Lambda proxy | 1 | ✅ Complete |
| 05 | Global exception handling | 1 | ✅ Complete |
| 06 | Domain modeling and naming | 2 | 🔄 In Progress |
| 07 | Caffeine cache | 2 | 🔄 In Progress |
| 08 | Bean validation | 2 | 🔄 In Progress |
| 09 | Campaign entity and status lifecycle | 3 | Planned |
| 10 | Spring Security and JWT auth | 3 | Planned |
| 11 | Salesforce integration | 3 | Planned |
| 12 | Stripe integration | 4 | Planned |
| 13 | Lambda webhooks (intentional) | 4 | Planned |
| 14 | Impact reporting | 4 | Planned |
| 15 | Agentforce and Data Cloud | 4 | Planned |
| 16 | CI/CD pipeline | 5 | Planned |
| 17 | Observability | 5 | Planned |

---

## Before you open any code

Make sure you can answer these questions about the current state of the repo:

- What does `./gradlew :Application:compileJava` do, and why does it compile more than just the Application module?
- What is `JAVA_HOME` and why does this project require it to point to Java 21 specifically?
- What was the Lambda layer doing before it was removed, and why was removing it the right call?
- What is a DAO, and how is `EventDao` different from the old `EventRepository`?

If you cannot answer all four, start at Module 01.
