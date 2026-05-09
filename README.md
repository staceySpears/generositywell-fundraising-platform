# GenerosityWell

## Overview

GenerosityWell is a **hyperlocal, community-first** fundraising platform designed to empower informal neighborhood groups, local schools, and grassroots nonprofits. Unlike traditional SaaS tools that treat giving time and giving money as separate workflows, GenerosityWell provides a unified hub for **volunteer and donor coordination**, allowing communities to track both financial and sweat-equity contributions in one place.

At its core, the platform is built to eliminate friction and build trust through radical **campaign transparency**. By closing the loop with structured impact reporting, GenerosityWell ensures that every contributor sees exactly how their hours or dollars drove real-world change.

---

## Architecture

### Current State

```mermaid
flowchart TD
    A[Frontend\nReact + Vite\nDeployed: AWS S3 + CloudFront] -->|REST / Axios| B[Application\nSpring Boot 3 · Java 21]

    B -->|Caffeine\nin-memory cache| B
    B -->|AWS SDK v2\nEnhanced Client| D[(DynamoDB)]

    B -->|Metrics| E[Micrometer]
    E --> F[Prometheus]
    E --> G[AWS CloudWatch]

    style A fill:#fef3c7,stroke:#d97706
    style B fill:#dbeafe,stroke:#3b82f6
    style D fill:#fef9c3,stroke:#eab308
    style E fill:#f3e8ff,stroke:#a855f7
    style F fill:#f3e8ff,stroke:#a855f7
    style G fill:#f3e8ff,stroke:#a855f7
```

### Target State (Phase 3 & 4)

```mermaid
flowchart TD
    A[Frontend\nReact + Vite\nDeployed: AWS S3 + CloudFront] -->|REST / Axios| B[Application\nSpring Boot 3 · Java 21]

    B -->|Caffeine\nin-memory cache| B
    B -->|AWS SDK v2\nEnhanced Client| D[(DynamoDB)]
    B -->|REST API| SF[Salesforce\nSystem of Record]
    SF -->|Data Cloud| DC[Salesforce Data Cloud\n360° Donor View]
    DC -->|Agentforce| AI[AI Impact\nUpdate Agent]

    ST[Stripe\nWebhook] -->|Event| L[AWS Lambda\nPayment Handler]
    L --> D
    L --> SF

    B -->|Metrics| E[Micrometer]
    E --> F[Prometheus]
    E --> G[AWS CloudWatch]

    style A fill:#fef3c7,stroke:#d97706
    style B fill:#dbeafe,stroke:#3b82f6
    style D fill:#fef9c3,stroke:#eab308
    style SF fill:#dbeafe,stroke:#0070d2
    style DC fill:#dbeafe,stroke:#0070d2
    style AI fill:#dbeafe,stroke:#0070d2
    style ST fill:#dcfce7,stroke:#22c55e
    style L fill:#dcfce7,stroke:#22c55e
    style E fill:#f3e8ff,stroke:#a855f7
    style F fill:#f3e8ff,stroke:#a855f7
    style G fill:#f3e8ff,stroke:#a855f7
```

---

## Purpose of the Platform & Architecture

This project demonstrates how a modern, cloud-native application can seamlessly integrate with enterprise CRM systems to solve complex business problems. A community platform requires a lightweight, low-friction experience for its end users, but organizers still need robust, enterprise-grade tools to manage the back office. GenerosityWell bridges this gap by separating the public-facing transaction layer from the secure management layer:

- **The Public Interface (Java / Spring Boot / AWS):** A robust, observable API layer and a modern React SPA handle the hyperlocal community experience — processing low-latency Stripe donations, capturing volunteer RSVPs, and displaying public impact reports.
- **The System of Record (Salesforce Integration):** Rather than rebuilding generic CRM features from scratch, the platform syncs all transactional and user data directly into Salesforce via REST API. Salesforce serves as the single source of truth where organizers manage donor relationships and track campaign health.
- **AI-Driven Transparency (Roadmap — Data Cloud & Agentforce):** Future phases will unify donation data in Salesforce Data Cloud to create a 360-degree view of community engagement. This foundation will enable a custom Agentforce agent to automatically draft and propose "Impact Updates" based on real-time campaign data, fulfilling the platform's core mission of transparency with zero administrative overhead.

---

## Project Structure

| Module | Responsibility |
|---|---|
| `Application` | Core Spring Boot API. Handles routing, request validation, Caffeine caching, and observability. Connects directly to DynamoDB via AWS SDK v2 Enhanced Client. |
| `ServiceLambda` | Reserved for Phase 4 — will house the Stripe payment webhook handler. Processes incoming Stripe events asynchronously and writes donation records to DynamoDB and Salesforce. |
| `ServiceLambdaModel` | Shared domain models used across the Lambda boundary. Will be slimmed down to webhook-specific DTOs in Phase 4. |
| `ServiceLambdaJavaClient` | Removed in Phase 1. The Spring Boot app connects directly to DynamoDB via the Enhanced Client — no Lambda proxy. Module retained in the repo tree but excluded from Application dependencies. |
| `Frontend` | React + Vite SPA. Component-based UI consuming the Spring Boot REST API via Axios. |
| `IntegrationTests` | Cross-module integration test suites backed by Testcontainers. |
| `Utilities` | Shared helper functions and build configurations used across the project. |

---

## Frontend

### Stack

| Tool | Role |
|---|---|
| React 18 | Component-based UI |
| Vite | Replaces Webpack 4; fast dev server with HMR, optimized production builds |
| React Router v6 | Client-side routing (replaces 8 separate HTML files) |
| Axios | HTTP client consuming the Spring Boot REST API (updated from 0.21.x) |
| TanStack Query | Server-state management — caching, background refetch, stale-data invalidation for live donation totals and campaign status *(Phase 3)* |
| React Hook Form + Zod | Client-side form validation mirroring Spring Boot Bean Validation; Zod schemas shared with the future React Native app *(Phase 3)* |
| Radix UI | Headless, accessible UI primitives (modals, dropdowns, accordions) compatible with CSS Modules *(Phase 3)* |
| CSS Modules | Scoped per-component styles (replaces 6 scattered global CSS files) |
| vite-plugin-pwa | Web app manifest + service worker; enables "Add to Home Screen" on any device *(Phase 4)* |

### Pages / Routes

| Route | Component | Access |
|---|---|---|
| `/` | `LandingPage` | Public |
| `/login` | `LoginPage` | Public |
| `/register` | `RegisterPage` | Public |
| `/forgot-password` | `ForgotPasswordPage` | Public |
| `/search` | `SearchPage` | Public |
| `/campaigns` | `CampaignsPage` | Public |
| `/campaigns/:id` | `CampaignDetailPage` | Public |
| `/dashboard` | `DashboardPage` | Authenticated |
| `/events` | `EventsPage` | Authenticated |
| `/calendar` | `CalendarPage` | Authenticated |

### Local Development

```bash
cd Frontend
npm install
npm run dev
```

The Vite dev server proxies `/api/*` to `http://localhost:5001`, matching the Spring Boot dev port.

### Build

```bash
npm run build
```

Output is written to `Frontend/dist/` and can be deployed to any static host (AWS S3 + CloudFront recommended).

---

## Building & Running (Backend)

### Local Development

**Prerequisites:**
- Java 21
- Gradle 8.7+
- Docker Desktop (or equivalent container runtime)

**Step 1 — Start local DynamoDB**

```bash
./local-dynamodb.sh
```

**Step 2 — Build and run the Spring Boot application**

```bash
./gradlew :Application:bootRunDev
```

**Step 3 — Explore the API**

The OpenAPI UI is auto-generated and available at: `http://localhost:5001/swagger-ui.html`

> Note: the production profile runs on port 5000.

### Building for Deployment

```bash
./gradlew build
```

---

## Testing

**Unit Tests:** Isolated service logic validation using JUnit and Mockito.

**Integration Tests:** A custom `ApplicationContextInitializer` (`DynamoDbInitializer`) uses Testcontainers to spin up an ephemeral `amazon/dynamodb-local` container, dynamically injecting the mapped port into the Spring context before test startup. This ensures reliable cross-module API testing without requiring a live AWS environment.

```bash
./gradlew :IntegrationTests:test
```

---

## Roadmap

| Phase | Focus | Status |
|---|---|---|
| **Phase 1 — Architecture Stabilization** | Spring Boot 3 / Java 21, AWS SDK v2, direct DynamoDB access, global exception handling, React + Vite frontend | ✅ Complete |
| **Phase 2 — Domain Rename & Model Cleanup** | Rename capstone entities to platform domain, eliminate duplicate models, add Bean Validation, proper date types | 🔄 In Progress |
| **Phase 3 — Feature Completion** | Campaign lifecycle, JWT auth / Spring Security, volunteer RSVP, Salesforce integration, frontend auth + TanStack Query + React Hook Form + Zod + Radix UI | Planned |
| **Phase 4 — Platform Enhancements** | Stripe donations, Lambda webhook handler, impact reporting, donor dashboard, PWA, Salesforce Data Cloud + Agentforce | Planned |
| **Phase 5 — Production Hardening** | CI/CD pipeline, S3 + CloudFront deploy, E2E tests, rate limiting, API Gateway | Planned |
| **Phase 6 — Mobile** | React Native + Expo donor/volunteer app; Salesforce Lightning Web Components for organizer mobile | Planned |

### Phase 1 — Architecture Stabilization ✅

- Spring Boot 3.2.5, Java 21, Gradle 8.7 upgrade
- AWS SDK v2 migration across all modules
- Direct DynamoDB access via Enhanced Client (removed Lambda proxy layer)
- Multi-module Gradle structure established
- Docker-based local DynamoDB dev infrastructure
- Frontend migrated from Webpack 4 / vanilla JS to React + Vite
- Global exception handling with `@RestControllerAdvice`

### Phase 2 — Domain Rename & Model Cleanup 🔄

- Rename `Customer` → `Attendee`; clarify `User` / `Organizer` / `Donor` roles
- Eliminate duplicate models between `Application` and `ServiceLambdaModel`
- Replace broken Spring Data DynamoDB repos with Enhanced Client DAOs
- Add `@Valid` + `@NotBlank` / `@NotNull` to all request DTOs
- Migrate `date` fields from `String` to `LocalDate`
- Migrate `CacheStore` from Guava to Caffeine; unify to a single typed cache

### Phase 3 — Feature Completion

- `Campaign` entity with goal, timeline, and status lifecycle (`DRAFT → ACTIVE → CLOSED`)
- `FundraisingEvent` linked to a Campaign
- Volunteer RSVP flow — attendees can commit time, not just money
- JWT auth / Spring Security — register, login, role-based access (`ORGANIZER`, `DONOR`)
- Salesforce REST API integration — sync users, campaigns, and donations as they are created
- Frontend auth flow — login, register, protected routes, Axios interceptors
- **TanStack Query** — server-state management; replaces `useEffect` data-fetching with automatic caching, background refetch, and stale-data invalidation (critical when Stripe webhooks or Agentforce updates change donation totals asynchronously)
- **React Hook Form + Zod** — client-side form validation mirroring the Spring Boot `@Valid` / Bean Validation layer; Zod schemas are shared between the campaign creation form, RSVP flow, and the future React Native app
- **Radix UI** — headless, fully accessible UI primitives (modals, dropdowns, accordions) that work with CSS Modules without imposing a design system

### Phase 4 — Platform Enhancements

- `Donation` entity backed by Stripe PaymentIntent API
- AWS Lambda Stripe webhook handler — processes payment events asynchronously
- Structured impact reporting — organizers post updates; donors see their contribution's effect
- Donor dashboard — giving history, volunteer hours, campaigns followed
- **Progressive Web App (PWA)** — `vite-plugin-pwa` adds a web app manifest and service worker to the existing React/Vite SPA; donors and volunteers can "Add to Home Screen" on any device without an app store; eliminates friction for one-time contributors
- Salesforce Data Cloud unification — 360° view of community engagement per organizer
- Agentforce agent — auto-drafts Impact Update posts from real-time campaign data

### Phase 5 — Production Hardening

- ✅ GitHub Actions CI — build and unit tests run on every PR and push to main
- AWS S3 + CloudFront for frontend hosting
- API Gateway — rate limiting, CORS, auth header validation
- Playwright or Cypress E2E tests covering the critical donor flow
- CloudWatch dashboards wired via Micrometer for key platform metrics

### Phase 6 — Mobile

Two tracks targeting distinct user groups — built to share maximum logic with the existing web frontend.

**Donor & Volunteer App (React Native + Expo)**
- Expo abstracts iOS (Xcode) and Android (Android Studio) build complexity
- Reuses the Zod validation schemas, Axios API client, and JWT auth flow from the web frontend
- TanStack Query for server-state management is identical API surface on React Native
- Core flows: browse campaigns, donate (Stripe), volunteer RSVP, push notifications for campaign milestones

**Organizer App (Salesforce Lightning Web Components)**
- Organizers already live in Salesforce — no separate app download required
- Custom LWCs expose campaign health, donation velocity, and volunteer pipeline directly in the Salesforce Mobile App
- As Data Cloud and Agentforce integrations land (Phase 4), LWC surfaces the AI-drafted Impact Updates for organizer review and one-tap publish
- Zero additional deployment infrastructure: LWCs deploy as part of the Salesforce org
