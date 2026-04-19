---
title: "Build a REST API in Java (Step-by-Step Guide with Examples)"
date: 2026-03-03
permalink: /build-rest-api-java-step-by-step/
categories:
  - tutorials
tags:
  - java
  - rest-api
  - spring-boot
  - json-api
  - jsonapi4j
excerpt: "Learn how to build a REST API in Java step by step. Includes a Spring Boot example and a simpler way to reduce boilerplate code."
---

Building a REST API in Java is one of the most common tasks for backend developers.

In this step-by-step guide, you'll learn how to create a Java REST API, see a real example using Spring Boot, and discover a simpler way to reduce boilerplate.

## What is a REST API?

A REST API (Representational State Transfer) allows applications to communicate over HTTP using standard methods like `GET`, `POST`, `PUT`, and `DELETE`.

In a typical Java REST API:
- Clients send HTTP requests
- The server processes them
- Responses are returned as JSON

## Step 1: Create a REST API in Java with Spring Boot

The most common way to build a REST API in Java is by using [Spring Boot](https://spring.io/projects/spring-boot).

```java
@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/{id}")
    public User getUser(@PathVariable String id) {
        return new User(id, "John");
    }
}
```

This simple Java REST API example returns JSON data when you call:

```
GET /users/1
```

## Step 2: Add Service Layer and Business Logic

In a real-world REST API, you typically add service classes, DTOs, and mapping logic:

```java
public class UserService {

    public User getUser(String id) {
        return new User(id, "John");
    }
}
```

## Step 3: Handle JSON Serialization

Spring Boot automatically converts Java objects to JSON using [Jackson](https://github.com/FasterXML/jackson). No extra configuration is needed for basic use cases.

## The Problem: Too Much Boilerplate

While Spring Boot is powerful, building a REST API often requires:
- Controllers
- Services
- DTOs
- Mappers
- Error handling

For even a simple API, you may end up writing hundreds of lines of repetitive code.

## A Simpler Way to Build Java APIs

What if you could define your API without writing controllers and repetitive code?

Instead of manually wiring everything, you can describe:
- Your **resources**
- **Relationships** between them
- **Operations** each resource supports

And let the framework handle routing, JSON responses, and API structure.

## Example: Building a REST API with Less Code

[JsonApi4j](https://api4.pro/) takes this declarative approach. Instead of writing controllers, DTO mapping, and manual response handling, you define how your data should be exposed.

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
        return new UserAttributes(dto.getFullName(), dto.getEmail());
    }
}
```

**2. Define an operation:**

```java
public class UserOperations implements ResourceOperations<UserDto> {

    @Override
    public UserDto readById(JsonApiRequest request) {
        return userDb.getUser(request.getResourceId());
    }
    
}
```

That's it. JsonApi4j automatically:
- Exposes `GET /users/{id}` endpoint
- Formats responses as standard [JSON:API](https://jsonapi.org/) documents
- Handles pagination, links, and error responses
- Keeps the API consistent across all your resources
- Provides extra features e.g. [JSON:API Sparse Fieldsets](https://jsonapi.org/format/#fetching-sparse-fieldsets) or [Compound Documents](https://jsonapi.org/format/#document-compound-documents) out from the box

No controllers. No DTO mappers. No manual response builders.

## When to Use This Approach

A declarative API approach is useful when:
- You want to avoid repetitive code
- You need consistent API structure across services
- You follow standards like [JSON:API](https://jsonapi.org/)
- You want to focus on business logic instead of infrastructure

## Conclusion

Building a REST API in Java doesn't have to be complex.

In this guide, you learned:
- How to create a Java REST API using Spring Boot
- Why boilerplate becomes a problem as your API grows
- How a more declarative approach can simplify development

While traditional frameworks like Spring Boot give you full control, newer approaches focus on reducing repetition and enforcing consistency — especially in larger systems.

Ready to try a declarative approach? Check out the [Getting Started guide](https://api4.pro/getting-started/).

---

## FAQ

### What is a REST API in Java?

A REST API allows applications to communicate over HTTP using `GET`, `POST`, `PUT`, and `DELETE`. In Java, frameworks like Spring Boot simplify routing and JSON serialization.

### How do I create a REST API in Java?

Define your resources (e.g., User, Product), create controllers to handle HTTP requests, and use a framework like Spring Boot for routing and JSON handling. For simpler API design, frameworks like [JsonApi4j](https://api4.pro/) can automatically generate endpoints.

### What is the easiest way to convert Java objects to JSON?

The most common method in Java is using [Jackson](https://github.com/FasterXML/jackson), which is included in Spring Boot. It automatically converts Java objects to JSON.

### How can I reduce boilerplate when building a Java REST API?

Boilerplate usually includes controllers, services, DTOs, and mapping logic. You can reduce it by using declarative frameworks like JsonApi4j, leveraging Spring Boot's annotations, and reusing common service and mapper classes.

### Can I build a Java REST API without writing controllers?

Yes. Declarative frameworks allow you to define resources and endpoints without manually creating controllers. [JsonApi4j](https://api4.pro/) is an example where you describe your data model and the API is generated automatically.

### Should I use Spring Boot or a declarative framework like JsonApi4j?

Spring Boot provides full flexibility and control, making it ideal for complex or custom APIs. Declarative frameworks like JsonApi4j focus on standardization and speed by reducing boilerplate and enforcing JSON:API standards. Many developers combine them: Spring Boot as the base framework and JsonApi4j for consistent API generation.

### How do I get started with JsonApi4j?

1. Add the JsonApi4j dependency to your project
2. Define your API resources (models, relationships, operations)
3. Let the framework automatically generate endpoints and JSON responses

See the [Getting Started guide](https://api4.pro/getting-started/) for a full walkthrough.

### What are common mistakes when building REST APIs in Java?

Common pitfalls include writing too many repetitive controllers and DTOs, inconsistent JSON responses, and not following API standards like JSON:API. Using a declarative approach helps avoid these issues and improves maintainability.

<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "FAQPage",
  "mainEntity": [
    {
      "@type": "Question",
      "name": "What is a REST API in Java?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "A REST API allows applications to communicate over HTTP using GET, POST, PUT, and DELETE. In Java, frameworks like Spring Boot simplify routing and JSON serialization."
      }
    },
    {
      "@type": "Question",
      "name": "How do I create a REST API in Java?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Define your resources (e.g., User, Product), create controllers to handle HTTP requests, and use a framework like Spring Boot for routing and JSON handling. For simpler API design, frameworks like JsonApi4j can automatically generate endpoints and manage JSON responses."
      }
    },
    {
      "@type": "Question",
      "name": "What is the easiest way to convert Java objects to JSON?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "The most common method in Java is using Jackson, which is included in Spring Boot. It automatically converts Java objects to JSON."
      }
    },
    {
      "@type": "Question",
      "name": "How can I reduce boilerplate when building a Java REST API?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Boilerplate usually includes controllers, services, DTOs, and mapping logic. You can reduce it by using declarative frameworks like JsonApi4j, leveraging Spring Boot's annotations, and reusing common service and mapper classes."
      }
    },
    {
      "@type": "Question",
      "name": "Can I build a Java REST API without writing controllers?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Yes. Declarative frameworks allow you to define resources and endpoints without manually creating controllers. JsonApi4j is an example where you describe your data model and the API is generated automatically."
      }
    },
    {
      "@type": "Question",
      "name": "Should I use Spring Boot or a declarative framework like JsonApi4j?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Spring Boot provides full flexibility and control, making it ideal for complex or custom APIs. Declarative frameworks like JsonApi4j focus on standardization and speed by reducing boilerplate and enforcing JSON:API standards. Many developers combine them: Spring Boot as the base framework and JsonApi4j for consistent API generation."
      }
    },
    {
      "@type": "Question",
      "name": "How do I get started with JsonApi4j?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "1. Add the JsonApi4j dependency to your project. 2. Define your API resources (models, relationships, operations). 3. Let the framework automatically generate endpoints and JSON responses."
      }
    },
    {
      "@type": "Question",
      "name": "What are common mistakes when building REST APIs in Java?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Common pitfalls include writing too many repetitive controllers and DTOs, inconsistent JSON responses, and not following API standards like JSON:API. Using a declarative approach helps avoid these issues and improves maintainability."
      }
    }
  ]
}
</script>
