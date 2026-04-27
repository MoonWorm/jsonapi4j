[![Build](https://github.com/moonworm/jsonapi4j/actions/workflows/build.yml/badge.svg)](https://github.com/moonworm/jsonapi4j/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/pro.api4/jsonapi4j.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/pro.api4/jsonapi4j)
[![Last Commit](https://img.shields.io/github/last-commit/moonworm/jsonapi4j)](https://github.com/moonworm/jsonapi4j/commits/main)
[![codecov](https://codecov.io/gh/moonworm/jsonapi4j/branch/main/graph/badge.svg)](https://codecov.io/gh/moonworm/jsonapi4j)
[![Issues](https://img.shields.io/github/issues/moonworm/jsonapi4j)](https://github.com/moonworm/jsonapi4j/issues)
[![License](https://img.shields.io/github/license/moonworm/jsonapi4j)](LICENSE)

![Logo](/docs/assets/images/jsonapi4j-logo-medium.png)

# JsonApi4j — JSON:API Framework for Java

A lightweight Java framework for building REST APIs compliant with the [JSON:API specification](https://jsonapi.org/). Works with [Spring Boot](https://spring.io/projects/spring-boot), [Quarkus](https://quarkus.io/), and the [Jakarta Servlet API](https://jakarta.ee/specifications/servlet/).

## Features

### 📋 Spec-Compliant by Default
Full [JSON:API](https://jsonapi.org/) compliance out of the box — resources, relationships, compound documents, pagination, sparse fieldsets, error handling, and links. No shortcuts, no partial implementations.

### 🔌 Works With Your Stack
First-class support for **Spring Boot**, **Quarkus**, and plain **Jakarta Servlet API**. One dependency, zero configuration — the framework auto-configures itself.

### 🗄️ Bring Your Own Data Source
No JPA or ORM required. Use SQL, NoSQL, REST clients, in-memory stores — anything that returns data. The framework never touches your persistence layer.

### 🧩 Plugin System
Extend the [request processing pipeline](https://api4.pro/request-processing-pipeline/) without modifying core logic. Build [your own plugins](https://api4.pro/custom-plugin/) or use the built-in ones:
- **[Access Control](https://api4.pro/access-control-plugin/)** — per-field authorization via annotations, OAuth2 scopes, resource ownership
- **[OpenAPI](https://api4.pro/openapi-plugin/)** — auto-generated spec from your domain model
- **[Sparse Fieldsets](https://api4.pro/sparse-fieldsets-plugin/)** — `fields[type]` filtering on the server
- **[Compound Documents](https://api4.pro/compound-docs/)** — multi-level `include` with parallel resolution

### ⚡ Built for Performance
Concurrent relationship resolution, parallel compound document fetching, and support for virtual threads (Project Loom). Designed for production throughput from day one.

## Why JsonApi4j?

### 🏛️ One Standard, Every Service
Every service ships with the same request/response format, the same pagination model, the same error structure — enforced by the framework, not by design reviews. Whether you're building a new microservice or standardizing an existing API layer, JsonApi4j gives your team an enforceable shared contract across Spring Boot, Quarkus, or plain Servlet.

### 🚀 Ship Faster
Define your resources and operations — routing, serialization, pagination links, error handling, and documentation are generated automatically. Less plumbing, more domain logic.

## Quick Start

### 1. Add dependency

The framework modules are published to [Maven Central](https://mvnrepository.com/artifact/pro.api4).

| Stack | Artifact |
|-------|----------|
| Spring Boot | `pro.api4:jsonapi4j-rest-springboot` |
| Quarkus | `pro.api4:jsonapi4j-rest-quarkus` |
| Servlet API | `pro.api4:jsonapi4j-rest` |

```xml
<dependency>
  <groupId>pro.api4</groupId>
  <artifactId>jsonapi4j-rest-springboot</artifactId>
  <version>${jsonapi4j.version}</version>
</dependency>
```

### 2. Define a resource

```java
@JsonApiResource(resourceType = "users")
public class UserResource implements Resource<UserDbEntity> {

    @Override
    public String resolveResourceId(UserDbEntity user) {
        return user.getId();
    }

    @Override
    public UserAttributes resolveAttributes(UserDbEntity user) {
        return new UserAttributes(user.getFullName(), user.getEmail());
    }
}
```

### 3. Implement an operation

```java
@JsonApiResourceOperation(resource = UserResource.class)
public class UserOperations implements ResourceOperations<UserDbEntity> {

    private final UserDb userDb;

    public UserOperations(UserDb userDb) {
        this.userDb = userDb;
    }

    @Override
    public PaginationAwareResponse<UserDbEntity> readPage(JsonApiRequest request) {
        var page = userDb.readAllUsers(request.getCursor());
        return PaginationAwareResponse.cursorAware(page.getEntities(), page.getCursor());
    }
}
```

### 4. Get a JSON:API response

`GET /users`

```json
{
  "data": [
    {
      "id": "1",
      "type": "users",
      "attributes": {
        "fullName": "John Doe",
        "email": "john@doe.com"
      },
      "links": {
        "self": "/users/1"
      }
    },
    {
      "id": "2",
      "type": "users",
      "attributes": {
        "fullName": "Jane Doe",
        "email": "jane@doe.com"
      },
      "links": {
        "self": "/users/2"
      }
    }
  ],
  "links": {
    "self": "/users",
    "next": "/users?page%5Bcursor%5D=DoJu"
  }
}
```

That's it — pagination, links, content negotiation, and error handling are all handled automatically.

See the **[Getting Started guide](https://api4.pro/getting-started/)** for the full walkthrough including relationships, compound documents, and more.

## Plugins

Each plugin is a separate dependency — add only what you need:

### 🔐 Access Control

Annotation-driven authorization with per-field granularity. Restrict attributes based on authentication status, OAuth2 scopes, and resource ownership — including automatic data anonymization for unauthorized fields.

```java
@AccessControl(authenticated = Authenticated.AUTHENTICATED)
public class UserAttributes {
    private final String fullName;
    private final String email;

    @AccessControl(
        scopes = @AccessControlScopes(requiredScopes = "users.sensitive.read"),
        ownership = @AccessControlOwnership(ownerIdFieldPath = "id")
    )
    private final String creditCardNumber;
}
```

Unauthenticated requests see no attributes. Authenticated non-owners see `fullName` and `email`. Only the resource owner with the required scope sees `creditCardNumber`. [Read more](https://api4.pro/access-control-plugin/)

### 📄 OpenAPI

Auto-generates an OpenAPI specification from your declared domain — resources, operations, relationships, and JSON:API parameters. Zero configuration required.

![Swagger UI](/docs/assets/images/swagger-ui-screenshot.png)

Access the spec at `/jsonapi/oas` in JSON or YAML format. [Read more](https://api4.pro/openapi-plugin/)

### 🔍 Sparse Fieldsets

Implements [JSON:API sparse fieldsets](https://jsonapi.org/format/#fetching-sparse-fieldsets). Clients request only the fields they need with `fields[type]=field1,field2`, reducing payload size on the server. [Read more](https://api4.pro/sparse-fieldsets-plugin/)

### 📦 Compound Documents

Resolves `include` queries with multi-level relationship chaining (e.g., `include=relatives.relatives`), parallel batch fetching, and built-in response caching with `Cache-Control` support. The resolver can also run standalone at an API Gateway level. [Read more](https://api4.pro/compound-docs/)

## Documentation

| | |
|---|---|
| **Full documentation** | [api4.pro](https://api4.pro/) |
| **Getting Started** | [api4.pro/getting-started](https://api4.pro/getting-started/) |
| **Maven Central** | [pro.api4](https://mvnrepository.com/artifact/pro.api4) |

### Sample Apps

| App | Stack | Description                                                            |
|-----|-------|------------------------------------------------------------------------|
| [Spring Boot](https://github.com/MoonWorm/jsonapi4j/tree/main/examples/jsonapi4j-springboot-sampleapp) | Spring Boot | Users, Countries, Currencies with relationships and compound documents |
| [Quarkus](https://github.com/MoonWorm/jsonapi4j/tree/main/examples/jsonapi4j-quarkus-sampleapp) | Quarkus | Same domain, CDI-based integration                                     |
| [Servlet](https://github.com/MoonWorm/jsonapi4j/tree/main/examples/jsonapi4j-servlet-sampleapp) | Servlet API | Same domain, objects registration in Servlet Context                   |

## Alternatives

Looking for a JSON:API implementation for Java? Here's a quick guide:

- **Choose JsonApi4j** if you want a lightweight, persistence-agnostic framework. No JPA or ORM required — bring your own data source. Works with Spring Boot, Quarkus, and plain Servlet API.
- **Choose [Elide](https://elide.io/)** if your stack is built around JPA/Hibernate and you also need GraphQL support alongside JSON:API.
- **Consider [crnk](https://www.crnk.io/)** for a mature, feature-rich JSON:API implementation with deep JPA integration.

## Contributing

Contributions are welcome! See [CONTRIBUTING.md](./CONTRIBUTING.md) for details.

## License

This project is licensed under the Apache 2.0 License — see the [LICENSE](LICENSE) file for details.
