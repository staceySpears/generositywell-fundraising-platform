# 16 — CI/CD Pipeline: GitHub Actions

## What is built vs what is planned

**Built (CI):** Two GitHub Actions workflows run on every push to any branch:

- **Build & Unit Tests** (`.github/workflows/build.yml`) — runs `./gradlew :Application:test`
  on Java 21. Catches compilation errors and test failures before code reaches `main`.
- **Frontend Lint & Format** (`.github/workflows/frontend.yml`) — runs `npm run lint` and
  `npm run format:check` on Node 22. Catches ESLint violations and Prettier formatting drift.

Both jobs must pass before a PR can be merged. This is continuous integration — every push is
automatically validated.

**Planned (CD):** A deployment pipeline that automatically ships passing builds to AWS.
This section covers what that will look like.

---

## Why CI matters even without CD

CI enforces a contract: every commit that reaches `main` compiles and passes tests. Without it,
the discipline depends entirely on individual developers remembering to run the build locally.
That breaks down as soon as someone is in a hurry or working in an unfamiliar part of the code.

For a portfolio project, a working CI pipeline signals that you understand how professional
software is developed. It is also evidence you can work in a team — the pipeline treats every
PR the same way regardless of who wrote it.

---

## The current CI structure

```yaml
# .github/workflows/build.yml (simplified)
on:
  push:
    branches: ['**']
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'corretto' }
      - run: ./gradlew :Application:test
```

```yaml
# .github/workflows/frontend.yml (simplified)
jobs:
  lint-and-format:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '22' }
      - working-directory: Frontend
        run: npm ci && npm run lint && npm run format:check
```

`npm ci` installs exactly what is in `package-lock.json` — no version drift, reproducible
installs. `npm install` could silently update minor versions across CI runs.

---

## The planned CD pipeline

When continuous deployment is added, the jobs will run in sequence:

```
push to main
  └─► test (unit tests)
        └─► build (Gradle build, Vite production build)
              ├─► deploy-backend (push Spring Boot image to ECR, update ECS service)
              └─► deploy-frontend (sync Vite build to S3, invalidate CloudFront)
```

Deploy only runs if build passes; build only runs if tests pass. A failure at any step stops
the pipeline — a broken build never reaches production.

GitHub Actions secrets will store `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`,
`STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, and `JWT_SECRET`. These must never appear
in workflow files or commit history. If a secret is committed, rotate it immediately — deleting
the commit is not enough, because git history is distributed.

---

## What to understand

1. What is the difference between continuous integration and continuous deployment?
2. The frontend workflow uses `npm ci` instead of `npm install`. What is the difference and
   why does it matter in a CI environment?
3. GitHub Actions secrets store your AWS and Stripe credentials. Why should these never appear
   in your `.yml` files or commit history?
4. If `deploy-backend` succeeds but `deploy-frontend` fails, what is the state of your
   production environment? How would you detect and recover from this?
5. The test job runs `./gradlew :Application:test`. The `:Application:` prefix is important —
   what would happen if you ran `./gradlew test` instead?

---

## Next

[17 — Observability](17-observability.md)
