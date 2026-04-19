---
title: "Java REST API Error Handling: Best Practices"
date: 2026-04-09
permalink: /java-rest-api-error-handling-best-practices/
categories:
  - tutorials
tags:
  - java
  - rest-api
  - error-handling
  - spring-boot
  - json-api
  - jsonapi4j
excerpt: "Learn best practices for error handling in Java REST APIs. Ensure consistent responses, proper HTTP status codes, and integrate with frameworks like JsonApi4j for JSON:API-compliant errors."
---

Error handling is one of the most important aspects of a well-designed REST API.

Poor error responses confuse clients, leak sensitive information, and make debugging harder. In this guide, you will learn how to handle errors properly in Java REST APIs, with real code examples and a look at how [JsonApi4j](https://api4.pro/) provides JSON:API-compliant error formatting out of the box.

## Step 1: Use HTTP Status Codes Properly

Every API response should include the correct HTTP status code. Do not return `200 OK` for errors.

Here are the most common status codes your API should use:

| Status Code | Meaning | When to Use |
|-------------|---------|-------------|
| `200 OK` | Success | Successful GET |
| `201 Created` | Resource created | Successful POST |
| `204 No Content` | Success, no body | Successful PATCH, DELETE |
| `400 Bad Request` | Invalid input | Malformed JSON, missing fields |
| `401 Unauthorized` | Not authenticated | Missing or invalid credentials |
| `404 Not Found` | Resource not found | ID does not exist |
| `500 Internal Server Error` | Server failure | Unexpected exceptions |

Using correct status codes allows clients to handle responses programmatically without parsing the body.

## Step 2: Centralize Error Handling

Scattering try-catch blocks across your controllers leads to inconsistent error responses. In Spring Boot, use `@ControllerAdvice` and `@ExceptionHandler` to centralize error handling.

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        Map<String, Object> error = Map.of(
            "status", 404,
            "detail", ex.getMessage()
        );
        return ResponseEntity.status(404).body(Map.of("errors", List.of(error)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        Map<String, Object> error = Map.of(
            "status", 500,
            "detail", "An unexpected error occurred"
        );
        return ResponseEntity.status(500).body(Map.of("errors", List.of(error)));
    }
}
```

This ensures every error response follows the same JSON structure, regardless of where the exception is thrown.

## Step 3: Provide Detailed Error Information

A good error response should include enough information for the client to understand what went wrong and how to fix it.

Include these fields in your error responses:
- **status** -- the HTTP status code
- **code** -- a machine-readable error code
- **title** -- a short summary of the error
- **detail** -- a human-readable explanation

Never expose internal details like stack traces, database queries, or server paths.

### JSON:API Error Format with JsonApi4j

[JsonApi4j](https://api4.pro/) follows the [JSON:API error object specification](https://jsonapi.org/format/#error-objects) and provides built-in classes for structured error responses.

The `ErrorObject` class includes fields for `id`, `status`, `code`, `title`, `detail`, and `source`:

```java
ErrorObject error = ErrorObject.builder()
    .status("404")
    .code("RESOURCE_NOT_FOUND")
    .title("Resource Not Found")
    .detail("User with ID 42 does not exist")
    .build();
```

The `ErrorsDoc` class wraps one or more `ErrorObject` instances into a valid JSON:API errors document. JsonApi4j handles this formatting automatically when exceptions are thrown during request processing.

JsonApi4j also provides a hierarchy of built-in exceptions:
- `JsonApi4jException` -- the root exception with `httpStatus`, `errorCode`, and `detail` fields
- `ResourceNotFoundException` -- for missing resources (404)
- `InvalidPayloadException` -- for bad request bodies (400)
- `BadJsonApiRequestException` -- for invalid request parameters

When any of these exceptions are thrown, JsonApi4j automatically formats the response as a JSON:API error document with the correct HTTP status code.

## Step 4: Validate Requests and Return Errors Early

Do not wait until your service layer to discover that the input is invalid. Use JSR-380 (`@Valid`) annotations to validate request bodies at the controller level.

```java
public class CreateUserRequest {

    @NotNull(message = "Name is required")
    private String name;

    @Email(message = "Email must be valid")
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
```

When validation fails, return `400 Bad Request` with clear messages indicating which fields are invalid.

JsonApi4j takes this further with built-in JSR-380 constraint violation mapping. Validation annotations are automatically mapped to JSON:API error codes:
- `@NotNull` maps to `VALUE_IS_ABSENT`
- `@Size` maps to `VALUE_TOO_LONG` or `ARRAY_LENGTH_TOO_LONG`
- `@Pattern` maps to `VALUE_INVALID_FORMAT`

### Custom Error Handlers

JsonApi4j supports registering custom `ErrorHandlerFactory` implementations through the `ErrorHandlerFactoriesRegistry`. This allows you to map your own application-specific exceptions to JSON:API error responses with the correct status codes and error details.

## Step 5: Logging and Monitoring

Always log exceptions on the server side. Logs should include the full stack trace, request details, and any relevant context.

Key practices:
- Log all `5xx` errors at `ERROR` level
- Log `4xx` errors at `WARN` level
- Never expose stack traces in API responses
- Include a correlation ID in both logs and error responses for traceability
- Integrate with monitoring tools (e.g., Prometheus, Grafana, Datadog) to track error rates

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
    log.error("Unexpected error processing request", ex);
    // Return a generic message to the client
    Map<String, Object> error = Map.of(
        "status", 500,
        "detail", "An internal error occurred. Please try again later."
    );
    return ResponseEntity.status(500).body(Map.of("errors", List.of(error)));
}
```

## Conclusion

Good error handling is not optional in a production REST API.

In this guide, you learned:
- How to use HTTP status codes correctly
- How to centralize error handling with `@ControllerAdvice`
- How to provide structured error responses without exposing internals
- How to validate input early and return clear error messages
- How to log errors for debugging and monitoring

If you are building a JSON:API-compliant API, [JsonApi4j](https://api4.pro/) handles error formatting automatically with classes like `ErrorObject`, `ErrorsDoc`, and built-in exception mapping. Check out the [Getting Started guide](https://api4.pro/getting-started/) to see it in action.

---

## FAQ

### How should I handle errors in a Java REST API?

Use proper HTTP status codes, centralize error handling with `@ControllerAdvice` and `@ExceptionHandler`, and return a consistent JSON error structure with fields like `status`, `code`, and `detail`.

### How does JsonApi4j help with error handling?

JsonApi4j automatically formats error responses according to the [JSON:API specification](https://jsonapi.org/format/#error-objects). It provides built-in exceptions like `JsonApi4jException` and `ResourceNotFoundException` that are automatically converted to properly structured error documents with the correct HTTP status codes.

### How do I avoid exposing server details in error responses?

Return generic error messages to clients and log the full exception details internally. Never include stack traces, file paths, or database information in API responses.

### How should I handle validation errors in a REST API?

Use JSR-380 annotations like `@NotNull`, `@Email`, and `@Size` on your request objects. Return `400 Bad Request` with clear per-field error messages when validation fails. JsonApi4j maps these violations to JSON:API error codes automatically.

### Should I use the same JSON structure for all error responses?

Yes. A consistent error format makes your API predictable and easier for clients to consume. The [JSON:API error object specification](https://jsonapi.org/format/#error-objects) provides a standard structure with `status`, `code`, `title`, and `detail` fields.

<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "FAQPage",
  "mainEntity": [
    {
      "@type": "Question",
      "name": "How should I handle errors in a Java REST API?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Use proper HTTP status codes, centralize error handling with @ControllerAdvice and @ExceptionHandler, and return a consistent JSON error structure with fields like status, code, and detail."
      }
    },
    {
      "@type": "Question",
      "name": "How does JsonApi4j help with error handling?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "JsonApi4j automatically formats error responses according to the JSON:API specification. It provides built-in exceptions like JsonApi4jException and ResourceNotFoundException that are automatically converted to properly structured error documents with the correct HTTP status codes."
      }
    },
    {
      "@type": "Question",
      "name": "How do I avoid exposing server details in error responses?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Return generic error messages to clients and log the full exception details internally. Never include stack traces, file paths, or database information in API responses."
      }
    },
    {
      "@type": "Question",
      "name": "How should I handle validation errors in a REST API?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Use JSR-380 annotations like @NotNull, @Email, and @Size on your request objects. Return 400 Bad Request with clear per-field error messages when validation fails. JsonApi4j maps these violations to JSON:API error codes automatically."
      }
    },
    {
      "@type": "Question",
      "name": "Should I use the same JSON structure for all error responses?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Yes. A consistent error format makes your API predictable and easier for clients to consume. The JSON:API error object specification provides a standard structure with status, code, title, and detail fields."
      }
    }
  ]
}
</script>
