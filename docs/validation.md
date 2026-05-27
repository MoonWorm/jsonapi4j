---
title: "Validation"
permalink: /validation/
---

JsonApi4j provides a fluent validation API that lets you declaratively validate any part of a JSON:API request — path segments, query parameters, HTTP headers, and request body — in a single builder chain. The framework collects all validation errors and returns them in one response.

### Two Layers of Validation

Validation runs at two levels:

1. **Built-in structural validation** — The framework automatically validates request structure before your operation code runs: known resource types, resource ID length, filter/sort/include limits, payload structure, and relationship integrity. These checks are driven by [validation properties](/configuration/#validation-properties).

2. **Developer validation** — Your custom business logic, wired up via the fluent API in operation-specific `validate*` methods. See [Operations — Validation](/operations/#validation) for which method to override per operation type.

Both layers use the same `JsonApiRequestValidator.forRequest()` API.

### Fluent API

The entry point is `JsonApiRequestValidator.forRequest(request)`. Chain `.path()`, `.parameters()`, `.headers()`, and body validators, then call `.validate()`:

```java
import static pro.api4.jsonapi4j.operation.validation.JsonApiRequestValidator.forRequest;
import static pro.api4.jsonapi4j.operation.validation.Validate.assertThat;

@Override
public void validateCreate(JsonApiRequest request) {
    forRequest(request)
            .path(path -> path
                    .withResourceTypeValidator(type -> type.type().isOneOf("users")))
            .parameters(params -> params
                    .withFiltersValidator(this::validateFilters)
                    .withIncludeValidator(this::validateIncludes))
            .headers(headers -> headers
                    .withHeaderValidator("X-Tenant-Id", tenantId -> tenantId.isNotBlank()))
            .singleResourceBody(UserAttributes.class, body -> body
                    .withDataValidator(data -> data.isNotNull())
                    .withResourceTypeValidator(type -> type.isOneOf("users"))
                    .withAttributesValidator(att -> {
                        att.isNotNull();
                        att.field("email", UserAttributes::getEmail).asString()
                                .isNotBlank()
                                .isEmail();
                    })
                    .withToManyRelationship("citizenships", this::citizenshipsValidator)
                    .withToOneRelationship("placeOfBirth", this::placeOfBirthValidator))
            .validate();
}
```

Every section is optional — include only the ones you need. Each validator callback receives a typed assertion object that supports fluent chaining — no need to import static assertion methods or specify error sources manually.

### Path Validation

Validates URL path segments: resource type, resource ID, and relationship name. Each callback receives a typed assertion object:

```java
.path(path -> path
        .withResourceTypeValidator(type -> type.type().isOneOf("users", "countries"))  // ResourceTypeValidationAssert
        .withResourceIdValidator(id -> id.isNotBlank().hasLengthLessThanOrEqualTo(64)) // StringValidationAssert
        .withRelationshipNameValidator(name -> name.name().isOneOf("citizenships")))   // RelationshipNameValidationAssert
```

The error source is set automatically — `{resourceType}`, `{resourceId}`, or `{relationshipName}` in the error response. `ResourceTypeValidationAssert` exposes `.type()` to navigate to the string value; `RelationshipNameValidationAssert` exposes `.name()`.

### Parameters Validation

Validates query parameters: filters, include, sort, cursor, limit, offset, field sets, and custom parameters. Each callback receives a typed assertion object:

```java
.parameters(params -> params
        .withFilterValidator("region", regions -> ...)     // CollectionValidationAssert<String>
        .withFiltersValidator(allFilters -> ...)            // MapValidationAssert<String, List<String>>
        .withIncludeValidator(includes -> ...)              // CollectionValidationAssert<String>
        .withSortValidator(sortBy -> ...)                   // MapValidationAssert<String, SortOrder>
        .withCursorValidator(cursor -> ...)                 // StringValidationAssert
        .withLimitValidator(limit -> ...)                   // NumberValidationAssert<Long>
        .withOffsetValidator(offset -> ...)                 // NumberValidationAssert<Long>
        .withFieldSetsValidator("users", fields -> ...)     // CollectionValidationAssert<String>
        .withCustomQueryParamValidator("myParam", values -> ...)) // CollectionValidationAssert<String>
```

`withFiltersValidator` receives the whole filters map as a `MapValidationAssert` — useful for cross-filter validation or checking total filter count. `withFilterValidator` validates a single named filter as a `CollectionValidationAssert`. Both can be used together.

### Headers Validation

Validates HTTP request headers. Each callback receives a `StringValidationAssert`:

```java
.headers(headers -> headers
        .withHeaderValidator("X-Tenant-Id", tenantId -> tenantId.isNotBlank())
        .withHeaderValidator("X-Correlation-Id", corrId -> corrId.isNotBlank().isUUID()))
```

### Body Validation

Three body types, matching the JSON:API operation types. Callbacks receive typed assertion objects:

**Single resource** (create/update):

```java
// Untyped attributes (framework-level)
.singleResourceBody(body -> body.withDataValidator(data -> data.isNotNull()))

// Typed attributes (developer-level)
.singleResourceBody(UserAttributes.class, body -> body
        .withDataValidator(data -> data.isNotNull())                  // ObjectValidationAssert — runs first
        .withResourceIdValidator(id -> id.isNotBlank())               // StringValidationAssert
        .withResourceTypeValidator(type -> type.isOneOf("users"))     // StringValidationAssert
        .withAttributesValidator(att -> {                              // ObjectValidationAssert<UserAttributes>
            att.isNotNull();
            att.field("email", UserAttributes::getEmail).asString()
                    .isNotBlank().isEmail();
        })
        .withRelationshipsValidator(rels -> rels.isNotEmpty())        // MapValidationAssert
        .withToOneRelationship("placeOfBirth", rel -> rel
                .withResourceIdValidator(id -> id.isNotBlank())       // StringValidationAssert
                .withResourceTypeValidator(type -> type.isOneOf("countries"))
                .withResourceIdentifierMetaValidator(meta -> meta.satisfies(...)))
        .withToManyRelationship("citizenships", rel -> rel
                .withResourceIdValidator(id -> id.isNotBlank())       // StringValidationAssert
                .withResourceTypeValidator(type -> type.isOneOf("countries"))))
```

**To-one relationship** (update):

```java
.toOneRelationshipBody(body -> body
        .withResourceIdValidator(id -> id.isNotBlank())
        .withResourceTypeValidator(type -> type.isOneOf("countries")))
```

**To-many relationship** (update/add/delete):

```java
.toManyRelationshipBody(body -> body
        .withResourceIdValidator(id -> id.isNotBlank())
        .withResourceTypeValidator(type -> type.isOneOf("countries")))
```

The `dataValidator` runs first. If it throws (e.g., data is null), the remaining body validators are skipped to prevent NPEs.

#### Partial Updates with `ifPresent()`

In update operations, clients may send only the fields they want to change — missing fields should be left untouched, not rejected. Use `ifPresent()` to skip validation when a value is null, and only validate format when the field is actually provided:

```java
@Override
public void validateUpdate(JsonApiRequest request) {
    forRequest(request)
            .singleResourceBody(UserAttributes.class, body -> body
                    .withResourceIdValidator(id -> id.exists(resourceId -> userDb.readById(resourceId) != null))
                    .withResourceTypeValidator(type -> type.isOneOf("users"))
                    .withAttributesValidator(att -> {
                        att.ifPresent(); // attributes block itself is optional in updates
                        att.field("fullName", UserAttributes::getFullName).ifPresent().asString()
                                .isNotBlank()
                                .hasLengthLessThanOrEqualTo(128);
                        att.field("email", UserAttributes::getEmail).ifPresent().asString()
                                .isEmail();
                    }))
            .validate();
}
```

Without `ifPresent()`, a null email would fail the `isEmail()` check. With it, the assertion chain is skipped entirely when the value is null — only non-null values are validated. This mirrors the JSON:API partial update semantics: absent fields mean "don't change", not "set to null".

### Automatic Error Source

The builder automatically populates the JSON:API error `source` field based on where the validator is registered. Body validators use [JSON Pointer (RFC 6901)](https://datatracker.ietf.org/doc/html/rfc6901) for the `pointer` field, as required by the [JSON:API specification](https://jsonapi.org/format/#error-objects):

| Validator location | Error source |
|---|---|
| `withResourceIdValidator` (path) | `"path": "{resourceId}"` |
| `withFilterValidator("region", ...)` | `"parameter": "filter[region]"` |
| `withHeaderValidator("X-Tenant", ...)` | `"header": "X-Tenant"` |
| `withResourceIdValidator` (body) | `"pointer": "/data/id"` |
| `withResourceTypeValidator` (body) | `"pointer": "/data/type"` |
| To-many relationship element | `"pointer": "/data/relationships/citizenships/data/0/type"` |

Single-field validators (`withResourceIdValidator`, `withResourceTypeValidator`, etc.) have their source set automatically — just chain assertions directly:

```java
// Good — builder adds the source automatically
.withResourceIdValidator(id -> id.isNotBlank())

// Source is also auto-set for relationship element validators
.withToManyRelationship("citizenships", rel -> rel
        .withResourceTypeValidator(type -> type.isOneOf("countries")))
```

Multi-field validators (`withDataValidator`, `withAttributesValidator`, `withRelationshipsValidator`) manage their own sources. Use `field(name, extractor)` to navigate to a specific field — the source is computed automatically based on the field path:

```java
.withAttributesValidator(att -> {
    att.isNotNull(); // source: /data/attributes
    att.field("email", UserAttributes::getEmail).asString()
            .isNotBlank()   // source: /data/attributes/email (auto-computed from field name)
            .isEmail();
})
```

You can also override the source explicitly using `.withSource()` on any assertion.

### Collect-All-Errors

The validator runs **all** configured validators and collects errors rather than stopping at the first failure. If multiple validators fail, the response contains all errors at once:

```json
{
  "errors": [
    {
      "id": "...",
      "status": "400",
      "code": "INVALID_ENUM_VALUE",
      "detail": "'wrong' value is not allowed, available values: [users]",
      "source": { "pointer": "/data/type" }
    },
    {
      "id": "...",
      "status": "400",
      "code": "VALUE_IS_ABSENT",
      "detail": "value can't be null",
      "source": { "pointer": "/data/attributes" }
    },
    {
      "id": "...",
      "status": "400",
      "code": "INVALID_ENUM_VALUE",
      "detail": "'wrong' value is not allowed, available values: [countries]",
      "source": { "pointer": "/data/relationships/citizenships/data/0/type" }
    }
  ]
}
```

Errors are collected **across** sections (path + parameters + headers + body) and **across** validators within each section. Each developer-provided lambda is atomic — fail-fast within a lambda, collect-all across lambdas.

When a single error is collected, a `JsonApiRequestValidationException` is thrown (single error object in the response). When multiple errors are collected, a `CompositeJsonApiRequestValidationException` is thrown (multiple error objects). Both are handled automatically by the error handler. See [Error Handling](/error-handling/) for the full exception hierarchy.

### Assertion Types

Validator callbacks receive typed assertion objects that support fluent chaining. You can also create standalone assertions via `Validate.assertThat()`:

```java
import static pro.api4.jsonapi4j.operation.validation.Validate.assertThat;

// Standalone usage (e.g., inside satisfies() or in validateDelete)
assertThat(request.getResourceId()).exists(id -> userDb.readById(id) != null);
```

**`ObjectValidationAssert`** — base type, available on all assertions:

| Method | Description |
|---|---|
| `isNull()` / `isNotNull()` | Null checks |
| `isEqualTo(expected)` / `isNotEqualTo(expected)` | Equality |
| `isIn(values)` / `isNotIn(values)` | Set membership |
| `isInstanceOf(type)` | Type check |
| `ifPresent()` | Skip remaining assertions if value is null |
| `exists(predicate)` | Throws `ResourceNotFoundException` if predicate fails |
| `field(name, extractor)` | Navigate to a nested field (auto-computes error source) |
| `satisfies(consumer)` | Run custom validation logic |
| `asString()` / `asNumber()` / `asCollection()` / `asMap()` | Type narrowing |
| `withSource(source)` / `withErrorCode(code)` / `withDetail(detail)` | Override error metadata |

**`StringValidationAssert`** — for string values (resource IDs, types, headers, cursors):

| Method | Description |
|---|---|
| `isBlank()` / `isNotBlank()` | Blank checks |
| `isEmpty()` / `isNotEmpty()` | Empty checks |
| `hasLength(n)` / `hasLengthBetween(min, max)` | Length constraints |
| `hasLengthLessThan(max)` / `hasLengthGreaterThan(min)` | Length bounds |
| `contains(s)` / `startsWith(prefix)` / `endsWith(suffix)` | Content checks |
| `matches(regex)` / `doesNotMatch(regex)` | Pattern matching |
| `isEmail()` / `isUUID()` / `isNumeric()` / `isAlphanumeric()` | Format validation |
| `isOneOf(values)` | Enum-style check |
| `isLowerCase()` / `isUpperCase()` | Case checks |

**`NumberValidationAssert`** — for numeric values (limit, offset):

| Method | Description |
|---|---|
| `isPositive()` / `isNegative()` / `isZero()` | Sign checks |
| `isGreaterThan(v)` / `isLessThan(v)` | Comparisons |
| `isBetween(start, end)` / `isStrictlyBetween(start, end)` | Range checks |

**`CollectionValidationAssert`** — for collections (filters, includes, field sets):

| Method | Description |
|---|---|
| `isEmpty()` / `isNotEmpty()` | Empty checks |
| `hasSize(n)` / `hasSizeBetween(min, max)` | Size constraints |
| `contains(element)` / `containsAll(elements)` | Content checks |
| `doesNotContainNull()` / `doesNotHaveDuplicates()` | Integrity checks |
| `allSatisfy(consumer)` | Validate each element |

**`MapValidationAssert`** — for maps (all filters, sort, relationships):

| Method | Description |
|---|---|
| `isEmpty()` / `isNotEmpty()` | Empty checks |
| `hasSize(n)` / `hasSizeLessThanOrEqualTo(max)` | Size constraints |
| `containsKey(key)` / `doesNotContainKey(key)` | Key checks |
| `containsEntry(key, value)` | Entry checks |

### Reusable Validators

Extract relationship validators as method references for reuse across create and update:

```java
private void citizenshipsValidator(ToManyRelationshipObjectValidationBuilder v) {
    v.withResourceIdValidator(id -> id.isNotBlank().satisfies(CountryResource::validateCountryId))
            .withResourceTypeValidator(type -> type.isOneOf("countries"));
}

private void placeOfBirthValidator(ToOneRelationshipObjectValidationBuilder v) {
    v.withResourceIdValidator(id -> id.isNotBlank().satisfies(CountryResource::validateCountryId))
            .withResourceTypeValidator(type -> type.isOneOf("countries"));
}

// Used in both validateCreate and validateUpdate:
.withToManyRelationship("citizenships", this::citizenshipsValidator)
.withToOneRelationship("placeOfBirth", this::placeOfBirthValidator)
```

The `satisfies(consumer)` method runs custom validation logic within the fluent chain — useful for delegating to domain-specific validators like `CountryResource::validateCountryId`.

### Built-in Structural Validation

The framework runs structural validation before your custom validators. These checks are configured via [validation properties](/configuration/#validation-properties):

| Check | Default Limit | Applies To |
|---|---|---|
| Known resource type in path | — | All operations |
| Resource ID non-blank and max length | 64 chars | Read-by-id, update, delete, all relationship ops |
| Known relationship name in path | — | All relationship operations |
| Include parameter element count | 10 | Read operations |
| Filter parameter count | 5 | Read operations |
| Filter values per parameter | 20 | Read operations |
| Sort field count | 5 | Read operations |
| Pagination limit max value | 100 | Read-multiple, read-to-many |
| Resource ID null in create body | — | Create |
| Resource ID matches path in update body | — | Update |
| Resource type matches path in body | — | Create, update |
| Relationship payload structure | — | Relationship write operations |
