# 16 — CI/CD Pipeline: GitHub Actions

> **Status: Validated for CI checks; deployment is planned.** GitHub Actions runs backend
> compilation/tests and frontend lint/format checks. The deployment example below is a target.

---

## Why this exists

GenerosityWell currently validates backend and frontend changes with GitHub Actions CI; this
repository does not establish an active AWS deployment. The target CD pipeline shown below would
build deployable artifacts and release them to AWS only after required checks pass. Until that
workflow and its credentials are implemented and validated, deployment remains planned.

For a portfolio project, a working CI/CD pipeline signals that you understand how professional
software ships. It is also a forcing function: if your tests are not passing, the pipeline catches
it before it reaches production.

---

## The pipeline structure

```yaml
# .github/workflows/deploy.yml
on:
  push:
    branches: [main]

jobs:
  test: # Run unit tests and integration tests
  build: # Gradle build, produce artifacts
  deploy-backend: # Deploy Spring Boot to AWS (ECS or Elastic Beanstalk)
  deploy-frontend: # Build Vite, sync to S3, invalidate CloudFront
  deploy-lambda: # Package and deploy ServiceLambda (Phase 4+)
```

GitHub Actions jobs run in parallel by default. A production version of this target workflow
must add explicit `needs` dependencies so build waits for tests and deployment waits for build;
the abbreviated example above does not yet define that ordering.

---

## What to understand before you build this

1. What is the difference between continuous integration and continuous deployment?
2. GitHub Actions secrets store your `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`.
   Why should these never appear in your `yml` files or commit history?
3. The integration tests spin up a local DynamoDB container via Testcontainers. What does
   the GitHub Actions runner need installed to support Docker containers?
4. If the `deploy-frontend` step fails but `deploy-backend` already succeeded, what is the
   state of your production environment?

---

## Next

[17 — Observability](17-observability.md)
