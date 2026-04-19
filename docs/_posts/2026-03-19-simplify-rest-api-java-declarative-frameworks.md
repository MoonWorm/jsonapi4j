---
title: "Simplify REST API Development in Java with Declarative Frameworks"
date: 2026-03-19
permalink: /simplify-rest-api-java-declarative-frameworks/
categories:
  - tutorials
tags:
  - java
  - rest-api
  - json-api
  - jsonapi4j
  - spring-boot
excerpt: "Compare declarative Java REST API frameworks — Spring Data REST, Elide, crnk, and JsonApi4j — to find the right fit for your project. See how each handles resources, relationships, and response formatting."
---

Most Java REST APIs follow the same pattern: write a controller, create DTOs, build mappers, format responses, handle errors. Repeat for every resource.

Declarative frameworks flip this. You define what your API exposes, and the framework generates the endpoints. But which framework should you use? In this guide, you'll compare the main options and see how they differ in practice.

## What Makes a Framework "Declarative"?

In a traditional Spring Boot API, you write a `@RestController` for every resource, map DTOs manually, and build JSON responses by hand. A declarative framework inverts this: you describe your data model, and the framework generates endpoints, serialization, and response formatting.

The key question is not whether to use a declarative approach — it is which framework matches your constraints. The main options in the Java ecosystem are:

- **[Spring Data REST](https://spring.io/projects/spring-data-rest)** — auto-exposes Spring Data repositories as REST endpoints
- **[Elide](https://elide.io/)** — JSON:API and GraphQL from JPA entities
- **[crnk](https://www.crnk.io/)** — JSON:API framework with a resource repository pattern
- **[JsonApi4j](https://api4.pro/)** — JSON:API from domain interfaces, persistence-agnostic

Each takes a different stance on how tightly your API should be coupled to your persistence layer.

## Spring Data REST: Quick but Coupled

Spring Data REST is the fastest path from JPA entities to endpoints. If you already have Spring Data repositories, adding a single dependency exposes them as a HAL-based REST API.

```java
public interface UserRepository extends JpaRepository<User, Long> {
}
```

That is the entire API definition. `GET /users`, `GET /users/{id}`, `POST /users`, etc. are available immediately.

**Where it works well:**
- Internal tools, admin panels, and prototypes
- Projects already using Spring Data JPA
- APIs where the database schema closely matches the API shape

**Where it falls short:**
- Your API is directly coupled to your entity model. Renaming a database column changes your API contract.
- Customizing response formats beyond HAL requires significant work.
- No native [JSON:API](https://jsonapi.org/) support — responses follow the HAL format.
- Complex business logic (validation, computed fields, authorization per field) requires custom controllers, which defeats the purpose.

## Elide: JSON:API from JPA

[Elide](https://elide.io/) generates JSON:API (and optionally GraphQL) endpoints from JPA-annotated entities. It adds a security model with annotation-based checks.

```java
@Include(name = "users")
@Entity
public class User {
    @Id
    private Long id;
    private String name;

    @ReadPermission(expression = "admin")
    private String email;
}
```

**Where it works well:**
- Projects that want JSON:API compliance with JPA entities
- APIs that need annotation-based security on individual fields
- Teams that want both JSON:API and GraphQL from the same model

**Where it falls short:**
- Tightly coupled to JPA — your API shape is your entity shape.
- The learning curve for Elide's security expression language is non-trivial.
- If you use a non-JPA data source (REST calls, NoSQL, in-memory), you need custom data stores.

## crnk: Repository Pattern

[crnk](https://www.crnk.io/) follows the JSON:API specification and uses a repository pattern to decouple the API from persistence.

```java
@JsonApiResource(type = "users")
public class User {
    @JsonApiId
    private String id;
    private String name;
}
```

```java
public class UserRepository extends ResourceRepositoryBase<User, String> {
    @Override
    public ResourceList<User> findAll(QuerySpec querySpec) {
        return querySpec.apply(userService.findAll());
    }
}
```

**Where it works well:**
- Projects that need JSON:API compliance with a repository abstraction
- Teams comfortable with the `QuerySpec` filtering and sorting model

**Where it falls short:**
- The project has seen less active development in recent years.
- Resource classes mix API concerns (annotations) with domain model.
- The `QuerySpec` API adds its own learning curve.

## JsonApi4j: Domain Interfaces, No Persistence Assumptions

[JsonApi4j](https://api4.pro/) separates the API layer from the data model entirely. Your domain objects (DTOs, entities, downstream service responses) remain unchanged. The API shape is defined through `Resource` and `ResourceOperations` interfaces.

```java
@JsonApiResource(resourceType = "projects")
public class ProjectResource implements Resource<ProjectDto> {

    @Override
    public String resolveResourceId(ProjectDto dto) {
        return dto.getId();
    }

    @Override
    public ProjectAttributes resolveAttributes(ProjectDto dto) {
        return new ProjectAttributes(dto.getName(), dto.getStatus(), dto.getDeadline());
    }
}
```

```java
public class ProjectOperations implements ResourceOperations<ProjectDto> {

    @Override
    public ProjectDto readById(JsonApiRequest request) {
        return projectService.findById(request.getResourceId());
    }

    @Override
    public PaginationAwareResponse<ProjectDto> readPage(JsonApiRequest request) {
        return PaginationAwareResponse.cursorAware(
            projectService.findAll(request.getPaginationRequest()),
            request.getPaginationRequest().getCursor()
        );
    }
}
```

The `Resource` interface controls which fields are exposed. The `ResourceOperations` interface handles data retrieval. Neither knows about the other.

**Where it works well:**
- APIs backed by multiple data sources (SQL, REST calls, NoSQL, in-memory)
- Projects where the API shape must differ from the internal data model
- Teams that want [JSON:API](https://jsonapi.org/) compliance without JPA coupling
- Microservices that aggregate data from downstream services

**Where it falls short:**
- More setup than Spring Data REST for simple CRUD-over-JPA use cases
- Smaller community compared to Spring Data REST

## Side-by-Side Comparison

| Aspect | Spring Data REST | Elide | crnk | JsonApi4j |
|--------|-----------------|-------|------|-----------|
| Response format | HAL | JSON:API, GraphQL | JSON:API | JSON:API |
| Persistence coupling | JPA required | JPA required | Flexible | None |
| Resource definition | Entity = API | Entity + annotations | Annotated model + repository | Separate Resource interface |
| Relationships | JPA associations | JPA associations | Annotation-based | `ToManyRelationship` / `ToOneRelationship` interfaces |
| Pagination | Page/Sort | Cursor, Offset | Offset | Cursor, Offset |
| Field-level access control | No | Annotation-based | No | Plugin-based (`@AccessControl`) |
| OpenAPI generation | Via springdoc | Limited | Via crnk-gen | Plugin-based |
| Framework support | Spring Boot | Spring Boot, Standalone | Spring Boot, CDI | Spring Boot, Quarkus, Servlet |

## How to Choose

**Use Spring Data REST** if you want the fastest possible path from JPA to API and your entity model closely matches your API contract. Accept the coupling.

**Use Elide** if you need both JSON:API and GraphQL from the same model, and your data lives in JPA entities.

**Use crnk** if you need JSON:API with a repository abstraction and are comfortable with its ecosystem.

**Use JsonApi4j** if your data comes from mixed sources (databases, REST services, caches), you need a clean separation between your internal model and API contract, or you want a plugin architecture for cross-cutting concerns like [access control](/access-control/), [sparse fieldsets](/sparse-fieldsets/), and [OpenAPI generation](/openapi/).

In practice, the decision often comes down to one question: **is your API a thin layer over JPA, or does it have its own shape?** If the former, Spring Data REST or Elide are simpler. If the latter, JsonApi4j gives you the separation.

## Conclusion

Declarative frameworks eliminate the repetitive controller-DTO-mapper pattern, but they are not interchangeable. Each makes different trade-offs between convenience and flexibility.

The right choice depends on your data sources, how much your API shape differs from your internal model, and which specification (HAL vs JSON:API vs GraphQL) your clients need.

Get started with JsonApi4j using the [Getting Started guide](https://api4.pro/getting-started/), or explore the [JSON:API specification](https://jsonapi.org/format/) to understand the standard these frameworks implement.

---

## FAQ

### What is a declarative REST API framework?

A declarative framework generates REST endpoints from your data model definitions instead of requiring you to write controllers and serialization logic manually. Examples include Spring Data REST, Elide, crnk, and [JsonApi4j](https://api4.pro/).

### How does JsonApi4j differ from Spring Data REST?

Spring Data REST auto-exposes JPA repositories as HAL endpoints — your entity model is your API contract. JsonApi4j separates the API layer from persistence entirely using `Resource` and `ResourceOperations` interfaces, outputs [JSON:API](https://jsonapi.org/)-compliant responses, and works with any data source (SQL, NoSQL, REST, in-memory).

### Can I use a declarative framework with non-JPA data sources?

Spring Data REST and Elide require JPA. crnk supports custom repositories. [JsonApi4j](https://api4.pro/) is fully persistence-agnostic — your `ResourceOperations` can call any data source, including REST services, in-memory stores, or NoSQL databases.

### Which framework supports JSON:API?

Elide, crnk, and [JsonApi4j](https://api4.pro/) all support the [JSON:API specification](https://jsonapi.org/format/). Spring Data REST uses the HAL format instead. Elide additionally supports GraphQL.

### Is a declarative approach suitable for large APIs?

Yes, but framework choice matters. For large APIs with complex authorization, mixed data sources, and strict API contracts, a framework with clean separation (like JsonApi4j or crnk) avoids the coupling issues that arise when entity changes break your API.

<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "FAQPage",
  "mainEntity": [
    {
      "@type": "Question",
      "name": "What is a declarative REST API framework?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "A declarative framework generates REST endpoints from your data model definitions instead of requiring you to write controllers and serialization logic manually. Examples include Spring Data REST, Elide, crnk, and JsonApi4j."
      }
    },
    {
      "@type": "Question",
      "name": "How does JsonApi4j differ from Spring Data REST?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Spring Data REST auto-exposes JPA repositories as HAL endpoints — your entity model is your API contract. JsonApi4j separates the API layer from persistence entirely using Resource and ResourceOperations interfaces, outputs JSON:API-compliant responses, and works with any data source."
      }
    },
    {
      "@type": "Question",
      "name": "Can I use a declarative framework with non-JPA data sources?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Spring Data REST and Elide require JPA. crnk supports custom repositories. JsonApi4j is fully persistence-agnostic — your ResourceOperations can call any data source, including REST services, in-memory stores, or NoSQL databases."
      }
    },
    {
      "@type": "Question",
      "name": "Which framework supports JSON:API?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Elide, crnk, and JsonApi4j all support the JSON:API specification. Spring Data REST uses the HAL format instead. Elide additionally supports GraphQL."
      }
    },
    {
      "@type": "Question",
      "name": "Is a declarative approach suitable for large APIs?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Yes, but framework choice matters. For large APIs with complex authorization, mixed data sources, and strict API contracts, a framework with clean separation avoids the coupling issues that arise when entity changes break your API."
      }
    }
  ]
}
</script>
