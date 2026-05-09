# Security Notes

## JWT Storage: localStorage vs HttpOnly Cookie

### Current approach (Phase 3)
The JWT is stored in `localStorage` (`gw_token`) and attached to every request
via an Axios interceptor in `Frontend/src/api/client.js`.

### Known risk
Any XSS vector — a malicious third-party dependency, a compromised npm package,
or (once organizer-supplied campaign descriptions are rendered) unsanitized HTML —
could read `localStorage` and exfiltrate the token. For a platform handling
donations and Salesforce/Stripe integrations, this risk warrants a migration
before going to production.

### Target approach (Phase 5 / production hardening)
Replace the client-side token with an **HttpOnly, Secure, SameSite=Strict** session
cookie issued by Spring Security on successful login. Stateless JWT can be retained
server-side inside a cookie container:

1. Spring Security issues the JWT inside a `Set-Cookie` header with the attributes above.
2. The frontend never touches the token — the browser sends it automatically on same-origin requests.
3. CSRF protection is required for all state-changing endpoints (Spring Security provides this via `CsrfTokenRepository`).
4. If cross-domain API calls are required, use the short-lived access token + long-lived
   refresh-token-in-cookie pattern instead of storing the access token in `localStorage`.

### Why localStorage was accepted for Phase 3
- The backend auth endpoints are not yet hardened for production (Phase 5 scope).
- No organizer-supplied HTML is rendered via `dangerouslySetInnerHTML` yet — campaign
  descriptions are rendered as plain text.
- The React dependency tree is small and fully audited at this stage.
- Phase 3 is a local-dev milestone; no real user data is at risk.

This decision must be revisited before any public deployment.
