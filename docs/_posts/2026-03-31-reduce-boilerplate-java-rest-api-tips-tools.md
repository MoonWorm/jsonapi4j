---
title: "Reduce Boilerplate in Java REST APIs: Tips and Tools"
date: 2026-03-31
permalink: /reduce-boilerplate-java-rest-api-tips-tools/
categories:
  - tutorials
tags:
  - java
  - rest-api
  - json-api
  - jsonapi4j
  - spring-boot
excerpt: "Practical tools and techniques to reduce boilerplate in Java REST APIs — from Lombok and MapStruct to OpenAPI codegen and declarative frameworks like JsonApi4j."
---

Java REST APIs accumulate boilerplate fast. Every new resource means another controller, service, DTOs, mapper, and error handling. Multiply by twenty resources and you have thousands of lines of nearly identical code.

This guide covers practical tools that each attack a different layer of the problem. Some reduce the code you write. Others eliminate entire categories of code.

## The Layers of Boilerplate

Before choosing tools, understand where the repetition comes from:

| Layer | What you write | Why it repeats |
|-------|---------------|----------------|
| **Model** | Getters, setters, constructors, equals, hashCode | Every POJO needs them |
| **Mapping** | Entity-to-DTO conversion logic | Every resource needs a mapper |
| **Controller** | `@GetMapping`, `@PostMapping`, request/response handling | Every resource needs CRUD endpoints |
| **Response format** | Envelope wrappers, pagination, links | Every endpoint needs consistent structure |
| **Error handling** | Exception mappers, validation formatting | Duplicated unless centralized |

Each layer has its own solution. The most effective strategy combines tools across multiple layers.

## Layer 1: Eliminate Model Boilerplate with Lombok

[Lombok](https://projectlombok.org/) removes the most tedious code in Java: getters, setters, constructors, builders, equals/hashCode, and toString.

**Without Lombok** (35 lines):

```java
public class ProductDto {
    private String id;
    private String name;
    private BigDecimal price;

    public ProductDto(String id, String name, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    // ... setters, equals, hashCode, toString
}
```

**With Lombok** (7 lines):

```java
@Data
@AllArgsConstructor
public class ProductDto {
    private String id;
    private String name;
    private BigDecimal price;
}
```

Lombok is a compile-time tool — no runtime overhead, no reflection. It works with every framework and IDE. Use `@Data` for mutable DTOs, `@Value` for immutable ones, and `@Builder` for objects with many optional fields.

**Impact:** Cuts model code by ~70%, but does not reduce the number of classes you need.

## Layer 2: Automate Mapping with MapStruct

[MapStruct](https://mapstruct.org/) generates type-safe mapper implementations at compile time. Instead of writing mapping logic by hand for every entity-DTO pair, you define an interface.

```java
@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductDto toDto(ProductEntity entity);
    ProductEntity toEntity(CreateProductRequest request);
}
```

MapStruct generates the implementation at compile time. It handles field name matching, type conversions, and nested objects. For custom mappings, add `@Mapping` annotations:

```java
@Mapping(source = "createdAt", target = "creationDate")
@Mapping(target = "fullName", expression = "java(entity.getFirst() + \" \" + entity.getLast())")
ProductDto toDto(ProductEntity entity);
```

**Impact:** Eliminates manual mapping code, but you still need a mapper interface per entity-DTO pair.

## Layer 3: Reduce Controller Code

### Option A: Centralize with @ControllerAdvice

At minimum, centralize error handling so it is not duplicated across controllers:

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(404)
            .body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }
}
```

This removes error handling from individual controllers, but you still write a controller for every resource. For a deeper look at error handling strategies, see [Java REST API Error Handling: Best Practices](/java-rest-api-error-handling-best-practices/).

### Option B: Generate Controllers from OpenAPI

If your API design starts with an OpenAPI specification, [OpenAPI Generator](https://openapi-generator.tech/) can generate controller interfaces and DTOs from the spec:

```bash
openapi-generator generate -i api.yaml -g spring -o ./generated
```

This produces controller interfaces, model classes, and Spring Boot configuration. You implement the interface methods with your business logic.

**Impact:** Removes hand-written controller and DTO boilerplate, but requires maintaining an OpenAPI spec as the source of truth. Works well for contract-first teams.

### Option C: Eliminate Controllers Entirely

Declarative frameworks remove the controller layer altogether. Instead of generating controllers from a spec, you define resources and let the framework handle routing.

With [JsonApi4j](https://api4.pro/), a resource with full CRUD support requires two classes:

```java
@JsonApiResource(resourceType = "projects")
public class ProjectResource implements Resource<ProjectDto> {

    @Override
    public String resolveResourceId(ProjectDto dto) {
        return dto.getId();
    }

    @Override
    public ProjectAttributes resolveAttributes(ProjectDto dto) {
        return new ProjectAttributes(dto.getName(), dto.getStatus());
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
            projectService.findPage(request.getPaginationRequest()),
            request.getPaginationRequest().getCursor()
        );
    }

    @Override
    public ProjectDto create(JsonApiRequest request) {
        return projectService.create(request.getSingleResourceDocPayload());
    }

    @Override
    public void update(JsonApiRequest request) {
        projectService.update(request.getResourceId(), request.getSingleResourceDocPayload());
    }

    @Override
    public void delete(JsonApiRequest request) {
        projectService.delete(request.getResourceId());
    }
}
```

No controller. No DTOs for request/response. No mapper. The framework generates `GET /projects`, `GET /projects/{id}`, `POST /projects`, `PATCH /projects/{id}`, and `DELETE /projects/{id}` — all returning [JSON:API](https://jsonapi.org/)-compliant responses with pagination, links, and standardized error formatting.

**Impact:** Eliminates the controller, DTO, and mapper layers entirely. The `resolveAttributes` method replaces your mapper, and the JSON:API structure replaces your response envelope.

## Layer 4: Standardize Response Format

Without a standard, every developer invents their own envelope:

```json
// Developer A                    // Developer B
{ "data": { ... } }              { "result": { ... } }
{ "error": "not found" }         { "errors": [{ "msg": "..." }] }
```

JsonApi4j enforces the [JSON:API specification](https://jsonapi.org/format/) across all endpoints automatically. Every response includes `data` with `type`, `id`, and `attributes`, `links` for pagination, and consistent `errors` documents. Features like [Sparse Fieldsets](https://jsonapi.org/format/#fetching-sparse-fieldsets) and [Compound Documents](https://jsonapi.org/format/#document-compound-documents) work out of the box.

For APIs that do not need JSON:API, you can still standardize by creating a shared `ApiResponse<T>` wrapper and enforcing it through code review — but you are maintaining that envelope code yourself.

## Combining Tools

These tools are not mutually exclusive. A practical stack might look like:

| If your API is... | Recommended stack |
|-------------------|-------------------|
| Simple CRUD over JPA | Lombok + Spring Data REST |
| Contract-first with OpenAPI | Lombok + MapStruct + OpenAPI Generator |
| JSON:API compliant, mixed data sources | Lombok + JsonApi4j |
| Large API with strict consistency | Lombok + JsonApi4j + plugins (access control, OpenAPI, sparse fieldsets) |

The common thread is Lombok — there is no reason not to use it in a Java project. Beyond that, the choice depends on whether you want to eliminate mapping (MapStruct), controllers (declarative frameworks), or both.

## Conclusion

Boilerplate in Java REST APIs is not one problem — it is several. Model ceremony, mapping logic, controller repetition, and inconsistent response formats each have their own solution.

In this guide, you learned:
- Lombok eliminates POJO ceremony (getters, setters, constructors)
- MapStruct generates type-safe mappers at compile time
- OpenAPI Generator creates controllers from specs for contract-first teams
- Declarative frameworks like [JsonApi4j](https://api4.pro/) eliminate controllers, mappers, and response formatting entirely
- Combining tools across layers gives the biggest reduction

Start with the layer that hurts the most in your project, and work outward.

---

## FAQ

### What is the biggest source of boilerplate in Java REST APIs?

Controller code and DTO mapping. Every resource needs CRUD endpoints, request/response DTOs, and conversion logic. Tools like [JsonApi4j](https://api4.pro/) eliminate all three by generating endpoints from resource definitions, while MapStruct automates mapping for traditional controller-based APIs.

### Should I use Lombok in my Java project?

Yes. Lombok eliminates getters, setters, constructors, and builder patterns with zero runtime overhead. It is a compile-time annotation processor supported by all major IDEs and build tools. There is almost no downside.

### How does MapStruct compare to manual DTO mapping?

MapStruct generates type-safe mapping implementations at compile time, catching mapping errors during build rather than at runtime. It handles field matching, type conversions, and nested objects automatically. Manual mapping is error-prone and tedious at scale.

### When should I use OpenAPI Generator vs a declarative framework?

Use OpenAPI Generator if your team follows a contract-first workflow where the API spec is designed before implementation. Use a declarative framework like [JsonApi4j](https://api4.pro/) if you prefer code-first development and want the framework to handle routing, serialization, and response formatting from your domain model.

### Can I combine multiple boilerplate reduction tools?

Absolutely. Lombok works alongside any framework. MapStruct pairs well with controller-based APIs. JsonApi4j replaces controllers and mappers entirely but benefits from Lombok on your DTOs and attribute classes.

<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "FAQPage",
  "mainEntity": [
    {
      "@type": "Question",
      "name": "What is the biggest source of boilerplate in Java REST APIs?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Controller code and DTO mapping. Every resource needs CRUD endpoints, request/response DTOs, and conversion logic. Tools like JsonApi4j eliminate all three by generating endpoints from resource definitions, while MapStruct automates mapping for traditional controller-based APIs."
      }
    },
    {
      "@type": "Question",
      "name": "Should I use Lombok in my Java project?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Yes. Lombok eliminates getters, setters, constructors, and builder patterns with zero runtime overhead. It is a compile-time annotation processor supported by all major IDEs and build tools."
      }
    },
    {
      "@type": "Question",
      "name": "How does MapStruct compare to manual DTO mapping?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "MapStruct generates type-safe mapping implementations at compile time, catching mapping errors during build rather than at runtime. It handles field matching, type conversions, and nested objects automatically."
      }
    },
    {
      "@type": "Question",
      "name": "When should I use OpenAPI Generator vs a declarative framework?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Use OpenAPI Generator if your team follows a contract-first workflow where the API spec is designed before implementation. Use a declarative framework like JsonApi4j if you prefer code-first development and want the framework to handle routing, serialization, and response formatting from your domain model."
      }
    },
    {
      "@type": "Question",
      "name": "Can I combine multiple boilerplate reduction tools?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Absolutely. Lombok works alongside any framework. MapStruct pairs well with controller-based APIs. JsonApi4j replaces controllers and mappers entirely but benefits from Lombok on your DTOs and attribute classes."
      }
    }
  ]
}
</script>
