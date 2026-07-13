# CLAUDE.md — GenerosityWell Fundraising Platform

## Project Overview

**GenerosityWell** is a full-stack fundraising platform (portfolio project) built on top of a
Kenzie Academy team Java capstone (2022). The repository contains substantial independent
modernization and fundraising-domain work alongside legacy capstone modules.

**This is a portfolio project.** Goal: demonstrate real-world backend engineering judgment.

---

## Evidence and Representation Guardrails

- Original project: Kenzie Academy/SNHU **team capstone (2022)**. Do not imply sole authorship.
- Independent modernization: **March 2026–Present**, attributed from repository history.
- **Implemented** means code/configuration exists; **Validated** requires automated or
  reproducible evidence; **Planned** is not built; **Optional/blocked by org capability**
  depends on unconfirmed Salesforce features or licenses.
- Do not claim production deployment, nonprofit adoption, real donor volume, business
  outcomes, NPSP/Nonprofit Cloud compatibility, or org validation without direct evidence.
- Use synthetic data only. Never store real donor, payment, volunteer, or beneficiary data.
- Agentforce, Data Cloud, Salesforce impact metadata, and impact reporting are planned.
  Any future AI draft requires human review before publication.

## Current Repository Stack (implemented — check validation separately)

| Layer       | Technology                                                                                     |
| ----------- | ---------------------------------------------------------------------------------------------- |
| REST API    | Spring Boot 3.2.5 / Java 21                                                                    |
| Persistence | AWS DynamoDB Enhanced Client (SDK v2)                                                          |
| Caching     | Caffeine in-memory cache (campaign reads)                                                      |
| Auth        | JWT (JwtAuthenticationFilter + SecurityConfig, stateless)                                      |
| Payments    | Stripe PaymentIntent + PaymentElement + Spring webhook (implemented; not production-validated) |
| CRM seam    | One-way async Salesforce REST writes to Campaign, Opportunity, and Contact (not org-validated) |
| Audit       | Append-only AuditLogTable (dual GSIs: entityId, actorId)                                       |
| Frontend    | React 18 + Vite, TanStack Query, react-hook-form + Zod, Radix UI                               |
| CI          | GitHub Actions (Build & Unit Tests, Frontend Lint & Format)                                    |
| Code review | CodeRabbit bot on every PR                                                                     |
| Build       | Gradle 8.7 multi-module                                                                        |

---

## Collaboration Rules (always follow these)

- **Never commit directly to main** — all changes go through a PR
- Always run `./gradlew :Application:test` and `npm run lint && npm run build` before pushing
- Wait for CI green + CodeRabbit review complete before merging
- Run `./cr-fix.sh` (or `/cr-fix` inside Claude Code) BEFORE merging — not after
  (GitHub auto-resolves CodeRabbit threads on merge even if unfixed)

---

## Repository Structure

```text
generositywell-fundraising-platform/
├── Application/src/main/java/com/kenzie/appserver/
│   ├── config/           — CacheStore (Caffeine), DynamoDbConfig, AsyncConfig, StripeConfig
│   ├── controller/       — REST controllers + request/response models
│   │   └── model/        — *Request, *Response POJOs (incl. DonationSummaryResponse)
│   ├── repositories/     — DAOs (CampaignDao, FundraisingEventDao, UserDao, AuditLogDao)
│   │   └── model/        — DynamoDB @DynamoDbBean records + type converters
│   ├── salesforce/       — SalesforceService, SalesforceClient, SalesforceTokenService
│   │                       (async one-way REST sync; no durable retry/outbox)
│   ├── security/         — JwtAuthenticationFilter, JwtUtil, SecurityConfig
│   └── service/          — Business logic (CampaignService, FundraisingEventService,
│                           UserService, AuditLogService, StripeService)
│       └── model/        — Domain objects (User, Supporter, Volunteer, enums)
├── Application/src/test/ — CampaignServiceTest, UserServiceTest (Mockito)
├── Frontend/src/
│   ├── api/              — campaignApi, eventApi, userApi, client (Axios, timeout:10s)
│   ├── components/       — Layout (nav + outlet), ProtectedRoute
│   ├── context/          — AuthContext (JWT in localStorage, cross-tab sync)
│   ├── hooks/            — useAuth
│   ├── pages/            — CampaignDetailPage, CampaignsPage, DashboardPage,
│   │                       EventsPage, CalendarPage, LoginPage, RegisterPage,
│   │                       ForgotPasswordPage (placeholder), SearchPage (placeholder)
│   └── schemas/          — auth.js (loginSchema, registerSchema, profileEditSchema)
│                           campaign.js (donationSchema)
├── AuditLogTable.yml     — CloudFormation: AuditLog table (entityId GSI, actorId GSI)
├── FundraisingEventsTable.yml
├── UsersTable.yml        — TABLE_NAME = "users" (lowercase)
├── Application-template.yml
├── cr-fix.sh             — poll for CodeRabbit + launch autofix (run after git push)
└── .claude/commands/cr-fix.md  — /cr-fix slash command for inside Claude Code
```

---

## Key Architectural Decisions

- **Money always in cents (Long)** — never double/float. Display formatting in frontend only.
- **Path variable is source of truth on PUT** — controller overwrites body id from path
- **JWT subject overrides client-supplied identity** — RSVP volunteerId locked to auth principal
- **Owner immutability** — campaign/event creator cannot be changed after creation
- **`Objects.equals()` for null-safe ownership** checks throughout
- **DynamoDB table name is "users" (lowercase)** — must match UsersTable.yml exactly
- **Supporter serialization v2** — pipe+URL-encode: `id|name|email|amountInCents|donationDate`
  Backward-compat read of legacy `id x name x email` format (v1)
- **addDonation(campaignId, amountInCents, donorId)** — donorId nullable; null on webhook path
- **GET /users/{id}/donations and /rsvps require JWT + self-only** (403 if wrong user)
- **RSVP history strips other attendees' PII** — only caller's own volunteer entry returned
- **Async audit logging** for non-financial events; synchronous for PAYMENT_SUCCEEDED

---

## Current Endpoints

### Campaigns (`/campaigns`)

- `GET /campaigns/all` — public
- `GET /campaigns/{id}` — public
- `POST /campaigns` — auth required
- `PUT /campaigns/{campaignId}` — auth, owner only
- `DELETE /campaigns/{campaignId}` — auth, owner only
- `POST /campaigns/{id}/donate` — public (donorId from JWT if present)
- `POST /campaigns/{id}/close` — auth, owner only
- `POST /campaigns/{id}/payment-intent` — auth

### Events (`/events`)

- `GET /events` — public
- `GET /events/{id}` — public
- `POST /events` — auth
- `PUT /events/{id}` — auth, organizer only
- `DELETE /events/{id}` — auth, organizer only
- `POST /events/{id}/publish` — auth, organizer only
- `POST /events/{id}/cancel` — auth, organizer only
- `POST /events/{id}/rsvp` — auth (volunteerId locked to JWT subject)
- `DELETE /events/{id}/rsvp/{volunteerId}` — auth

### Users (`/users`)

- `GET /users/{id}` — public
- `POST /users` — public (registration)
- `PUT /users/{id}` — auth
- `DELETE /users/{id}` — auth
- `GET /users/{id}/campaigns` — public
- `GET /users/{id}/events` — public
- `GET /users/{id}/donations` — **auth + self-only**
- `GET /users/{id}/rsvps` — **auth + self-only**

### Auth / Webhooks

- `POST /auth/login`
- `POST /webhooks/stripe` — Stripe signature verified, not JWT

---

## Frontend Pages (all built)

| Page            | Route              | Notes                                                                             |
| --------------- | ------------------ | --------------------------------------------------------------------------------- |
| Landing         | `/`                | Public marketing page                                                             |
| Login           | `/login`           | JWT stored in localStorage                                                        |
| Register        | `/register`        |                                                                                   |
| Forgot Password | `/forgot-password` | Placeholder — Phase 5                                                             |
| Dashboard       | `/dashboard`       | Protected; 6 cards: profile edit, campaigns, events, RSVPs, giving history, links |
| Campaigns       | `/campaigns`       | Public list                                                                       |
| Campaign Detail | `/campaigns/:id`   | Donation form + QR code card with SVG download                                    |
| Events          | `/events`          | RSVP + cancel flow, confirmed/waitlisted status                                   |
| Calendar        | `/calendar`        | Events grouped by month                                                           |
| Search          | `/search`          | Placeholder                                                                       |

---

## PR History (selected merged work)

- PRs #1–16: Initial structure → Spring Boot 3 upgrade → domain rename → DynamoDB migration
  → GSI for email lookup → Bean Validation → LocalDate migration → React/Vite frontend
  → ESLint/Prettier → CI → Salesforce REST integration → AuditLog → FundraisingEvent + RSVP
- PR #17: AuditLog with dual GSIs
- PR #18: Audit log null safety + DynamoDbException containment
- PR #19: 18 CodeRabbit backlog fixes (PRs #2–16)
- PR #20: QR code card on CampaignDetailPage (qrcode.react, SVG download for flyers)
- PR #21: User dashboard — GET /users/{id}/campaigns and /events + live cards
- PR #22: Phase 4 dashboard — inline profile edit, My RSVPs, Giving History;
  Supporter model gains amountInCents+donationDate; SupporterTypeConverter v2
- PR #23: 7 CodeRabbit fixes (marked resolved but not fixed) + cr-fix tooling
- PR #24: Rewrite contributor guidance for the implemented state
- PR #25: CodeRabbit markdown follow-ups
- PR #26: Stripe PaymentElement donation flow
- PR #27: Public event routing, RSVP identity fix, and landing-page copy
- PR #29: Project provenance and careful Stripe status language

PR #28 is an older, conflicting documentation branch. Do not merge it as-is; supersede it only
after the current-main replacement PR exists and its still-accurate changes have been compared.

---

## What's Next

1. **Password reset** — `ForgotPasswordPage` is a placeholder; needs backend email endpoint
2. **Salesforce hardening** — add token/client/mapping/error-path tests, observable sync
   status, external IDs/idempotency, and bounded retry/backoff or a durable outbox
3. **Salesforce metadata** — add a source-controlled Salesforce DX project, synthetic-only
   sample data, setup/reset instructions, least-privilege permissions, and org validation
4. **GSIs for scale** — current DAO methods use full table scan + client-side filter:
   - `creatorId` GSI on CampaignRecord → replace `findByUserId()`
   - `organizerId` GSI on FundraisingEventRecord → replace `findByOrganizerId()`
   - `volunteerId` GSI on FundraisingEventRecord → replace `findByVolunteerId()`
5. **Search / filter** — SearchPage is a placeholder
6. **Planned portfolio extension** — Impact Update approval workflow and reporting first;
   Agentforce draft assistance only after human-review controls exist; Data Cloud optional

---

## CodeRabbit Workflow

The GitHub API is pull-only. CodeRabbit takes ~90s after a push to post its review.
**Run `cr-fix` BEFORE merging — GitHub auto-resolves all threads on merge even if unfixed.**

```bash
git push && ./cr-fix.sh        # polls until CodeRabbit is done, then launches Claude
```

Or from inside Claude Code: `/cr-fix`

---

## Coding Standards

- **Never store money as `double`/`float`** — see _Key Architectural Decisions_ above
- **Never compare Strings with `==`** — use `.equals()` or `.isBlank()`
- **Never hardcode secrets** — use `@Value("${property}")` + env vars
- **Null-safe ownership checks** — `Objects.equals(requestingUserId, record.getUser().getId())`
- **Controllers hold no business logic** — call service, return `ResponseEntity`
- **auditPayload()** — fail-fast varargs helper; even-length, String keys required
- **Monetary display** — `minimumFractionDigits:0, maximumFractionDigits:2` in Intl.NumberFormat

---

## Build & Test Commands

```bash
# Backend
./gradlew :Application:compileJava        # compile check
./gradlew :Application:test               # unit tests

# Frontend (from Frontend/)
npm run lint                              # ESLint
npm run format:check                      # Prettier
npm run build                             # Vite production build
npx prettier --write src/                 # auto-fix formatting
```
