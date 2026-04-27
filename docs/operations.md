---
title: "Operations"
permalink: /operations/
---

Operations define how your API reads and writes data. Read operations focus on retrieving internal models, which are then converted into JSON:API-compliant responses. Write operations accept JSON:API-compliant payloads and update the internal data accordingly.

All operation interfaces are located in the `jsonapi4j-core` module under the `pro.api4.jsonapi4j.operation` package.

By default, all **JsonApi4j** operations are exposed under the `/jsonapi` root path. This prevents conflicts when integrating JSON:API endpoints into an existing application that may have other REST endpoints. To change the root path, simply set the `jsonapi4j.root-path` property.

### Resource Operations

Every resource operation class must be annotated with `@JsonApiResourceOperation` to bind it to its resource:

```java
@JsonApiResourceOperation(resource = UserResource.class)
public class UserOperations implements ResourceOperations<UserDbEntity> {
    // ...
}
```

The `resource` attribute references the `@JsonApiResource`-annotated class this operation belongs to.

| HTTP | Endpoint | Method | Returns | Interface |
|------|----------|--------|---------|-----------|
| `GET` | `/{type}/{id}` | `readById(request)` | `RESOURCE_DTO` / `200 OK` | `ReadResourceByIdOperation` |
| `GET` | `/{type}` | `readPage(request)` | `PaginationAwareResponse<RESOURCE_DTO>` / `200 OK` | `ReadMultipleResourcesOperation` |
| `POST` | `/{type}` | `create(request)` | `RESOURCE_DTO` / `201 Created` | `CreateResourceOperation` |
| `PATCH` | `/{type}/{id}` | `update(request)` | `void` / `204 No Content` | `UpdateResourceOperation` |
| `DELETE` | `/{type}/{id}` | `delete(request)` | `void` / `204 No Content` | `DeleteResourceOperation` |

`readById` and `readPage` support compound documents (`include` query parameter). `readPage` also supports [pagination](/pagination/), [filtering and sorting](/filtering-and-sorting/).

All five operations are assembled into a single interface — `ResourceOperations<RESOURCE_DTO>`. You only need to override the methods you actually need; unimplemented operations throw `OperationNotFoundException` (404) by default.

### To-One Relationship Operations

Every relationship operation class must be annotated with `@JsonApiRelationshipOperation` to bind it to its relationship:

```java
@JsonApiRelationshipOperation(relationship = UserPlaceOfBirthRelationship.class)
public class UserPlaceOfBirthOperations implements ToOneRelationshipOperations<UserDbEntity, DownstreamCountry> {
    // ...
}
```

The `relationship` attribute references the `@JsonApiRelationship`-annotated class this operation belongs to.

| HTTP | Endpoint | Method | Returns | Interface |
|------|----------|--------|---------|-----------|
| `GET` | `/{type}/{id}/relationships/{rel}` | `readOne(request)` | `RELATIONSHIP_DTO` / `200 OK` | `ReadToOneRelationshipOperation` |
| `PATCH` | `/{type}/{id}/relationships/{rel}` | `update(request)` | `void` / `204 No Content` | `UpdateToOneRelationshipOperation` |

`readOne` supports compound documents. PATCH with `null` data removes the relationship.

All operations are assembled into `ToOneRelationshipOperations<RESOURCE_DTO, RELATIONSHIP_DTO>`.

#### Optimized Relationship Resolution with `readForResource`

`ReadToOneRelationshipOperation` also defines an optional `readForResource(request, resourceDto)` method. This is invoked during the [relationship resolution stage](/request-processing-pipeline/#6-fetch-relationship-data-parallel) when the `include` query parameter is specified for a resource read operation.

The default implementation calls `readOne(request)`, which typically makes a separate data source call. If the parent resource already contains the relationship data, you can override `readForResource` to extract it directly — avoiding an extra query:

```java
@Override
public DownstreamCountry readForResource(JsonApiRequest request, UserDbEntity user) {
    // The user entity already holds the country — no need for a separate lookup
    return user.getPlaceOfBirth();
}
```

### To-Many Relationship Operations

Same as To-One — annotate with `@JsonApiRelationshipOperation`:

```java
@JsonApiRelationshipOperation(relationship = UserCitizenshipsRelationship.class)
public class UserCitizenshipsOperations implements ToManyRelationshipOperations<UserDbEntity, DownstreamCountry> {
    // ...
}
```

| HTTP | Endpoint | Method | Returns | Interface |
|------|----------|--------|---------|-----------|
| `GET` | `/{type}/{id}/relationships/{rel}` | `readMany(request)` | `PaginationAwareResponse<RELATIONSHIP_DTO>` / `200 OK` | `ReadToManyRelationshipOperation` |
| `PATCH` | `/{type}/{id}/relationships/{rel}` | `update(request)` | `void` / `204 No Content` | `UpdateToManyRelationshipOperation` |
| `POST` | `/{type}/{id}/relationships/{rel}` | `add(request)` | `void` / `204 No Content` | `AddToManyRelationshipOperation` |
| `DELETE` | `/{type}/{id}/relationships/{rel}` | `delete(request)` | `void` / `204 No Content` | `DeleteToManyRelationshipOperation` |

`readMany` supports compound documents, filtering, and ordering. PATCH performs a complete replacement (empty array removes all members). POST is idempotent — members that already exist are not duplicated.

All operations are assembled into `ToManyRelationshipOperations<RESOURCE_DTO, RELATIONSHIP_DTO>`.

`ReadToManyRelationshipOperation` also supports the same `readForResource` optimization described above, returning `PaginationAwareResponse<RELATIONSHIP_DTO>` instead of a single item.

### Accessing Request Payload

Write operations (POST, PATCH, DELETE on relationships) receive the client's JSON:API document via `JsonApiRequest`. The payload access method depends on the operation type:

| Operation target | Method | Returns |
|-----------------|--------|---------|
| Resource (POST/PATCH) | `request.getSingleResourceDocPayload(attType, relType)` | `SingleResourceDoc<ResourceObject<A, R>>` |
| To-One relationship | `request.getToOneRelationshipDocPayload()` | `ToOneRelationshipDoc` |
| To-Many relationship | `request.getToManyRelationshipDocPayload()` | `ToManyRelationshipsDoc` |

For resource payloads, pass your attributes and relationships classes to get typed access. Without type arguments, attributes and relationships are deserialized as `LinkedHashMap`:

```java
@Override
public UserDto create(JsonApiRequest request) {
    // Typed — attributes are deserialized into UserAttributes
    var payload = request.getSingleResourceDocPayload(UserAttributes.class, Void.class);
    UserAttributes attributes = payload.getData().getAttributes();

    return userDb.create(attributes.getFullName(), attributes.getEmail());
}
```

### Validation

Every operation has a `validate(JsonApiRequest request)` method that runs before the main logic. When implementing `ResourceOperations`, `ToOneRelationshipOperations`, or `ToManyRelationshipOperations`, the default `validate` dispatches to operation-specific validators based on the operation type:

**ResourceOperations:**

| Validator method | Runs before |
|-----------------|-------------|
| `validateReadById(request)` | `readById` |
| `validateReadMultiple(request)` | `readPage` |
| `validateCreate(request)` | `create` |
| `validateUpdate(request)` | `update` |
| `validateDelete(request)` | `delete` |

**ToOneRelationshipOperations:**

| Validator method | Runs before |
|-----------------|-------------|
| `validateReadToOne(request)` | `readOne` |
| `validateUpdateToOne(request)` | `update` |

**ToManyRelationshipOperations:**

| Validator method | Runs before |
|-----------------|-------------|
| `validateReadToMany(request)` | `readMany` |
| `validateUpdateToMany(request)` | `update` |
| `validateAddToMany(request)` | `add` |
| `validateDeleteFromToMany(request)` | `delete` |

Each validator has a default implementation that runs the operation's built-in validation. Override only the ones where you need custom logic.

Every operation also provides a `getValidator()` method that returns a pre-configured `JsonApi4jDefaultValidator` instance. This validator offers common checks — resource ID length, payload structure, filter/include/sort limits — all driven by the [validation properties](/configuration/#validation-properties). Use it in your custom validators to avoid re-implementing standard checks:

```java
@Override
public void validateCreate(JsonApiRequest request) {
    var payload = request.getSingleResourceDocPayload(UserAttributes.class, Void.class);
    getValidator().validateSingleResourceDoc(payload);
    String email = payload.getData().getAttributes().getEmail();
    if (email == null || !email.contains("@")) {
        throw new ConstraintViolationException(
            DefaultErrorCodes.VALUE_INVALID_FORMAT,
            "Invalid email format", "email"
        );
    }
}
```

For validation failures, throw `ConstraintViolationException` with an error code, detail message, and the offending parameter name. If a resource is not found, throw `ResourceNotFoundException`. For other scenarios, throw `JsonApi4jException` with `httpStatus`, `errorCode`, and `detail`.

All exceptions are automatically converted into JSON:API-compliant error responses. See the [Error Handling](/error-handling/) page for the full exception hierarchy, built-in error mappings, JSR-380 integration, and how to register custom error handler factories.

### Override HTTP Response Status and Headers

JsonApi4j automatically determines the HTTP response status code based on the operation type (e.g. `200` for reads, `201` for creates, `204` for updates and deletes). However, there are cases where you may need to override the default status code or add custom headers to the response from within your operation logic.

#### Override response status

Use `ResponseStatus.overrideResponseStatus(HttpStatusCodes)` to override the HTTP status code for the current request. This is useful when the default status code doesn't match your business logic — for example, returning `202 Accepted` instead of the default `204 No Content` for an asynchronous delete operation.

```java
@Override
public void delete(JsonApiRequest request) {
    myService.deleteAsync(request.getResourceId());
    ResponseStatus.overrideResponseStatus(HttpStatusCodes.SC_202_ACCEPTED);
}
```

The overridden status is stored in a `ThreadLocal` and applied after the operation completes. It takes effect only for the current request and is automatically cleaned up.

All standard HTTP status codes are available via the `HttpStatusCodes` enum.

#### Propagate custom response headers

Use `ResponseHeaders.propagateHeader(String, String)` to add custom headers to the outgoing HTTP response from within your operation logic.

```java
@Override
public UserDto readById(JsonApiRequest request) {
    UserDto user = myService.getUser(request.getResourceId());
    ResponseHeaders.propagateHeader("X-Resource-Version", user.getVersion());
    return user;
}
```

Multiple values for the same header name are supported — each call accumulates values that are all added as separate header entries.

```java
ResponseHeaders.propagateHeader("X-Upstream-Timing", "db=12ms");
ResponseHeaders.propagateHeader("X-Upstream-Timing", "cache=3ms");
```

Headers are stored in a request-scoped `ThreadLocal` context and flushed to the HTTP response at the end of request processing.

`Cache-Control` headers receive special treatment — they are only propagated for `2xx` responses and will not override an existing `Cache-Control` header. For `Cache-Control` specifically, use the dedicated `ResponseHeaders.propagateCacheControl(CacheControlDirectives)` method.
