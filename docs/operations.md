---
title: "Operations"
permalink: /operations/
---

Operations focus on retrieving internal models, which are then converted into JSON:API-compliant responses. Operations that modify data accept JSON:API-compliant payloads and update the internal data accordingly.

The JSON:API specification defines a limited set of standard operations. Some variations with JSON:API specification are acceptable, but the framework selects the one that makes the most sense for a given context.

All operation interfaces are located in the `jsonapi4j-core` module under the `pro.api4.jsonapi4j.operation` package.

By default, all **JsonApi4j** operations are exposed under the `/jsonapi` root path. This prevents conflicts when integrating JSON:API endpoints into an existing application that may have other REST endpoints. To change the root path, simply set the `jsonapi4j.root-path` property.

Let's dig deeper into supported operations.

### Resource-related operations

Here is the list of resource-related operations supported by the framework:
* `ReadResourceByIdOperation<RESOURCE_DTO>` - available under `GET /{resource-type}/{resource-id}`, supports compound documents JSON:API feature. Returns `200 OK`.
  * `RESOURCE_DTO readById(JsonApiRequest request)` - reads a single internal object representing a JSON:API resource of the specified type.
* `ReadMultipleResourcesOperation<RESOURCE_DTO>` - available under `GET /{resource-type}`, supports compound documents, filtering, and ordering JSON:API features. Returns `200 OK`.
  * `CursorPageableResponse<RESOURCE_DTO> readPage(JsonApiRequest request)` - reads multiple internal objects representing JSON:API resources of the specified type.
* `CreateResourceOperation<RESOURCE_DTO>` - available under `POST /{resource-type}`, accepts valid JSON:API Document as a payload. Returns `201 Created`.
  * `RESOURCE_DTO create(JsonApiRequest request)` - creates a single object in the backend system and returns its internal representation.
* `UpdateResourceOperation` - available under `PATCH /{resource-type}/{resource-id}`, accepts valid JSON:API Document as a payload. Returns `204 No Content`.
  * `void update(JsonApiRequest request)` - updates a single object in the backend system.
* `DeleteResourceOperation` - available under `DELETE /{resource-type}/{resource-id}`. Returns `204 No Content`.
  * `void delete(JsonApiRequest request)` - deletes a single object in the backend system.

All these operations are assembled into a single interface - `ResourceOperations<RESOURCE_DTO>` - for simplicity. This way, the developer does not need to remember which operation to implement, as everything is defined in one place. You only need to override the methods you actually need. Although the framework supports multiple approaches, this is the recommended way to implement resource-related operations.

### To-One-Relationship-related operations

Here is the list of To-One-Relationship-related operations supported by the framework:
* `ReadToOneRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO>` - available under `GET /{resource-type}/{resource-id}/relationships/{relationship-name}`, supports compound documents JSON:API feature. Returns `200 OK`.
  * `readOne(JsonApiRequest relationshipRequest)` - reads a single internal object representing a JSON:API resource identifier for the given to-one resource relationship.
  * `readForResource(JsonApiRequest relationshipRequest, RESOURCE_DTO resourceDto)` - optional. Resolves an internal relationship's object directly from the parent resource's internal object if it's possible. This avoids an external request. Used during the [relationship resolution stage](/request-processing-pipeline/#6-fetch-relationship-data-parallel) when the `include` query parameter is specified for any resource-related read operation.
* `UpdateToOneRelationshipOperation` - available under `PATCH /{resource-type}/{resource-id}/relationships/{relationship-name}`, accepts valid JSON:API Document as a payload. Returns `204 No Content`.
    * `void update(JsonApiRequest request)` - updates or deletes a single resource linkage representing a To-One JSON:API relationship in the backend.

The same as for resource - all these operations are also assembled into a single interface - `ToOneRelationshipOperations<RESOURCE_DTO, RELATIONSHIP_DTO>`. This is the preferred way to implement operations for To-One relationships.

### To-Many-Relationship-related operations

Here is the list of To-Many-Relationship-related operations supported by the framework:
* `ReadToManyRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO>` - available under `GET /{resource-type}/{resource-id}/relationships/{relationship-name}`, supports compound documents, filtering, and ordering JSON:API features. Returns `200 OK`.
  * `CursorPageableResponse<RELATIONSHIP_DTO> readMany(JsonApiRequest relationshipRequest)` - similar to `ReadToOneRelationshipOperation` but returns a pageable collection of objects.
  * `CursorPageableResponse<RELATIONSHIP_DTO> readForResource(JsonApiRequest relationshipRequest, RESOURCE_DTO resourceDto)` - similar to `ReadToOneRelationshipOperation` but returns a pageable collection of objects. Invoked during the [relationship resolution stage](/request-processing-pipeline/#6-fetch-relationship-data-parallel).
* `UpdateToManyRelationshipOperation` - available under `PATCH /{resource-type}/{resource-id}/relationships/{relationship-name}`, accepts valid JSON:API Document as a payload. Returns `204 No Content`.
  * `void update(JsonApiRequest request)` - performs a complete replacement of all resource linkages for a To-Many JSON:API relationship in the backend. Sending an empty array removes all members.
* `AddToManyRelationshipOperation` - available under `POST /{resource-type}/{resource-id}/relationships/{relationship-name}`, accepts valid JSON:API Document as a payload. Returns `204 No Content`.
  * `void add(JsonApiRequest request)` - adds the specified members to the to-many relationship. Members that already exist in the relationship are not added again (idempotent).
* `DeleteToManyRelationshipOperation` - available under `DELETE /{resource-type}/{resource-id}/relationships/{relationship-name}`, accepts valid JSON:API Document as a payload. Returns `204 No Content`.
  * `void delete(JsonApiRequest request)` - removes the specified members from the to-many relationship.

The same as for other two operation types - all these operations are also assembled into a single interface - `ToManyRelationshipOperations<RESOURCE_DTO, RELATIONSHIP_DTO>`. This is the preferred way to implement operations for To-Many relationships.

### Validation
* Every operation has an optional `validate(JsonApiRequest request)` method sometimes with a default generic implementation. It is recommended to place all input validation logic here, keeping the main business logic in the corresponding operation method.
* There is more validation-specific methods you can override when implementing `ResourceOperations<RESOURCE_DTO>`, `ToOneRelationshipOperations<RESOURCE_DTO, RELATIONSHIP_DTO>` or `ToManyRelationshipOperations<RESOURCE_DTO, RELATIONSHIP_DTO>`.
* If a resource is not found in the backend system, throw `ResourceNotFoundException` or use `throwResourceNotFoundException(...)` method. This will generate a JSON:API compliant error response.
* For other scenarios, throw `JsonApi4jException` and specify `httpStatus`, `errorCode`, and `detail`. This will generate a JSON:API compliant error response.
* See **Register custom error handlers** chapter for additional ways to handle errors, for example, integration with custom validation frameworks.

### Register custom error handlers

It's also possible to declare a custom `ErrorHandlerFactory` and register it in the `JsonApi4jErrorHandlerFactoriesRegistry`. This allows you to extend the default error-handling behavior.

Two error handler factories are registered by default:

* `DefaultErrorHandlerFactory` - encapsulates the logic for mapping framework-specific exceptions (such as `JsonApi4jException`, `ResourceNotFoundException`, and other technical exceptions) into JSON:API-compliant error documents
* `Jsr380ErrorHandlers` - encapsulates the logic for mapping `jakarta.validation.ConstraintViolationException` exception (JSR-380) into JSON:API error documents.

### Override HTTP response status and headers

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
