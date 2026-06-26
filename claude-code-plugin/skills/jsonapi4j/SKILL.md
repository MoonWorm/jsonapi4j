---
name: jsonapi4j
description: How to build and reason about JSON:API backends with the jsonapi4j framework (pro.api4) — resources, relationships (to-one/to-many), operations, compound documents/includes, validation, security, config, and RestAssured testing. Use whenever working in a codebase that depends on jsonapi4j (imports under pro.api4.jsonapi4j, @JsonApiResource / @JsonApiRelationship / @JsonApiResourceOperation annotations, a /jsonapi rootPath), or when the user mentions jsonapi4j, JSON:API resources/relationships/compound-docs in such a project.
---

# Working with jsonapi4j

jsonapi4j (`pro.api4`, by Aliaksei Taliuk) is a persistence-agnostic Java framework for building
[JSON:API](https://jsonapi.org/)-compliant REST APIs. No JPA/ORM required — it works over any data
source (JdbcTemplate, a REST client, in-memory, …). It ships **three** integrations: **Spring Boot**,
**Quarkus**, and plain **Jakarta Servlet**. Docs: https://api4.pro/.

This skill is the consumer's guide — how to *build with* the framework. Keep `SKILL.md` lean; open the
matching `reference/<file>.md` when you go deep on a topic (paths below). When you confirm a new pattern
or quirk, add it to the relevant reference file.

**Versioned behavior:** edge behaviors are version-specific (current line is 1.8.x). Treat the
"known behaviors" notes as "verify against the version on your classpath and the framework's own
tests," not gospel. The canonical, runnable reference is the framework's `examples/` sample apps
(Spring Boot / Quarkus / Servlet over a shared domain).

## Mental model: the three-part anatomy

Every resource (and every relationship) is split into small single-purpose classes, auto-discovered as
beans (Spring `@Component` / Quarkus CDI / registered in the Servlet context):

1. **`Resource<DTO>`** — maps a backend DTO to a JSON:API resource: `resolveResourceId` +
   `resolveAttributes`. Annotated `@JsonApiResource(resourceType = "users")`.
2. **`Relationship<REF>`** — maps to a *resource identifier* (`{type, id}` + optional identifier meta/links).
   It has **no attributes**. Annotated
   `@JsonApiRelationship(relationshipName = "citizenships", parentResource = UserResource.class)`.
   Use `ToOneRelationship<REF>` / `ToManyRelationship<REF>`.
3. **`*Operations`** — the data access. Annotated `@JsonApiResourceOperation(resource = ...)` or
   `@JsonApiRelationshipOperation(relationship = ...)`. Implement the composite
   `ResourceOperations<DTO>` / `ToOneRelationshipOperations<PARENT, CHILD>` /
   `ToManyRelationshipOperations<PARENT, CHILD>` — each method defaults to throwing
   `OperationNotFoundException`, so you override only what your resource actually supports, plus the
   matching `validateXxx` hooks.
4. **The DTO is deliberately *wider* than the attributes** — it also carries the foreign keys that
   relationships need (so relationships can resolve linkage off the parent without extra queries).

```java
@Component
@JsonApiResource(resourceType = "users")
public class UserResource implements Resource<UserDbEntity> {
    public String resolveResourceId(UserDbEntity u) { return u.getId(); }
    public UserAttributes resolveAttributes(UserDbEntity u) {
        return new UserAttributes(u.getFullName(), u.getEmail());
    }
}
```

(See the real thing: `examples/jsonapi4j-sampleapp-domain/.../domain/user/UserResource.java`.)

## Quick checklist for a new resource

1. **DTO** (carry FKs for relationships) + `Resource<DTO>` + an `*Attributes` class holding **only this
   resource's own data**.
2. **`ResourceOperations<DTO>`**: `readById`, `readPage` (+ `validateReadById` / `validateReadMultiple`);
   throw `ResourceNotFoundException` on a miss. Choose a pagination strategy for `readPage`.
3. **Relationships**: `ToOne/ToManyRelationship<Ref>` — **prefer lightweight refs** over heavy DTOs (any
   type works; only `id`/`type`/meta are read, so it's your call);
   `*Operations` with `readOne`/`readMany` (+ `readOneForResource`/`readManyForResource` and/or batch
   ops to avoid N+1 on includes).
4. **Security**: add your web framework's URL-prefix rule if the resource is public; layer the AC plugin
   for field-level/ownership gating.
5. **Compound docs**: if the type is includable, add a `jsonapi4j.cd.mapping.<type>` entry
   (+ `batchSizeMapping`) and support `filter[id]` on its `readPage`.
6. **Tests**: RestAssured by-id/filter/404/400 (random port) + `?include=` cases (fixed port / CD profile).

## Reference index — open on demand

- **`reference/resources-and-operations.md`** — Resource contract, the composite operations, pagination
  modes and `PaginationAwareResponse` factories (incl. `null` vs `empty()`), the one-mapper-per-resource
  data-layer rule.
- **`reference/relationships.md`** — to-one/to-many, lightweight refs vs full DTOs,
  `readOneForResource`/`readManyForResource` and batch ops (N+1 avoidance), edge data in identifier meta.
- **`reference/compound-documents.md`** — `?include=`, `cd.mapping` self-HTTP resolution, multi-hop,
  synthetic-primary caveat, header/param propagation.
- **`reference/performance.md`** — `filter[id]` batching, in-house resolution, parallel `ExecutorService`,
  hop/size caps, the compound-docs cache + `Cache-Control`.
- **`reference/validation-and-security.md`** — `validateXxx` hooks, the fluent `JsonApiRequestValidator`,
  exceptions (400/404/custom), the two independent security layers + the AC plugin.
- **`reference/configuration.md`** — the `jsonapi4j.*` property reference.
- **`reference/testing.md`** — RestAssured black-box patterns; random vs **fixed** port for `?include=`
  tests; test profiles; Testcontainers/Flyway/auth stubs.
- **`reference/separation-of-concerns.md`** — attribute ownership; migrating a denormalized field out to
  an `?include`d relationship safely.
- **`reference/known-behaviors.md`** — version-specific quirks (related-URL 404s, to-one linkage
  rendering, null refs, cursor meta key, enum attributes, etc.).
