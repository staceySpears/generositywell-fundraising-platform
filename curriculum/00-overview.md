# 00 — Curriculum Overview

Read this file first, every time you start a new session.

---

## What this curriculum is

This is a guided refactor of a real Java codebase — originally created as a collaborative academic
capstone in 2022 — into a modern portfolio application. Independent modernization began in March
2026 and continues to the present. Every module covers one concept, walks through actual code from
this repo, and ends with a hands-on task or exercise. The repository demonstrates engineering
decisions; it does not claim production readiness or live nonprofit operation.

The goal is not just to have a working app at the end. The goal is that you can sit in a
technical interview, point at any file in this repo, and explain exactly why it is written
the way it is, what tradeoff it represents, and what you would change if the requirements shifted.

That is what separates a portfolio project from homework.

---

## What you are building

**GenerosityWell** is a hyperlocal, community-first fundraising platform for neighborhood groups,
local schools, and grassroots nonprofits. Its implemented and planned product themes are:

1. **Unified volunteer + donor coordination** — contributors can give time or money through the same platform
2. **Campaign transparency (planned)** — reviewed impact updates would connect contributions to outcomes
3. **Salesforce portfolio extension** — an implemented REST seam is the starting point for planned, source-controlled CRM configuration and reporting

The current technical boundary is:

- **Implemented application** — Spring Boot 3/Java 21 REST API and React/Vite SPA; no public deployment is asserted
- **Implemented CRM seam** — disabled-by-default, one-way asynchronous REST writes to Salesforce Campaign, Opportunity, and Contact
- **Planned portfolio metadata** — Salesforce configuration, approval Flow, reports, dashboards, and synthetic demo scripts
- **Planned/optional AI layer** — Agentforce drafts with mandatory human review; Data Cloud only if org capability is verified

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

## Status vocabulary

- **Implemented** — code or configuration is present in the repository.
- **Validated** — the implementation also has automated or reproducible evidence; this is
  not the same as production validation.
- **Planned** — the item is not implemented.
- **Optional/blocked by org capability** — Salesforce licenses or features are unconfirmed.

## The full arc

| Module | Topic                                            | Phase | Status                                                    |
| ------ | ------------------------------------------------ | ----- | --------------------------------------------------------- |
| 01     | Spring Boot 3 upgrade, Java 21, Gradle 8         | 1     | **Implemented**                                           |
| 02     | Gradle multi-module builds                       | 1     | **Implemented**                                           |
| 03     | AWS SDK v2 migration                             | 1     | **Implemented**                                           |
| 04     | Removing the Lambda proxy                        | 1     | **Implemented**                                           |
| 05     | Global exception handling                        | 1     | **Implemented**                                           |
| 06     | Domain modeling and naming                       | 2     | **Implemented** (legacy modules remain)                   |
| 07     | Caffeine cache                                   | 2     | **Implemented**                                           |
| 08     | Bean validation                                  | 2     | **Implemented**                                           |
| 09     | Campaign entity and status lifecycle             | 3     | **Implemented**                                           |
| 10     | Spring Security and JWT auth                     | 3     | **Implemented**; production hardening planned             |
| 11     | Salesforce REST seam                             | 3     | **Implemented**; actual-org validation planned            |
| 12     | Stripe integration                               | 4     | **Implemented**; production operation not validated       |
| 13     | Lambda webhooks (intentional)                    | 4     | **Planned**                                               |
| 14     | Impact reporting                                 | 4     | **Planned**                                               |
| 15     | Agentforce and Data Cloud                        | 4     | **Planned** / **Optional/blocked by org capability**      |
| 16     | CI checks and future deployment pipeline         | 5     | **Validated** for CI checks; deployment planned           |
| 17     | Observability dependencies and future operations | 5     | **Implemented** foundation; deployment validation planned |

---

## Before you open any code

Make sure you can answer these questions about the current state of the repo:

- What does `./gradlew :Application:compileJava` do, and why does it compile more than just the Application module?
- What is `JAVA_HOME` and why does this project require it to point to Java 21 specifically?
- What was the Lambda layer doing before it was removed, and why was removing it the right call?
- What is a DAO, and how is `EventDao` different from the old `EventRepository`?

If you cannot answer all four, start at Module 01.
