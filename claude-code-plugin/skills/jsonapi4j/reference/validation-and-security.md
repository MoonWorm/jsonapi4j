# Validation & security

## Validation & errors

Do input validation in the `validateXxx` hooks (run **before** the operation) so malformed input is
**400, not 500**. The composite operation interfaces dispatch to them automatically via `validate()`.

Use the fluent `JsonApiRequestValidator` (static `forRequest(...)`):

```java
import static pro.api4.jsonapi4j.operation.validation.JsonApiRequestValidator.forRequest;

@Override
public void validateReadById(JsonApiRequest req) {
    forRequest(req)
        .path(p -> p.withResourceIdValidator(id -> id.isNotBlank().isUUID()))
        .validate();
}

@Override
public void validateReadMultiple(JsonApiRequest req) {
    forRequest(req)
        .parameters(p -> p.withFilterValidator("id", ids -> ids.ifPresent().allSatisfy(/* ... */)))
        .validate();
}
```

The fluent API covers `path(...)` (`withResourceIdValidator`, `withResourceTypeValidator`,
`withRelationshipNameValidator`), `parameters(...)` (`withFilterValidator`, `withFiltersValidator`,
`withIncludeValidator`, `withSortValidator`, `withCursorValidator`, `withLimitValidator`,
`withOffsetValidator`, `withFieldSetsValidator`, `withCustomQueryParamValidator`), headers, and a
single-resource-doc payload builder (`withAttributesValidator`, `withToOneRelationship`, …) for
create/update bodies.

Rules of thumb:
- Never let a raw `UUID.fromString` / `LocalDate.parse` bubble up from the operation — validate first.
- Not found → `throw new ResourceNotFoundException(request.getResourceId(), new ResourceType("<type>"))`
  (404).
- Custom business errors → `new JsonApi4jException(httpStatus, () -> "CODE", "detail")`.
- Global validation caps live under `jsonapi4j.validation.*` (see `configuration.md`).

JSR-380 (`@NotNull`/`@Size`/`@Pattern`/…) on attributes/payloads is also supported and mapped to
JSON:API error codes (e.g. `@NotNull` → `VALUE_IS_ABSENT`, `@Size` → `VALUE_TOO_LONG`).

## Security — two independent layers

1. **Transport auth** is your web framework's job (e.g. Spring Security), by URL prefix — e.g.
   `GET /jsonapi/users/**` rules, `/jsonapi/**` authenticated. Relationship/sub-resource paths inherit
   the prefix rule, so a new public resource needs its own `permitAll` line.
2. **jsonapi4j AC plugin** (`@AccessControl`) is *field-level / ownership* gating layered on top — hide
   sensitive attributes, owner-only fields, OAuth2 scope requirements, automatic anonymization.
   A `PrincipalResolver` maps the authenticated caller to the principal. **AC ownership does not by itself
   turn a cross-user read into a 403** — without an owner-gated field it returns 200 with full attributes
   (see `known-behaviors.md`).

### Picking a PrincipalResolver — don't hand-roll one

Four ship with the framework; write your own only for non-JWT, non-header schemes (API keys, mTLS, sessions):

| Resolver | Reads from | Verifies? |
|---|---|---|
| `DefaultPrincipalResolver` (active by default) | `X-Authenticated-*` headers | upstream gateway |
| `JwtPrincipalResolver` | `Authorization: Bearer` | **no — decodes only** |
| `SpringSecurityPrincipalResolver` | `SecurityContextHolder` | yes, Spring Security |
| `QuarkusJwtPrincipalResolver` | injected `JsonWebToken` | yes, Quarkus OIDC |

The three JWT resolvers are **opt-in** — declare one as a `@Bean` / `@Produces` and it replaces the default.
All share `ClaimsPrincipalMapper`: `sub` for user id, `scope` with `scp` fallback (space-delimited string
*or* array), dotted paths for nested claims (`realm_access.roles`), literal-name-first so Auth0 namespaced
claims (`https://acme.com/roles`) still resolve.

**Two traps worth flagging to users:**
- **Access tier has no standard JWT claim.** Unless you pass an explicit tier claim
  (`withAccessTierClaim("access_tier", registry)`), the tier is `null` and *every* `@AccessControl(tier=…)`
  operation is denied — the usual cause of "everything 403s after switching to JWT".
- **`JwtPrincipalResolver` never validates the token.** Only use it behind a gateway or security layer that
  rejects invalid tokens; otherwise a forged token yields an authenticated admin. Prefer the Spring/Quarkus
  resolvers, which cannot produce a principal from an unverified token.

```java
@AccessControl(authenticated = Authenticated.AUTHENTICATED)
public class UserAttributes {
    private final String fullName;
    private final String email;

    @AccessControl(
        scopes = @AccessControlScopes(requiredScopes = "users.sensitive.read"),
        ownership = @AccessControlOwnership(ownerIdFieldPath = "id"))
    private final String creditCardNumber;
}
```

---

**Canonical examples in the framework**
- `examples/jsonapi4j-sampleapp-domain/.../domain/user/UserAttributes.java` (`@AccessControl`),
  `.../operations/user/UserPlaceOfBirthOperations.java` (`forRequest(...)` validation + `@AccessControl`)
- Tests: `.../operations/SpringAccessControlOperationsTests.java` (+ Quarkus/Servlet)
- Docs: https://api4.pro/validation/ · https://api4.pro/error-handling/ ·
  https://api4.pro/access-control-plugin/ · https://api4.pro/principal-resolution/
