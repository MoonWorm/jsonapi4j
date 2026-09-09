---
title: "Principal Resolution"
permalink: /principal-resolution/
---

### Overview

Every JSON:API request handled by JsonApi4j carries a **principal** — the authenticated caller on whose
behalf the request is executed. The principal is resolved once per request by a `PrincipalResolver`, stored
on the current thread, and consumed downstream by the
[Access Control plugin](/access-control-plugin/) and by your own operations.

JsonApi4j **never authenticates anyone**. Verifying credentials is the job of your web framework or API
gateway. A `PrincipalResolver` only answers a narrower question: *given a request that has already passed
authentication, who is the caller and what are they allowed to do?*

A `Principal` carries four things:

| Method | Purpose |
|---|---|
| `authenticatedUserId()` | Identifies the caller. Also used for ownership checks. |
| `authenticatedClientEntitlements()` | Unordered entitlement labels (`ADMIN`, `PARTNER`, …). |
| `authenticatedClientScopes()` | Fine-grained OAuth2 scopes. |
| `attributes()` | Everything else the token carried — email, tenant, expiry. Read by [access control policies](/access-control-plugin/#policies-deciding-access-in-code). |

Read the current principal anywhere in your code through `AuthenticatedPrincipalContextHolder`:

```java
Optional<String> userId = AuthenticatedPrincipalContextHolder.getAuthenticatedUserId();
Optional<Map<String, Object>> claims = AuthenticatedPrincipalContextHolder.getAttributes();
```

### Choosing a resolver

JsonApi4j ships four implementations. The one you pick depends on **where authentication happens** in your
deployment.

| Resolver | Reads from | Token verified? | Module |
|---|---|---|---|
| `DefaultPrincipalResolver` | `X-Authenticated-*` headers | Upstream — trusts the gateway | `jsonapi4j-rest` |
| `JwtPrincipalResolver` | `Authorization: Bearer …` | **No** — decodes only | `jsonapi4j-rest` |
| `SpringSecurityPrincipalResolver` | `SecurityContextHolder` | Yes, by Spring Security | `jsonapi4j-rest-springboot` |
| `QuarkusJwtPrincipalResolver` | injected `JsonWebToken` | Yes, by Quarkus OIDC | `jsonapi4j-rest-quarkus` |

`DefaultPrincipalResolver` is active out of the box. The other three are **opt-in**: you declare one as a
bean and it takes precedence. This is deliberate — auto-selecting a JWT resolver because a security library
happens to be on the classpath would silently change behaviour for applications that authenticate at the
gateway and forward headers.

Rules of thumb:

* **A gateway terminates auth and forwards headers** → `DefaultPrincipalResolver`.
* **Spring Boot with `spring-boot-starter-oauth2-resource-server`** → `SpringSecurityPrincipalResolver`
  (works on Spring Security 6 and 7, so Spring Boot 3 and 4 alike).
* **Quarkus with `quarkus-oidc` or `quarkus-smallrye-jwt`** → `QuarkusJwtPrincipalResolver`.
* **Plain servlet, or a gateway that forwards the raw JWT** → `JwtPrincipalResolver`, after reading the
  warning below.

### Header-based resolution

`DefaultPrincipalResolver` reads three headers, typically set by an API gateway that has already validated
the caller's credentials:

1. `X-Authenticated-User-Id` — the caller's id. The request counts as authenticated when this is neither
   null nor blank. Also used for ownership checks.
2. `X-Authenticated-Client-Entitlements` — a space-separated list of entitlements. Any string works;
   `DefaultEntitlements` offers **PARTNER**, **ADMIN** and **ROOT_ADMIN** as constants.
3. `X-Authenticated-User-Granted-Scopes` — a space-separated list of granted scopes.

```bash
curl -H 'X-Authenticated-User-Id: 42' \
     -H 'X-Authenticated-Client-Entitlements: ADMIN' \
     -H 'X-Authenticated-User-Granted-Scopes: users.read users.write' \
     http://localhost:8080/jsonapi/users/42
```

Header names are configurable through the `DefaultPrincipalResolver` constructor.

Because these headers are trusted verbatim, the JSON:API root path **must not be reachable directly** —
only through the gateway that sets them.

### JWT-based resolution

The three JWT resolvers share one claim-mapping engine, `ClaimsPrincipalMapper`, so claim configuration
behaves identically no matter which one you use.

#### `JwtPrincipalResolver` — decodes, does not verify

**`JwtPrincipalResolver` does not validate tokens.** It base64-decodes the payload of whatever bearer token
arrives and maps the claims. It checks no signature, no expiry, no issuer, no audience. If the JSON:API root
path is not protected by your security layer or gateway, **anyone can forge a token and become an
authenticated administrator**. Use it only where something in front of the application has already rejected
invalid tokens — and prefer `SpringSecurityPrincipalResolver` or `QuarkusJwtPrincipalResolver`, which cannot
produce a principal from an unverified token at all.
{: .notice--danger}

```java
// Servlet — register before the framework initializes
servletContext.setAttribute(
        JsonApi4jServletContainerInitializer.PRINCIPAL_RESOLVER_ATT_NAME,
        JwtPrincipalResolver.withEntitlementsClaim("entitlements"));
```

The token is decoded once per request — `resolvePrincipal` is called a single time by the filter and maps
every principal field from the same claim set.

#### Spring Boot

Requires `spring-boot-starter-oauth2-resource-server`, and the JSON:API root path to be authenticated in
your `SecurityFilterChain`. Claims come from the `SecurityContextHolder`, so they have already been verified
by Spring Security.

```java
@Bean
public PrincipalResolver jsonapi4jPrincipalResolver() {
    return SpringSecurityPrincipalResolver.withEntitlementsClaim("entitlements");
}
```

#### Quarkus

Requires `quarkus-oidc` or `quarkus-smallrye-jwt`, and the JSON:API root path to be authenticated (e.g.
through `quarkus.http.auth.permission.*`). The injected `JsonWebToken` is the request-scoped CDI proxy, so a
single resolver instance serves every request.

```java
@Produces
@Singleton
public PrincipalResolver principalResolver(JsonWebToken jwt) {
    return QuarkusJwtPrincipalResolver.withEntitlementsClaim(jwt, "entitlements");
}
```

### Entitlements need an explicit claim

**This is the most common cause of unexpected 403s after switching to a JWT resolver.** JWT defines no
standard claim for entitlements. JsonApi4j reads `entitlements` by convention, but no identity provider emits
that claim on its own — you have to configure your IdP to include it, or point the resolver at whichever
claim you already use. If the claim is missing from the token, every principal resolves no entitlements at all, and
every operation guarded by `@AccessControl(entitlements = …)` is denied, even for a perfectly valid token.
{: .notice--warning}

Point the resolver at the claim that carries your entitlements. The claim may hold a single entitlement or several — a
space-delimited string and an array of strings are both accepted, matching the scopes claim:

```java
SpringSecurityPrincipalResolver.withEntitlementsClaim("https://api4.pro/entitlements");
```

Passing `null` as the entitlements claim disables entitlements resolution altogether. Do that only when your
application declares no entitlement requirements.

When a request is denied because the caller authenticated successfully but carries no entitlement at all — as
opposed to holding entitlements that simply are not the ones required — the Access Control plugin says so once per
process:

```
WARN  Access denied: OR[ADMIN] is required, but the authenticated principal carries no entitlement at all.
      The configured PrincipalResolver resolved none — when using a JWT resolver, check that issued tokens
      actually carry the configured entitlements claim.
```

Anonymous callers never trigger it: having no entitlement is the expected state for them, and denying them is the
point of the requirement. The same warning exists for scopes, for the same reason.

If the claim is present but carries an entitlement no requirement asks for, that is an ordinary denial — entitlement names
are matched exactly, and nothing is inferred from an unrecognized value. Because a misspelling on either
side looks exactly like a legitimate denial, the plugin logs both sides at `DEBUG`:

```
DEBUG Access denied: OR[ADMIN] is required, but the authenticated principal carries [ADMNI, PARTNER].
      Entitlement names are matched exactly, so a name that merely looks alike does not match — check both
      sides for typos.
```

Scopes and user id have standard defaults that work out of the box; entitlements never can.

### Claim mapping

The defaults follow the registered JWT claims, and every name is configurable:

| Principal field | Default claim | Notes |
|---|---|---|
| `authenticatedUserId()` | `sub` | RFC 7519. Override for providers that prefer `oid` or `email`. |
| `authenticatedClientScopes()` | `scope`, falling back to `scp` | See shapes below. |
| `authenticatedClientEntitlements()` | `entitlements` | A jsonapi4j convention, not a standard — see above. |
| `attributes()` | all claims | Exposed as an unmodifiable map. |

**Scope shapes.** Providers disagree, so both accepted forms are handled automatically:

* `"read write"` — a space-delimited string (RFC 9068, Keycloak, Auth0)
* `["read", "write"]` — an array of strings (Okta)
* the `scp` claim is consulted when the default `scope` claim is absent (Azure AD)

**Nested claims.** A claim name containing dots is first looked up literally, then — only if no such claim
exists — treated as a path into nested objects. Both of these work:

```java
// Keycloak — roles nested under a realm_access object
new ClaimsPrincipalMapper("sub", "realm_access.roles", "entitlements");

// Auth0 — a namespaced claim whose *name* contains dots
new ClaimsPrincipalMapper("sub", "https://api4.pro/roles", "entitlements");
```

**Claim value types differ between resolvers.** Spring Security converts the time claims (`exp`, `iat`,
`nbf`) to `java.time.Instant`; Quarkus exposes them as `Long`; `JwtPrincipalResolver` exposes them as
epoch-second `Integer`s. If your ABAC rules read these through `attributes()`, they are not portable across
resolvers without a type check.
{: .notice--info}

### Writing your own resolver

Implement `PrincipalResolver` when none of the above fits — a session cookie, an API key table, mTLS
certificate attributes. It has a single method; leave `null` or empty whatever your source cannot supply:

```java
public class ApiKeyPrincipalResolver implements PrincipalResolver {

    @Override
    public Principal resolvePrincipal(ServletRequest servletRequest) {
        String apiKey = ((HttpServletRequest) servletRequest).getHeader("X-Api-Key");
        String userId = apiKeyRegistry.lookupUserId(apiKey);
        return userId == null
                ? null
                : new DefaultPrincipal(List.of(), Set.of(), userId, Map.of());
    }
}
```

Returning `null` marks the request as anonymous. A `null` or empty value on the principal means "unknown",
and the corresponding access control check fails closed.

The resolver is called once per request and a single instance serves every request, so implementations must
be thread-safe and must resolve everything from the request argument rather than caching state on the
resolver itself.

To reuse JWT claim mapping from a different claim source, hand a claims map to `ClaimsPrincipalMapper`
rather than reimplementing the `scope`/`scp` and nested-path handling.

### Related

* [Access Control Plugin](/access-control-plugin/) — how the resolved principal is enforced
* [Configuration](/configuration/) — registering custom beans per framework
* [Request Processing Pipeline](/request-processing-pipeline/) — where principal resolution happens
