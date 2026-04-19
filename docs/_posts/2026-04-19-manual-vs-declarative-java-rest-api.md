---
title: "Spring Boot vs Manual Approach: When to Use Declarative APIs"
date: 2026-04-19
permalink: /manual-vs-declarative-java-rest-api/
categories:
  - tutorials
tags:
  - java
  - rest-api
  - spring-boot
  - json-api
  - jsonapi4j
excerpt: "Compare manual and declarative approaches in Java REST API development. Learn how declarative frameworks like JsonApi4j reduce boilerplate, improve consistency, and simplify maintenance."
---

When building a Java REST API, you have two broad approaches: write everything manually with controllers, DTOs, and mappers, or use a declarative framework that generates endpoints from your domain model.

Both approaches have trade-offs. In this guide, you will see real code examples of each, understand when to use which, and learn how declarative frameworks like [JsonApi4j](https://api4.pro/) can reduce boilerplate without sacrificing flexibility.

## The Manual Approach

The traditional way to build a REST API in Java is to write controllers, services, DTOs, and mapping logic by hand.

```java
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String id) {
        UserDto user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        UserResponse response = new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.findAll().stream()
            .map(u -> new UserResponse(u.getId(), u.getName(), u.getEmail()))
            .toList();
        return ResponseEntity.ok(users);
    }
}
```

**Pros:**
- Full control over every aspect of the response
- Easy to optimize individual endpoints for specific use cases
- No learning curve beyond Spring Boot itself

**Cons:**
- Repetitive code across controllers (mapping, error handling, response wrapping)
- Manual DTO mapping for every resource
- Inconsistent response formats unless you enforce conventions by hand
- Error handling must be implemented separately for each controller or centralized manually
- Relationships between resources require custom logic

As your API grows, this boilerplate multiplies. Ten resources with CRUD operations means dozens of controllers, DTOs, mappers, and tests that all follow the same pattern.

## The Declarative Approach

A declarative approach lets you define what your API exposes, and the framework handles how it is served.

With [JsonApi4j](https://api4.pro/), you describe your resources and operations:

**1. Define a resource:**

```java
@JsonApiResource(resourceType = "users")
public class UserResource implements Resource<UserDto> {

    @Override
    public String resolveResourceId(UserDto dto) {
        return dto.getId();
    }

    @Override
    public UserAttributes resolveAttributes(UserDto dto) {
        return new UserAttributes(dto.getName(), dto.getEmail());
    }
}
```

**2. Define operations:**

```java
public class UserOperations implements ResourceOperations<UserDto> {

    @Override
    public UserDto readById(JsonApiRequest request) {
        return userService.findById(request.getResourceId());
    }

    @Override
    public PaginationAwareResponse<UserDto> readPage(JsonApiRequest request) {
        return PaginationAwareResponse.fromItemsNotPageable(userService.findAll());
    }
}
```

That is it. JsonApi4j automatically:
- Exposes `GET /users` and `GET /users/{id}` endpoints
- Formats responses as [JSON:API](https://jsonapi.org/) documents with `data`, `links`, and `meta`
- Handles error responses using `ErrorObject` and `ErrorsDoc`
- Resolves relationships and supports [compound documents](https://jsonapi.org/format/#document-compound-documents) via `?include=`
- Supports [sparse fieldsets](https://jsonapi.org/format/#fetching-sparse-fieldsets) via `?fields[users]=name,email`
- Generates pagination links automatically

**Pros:**
- Dramatically less code to write and maintain
- Consistent response format across all endpoints
- Automatic relationship resolution and compound documents
- Built-in error formatting per the JSON:API specification
- Plugins for access control, OpenAPI generation, and sparse fieldsets

**Cons:**
- Less control over highly custom endpoints
- Learning curve for the framework's conventions
- May need to drop to manual approach for edge cases

## When to Use Each Approach

### Use the manual approach when:
- You are building a small API with a few endpoints
- You need highly custom response formats that do not follow a standard
- You have performance-critical endpoints that require fine-tuned serialization
- Your API does not serve standard CRUD resources

### Use the declarative approach when:
- You are building a medium to large API with many resources
- You want consistent response formats across all endpoints
- You follow standards like [JSON:API](https://jsonapi.org/)
- You want to reduce boilerplate and focus on business logic
- Multiple teams or services need to produce APIs with the same structure

### Combine both approaches when:
- Most of your API is standard CRUD but a few endpoints need custom logic
- You want declarative resource management with manual overrides for specific cases
- You are migrating an existing API to a standardized format incrementally

JsonApi4j works alongside Spring Boot, Quarkus, or plain servlets. You can use it for the resources that fit the declarative model and write traditional controllers for the rest.

## Side-by-Side Comparison

| Aspect | Manual | Declarative (JsonApi4j) |
|--------|--------|------------------------|
| Code per resource | Controller + DTO + mapper + error handling | Resource class + Operations class |
| Response format | Custom, varies | Standardized JSON:API |
| Relationships | Manual joins and nesting | Automatic resolution |
| Error handling | `@ControllerAdvice` or per-controller | Built-in `JsonApi4jException` hierarchy |
| Pagination | Manual implementation | Automatic links generation |
| Documentation | Manual or SpringDoc setup | [OpenAPI plugin](https://api4.pro/openapi/) available |
| Maintenance effort | Grows with each resource | Stays flat |

## Conclusion

Neither approach is universally better. The right choice depends on your project's size, consistency requirements, and how much control you need.

In this guide, you learned:
- How the manual approach works and where it excels
- How declarative frameworks like JsonApi4j eliminate repetitive code
- When to use each approach and when to combine them

For most medium-to-large APIs that serve standard resources, the declarative approach saves significant development time while enforcing consistency. Check out the [Getting Started guide](https://api4.pro/getting-started/) to see how JsonApi4j works with your existing Spring Boot or Quarkus project.

---

## FAQ

### What is the difference between manual and declarative API development?

In the manual approach, you write controllers, DTOs, and mapping logic yourself for each endpoint. In the declarative approach, you define your resources and operations, and the framework generates endpoints, handles serialization, and manages response formatting automatically.

### When should I use a manual approach?

Use the manual approach for small projects, highly custom APIs, or performance-critical endpoints where you need full control over every aspect of the request and response handling.

### When should I use a declarative approach?

Use a declarative approach for medium-to-large APIs where you need consistency across many endpoints, want to reduce boilerplate, and follow standards like [JSON:API](https://jsonapi.org/). Frameworks like [JsonApi4j](https://api4.pro/) handle resource formatting, relationships, and error responses automatically.

### Can a declarative framework handle relationships between resources?

Yes. JsonApi4j automatically resolves to-one and to-many relationships, supports [compound documents](https://jsonapi.org/format/#document-compound-documents) with the `?include` parameter, and generates relationship links in every response.

### How much boilerplate does a declarative approach actually reduce?

Significantly. Instead of writing a controller, DTO, mapper, and error handler for each resource, you write a `Resource` class and a `ResourceOperations` class. The framework handles routing, JSON serialization, pagination, error formatting, and relationship resolution.

<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "FAQPage",
  "mainEntity": [
    {
      "@type": "Question",
      "name": "What is the difference between manual and declarative API development?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "In the manual approach, you write controllers, DTOs, and mapping logic yourself for each endpoint. In the declarative approach, you define your resources and operations, and the framework generates endpoints, handles serialization, and manages response formatting automatically."
      }
    },
    {
      "@type": "Question",
      "name": "When should I use a manual approach?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Use the manual approach for small projects, highly custom APIs, or performance-critical endpoints where you need full control over every aspect of the request and response handling."
      }
    },
    {
      "@type": "Question",
      "name": "When should I use a declarative approach?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Use a declarative approach for medium-to-large APIs where you need consistency across many endpoints, want to reduce boilerplate, and follow standards like JSON:API. Frameworks like JsonApi4j handle resource formatting, relationships, and error responses automatically."
      }
    },
    {
      "@type": "Question",
      "name": "Can a declarative framework handle relationships between resources?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Yes. JsonApi4j automatically resolves to-one and to-many relationships, supports compound documents with the ?include parameter, and generates relationship links in every response."
      }
    },
    {
      "@type": "Question",
      "name": "How much boilerplate does a declarative approach actually reduce?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Significantly. Instead of writing a controller, DTO, mapper, and error handler for each resource, you write a Resource class and a ResourceOperations class. The framework handles routing, JSON serialization, pagination, error formatting, and relationship resolution."
      }
    }
  ]
}
</script>
