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
3. Enable CSRF protection in `SecurityConfig` with `CookieCsrfTokenRepository.withHttpOnlyFalse()`.
   The authentication cookie remains HttpOnly; the separate `XSRF-TOKEN` cookie is intentionally
   readable by the browser client so it can return the token.
4. Expose a safe token bootstrap such as `GET /csrf` (or another documented request that causes
   the repository to create `XSRF-TOKEN`). Configure Axios with `withCredentials: true`,
   `xsrfCookieName: "XSRF-TOKEN"`, and `xsrfHeaderName: "X-XSRF-TOKEN"`; send that header on
   every state-changing request.
5. If cross-domain API calls are required, use the short-lived access token + long-lived
   refresh-token-in-cookie pattern instead of storing the access token in `localStorage`.

### Why localStorage is currently present

- The backend auth endpoints are not hardened for production.
- No organizer-supplied HTML is rendered via `dangerouslySetInnerHTML` yet — campaign
  descriptions are rendered as plain text.
- The current repository milestone is for local portfolio development with synthetic data only.

This is an acknowledged production-hardening concern, not a recommendation to put sensitive
data in `localStorage`. The decision must be revisited before any public deployment.
