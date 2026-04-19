---
title: "How to Build a JSON API in Java Without Boilerplate"
date: 2026-03-08
permalink: /build-json-api-java-without-boilerplate/
categories:
  - tutorials
tags:
  - java
  - json-api
  - jsonapi4j
  - rest-api
excerpt: "Learn how to build a JSON API in Java without writing repetitive code. Reduce boilerplate and simplify REST API development using modern declarative frameworks."
---

Building a JSON API in Java usually means writing controllers, DTOs, mappers, and response builders over and over again.

In this guide, you'll learn how to eliminate that boilerplate by using a declarative approach where you define your resources and let the framework handle the rest.

## Why Boilerplate is a Problem

In a traditional Java REST API, every new resource requires:
- A controller class with route mappings
- A DTO class for the request and response body
- A mapper to convert between entities and DTOs
- Manual JSON response formatting
- Error handling for each endpoint

This repetitive code slows down development and makes APIs harder to maintain. When you have dozens of resources, the amount of nearly identical code grows fast.

## Step 1: Define Resources Declaratively

Instead of writing controllers and DTOs, you define your resources using annotations and interfaces.

**Define a User resource:**

```java
@JsonApiResource(resourceType = "users")
public class UserResource implements Resource<UserDto> {

    @Override
    public String resolveResourceId(UserDto dto) {
        return dto.getId();
    }

    @Override
    public UserAttributes resolveAttributes(UserDto dto) {
        return new UserAttributes(dto.getFullName());
    }
}
```

**Define a Product resource:**

```java
@JsonApiResource(resourceType = "products")
public class ProductResource implements Resource<ProductDto> {

    @Override
    public String resolveResourceId(ProductDto dto) {
        return dto.getId();
    }

    @Override
    public ProductAttributes resolveAttributes(ProductDto dto) {
        return new ProductAttributes(dto.getTitle(), dto.getPrice());
    }
}
```

No controllers. No manual JSON formatting. Just your data model.

## Step 2: Let the Framework Generate Endpoints

With [JsonApi4j](https://api4.pro/), once you define a resource and its operations, the framework automatically generates API endpoints that return standard [JSON:API](https://jsonapi.org/) responses.

**Define operations for your resource:**

```java
public class UserOperations implements ResourceOperations<UserDto> {

    @Override
    public UserDto readById(JsonApiRequest request) {
        return userRepository.findById(request.getResourceId());
    }

    @Override
    public PaginationAwareResponse<UserDto> readPage(JsonApiRequest request) {
        return PaginationAwareResponse.fromItemsNotPageable(userRepository.findAll());
    }
}
```

The framework takes care of:
- Exposing `GET /users` and `GET /users/{id}` endpoints
- Formatting responses as [JSON:API documents](https://jsonapi.org/format/#document-structure)
- Adding pagination links and metadata
- Returning standardized error responses

## Step 3: Handle Relationships and Nested Resources

Real APIs have relationships between resources. In a traditional approach, managing one-to-many and many-to-many relationships means writing custom serialization logic for nested JSON.

With a declarative approach, you define relationships alongside your resources:

```java
@JsonApiRelationship(
    relationshipName = "orders",
    parentResource = UserResource.class
)
public class UserOrdersRelationship implements ToManyRelationship<OrderDto> {

    @Override
    public String resolveResourceIdentifierType(OrderDto dto) {
        return "orders";
    }

    @Override
    public String resolveResourceIdentifierId(OrderDto dto) {
        return dto.getId();
    }
}
```

JsonApi4j handles the nested structure, [resource linkage](https://jsonapi.org/format/#document-resource-object-linkage), and [compound documents](https://jsonapi.org/format/#document-compound-documents) automatically. No manual JSON building required.

## Step 4: Integrate with Spring Boot

JsonApi4j works alongside Spring Boot. Spring Boot handles routing, dependency injection, and HTTP while JsonApi4j takes care of JSON:API endpoint generation and response formatting.

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>pro.api4</groupId>
    <artifactId>jsonapi4j-rest-springboot</artifactId>
    <version>${jsonapi4j.version}</version>
</dependency>
```

Spring Boot auto-configuration discovers your `Resource` and `ResourceOperations` beans automatically. No extra wiring is needed.

## Benefits of Declarative JSON API

Switching from manually coded controllers to a declarative approach gives you:

- **Less code** -- no controllers, DTOs, or mappers for every resource
- **Faster development** -- define a resource and its operations, and the API is ready
- **Standardized responses** -- every endpoint follows the [JSON:API specification](https://jsonapi.org/format/)
- **Easy maintenance** -- add new resources without rewriting infrastructure code
- **Built-in features** -- pagination, [sparse fieldsets](https://jsonapi.org/format/#fetching-sparse-fieldsets), error handling, and [compound documents](https://jsonapi.org/format/#document-compound-documents) come out of the box

## Conclusion

Building a JSON API in Java doesn't require hundreds of lines of boilerplate.

By defining your resources and operations declaratively, you let the framework generate endpoints, format responses, and handle relationships. This approach reduces code, enforces consistency, and lets you focus on business logic.

Ready to get started? Check out the [Getting Started guide](https://api4.pro/getting-started/).

---

## FAQ

### What is a declarative JSON API in Java?

A declarative JSON API lets you define your resources, relationships, and operations, and the framework generates the endpoints automatically. Instead of writing controllers and manual response formatting, you describe your data model and the API is created for you.

### How does JsonApi4j reduce boilerplate?

[JsonApi4j](https://api4.pro/) generates endpoint routing, JSON serialization, and response formatting automatically. You define your resources using `@JsonApiResource` and implement `ResourceOperations` for data retrieval. The framework handles everything else, including pagination, error responses, and relationship handling.

### Can I use Spring Boot with a declarative JSON API framework?

Yes. Spring Boot handles HTTP, dependency injection, and application configuration, while [JsonApi4j](https://api4.pro/) takes care of JSON:API endpoint generation and response standardization. They work together seamlessly through Spring Boot auto-configuration.

### Is a declarative approach suitable for large APIs?

Absolutely. A declarative approach ensures consistency across all your resources, which becomes more valuable as the API grows. Every endpoint follows the same [JSON:API](https://jsonapi.org/) structure, making it easier to maintain and extend.

### Do I still need DTOs with JsonApi4j?

Not necessarily. JsonApi4j maps your data models directly to JSON:API responses through the `Resource` interface and its `resolveAttributes()` method. You define how attributes are exposed without creating separate DTO and mapper classes for every endpoint.

<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "FAQPage",
  "mainEntity": [
    {
      "@type": "Question",
      "name": "What is a declarative JSON API in Java?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "A declarative JSON API lets you define your resources, relationships, and operations, and the framework generates the endpoints automatically. Instead of writing controllers and manual response formatting, you describe your data model and the API is created for you."
      }
    },
    {
      "@type": "Question",
      "name": "How does JsonApi4j reduce boilerplate?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "JsonApi4j generates endpoint routing, JSON serialization, and response formatting automatically. You define your resources using @JsonApiResource and implement ResourceOperations for data retrieval. The framework handles everything else, including pagination, error responses, and relationship handling."
      }
    },
    {
      "@type": "Question",
      "name": "Can I use Spring Boot with a declarative JSON API framework?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Yes. Spring Boot handles HTTP, dependency injection, and application configuration, while JsonApi4j takes care of JSON:API endpoint generation and response standardization. They work together seamlessly through Spring Boot auto-configuration."
      }
    },
    {
      "@type": "Question",
      "name": "Is a declarative approach suitable for large APIs?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Absolutely. A declarative approach ensures consistency across all your resources, which becomes more valuable as the API grows. Every endpoint follows the same JSON:API structure, making it easier to maintain and extend."
      }
    },
    {
      "@type": "Question",
      "name": "Do I still need DTOs with JsonApi4j?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Not necessarily. JsonApi4j maps your data models directly to JSON:API responses through the Resource interface and its resolveAttributes() method. You define how attributes are exposed without creating separate DTO and mapper classes for every endpoint."
      }
    }
  ]
}
</script>
