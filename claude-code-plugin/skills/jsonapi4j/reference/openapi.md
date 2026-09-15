# OpenAPI document (`jsonapi4j-oas-plugin`)

Add the `jsonapi4j-oas-plugin` dependency and the document is generated from the same registries the
runtime serves from — paths, schemas, JSON:API query params, request bodies and error responses, with
no annotations required. Served at `jsonapi4j.oas.oasRootPath` (default `/jsonapi/oas`), with
`?format=json|yaml`. It must differ from `jsonapi4j.rootPath`; a clash fails at startup. The built-in
meta types (`state`, `plugins`, `resources`, …) are never documented.

**Declare your attributes class or the document describes nothing.** `<Type>Attributes` is generated
from `@OasResourceInfo(attributes = …)`; the resource's own `resolveAttributes` return type is erased,
so with no annotation the schema is published **empty** — the endpoints look right and every attribute
is missing. This is the one annotation that is effectively mandatory:

```java
@JsonApiResource(resourceType = "users")
@OasResourceInfo(attributes = UserAttributes.class, resourceNameSingle = "user",
                 resourceIdDescription = "User unique identifier", resourceIdExample = "3")
public class UserResource implements Resource<UserDbEntity> { … }
```

## What you declare, and what it buys

| Annotation | Declares |
|---|---|
| `@OasResourceInfo` on the `Resource` | `attributes` (see above — published per direction: `<Type>Attributes` for responses and `<Type>UpdateAttributes` with no `required`, `<Type>CreateAttributes` with `required` intact), `resourceNameSingle` (the singular guess — `countries`→`country`; override when it reads wrong), `resourceIdDescription` / `resourceIdExample` (reach the `{id}` path param of *every* operation on the resource plus the `id` member of its schema), `deprecated` |
| `@OasRelationshipInfo` on the `Relationship` | `relationshipTypes` (which resource types the linkage may point at) and `resourceLinkageMetaType` (the type of the linkage's `meta`) |
| `@OasOperationInfo` on the operation class **or** one method | `summary`, `description`, `sortableFields`, `filters`, `pagination`, `deprecated`, `parameters`, `payloadType`, `securityConfig`. A method-level annotation wins over the class-level one |

## Query params are published only when declared

The framework *parses* `sort`, `filter[…]` and `page[…]` on every request, but only your operation can
act on one — so the document advertises what you declare, not what the request layer tolerates. This is
the most common surprise: a working `sort` that no client can discover.

| Param | Published when |
|---|---|
| `include` | the resource has relationships with a read operation — automatic |
| `fields[TYPE]` | the sparse-fieldsets plugin is enabled — automatic |
| `page[cursor]` | the operation is paginated — the default |
| `page[limit]` / `page[offset]` | `@OasOperationInfo(pagination = {CURSOR, LIMIT_OFFSET})` |
| `sort` | `@OasOperationInfo(sortableFields = {"fullName", "email"})` — also becomes the enum of allowed values, both directions (`fullName`, `-fullName`) |
| `filter[x]` | `@OasOperationInfo(filters = @Filter(name = "region", example = "Asia"))` — the framework owns the spelling, array shape and `maxItems` |

Limits under `jsonapi4j.validation` are projected into the schemas automatically (`page[limit]`'s
`maximum`, `include`/`sort`'s `maxItems`, the id's `maxLength`), so don't restate them.

`@OasOperationInfo(parameters = …)` documents your *own* query/path/header params — the ones reaching
you via `getCustomQueryParams()`. Naming one the framework already generates contributes only a
description and example; it will not replace the generated schema (which carries those validation
limits).

## Security

`@OasOperationInfo(securityConfig = @SecurityConfig(clientCredentialsSupported = true, pkceSupported =
true, requiredScopes = "users.read"))` opts an operation into the flows configured under
`jsonapi4j.oas.oauth2`. With the **AC plugin** on the classpath, `@AccessControl` is read instead and
wins — the document then states what is actually enforced, including `403` on guarded writes,
`security: []` for `Authenticated.ANONYMOUS` operations, and each requirement's `description()`
appended to the operation's.

**A scope is published only on a grant flow whose own `scopes` list declares it.** Requiring
`users.read` while only the PKCE flow declares it publishes PKCE alone — client-credentials is left out
rather than named with a scope it can never be granted. A scope *no* flow declares fails generation
(a typo); one declared but not by a usable flow warns and goes unpublished.

## Anything else: a customizer

`OasCustomizer` is the seam for what the framework cannot derive — gateway response headers, your own
status codes, reworded descriptions. Register it as a `@Bean` (Spring) / `@Produces` (Quarkus), or pass
it to `new JsonApiOasPlugin(props, List.of(...))` with the plain Servlet API. It runs after the
built-ins, so it sees the finished document.

Already serving a document through **springdoc**? Don't run both endpoints — springdoc's
`OpenApiCustomizer` has the same `customise(OpenAPI)` signature, so one bean is the whole integration:
`OasDocument.customizers(jsonApi4j).forEach(c -> c.customise(openApi))`.

## Watch out

- **`operationId` is part of your published contract** — client generators name methods after it
  (`get-all-users`, `create-single-user`, `get-user-citizenships-relationship`). Renaming a resource or
  relationship renames it, which is a breaking change for generated clients. Note it lower-cases
  relationship names: `placeOfBirth` → `get-user-placeofbirth-relationship`.
- **Only `diagnostics: FAIL_ON_STARTUP` is a real gate.** `WARN` and `FAIL_ON_REQUEST` build the
  document lazily, so a broken one surfaces on whichever request asks for it first — in a deployed app,
  in production. `FAIL_ON_STARTUP` builds it during boot and refuses to start, at the cost of a few
  hundred ms per boot. And note the setting governs *reports* only: a document that would say something
  untrue (two schemas claiming one name) is rejected in every mode, `DISABLED` included.
- **`requiredMode = REQUIRED` reaches the create schema only.** Attributes are published in three
  forms: `<Type>CreateAttributes` keeps `required`; `<Type>UpdateAttributes` drops it (PATCH is a
  partial update); `<Type>Attributes` (responses) drops it too, because an unset value is not
  serialized, `?fields[…]=` selects a subset, and access control withholds what the caller may not see.
  Each is published only where its operation exists — on a read-only resource the annotation publishes
  nothing. And `required` is a claim, not enforcement: the framework won't reject a create for a missing
  attribute, so keep it in step with what your `validateCreate` actually checks. Where AC guards an
  attribute, the *response* schema names the scope on it.
- **Schema names come from Java simple names**, so two resources with same-named nested types collide —
  generation fails rather than publishing one under the other's shape. Rename the Java type.
- Deprecation only widens: `@OasResourceInfo(deprecated = true)` marks every operation on the resource,
  and an operation can retire on its own. Java's `@Deprecated` is not read — use the attribute.

---

**Canonical examples in the framework**
- `examples/jsonapi4j-sampleapp-domain/.../domain/user/UserResource.java` (`@OasResourceInfo`),
  `.../operations/user/UserOperations.java` (`@OasOperationInfo`, `securityConfig`),
  `.../oas/RateLimitHeadersCustomizer.java` (`OasCustomizer`)
- Golden document (the full generated output):
  `examples/jsonapi4j-sampleapp-testsuite/src/main/resources/oas/expected-oas.json`
- Docs: https://api4.pro/openapi-plugin/ · config keys in `reference/configuration.md`
