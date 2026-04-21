---
title: "Error Handling"
permalink: /error-handling/
---

JsonApi4j automatically converts exceptions into [JSON:API error responses](https://jsonapi.org/format/#errors). Every exception thrown during request processing is caught, mapped to an `ErrorsDoc`, and returned with the appropriate HTTP status code. No manual error formatting is needed.

## How It Works

When an exception is thrown during request processing:

1. The dispatcher servlet catches the exception
2. The `ErrorHandlerFactoriesRegistry` finds a matching `ErrorsDocSupplier` for the exception class
3. The supplier converts the exception into an `ErrorsDoc` (containing one or more `ErrorObject`s) and an HTTP status code
4. The servlet writes the JSON:API error response

If no handler matches the exact exception class, the registry searches all registered exception classes and selects the **most specific** ancestor (closest parent in the class hierarchy). If nothing matches, a generic 500 Internal Server Error is returned.

```
Exception thrown
    │
    ▼
ErrorHandlerFactoriesRegistry.getErrorResponseMapper(exception)
    │
    ├── Exact class match found? → Use that handler
    │
    ├── Ancestor match found? → Use the most specific ancestor handler
    │
    └── No match → 500 Internal Server Error
    │
    ▼
ErrorsDocSupplier.getErrorResponse(exception) → ErrorsDoc
ErrorsDocSupplier.getHttpStatus(exception) → HTTP status code
    │
    ▼
JSON:API error response written to client
```

4xx errors are logged at `WARN` level (client errors). 5xx errors are logged at `ERROR` level with full stack trace.

## Error Response Structure

Every error response follows the [JSON:API error format](https://jsonapi.org/format/#error-objects). The response body is an `ErrorsDoc` containing a list of `ErrorObject`s:

```json
{
  "errors": [
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "status": "400",
      "code": "VALUE_IS_ABSENT",
      "detail": "Field 'name' must not be null",
      "source": {
        "parameter": "name"
      }
    }
  ]
}
```

Each `ErrorObject` has the following fields:

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Unique identifier (UUID) for this error occurrence |
| `status` | String | HTTP status code as a string (e.g., `"404"`) |
| `code` | String | Application-specific error code (e.g., `"NOT_FOUND"`) |
| `detail` | String | Human-readable explanation of the error |
| `source` | Object | Points to the error source — contains `pointer`, `parameter`, or `header` |
| `title` | String | Short summary of the error (optional) |
| `links` | Object | Links related to the error (optional) |
| `meta` | Object | Additional metadata (optional) |

## Built-in Exception Handling

JsonApi4j registers two default error handler factories.

### DefaultErrorHandlerFactory

Covers all framework-specific exceptions from the core and REST layers, as well as user-facing exceptions from the `pro.api4.jsonapi4j.exception` package. The registry matches the most specific exception class first — subclass handlers (e.g., `ConstraintViolationException`) take priority over the `JsonApi4jException` catch-all.

| Exception | HTTP Status | Error Code | When It Occurs |
|-----------|------------|------------|----------------|
| `ConstraintViolationException` | 400 | From exception | Validation failures — request parameters, operation constraints, business rules |
| `ResourceNotFoundException` | 404 | `NOT_FOUND` | Resource with the given ID does not exist |
| `JsonApi4jException` | From exception | From exception | Catch-all for exceptions that extend `JsonApi4jException` — uses the exception's own `httpStatus` and `errorCode` |
| `DataRetrievalException` | 502 | `BAD_GATEWAY` | Downstream service failed to return data |
| `MappingException` | 500 | `INTERNAL_SERVER_ERROR` | DTO-to-JSON:API object failure |
| `OperationNotFoundException` | 404 | `NOT_FOUND` | Requested operation is not implemented for this resource |

The first three rows handle exceptions from the [user-facing hierarchy](#exception-hierarchy). The remaining rows handle framework-internal exceptions.

### Jsr380ErrorHandlers

Optional. Can be added if needed since JSR-380 is de-facto one of the most common ways to validate input in Java applications. Handles `jakarta.validation.ConstraintViolationException` thrown when JSR-380 annotations on your models are violated. Each constraint violation becomes a separate `ErrorObject` in the response, all returned with HTTP 400.

The constraint annotation determines the error code:

| Annotation | Error Code |
|-----------|------------|
| `@NotNull` | `VALUE_IS_ABSENT` |
| `@NotBlank` | `VALUE_EMPTY` |
| `@Size` (on String/number) | `VALUE_TOO_LONG` |
| `@Size` (on Collection) | `ARRAY_LENGTH_TOO_LONG` |
| `@Pattern` | `VALUE_INVALID_FORMAT` |
| `@Digits` | `VALUE_INVALID_FORMAT` |
| `@Positive` | `VALUE_TOO_LOW` |
| `@Max` | `VALUE_TOO_HIGH` |
| Other annotations | `GENERIC_REQUEST_ERROR` |

For example, an `UserAttributes` object with `@NotNull` and `@Size` constraints:

```java
public class UserAttributes {
    @NotNull
    private String name;

    @Size(max = 100)
    private String bio;
}
```

If both constraints are violated, the response contains two errors:

```json
{
  "errors": [
    {
      "id": "...",
      "status": "400",
      "code": "VALUE_IS_ABSENT",
      "detail": "must not be null",
      "source": { "parameter": "name" }
    },
    {
      "id": "...",
      "status": "400",
      "code": "VALUE_TOO_LONG",
      "detail": "size must be between 0 and 100",
      "source": { "parameter": "bio" }
    }
  ]
}
```

## Error Codes

Error codes are represented by the `ErrorCode` interface (single method: `toCode()`). The framework provides `DefaultErrorCodes` with 32 built-in codes organized by category:

**Request validation:** `GENERIC_REQUEST_ERROR`, `MISSING_REQUIRED_PARAMETER`, `MISSING_REQUIRED_HEADER`, `INVALID_ENUM_VALUE`, `VALUE_IS_ABSENT`, `VALUE_EMPTY`, `VALUE_TOO_SHORT`, `VALUE_TOO_LONG`, `VALUE_TOO_HIGH`, `VALUE_TOO_LOW`, `VALUE_INVALID_FORMAT`, `ARRAY_LENGTH_TOO_SHORT`, `ARRAY_LENGTH_TOO_LONG`, `CONFLICTING_PARAMETERS`, `INVALID_CURSOR`, `INVALID_LIMIT`, `INVALID_PAYLOAD`

**HTTP/server:** `NOT_FOUND`, `METHOD_NOT_SUPPORTED`, `NOT_ACCEPTABLE`, `UNSUPPORTED_MEDIA_TYPE`, `CONFLICT`, `BAD_GATEWAY`, `INTERNAL_SERVER_ERROR`, `SERVICE_UNAVAILABLE`, `MAX_AMOUNT_OF_RESOURCES`

**Authentication/authorization:** `UNAUTHORIZED`, `ACCESS_TOKEN_REVOKED`, `ACCESS_TOKEN_EXPIRED`, `FORBIDDEN`, `INSUFFICIENT_SCOPES`, `INSUFFICIENT_ACCESS_TIER`

You can define your own error codes by implementing `ErrorCode`:

```java
public enum MyErrorCodes implements ErrorCode {

    DUPLICATE_EMAIL,
    ACCOUNT_SUSPENDED,
    QUOTA_EXCEEDED;

    @Override
    public String toCode() {
        return name();
    }
}
```

## Custom Error Handlers

To handle your own exceptions, implement `ErrorHandlerFactory` and register it as a bean. The framework auto-discovers custom factories and adds them to the registry alongside the built-in ones.

### 1. Define Your Exception

```java
public class DuplicateEmailException extends RuntimeException {

    private final String email;

    public DuplicateEmailException(String email) {
        super("Email already registered: " + email);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
```

### 2. Create an ErrorHandlerFactory

```java
public class MyErrorHandlerFactory implements ErrorHandlerFactory {

    @Override
    public Map<Class<? extends Throwable>, ErrorsDocSupplier<?>> getErrorResponseMappers() {
        return Map.of(
            DuplicateEmailException.class, new ErrorsDocSupplier<DuplicateEmailException>() {
                @Override
                public ErrorsDoc getErrorResponse(DuplicateEmailException ex) {
                    return ErrorsDocFactory.conflictErrorsDoc(
                        "Email " + ex.getEmail() + " is already registered"
                    );
                }

                @Override
                public int getHttpStatus(DuplicateEmailException ex) {
                    return 409;
                }
            }
        );
    }
}
```

`ErrorsDocFactory` provides convenience methods for common HTTP statuses: `badRequestErrorsDoc()`, `resourceNotFoundErrorsDoc()`, `conflictErrorsDoc()`, `badGatewayErrorsDoc()`, `internalServerErrorsDoc()`, `unsupportedMediaTypeErrorsDoc()`, `notAcceptableErrorsDoc()`, `methodNotSupportedErrorsDoc()`, and the generic `genericErrorsDoc(status, code, detail)`.

### 3. Register It

<div class="tabs" markdown="0">
  <div class="tab-buttons">
    <button class="tab-btn active" data-tab="err-springboot">Spring Boot</button>
    <button class="tab-btn" data-tab="err-quarkus">Quarkus</button>
    <button class="tab-btn" data-tab="err-servlet">Servlet API</button>
  </div>
  <div id="err-springboot" class="tab-panel active">
    <p>Define the factory as a Spring bean. The framework auto-discovers all <code>ErrorHandlerFactory</code> beans via <code>ObjectProvider</code>.</p>
    <div class="language-java highlighter-rouge"><div class="highlight"><pre class="highlight"><code><span class="nd">@Configuration</span>
<span class="kd">public</span> <span class="kd">class</span> <span class="nc">ErrorConfig</span> <span class="o">{</span>

    <span class="nd">@Bean</span>
    <span class="kd">public</span> <span class="nc">ErrorHandlerFactory</span> <span class="nf">myErrorHandlerFactory</span><span class="o">()</span> <span class="o">{</span>
        <span class="k">return</span> <span class="k">new</span> <span class="nc">MyErrorHandlerFactory</span><span class="o">();</span>
    <span class="o">}</span>
<span class="o">}</span></code></pre></div></div>
  </div>
  <div id="err-quarkus" class="tab-panel">
    <p>Provide the factory as a CDI bean. The framework discovers all <code>ErrorHandlerFactory</code> instances via <code>Instance&lt;ErrorHandlerFactory&gt;</code>.</p>
    <div class="language-java highlighter-rouge"><div class="highlight"><pre class="highlight"><code><span class="kd">public</span> <span class="kd">class</span> <span class="nc">ErrorConfig</span> <span class="o">{</span>

    <span class="nd">@Produces</span>
    <span class="nd">@Singleton</span>
    <span class="kd">public</span> <span class="nc">ErrorHandlerFactory</span> <span class="nf">myErrorHandlerFactory</span><span class="o">()</span> <span class="o">{</span>
        <span class="k">return</span> <span class="k">new</span> <span class="nc">MyErrorHandlerFactory</span><span class="o">();</span>
    <span class="o">}</span>
<span class="o">}</span></code></pre></div></div>
  </div>
  <div id="err-servlet" class="tab-panel">
    <p>Register the factory on the <code>ErrorHandlerFactoriesRegistry</code> before the framework initializes, or set it as a <code>ServletContext</code> attribute.</p>
    <div class="language-java highlighter-rouge"><div class="highlight"><pre class="highlight"><code><span class="nc">ErrorHandlerFactoriesRegistry</span> <span class="n">registry</span> <span class="o">=</span> <span class="k">new</span> <span class="nc">JsonApi4jErrorHandlerFactoriesRegistry</span><span class="o">();</span>
<span class="n">registry</span><span class="o">.</span><span class="na">registerAll</span><span class="o">(</span><span class="k">new</span> <span class="nc">DefaultErrorHandlerFactory</span><span class="o">());</span>
<span class="n">registry</span><span class="o">.</span><span class="na">registerAll</span><span class="o">(</span><span class="k">new</span> <span class="nc">Jsr380ErrorHandlers</span><span class="o">());</span>
<span class="n">registry</span><span class="o">.</span><span class="na">registerAll</span><span class="o">(</span><span class="k">new</span> <span class="nc">MyErrorHandlerFactory</span><span class="o">());</span>
<span class="n">servletContext</span><span class="o">.</span><span class="na">setAttribute</span><span class="o">(</span>
    <span class="nc">JsonApi4jServletContainerInitializer</span><span class="o">.</span><span class="na">ERROR_HANDLER_FACTORIES_REGISTRY_ATT_NAME</span><span class="o">,</span>
    <span class="n">registry</span>
<span class="o">);</span></code></pre></div></div>
  </div>
</div>

Custom factories are registered **after** the built-in ones. If you register a handler for an exception class that already has a built-in handler, your handler replaces it. When no exact match exists, the registry selects the most specific registered ancestor — so a custom handler for a parent exception class won't shadow a more specific built-in handler (or vice versa).

## Throwing Errors from Operations

The simplest way to return an error from your operation code is to throw a `JsonApi4jException` with the appropriate status, code, and detail:

```java
@Override
public UserDto readById(JsonApiRequest request) {
    UserDto user = userDb.findById(request.getResourceId());
    if (user == null) {
        throw new ResourceNotFoundException(
            "users", request.getResourceId()
        );
    }
    if (user.isSuspended()) {
        throw new JsonApi4jException(
            403, MyErrorCodes.ACCOUNT_SUSPENDED,
            "Account " + request.getResourceId() + " is suspended"
        );
    }
    return user;
}
```

For validation errors, throw `ConstraintViolationException` with an error code, detail message, and the offending parameter name. The framework formats the `source.parameter` field automatically:

```java
@Override
public void validate(JsonApiRequest request) {
    String region = request.getFilterValue("region");
    if (region != null && !SUPPORTED_REGIONS.contains(region)) {
        throw new ConstraintViolationException(
            DefaultErrorCodes.INVALID_ENUM_VALUE,
            "Unsupported region: " + region,
            "filter[region]"
        );
    }
}
```

If you don't need a specific error code, use the two-argument constructor — it defaults to `GENERIC_REQUEST_ERROR`:

```java
throw new ConstraintViolationException("name must not be blank", "name");
```

For situations where a resource is not found — either throw `ResourceNotFoundException` or use built-in helper methods on the operation level: `ResourceOperations#throwResourceNotFoundException(JsonApiRequest)`. 

## Exception Hierarchy

```
RuntimeException
│
├── JsonApi4jException (httpStatus, errorCode, detail)        — user-facing
│   ├── ConstraintViolationException (+ parameter)            — 400
│   │   ├── InvalidCursorException                            — 400 INVALID_CURSOR
│   │   ├── InvalidLimitException                             — 400 INVALID_LIMIT
│   │   └── InvalidPayloadException                           — 400 INVALID_PAYLOAD
│   ├── ResourceNotFoundException                             — 404 NOT_FOUND
│   ├── MethodNotSupportedException                           — 405 METHOD_NOT_SUPPORTED
│   ├── NotAcceptableException                                — 406 NOT_ACCEPTABLE
│   └── UnsupportedMediaTypeException                         — 415 UNSUPPORTED_MEDIA_TYPE
│
├── DataRetrievalException                                    — framework-internal
│   └── (caught and mapped to 502 BAD_GATEWAY)
├── MappingException                                          — 500 INTERNAL_SERVER_ERROR
└── OperationNotFoundException                                — 404 NOT_FOUND
```

**User-facing exceptions** (extend `JsonApi4jException`) are designed to be thrown from your operation code. They carry their own HTTP status and error code, so the framework converts them into proper JSON:API error responses automatically.

**Framework-internal exceptions** (`DataRetrievalException`, `MappingException`, `OperationNotFoundException`) are thrown by the framework itself and handled by `DefaultErrorHandlerFactory` with dedicated handlers. You generally don't throw these directly, except `DataRetrievalException` when a downstream service call fails.
