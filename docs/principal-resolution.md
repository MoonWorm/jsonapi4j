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
| `authenticatedClientAccessTier()` | Coarse-grained privilege level (`PUBLIC`, `ADMIN`, …). |
| `authenticatedClientScopes()` | Fine-grained OAuth2 scopes. |
| `attributes()` | Everything else the token carried — email, tenant, expiry — for ABAC rules. |

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
* **Spring Boot with `spring-boot-starter-oauth2-resource-server`** → `SpringSecurityPrincipalResolver`.
* **Quarkus with `quarkus-oidc` or `quarkus-smallrye-jwt`** → `QuarkusJwtPrincipalResolver`.
* **Plain servlet, or a gateway that forwards the raw JWT** → `JwtPrincipalResolver`, after reading the
  warning below.

### Header-based resolution

`DefaultPrincipalResolver` reads three headers, typically set by an API gateway that has already validated
the caller's credentials:

1. `X-Authenticated-User-Id` — the caller's id. The request counts as authenticated when this is neither
   null nor blank. Also used for ownership checks.
2. `X-Authenticated-Client-Access-Tier` — the access tier. Built-in values are **NO_ACCESS**, **PUBLIC**,
   **PARTNER**, **ADMIN** and **ROOT_ADMIN**; register your own via `AccessTierRegistry`.
3. `X-Authenticated-User-Granted-Scopes` — a space-separated list of granted scopes.

```bash
curl -H 'X-Authenticated-User-Id: 42' \
     -H 'X-Authenticated-Client-Access-Tier: ADMIN' \
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
        JwtPrincipalResolver.withAccessTierClaim("access_tier", accessTierRegistry));
```

The decoded claims are cached per request, so the token is parsed once regardless of how many principal
fields are read.

#### Spring Boot

Requires `spring-boot-starter-oauth2-resource-server`, and the JSON:API root path to be authenticated in
your `SecurityFilterChain`. Claims come from the `SecurityContextHolder`, so they have already been verified
by Spring Security.

```java
@Bean
public PrincipalResolver jsonapi4jPrincipalResolver(AccessTierRegistry accessTierRegistry) {
    return SpringSecurityPrincipalResolver.withAccessTierClaim("access_tier", accessTierRegistry);
}
```

#### Quarkus

Requires `quarkus-oidc` or `quarkus-smallrye-jwt`, and the JSON:API root path to be authenticated (e.g.
through `quarkus.http.auth.permission.*`). The injected `JsonWebToken` is the request-scoped CDI proxy, so a
single resolver instance serves every request.

```java
@Produces
@Singleton
public PrincipalResolver principalResolver(JsonWebToken jwt, AccessTierRegistry accessTierRegistry) {
    return QuarkusJwtPrincipalResolver.withAccessTierClaim(jwt, "access_tier", accessTierRegistry);
}
```

### Access tiers need an explicit claim

**This is the most common cause of unexpected 403s after switching to a JWT resolver.** JWT defines no
standard claim for access tiers, so tier resolution is **off unless you name a claim**. With no tier claim
configured, every principal resolves a `null` tier, and every operation guarded by
`@AccessControl(tier = …)` is denied — even for a perfectly valid, fully authenticated token.
{: .notice--warning}

Name the claim that carries your tier, and register the tier values through `AccessTierRegistry`:

```java
SpringSecurityPrincipalResolver.withAccessTierClaim("access_tier", accessTierRegistry);
```

If the claim is present but its value is not registered, the registry's default tier (`PUBLIC`) is used
instead. Scopes and user id have sensible standard defaults and need no configuration; tiers never can.

### Claim mapping

The defaults follow the registered JWT claims, and every name is configurable:

| Principal field | Default claim | Notes |
|---|---|---|
| `authenticatedUserId()` | `sub` | RFC 7519. Override for providers that prefer `oid` or `email`. |
| `authenticatedClientScopes()` | `scope`, falling back to `scp` | See shapes below. |
| `authenticatedClientAccessTier()` | *none* | Must be named explicitly — see above. |
| `attributes()` | all claims | Exposed as an unmodifiable map. |

**Scope shapes.** Providers disagree, so both accepted forms are handled automatically:

* `"read write"` — a space-delimited string (RFC 9068, Keycloak, Auth0)
* `["read", "write"]` — an array of strings (Okta)
* the `scp` claim is consulted when the default `scope` claim is absent (Azure AD)

**Nested claims.** A claim name containing dots is first looked up literally, then — only if no such claim
exists — treated as a path into nested objects. Both of these work:

```java
// Keycloak — roles nested under a realm_access object
new ClaimsPrincipalMapper("sub", "realm_access.roles", "access_tier", accessTierRegistry);

// Auth0 — a namespaced claim whose *name* contains dots
new ClaimsPrincipalMapper("sub", "https://api4.pro/roles", "access_tier", accessTierRegistry);
```

**Claim value types differ between resolvers.** Spring Security converts the time claims (`exp`, `iat`,
`nbf`) to `java.time.Instant`; Quarkus exposes them as `Long`; `JwtPrincipalResolver` exposes them as
epoch-second `Integer`s. If your ABAC rules read these through `attributes()`, they are not portable across
resolvers without a type check.
{: .notice--info}

### Writing your own resolver

Implement `PrincipalResolver` when none of the above fits — a session cookie, an API key table, mTLS
certificate attributes. Every method has a default returning `null`, so implement only what applies:

```java
public class ApiKeyPrincipalResolver implements PrincipalResolver {

    @Override
    public String resolveUserId(ServletRequest servletRequest) {
        String apiKey = ((HttpServletRequest) servletRequest).getHeader("X-Api-Key");
        return apiKeyRegistry.lookupUserId(apiKey);
    }
}
```

Returning `null` from a method means "unknown", and the corresponding access control check fails closed.

To reuse JWT claim mapping from a different claim source, hand a claims map to `ClaimsPrincipalMapper`
rather than reimplementing the `scope`/`scp` and nested-path handling.

### Related

* [Access Control Plugin](/access-control-plugin/) — how the resolved principal is enforced
* [Configuration](/configuration/) — registering custom beans per framework
* [Request Processing Pipeline](/request-processing-pipeline/) — where principal resolution happens
