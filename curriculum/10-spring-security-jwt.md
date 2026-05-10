# 10 — Spring Security and JWT Auth

## Why this exists

Without authentication, any caller can create campaigns, delete other users' events, and access
anyone's giving history. Phase 3 adds two things: **authentication** (who are you?) and
**authorization** (what are you allowed to do?).

JWT (JSON Web Token) is the standard for stateless auth in REST APIs. The server issues a signed
token at login. Every subsequent request carries that token in the `Authorization` header. The
server validates the signature and extracts the user's identity — no session state, no database
lookup per request.

Spring Security's filter chain intercepts every HTTP request before it reaches your controllers.
The JWT filter plugs into that chain.

---

## The pieces

### `JwtUtil` — token creation and validation

```java
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,          // (1)
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String userId, String email) {  // (2)
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

**(1)** The secret is injected from a property, never hardcoded. In production this comes from
an environment variable. Hardcoding a JWT secret is a critical security vulnerability — anyone
who sees the code can forge tokens.

**(2)** The JWT subject is the user ID, not the username or email. Downstream code that needs
to enforce ownership (`campaign.creatorId.equals(currentUserId)`) reads the subject. Email is
a convenience claim for display purposes only.

---

### `JwtAuthenticationFilter` — intercepting requests

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.isValid(token)) {
                String userId = jwtUtil.extractUserId(token);
                UsernamePasswordAuthenticationToken auth =    // (1)
                        new UsernamePasswordAuthenticationToken(userId, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);             // (2)
    }
}
```

**(1)** `UsernamePasswordAuthenticationToken` is Spring Security's generic auth token. We pass
the userId as the principal, no credentials (null), and an empty roles list. The principal is
later read in controllers via `authentication.getName()`.

**(2)** The filter always calls `filterChain.doFilter` — it never short-circuits on a bad or
missing token. Invalid tokens are silently ignored; the request continues unauthenticated. Spring
Security's route-level rules (in `SecurityConfig`) decide whether that is acceptable.

---

### `SecurityConfig` — route-level rules

```java
.authorizeHttpRequests(auth -> auth
    // public read access
    .requestMatchers(HttpMethod.GET, "/campaigns/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/events/**").permitAll()

    // giving history and RSVPs are self-only — must come before the broad users permit
    .requestMatchers(HttpMethod.GET, "/users/*/donations").authenticated()
    .requestMatchers(HttpMethod.GET, "/users/*/rsvps").authenticated()
    .requestMatchers(HttpMethod.GET, "/users/**").permitAll()

    // donations are public (Stripe handles identity for card payments)
    .requestMatchers(HttpMethod.POST, "/campaigns/*/donate").permitAll()

    // registration and login are always open
    .requestMatchers(HttpMethod.POST, "/users").permitAll()
    .requestMatchers("/auth/**").permitAll()

    // Stripe webhook — authenticated by Stripe signature, not JWT
    .requestMatchers("/webhooks/stripe").permitAll()

    // everything else requires a valid JWT
    .anyRequest().authenticated()
)
```

The ordering matters. Spring Security evaluates matchers top to bottom and stops at the first
match. The specific `/users/*/donations` rule must appear before `.requestMatchers(GET, "/users/**")`
or the broad permit-all would swallow it.

---

## Where ownership is enforced

`SecurityConfig` tells Spring which routes require a token. It does not enforce who owns what.
Ownership checks happen in the service layer:

```java
// CampaignService.updateCampaign
if (!Objects.equals(requestingUserId, record.getUser().getId())) {
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the campaign owner");
}
```

`Objects.equals()` is null-safe — it handles the case where `requestingUserId` is null (caller
is unauthenticated even on a route that doesn't strictly require auth). Using `==` for String
comparison is a bug here; using `.equals()` directly risks a NullPointerException.

---

## How controllers read the current user

Controllers extract the authenticated user ID from the Spring Security context:

```java
@PutMapping("/{id}")
public ResponseEntity<CampaignResponse> updateCampaign(
        @PathVariable String id,
        @RequestBody CampaignUpdateRequest request,
        Authentication authentication) {        // (1) Spring injects this automatically

    String requestingUserId = authentication != null ? authentication.getName() : null;
    CampaignResponse response = campaignService.updateCampaign(id, request, requestingUserId);
    return ResponseEntity.ok(response);
}
```

**(1)** Spring injects `Authentication` from the `SecurityContext` that `JwtAuthenticationFilter`
populated. If the route is protected (`.authenticated()`), Spring guarantees this is non-null.
If the route is public, it may be null for unauthenticated callers.

---

## What to understand

1. What is the difference between authentication (who are you?) and authorization (what can
   you do)?
2. A JWT has three parts: header, payload, signature. What does the signature prevent? What
   does it not prevent?
3. Why is JWT called "stateless"? What does that mean for horizontal scaling?
4. The filter calls `filterChain.doFilter` even when the token is invalid. Why not return a 401
   immediately?
5. `SecurityConfig` permits `POST /campaigns/*/donate` publicly. The controller still checks for
   an optional JWT to extract `donorId`. How do both behaviors coexist?
6. If a user's token is stolen, how would you invalidate it before it expires? What would you
   need to add to the architecture?

---

## Next

[11 — Salesforce Integration](11-salesforce-integration.md)
