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
import static pro.api4.jsonapi4j.operation.validation.ValidationAssertions.*;

@Override
public void validateCreate(JsonApiRequest request) {
    forRequest(request)
            .path(path -> path
                    .withResourceTypeValidator(type -> validateValueAnyOf(type.getType(), Set.of("users"))))
            .parameters(params -> params
                    .withFiltersValidator(this::validateFilters)
                    .withIncludeValidator(this::validateIncludes))
            .headers(headers -> headers
                    .withHeaderValidator("X-Tenant-Id", this::validateTenantHeader))
            .singleResourceBody(UserAttributes.class, body -> body
                    .withDataValidator(data -> validateNonNull(data, ErrorSources.pointer().data().toPointer()))
                    .withResourceTypeValidator(type -> {
                        validateNonBlank(type);
                        validateEqualTo(type, "users");
                    })
                    .withAttributesValidator(att -> {
                        validateNonNull(att, ErrorSources.pointer().data().attributes());
                        validateNonNull(att.getEmail(), ErrorSources.pointer().data().attributes("email"));
                    })
                    .withToManyRelationship("citizenships", this::citizenshipsValidator)
                    .withToOneRelationship("placeOfBirth", this::placeOfBirthValidator))
            .validate();
}
```

Every section is optional — include only the ones you need.

### Path Validation

Validates URL path segments: resource type, resource ID, and relationship name.

```java
.path(path -> path
        .withResourceTypeValidator(type -> ...)
        .withResourceIdValidator(id -> ...)
        .withRelationshipNameValidator(name -> ...))
```

The source is set automatically — `{resourceType}`, `{resourceId}`, or `{relationshipName}` in the error response.

### Parameters Validation

Validates query parameters: filters, include, sort, cursor, limit, offset, field sets, and custom parameters.

```java
.parameters(params -> params
        .withFilterValidator("region", regions -> ...)     // single filter by name
        .withFiltersValidator(allFilters -> ...)            // entire filters map
        .withIncludeValidator(includes -> ...)
        .withSortValidator(sortBy -> ...)
        .withCursorValidator(cursor -> ...)
        .withLimitValidator(limit -> ...)
        .withOffsetValidator(offset -> ...)
        .withFieldSetsValidator("users", fields -> ...)
        .withCustomQueryParamValidator("myParam", values -> ...))
```

`withFiltersValidator` receives the whole `Map<String, List<String>>` — useful for cross-filter validation or checking total filter count. `withFilterValidator` validates a single named filter. Both can be used together.

### Headers Validation

Validates HTTP request headers.

```java
.headers(headers -> headers
        .withHeaderValidator("X-Tenant-Id", tenantId -> validateNonBlank(tenantId))
        .withHeaderValidator("X-Correlation-Id", corrId -> ...))
```

### Body Validation

Three body types, matching the JSON:API operation types:

**Single resource** (create/update):

```java
// Untyped attributes (framework-level)
.singleResourceBody(body -> body.withDataValidator(...))

// Typed attributes (developer-level)
.singleResourceBody(UserAttributes.class, body -> body
        .withDataValidator(data -> ...)          // validates the entire ResourceObject, runs first
        .withResourceIdValidator(id -> ...)      // validates data.id
        .withResourceTypeValidator(type -> ...)   // validates data.type
        .withAttributesValidator(att -> ...)      // validates typed attributes
        .withRelationshipsValidator(rels -> ...)  // validates raw relationships map
        .withToOneRelationship("placeOfBirth", rel -> rel
                .withResourceIdValidator(id -> ...)
                .withResourceTypeValidator(type -> ...)
                .withResourceIdentifierMetaValidator(meta -> ...))
        .withToManyRelationship("citizenships", rel -> rel
                .withResourceIdValidator(id -> ...)
                .withResourceTypeValidator(type -> ...)))
```

**To-one relationship** (update):

```java
.toOneRelationshipBody(body -> body
        .withResourceIdValidator(id -> ...)
        .withResourceTypeValidator(type -> ...))
```

**To-many relationship** (update/add/delete):

```java
.toManyRelationshipBody(body -> body
        .withResourceIdValidator(id -> ...)
        .withResourceTypeValidator(type -> ...))
```

The `dataValidator` runs first. If it throws (e.g., data is null), the remaining body validators are skipped to prevent NPEs.

### Automatic Error Source

The builder automatically populates the JSON:API error `source` field based on where the validator is registered:

| Validator location | Error source |
|---|---|
| `withResourceIdValidator` (path) | `"path": "{resourceId}"` |
| `withFilterValidator("region", ...)` | `"parameter": "filter[region]"` |
| `withHeaderValidator("X-Tenant", ...)` | `"header": "X-Tenant"` |
| `withResourceIdValidator` (body) | `"pointer": "/data/id"` |
| `withResourceTypeValidator` (body) | `"pointer": "/data/type"` |
| To-many relationship element | `"pointer": "/data/relationships/citizenships/data/0/type"` |

Validators that receive the source automatically (resource ID, resource type, single-field validators) should throw **without** specifying a source — the builder wraps it:

```java
// Good — builder adds the source
.withResourceIdValidator(id -> validateNonBlank(id))

// Not needed — source would be overridden anyway
.withResourceIdValidator(id -> validateNonBlank(id, ErrorSources.pointer().data().id()))
```

Validators that can check multiple fields (`withDataValidator`, `withAttributesValidator`, `withRelationshipsValidator`) manage their own sources, because the builder can't know which specific field triggered the error.

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

### ValidationAssertions

Common assertion methods available via static import:

```java
import static pro.api4.jsonapi4j.operation.validation.ValidationAssertions.*;
```

| Method | Error Code | Description |
|---|---|---|
| `validateNonNull(value)` | `VALUE_IS_ABSENT` | Value must not be null |
| `validateNonBlank(value)` | `VALUE_EMPTY` | String must not be blank |
| `validateIsNull(value)` | `VALUE_IS_NOT_ABSENT` | Value must be null |
| `validateEqualTo(actual, expected)` | `VALUE_IS_NOT_EQUAL_TO` | Values must be equal |
| `validateValueAnyOf(value, allowedValues)` | `INVALID_ENUM_VALUE` | Value must be one of the allowed set |

All methods have an overload that accepts an `ErrorSources.Source` for cases where you need to set the source explicitly (e.g., inside `withAttributesValidator`).

### Reusable Validators

Extract relationship validators as method references for reuse across create and update:

```java
private void citizenshipsValidator(ToManyRelationshipObjectValidationBuilder v) {
    v.withResourceIdValidator(CountryValidator::validateCountryId)
            .withResourceTypeValidator(type -> validateValueAnyOf(type, Set.of("countries")));
}

private void placeOfBirthValidator(ToOneRelationshipObjectValidationBuilder v) {
    v.withResourceIdValidator(CountryValidator::validateCountryId)
            .withResourceTypeValidator(type -> validateValueAnyOf(type, Set.of("countries")));
}

// Used in both validateCreate and validateUpdate:
.withToManyRelationship("citizenships", this::citizenshipsValidator)
.withToOneRelationship("placeOfBirth", this::placeOfBirthValidator)
```

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
