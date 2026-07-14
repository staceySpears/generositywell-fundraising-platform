# 10 — Spring Security and JWT Auth

> **Status: Implemented.** Production hardening remains planned; see [`SECURITY.md`](../SECURITY.md).

---

## Why this exists

GenerosityWell uses stateless authentication so protected operations can identify the caller
without a server-side session. Login returns a signed JWT, and the frontend sends it in the
`Authorization: Bearer` header. `JwtAuthenticationFilter` validates the token and populates the
Spring Security context before controllers run.

## What is implemented

- `POST /users` registers an account; `POST /auth/login` returns a JWT and user ID.
- Passwords are hashed with BCrypt.
- Public reads include campaigns, events, and user profiles.
- Campaign/event writes and payment-intent creation require authentication.
- The JWT subject overrides client-supplied identity where applicable, including RSVP creation.
- Campaign and event mutation services enforce owner/organizer checks.
- Donation and RSVP history endpoints require authentication and are self-only.
- The Stripe webhook is public at the JWT layer and authenticated by its Stripe signature.

Explicit `ORGANIZER` and `DONOR` role-based access with `@PreAuthorize` is not implemented. Current
authorization is identity-, ownership-, and endpoint-based; do not describe it as full RBAC.

## Current security boundary

The frontend stores the JWT in `localStorage`, which exposes it to successful XSS. CSRF is disabled
because the current API uses bearer tokens rather than authentication cookies. Before a public
deployment, the project must revisit token storage, cookie and CSRF design, token lifetime/refresh,
revocation, rate limiting, and end-to-end authorization tests.

## What to understand

1. What is the difference between authentication and authorization?
2. What does a JWT signature protect, and what does it not protect?
3. Why does extracting identity from the verified JWT matter more than trusting a request-body ID?
4. Why would moving the JWT into an HttpOnly cookie require a CSRF strategy?
5. How do ownership checks differ from role-based access control?

---

## Next

[11 — Salesforce Integration](11-salesforce-integration.md)
