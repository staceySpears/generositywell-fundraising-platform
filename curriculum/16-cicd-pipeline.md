# 16 — CI/CD Pipeline: GitHub Actions

> **Phase 5 — Not yet implemented.**

---

## Why this exists

Right now, deploying GenerosityWell is a manual process: run the Gradle build locally, push to
AWS manually, hope nothing breaks. A CI/CD pipeline automates this — every push to `main` runs
the test suite, builds the artifacts, and deploys to AWS. If any step fails, the deploy stops
and you get a notification.

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
  test:       # Run unit tests and integration tests
  build:      # Gradle build, produce artifacts
  deploy-backend:   # Deploy Spring Boot to AWS (ECS or Elastic Beanstalk)
  deploy-frontend:  # Build Vite, sync to S3, invalidate CloudFront
  deploy-lambda:    # Package and deploy ServiceLambda (Phase 4+)
```

Jobs run sequentially — deploy only runs if build passes, build only runs if tests pass.

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
