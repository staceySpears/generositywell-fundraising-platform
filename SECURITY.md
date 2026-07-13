# Security Notes

## JWT Storage: localStorage vs HttpOnly Cookie

### Current implemented approach

The JWT is stored in `localStorage` (`gw_token`) and attached to every request
via an Axios interceptor in `Frontend/src/api/client.js`.

### Known risk

Any XSS vector — a malicious third-party dependency, a compromised npm package,
or (once organizer-supplied campaign descriptions are rendered) unsanitized HTML —
could read `localStorage` and exfiltrate the token. For a platform handling
donations and Salesforce/Stripe integrations, this risk warrants a migration
before any public or production deployment.

### Planned production-hardening approach

Replace the client-side token with an **HttpOnly, Secure, SameSite=Strict** session
cookie issued by Spring Security on successful login. Stateless JWT can be retained
server-side inside a cookie container:

1. Spring Security issues the JWT inside a `Set-Cookie` header with the attributes above.
2. The frontend never touches the token — the browser sends it automatically on same-origin requests.
3. CSRF protection is required for all state-changing endpoints (Spring Security provides this via `CsrfTokenRepository`).
4. If cross-domain API calls are required, use the short-lived access token + long-lived
   refresh-token-in-cookie pattern instead of storing the access token in `localStorage`.

### Why localStorage is currently present

- The backend auth endpoints are not hardened for production.
- No organizer-supplied HTML is rendered via `dangerouslySetInnerHTML` yet — campaign
  descriptions are rendered as plain text.
- The current repository milestone is for local portfolio development with synthetic data only.

This is an acknowledged production-hardening concern, not a recommendation to put sensitive
data in `localStorage`. The decision must be revisited before any public deployment.
