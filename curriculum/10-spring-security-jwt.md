# 10 — Spring Security and JWT Auth

> **Phase 3 — Not yet implemented.**

---

## Why this exists

Currently any client can call any endpoint. There is no concept of who the caller is.
Phase 3 adds authentication (who are you?) and authorization (what are you allowed to do?).

JWT (JSON Web Token) is the standard for stateless auth in REST APIs. When a user logs in,
the server issues a signed JWT. Every subsequent request includes that token in the
`Authorization` header. The server validates the signature and extracts the user's identity
and roles — no session state, no database lookup per request.

Spring Security is the standard Spring framework for auth. Its filter chain intercepts every
HTTP request before it reaches your controllers.

---

## The pieces you will build

**Endpoints:**
- `POST /auth/register` — create a User account, return JWT
- `POST /auth/login` — validate credentials, return JWT

**Spring Security filter chain:**
- `JwtAuthenticationFilter` — validates the token, populates the `SecurityContext`
- Route-level rules: public routes (`GET /campaigns`, `GET /events`) vs. protected routes
  (`POST /events`, `PUT /campaigns/:id`)

**Role-based access:**
- `ORGANIZER` — can create and manage campaigns and events
- `DONOR` — can make donations and RSVP to events
- Enforced with `@PreAuthorize("hasRole('ORGANIZER')")`

---

## What to understand before you build this

1. What is the difference between authentication and authorization?
2. A JWT has three parts: header, payload, and signature. What does the signature prevent?
3. Why is JWT considered "stateless"? What does that mean for horizontal scaling?
4. What is the `SecurityContext`, and how does the filter chain populate it?
5. If a user's token is stolen, how do you invalidate it before it expires?

---

## Next

[11 — Salesforce Integration](11-salesforce-integration.md)
