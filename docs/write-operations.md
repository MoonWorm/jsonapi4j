---
title: "Write Operations"
permalink: /write-operations/
---

This guide continues the domain from the [Getting Started](/getting-started/) guide — the same `users` resource and `UserOperations` class. Here we'll add **create**, **update**, and **delete** operations to turn the read-only API into a full CRUD service.

If you haven't completed the Getting Started guide yet, start there first.

### What We'll Build

By the end of this page, the `users` resource will support:

| HTTP | Endpoint | Operation | Response |
|------|----------|-----------|----------|
| `GET` | `/users` | Read multiple | `200 OK` (already implemented) |
| `POST` | `/users` | Create | `201 Created` |
| `PATCH` | `/users/{id}` | Update | `204 No Content` |
| `DELETE` | `/users/{id}` | Delete | `204 No Content` |

### 1. Accessing the Request Payload

Write operations receive a JSON:API document as the request body. The framework parses it and makes it available via `request.getSingleResourceDocPayload()`. You pass your attributes class to get typed access:

```java
var payload = request.getSingleResourceDocPayload(UserAttributes.class);
UserAttributes attributes = payload.getData().getAttributes();
```

Without a type argument, attributes are deserialized as `LinkedHashMap`.

### 2. Add Create Operation

Add the `create` method to the existing `UserOperations` class:

```java
@JsonApiResourceOperation(resource = UserResource.class)
public class UserOperations implements ResourceOperations<UserDbEntity> {

    private final UserDb userDb;

    public UserOperations(UserDb userDb) {
        this.userDb = userDb;
    }

    // readPage — already implemented in Getting Started
    @Override
    public PaginationAwareResponse<UserDbEntity> readPage(JsonApiRequest request) {
        // ...
    }

    @Override
    public UserDbEntity create(JsonApiRequest request) {
        var payload = request.getSingleResourceDocPayload(UserAttributes.class);
        UserAttributes attributes = payload.getData().getAttributes();
        return userDb.createUser(
                attributes.getFullName(),
                attributes.getEmail(),
                attributes.getCreditCardNumber()
        );
    }
}
```

The `create` method returns a `UserDbEntity` — the framework uses it to compose a `201 Created` response with the newly created resource, including its server-generated `id`.

Extend `UserDb` to support creation:

```java
public class UserDb {

    // ... existing code ...

    public UserDbEntity createUser(String fullName, String email, String creditCardNumber) {
        String id = String.valueOf(users.size() + 1);
        UserDbEntity entity = new UserDbEntity(id, fullName, email, creditCardNumber);
        users.put(id, entity);
        return entity;
    }
}
```

#### Request

`POST /users`

```json
{
  "data": {
    "type": "users",
    "attributes": {
      "fullName": "Alice Smith",
      "email": "alice@example.com",
      "creditCardNumber": "999888777"
    }
  }
}
```

#### Response — `201 Created`

```json
{
  "data": {
    "attributes": {
      "fullName": "Alice Smith",
      "email": "alice@example.com",
      "creditCardNumber": "999888777"
    },
    "links": {
      "self": "/users/6"
    },
    "id": "6",
    "type": "users"
  }
}
```

### 3. Add Update Operation

Add the `update` method to the same `UserOperations` class:

```java
@Override
public void update(JsonApiRequest request) {
    var payload = request.getSingleResourceDocPayload(UserAttributes.class);
    UserAttributes attributes = payload.getData().getAttributes();
    userDb.updateUser(
            request.getResourceId(),
            attributes.getFullName(),
            attributes.getEmail(),
            attributes.getCreditCardNumber()
    );
}
```

The `update` method returns `void` — the framework returns `204 No Content` automatically.

The resource ID comes from the URL path (`/users/3`), available via `request.getResourceId()`. The updated attributes come from the request body.

Extend `UserDb`:

```java
public void updateUser(String id, String fullName, String email, String creditCardNumber) {
    if (!users.containsKey(id)) {
        throw new ResourceNotFoundException(id, new ResourceType("users"));
    }
    users.put(id, new UserDbEntity(id, fullName, email, creditCardNumber));
}
```

#### Request

`PATCH /users/3`

```json
{
  "data": {
    "type": "users",
    "id": "3",
    "attributes": {
      "fullName": "Jack Updated",
      "email": "jack.updated@doe.com",
      "creditCardNumber": "333456789"
    }
  }
}
```

#### Response — `204 No Content`

Empty body.

#### Partial updates

The request above sends every attribute, which sidesteps a question real clients raise immediately: what
happens to an attribute the payload does not mention?

The specification answers it, and does not leave the choice to you —
[Updating a Resource's Attributes](https://jsonapi.org/format/#crud-updating-resource-attributes) requires
that missing attributes be interpreted as if they were sent with their current values, and explicitly
forbids treating them as `null`. A `PATCH` carrying only `email` must leave `fullName` alone.

A member that *is* present replaces the old value in full. For an array- or object-valued attribute that
means wholesale replacement, not a merge — the same stance the specification takes for to-many
relationships.

That leaves one practical problem: a typed payload cannot tell the two cases apart. Jackson binds both an
absent member and an explicit `"fullName": null` to `null`, so `attributes.getFullName()` cannot answer
"did the client send this?". Checking for null is the usual workaround and it satisfies the specification,
but it has a cost worth knowing about: an attribute can never be *cleared*, because the request that would
clear it is indistinguishable from the request that omits it.

When that distinction matters, read the attributes untyped — `getSingleResourceDocPayload()` with no
argument returns them as a `LinkedHashMap`, where the key's presence is the answer:

```java
@Override
public void update(JsonApiRequest request) {
    var raw = request.getSingleResourceDocPayload().getData().getAttributes();

    Map<String, Object> changes = new LinkedHashMap<>();
    for (String attribute : List.of("fullName", "email", "creditCardNumber")) {
        if (raw.containsKey(attribute)) {
            changes.put(attribute, raw.get(attribute));
        }
    }

    userDb.applyChanges(request.getResourceId(), changes);
}
```

`changes` now holds exactly what the client sent, with an explicit `null` preserved and distinguishable
from an omission — which is what lets a persistence layer apply a genuine partial update.

Both forms may be used in the same operation — payloads are deserialized once per type and cached — so the
typed accessors can stay in place for attributes that are always sent, with the untyped view reserved for
those that need "absent" and "null" to mean different things.

### 4. Add Delete Operation

Add the `delete` method:

```java
@Override
public void delete(JsonApiRequest request) {
    userDb.deleteUser(request.getResourceId());
}
```

No request body is needed — the resource ID comes from the URL path.

Extend `UserDb`:

```java
public void deleteUser(String id) {
    if (!users.containsKey(id)) {
        throw new ResourceNotFoundException(id, new ResourceType("users"));
    }
    users.remove(id);
}
```

#### Request

`DELETE /users/3`

#### Response — `204 No Content`

Empty body.

### 5. Add Validation

Each operation has a dedicated validation method that runs before the main logic. Use `JsonApiRequestValidator.forRequest(request)` to build validation rules declaratively. Each validator callback receives a typed assertion object that supports fluent chaining:

```java
import static pro.api4.jsonapi4j.operation.validation.JsonApiRequestValidator.forRequest;

@Override
public void validateCreate(JsonApiRequest request) {
    forRequest(request)
            .singleResourceBody(UserAttributes.class, body -> body
                    .withResourceTypeValidator(type -> type.isOneOf("users"))
                    .withAttributesValidator(att -> {
                        att.isNotNull();
                        att.field("email", UserAttributes::getEmail).asString()
                                .isNotBlank()
                                .isEmail();
                    }))
            .validate();
}
```

The validator **collects all errors** across sections and returns them in a single response. If both the resource type and email are invalid, the client gets both errors at once:

```json
{
  "errors": [
    {
      "id": "...",
      "status": "400",
      "code": "INVALID_ENUM_VALUE",
      "detail": "'wrong' value is not allowed, available values: [users]",
      "source": {
        "pointer": "/data/type"
      }
    },
    {
      "id": "...",
      "status": "400",
      "code": "VALUE_INVALID_FORMAT",
      "detail": "Invalid email format",
      "source": {
        "pointer": "/data/attributes/email"
      }
    }
  ]
}
```

For a full list of validation methods per operation type, see [Operations — Validation](/operations/#validation). For the complete error handling story, see [Error Handling](/error-handling/).

### Summary

The complete `UserOperations` now looks like this:

```java
@JsonApiResourceOperation(resource = UserResource.class)
public class UserOperations implements ResourceOperations<UserDbEntity> {

    private final UserDb userDb;

    public UserOperations(UserDb userDb) {
        this.userDb = userDb;
    }

    @Override
    public PaginationAwareResponse<UserDbEntity> readPage(JsonApiRequest request) {
        UserDb.DbPage<UserDbEntity> pagedResult = userDb.readAllUsers(request.getCursor());
        return PaginationAwareResponse.cursorAware(
                pagedResult.getEntities(),
                pagedResult.getCursor()
        );
    }

    @Override
    public UserDbEntity create(JsonApiRequest request) {
        var payload = request.getSingleResourceDocPayload(UserAttributes.class);
        UserAttributes attributes = payload.getData().getAttributes();
        return userDb.createUser(
                attributes.getFullName(),
                attributes.getEmail(),
                attributes.getCreditCardNumber()
        );
    }

    @Override
    public void update(JsonApiRequest request) {
        var payload = request.getSingleResourceDocPayload(UserAttributes.class);
        UserAttributes attributes = payload.getData().getAttributes();
        userDb.updateUser(
                request.getResourceId(),
                attributes.getFullName(),
                attributes.getEmail(),
                attributes.getCreditCardNumber()
        );
    }

    @Override
    public void delete(JsonApiRequest request) {
        userDb.deleteUser(request.getResourceId());
    }
}
```

Three methods added to the same class from Getting Started. The `Resource`, `UserAttributes`, and `UserDbEntity` classes remain unchanged — write operations use the same domain model as reads.
