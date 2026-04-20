---
title: "Spring Boot REST API Examples: Return JSON Correctly"
date: 2026-03-25
permalink: /spring-boot-rest-api-return-json/
categories:
  - tutorials
tags:
  - spring-boot
  - java
  - json
  - jackson
  - rest-api
  - jsonapi4j
excerpt: "Learn how to correctly return JSON in Spring Boot REST APIs with best practices. Includes examples, common pitfalls, and integration with JsonApi4j for consistent JSON:API responses."
---

Returning JSON from a Spring Boot REST API seems simple at first, but getting it right across your entire application takes some thought.

In this guide, you'll learn how to return JSON correctly in Spring Boot, customize the output, avoid common pitfalls, and standardize your responses using JSON:API.

## Step 1: Basic JSON Response

Spring Boot uses [Jackson](https://github.com/FasterXML/jackson) under the hood to serialize Java objects to JSON automatically. When you annotate a class with `@RestController`, every method return value is converted to JSON by default.

```java
@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/{id}")
    public User getUser(@PathVariable String id) {
        return new User(id, "John", "john@example.com");
    }
}
```

Calling `GET /users/1` returns:

```json
{
  "id": "1",
  "name": "John",
  "email": "john@example.com"
}
```

No explicit JSON conversion is needed. Spring Boot configures Jackson automatically and sets the `Content-Type` header to `application/json`.

## Step 2: Returning Lists and Collections

Returning a collection works the same way. Spring Boot serializes any `List`, `Set`, or other collection type to a JSON array.

```java
@GetMapping
public List<User> getAllUsers() {
    return List.of(
        new User("1", "John", "john@example.com"),
        new User("2", "Jane", "jane@example.com")
    );
}
```

This produces:

```json
[
  { "id": "1", "name": "John", "email": "john@example.com" },
  { "id": "2", "name": "Jane", "email": "jane@example.com" }
]
```

## Step 3: Customize JSON Output

Jackson provides several annotations to control how your objects are serialized.

**Hide sensitive fields:**

```java
public class User {
    private String id;
    private String name;

    @JsonIgnore
    private String passwordHash;
}
```

**Rename fields in the JSON output:**

```java
public class User {
    @JsonProperty("full_name")
    private String name;
}
```

**Exclude null values:**

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {
    private String id;
    private String name;
    private String bio; // omitted from JSON when null
}
```

These annotations give you fine-grained control over the JSON structure without changing your domain model.

## Step 4: Use JsonApi4j for Standardization

While Jackson annotations handle individual fields, they don't solve the broader problem: every endpoint in your API may return JSON in a slightly different structure. One endpoint wraps results in `{ "data": [...] }`, another returns a plain array, and error responses look different everywhere.

[JsonApi4j](https://api4.pro/) solves this by wrapping all your responses in the [JSON:API](https://jsonapi.org/) standard format. Relationships, links, pagination, and error responses are all handled consistently.

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

Now every response follows the same structure:

```json
{
  "data": {
    "type": "users",
    "id": "1",
    "attributes": {
      "name": "John",
      "email": "john@example.com"
    },
    "links": {
      "self": "/users/1"
    }
  }
}
```

No manual response wrapping. No inconsistencies between endpoints. JsonApi4j integrates with Spring Boot's auto-configuration, so setup is minimal.

## Step 5: Common Pitfalls

**Circular references.** When two objects reference each other (e.g., `User` has a list of `Order`, and `Order` has a `User`), Jackson throws a `StackOverflowError`. Fix this with:

```java
public class User {
    @JsonManagedReference
    private List<Order> orders;
}

public class Order {
    @JsonBackReference
    private User user;
}
```

Alternatively, use `@JsonIgnore` on one side of the relationship, or switch to a framework like JsonApi4j where relationships are defined declaratively and serialized as [JSON:API resource linkages](https://jsonapi.org/format/#document-resource-object-linkage) without circular reference issues.

**Date formatting.** By default, Jackson serializes `java.time` types as timestamps (e.g., `1713484800`). Configure ISO 8601 format instead:

```java
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
```

This produces readable dates like `"2026-04-19T10:00:00Z"` instead of numeric timestamps.

**Inconsistent error responses.** Without centralization, each controller handles errors differently. Use `@ControllerAdvice` to return consistent error JSON across your entire API:

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404)
            .body(Map.of("error", ex.getMessage()));
    }
}
```

JsonApi4j takes this further by automatically formatting all errors as [JSON:API error documents](https://jsonapi.org/format/#errors), including proper HTTP status codes and error detail fields.

## Conclusion

Returning JSON in Spring Boot is straightforward for simple cases, but real-world APIs need consistent structure, proper serialization, and good error handling.

In this guide, you learned:
- How Spring Boot and Jackson handle JSON serialization automatically
- How to customize JSON output with annotations
- How to standardize all responses with JsonApi4j and JSON:API
- How to avoid common pitfalls like circular references and inconsistent errors

For a full walkthrough on setting up JsonApi4j with Spring Boot, see the [Getting Started guide](https://api4.pro/getting-started/).

---

## FAQ

### How does Spring Boot return JSON?

Spring Boot uses [Jackson](https://github.com/FasterXML/jackson) to automatically serialize Java objects to JSON. When you use `@RestController`, every method return value is converted to JSON and the `Content-Type` header is set to `application/json`.

### How do I return a JSON array in Spring Boot?

Return a `List` or any other `Collection` type from your controller method. Spring Boot automatically serializes it to a JSON array.

### How do I customize JSON output in Spring Boot?

Use Jackson annotations like `@JsonIgnore` to hide fields, `@JsonProperty` to rename fields, and `@JsonInclude(NON_NULL)` to exclude null values from the output.

### How do I enforce JSON:API format in Spring Boot?

Use [JsonApi4j](https://api4.pro/) to automatically wrap all your responses in the [JSON:API](https://jsonapi.org/) standard format. Define your resources with `@JsonApiResource` and your operations with `ResourceOperations`, and the framework handles the rest.

### How do I avoid circular reference errors in Spring Boot JSON?

Use `@JsonManagedReference` and `@JsonBackReference` on bidirectional relationships. Alternatively, use `@JsonIgnore` on one side, or use a framework like JsonApi4j that handles relationships as resource linkages without circular serialization.

<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "FAQPage",
  "mainEntity": [
    {
      "@type": "Question",
      "name": "How does Spring Boot return JSON?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Spring Boot uses Jackson to automatically serialize Java objects to JSON. When you use @RestController, every method return value is converted to JSON and the Content-Type header is set to application/json."
      }
    },
    {
      "@type": "Question",
      "name": "How do I return a JSON array in Spring Boot?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Return a List or any other Collection type from your controller method. Spring Boot automatically serializes it to a JSON array."
      }
    },
    {
      "@type": "Question",
      "name": "How do I customize JSON output in Spring Boot?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Use Jackson annotations like @JsonIgnore to hide fields, @JsonProperty to rename fields, and @JsonInclude(NON_NULL) to exclude null values from the output."
      }
    },
    {
      "@type": "Question",
      "name": "How do I enforce JSON:API format in Spring Boot?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Use JsonApi4j to automatically wrap all your responses in the JSON:API standard format. Define your resources with @JsonApiResource and your operations with ResourceOperations, and the framework handles the rest."
      }
    },
    {
      "@type": "Question",
      "name": "How do I avoid circular reference errors in Spring Boot JSON?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Use @JsonManagedReference and @JsonBackReference on bidirectional relationships. Alternatively, use @JsonIgnore on one side, or use a framework like JsonApi4j that handles relationships as resource linkages without circular serialization."
      }
    }
  ]
}
</script>
