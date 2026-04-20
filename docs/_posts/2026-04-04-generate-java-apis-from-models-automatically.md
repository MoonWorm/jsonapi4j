---
title: "How to Generate Java APIs from Models Automatically"
date: 2026-04-04
permalink: /generate-java-apis-from-models-automatically/
categories:
  - tutorials
tags:
  - java
  - rest-api
  - json-api
  - jsonapi4j
excerpt: "Learn how to automatically generate REST APIs in Java from your data models. Tools like JsonApi4j can reduce boilerplate, enforce JSON:API standards, and simplify endpoint creation."
---

Writing controllers, DTOs, and mapping logic for every model in your Java application is tedious and error-prone. What if your API endpoints could be generated directly from your data models?

In this guide, you'll learn how to define your models once and let a framework generate a fully functional, standards-compliant REST API automatically.

## Step 1: Define Your Models

Start with plain Java objects that represent your domain. No special base classes or complex annotations are required.

```java
@Data
public class UserDto {
    private String id;
    private String name;
    private String email;
}
```

```java
@Data
public class UserAttributes {
    private final String name;
    private final String email;
}
```

These are the objects your application already works with. The goal is to expose them as API resources without writing controllers or response wrappers.

## Step 2: Configure API Generation

Add the [JsonApi4j](https://api4.pro/) dependency to your Spring Boot project:

```xml
<dependency>
    <groupId>pro.api4</groupId>
    <artifactId>jsonapi4j-rest-springboot</artifactId>
    <version>${jsonapi4j.version}</version>
</dependency>
```

Then annotate your model as a JSON:API resource and define how its attributes are resolved:

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

Define what operations the resource supports:

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

JsonApi4j scans for these definitions at startup and generates the corresponding REST endpoints. `GET /users` and `GET /users/{id}` are available immediately, returning properly formatted [JSON:API](https://jsonapi.org/format/) documents.

## Step 3: Handle Relationships Automatically

Real APIs rarely have isolated resources. Users have orders, orders have line items, and line items reference products. Defining these relationships manually in every controller is a major source of boilerplate.

With JsonApi4j, relationships are declared as part of your resource model:

```java
@JsonApiRelationship(relationshipName = "orders", parentResource = UserResource.class)
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

The framework automatically includes relationship links in the response:

```json
{
  "data": {
    "type": "users",
    "id": "1",
    "attributes": {
      "name": "John",
      "email": "john@example.com"
    },
    "relationships": {
      "orders": {
        "links": {
          "related": "/users/1/orders"
        }
      }
    },
    "links": {
      "self": "/users/1"
    }
  }
}
```

For [Compound Documents](https://jsonapi.org/format/#document-compound-documents), clients can request `GET /users/1?include=orders` and the related resources are resolved and included in the response automatically, with multi-hop traversal support.

## Step 4: Customize JSON and Endpoints

While the framework generates sensible defaults, you have full control over what is exposed and how.

**Control which attributes are visible:**

The `resolveAttributes` method determines exactly which fields appear in the JSON output. Only the fields you explicitly return are included:

```java
@Override
public UserAttributes resolveAttributes(UserDto dto) {
    return new UserAttributes(dto.getName()); // email is not exposed
}
```

**Configure pagination and sorting:**

JsonApi4j supports cursor-based pagination and sorting out of the box. Configure limits in your `application.yml`:

```yaml
jsonapi4j:
  pagination:
    default-limit: 20
    max-limit: 100
```

**Use Sparse Fieldsets:**

Clients can request only the fields they need using the [JSON:API Sparse Fieldsets](https://jsonapi.org/format/#fetching-sparse-fieldsets) feature:

```
GET /users?fields[users]=name
```

This is handled by the Sparse Fieldsets plugin without any changes to your resource definitions.

**Add access control:**

The Access Control plugin lets you restrict fields based on authentication, scopes, or ownership:

```java
@AccessControl(authenticated = true, scopes = {"admin"})
private String email;
```

## Step 5: Test and Extend

Adding a new resource to your API follows the same pattern every time: define the resource class, define the operations class, and write tests.

```java
@Test
void shouldReturnUserById() {
    given()
        .accept("application/vnd.api+json")
    .when()
        .get("/users/1")
    .then()
        .statusCode(200)
        .body("data.type", equalTo("users"))
        .body("data.id", equalTo("1"))
        .body("data.attributes.name", equalTo("John"));
}
```

When you add a new model, you don't touch existing code. Define a new `Resource` implementation and a new `ResourceOperations` class, and the framework generates the endpoints. Relationships between new and existing resources are wired through `@JsonApiRelationship` annotations.

This approach scales well. Whether your API has five resources or fifty, the pattern remains the same: models define the shape of your data, and the framework generates the infrastructure.

## Conclusion

Generating Java APIs from models eliminates the repetitive work of writing controllers, DTOs, and mappers for every resource.

In this guide, you learned:
- How to define models as JSON:API resources with `@JsonApiResource`
- How JsonApi4j generates endpoints from resource and operation definitions
- How relationships are declared and resolved automatically
- How to customize the output with attribute resolvers, pagination, sparse fieldsets, and access control
- How adding new resources requires only new model definitions, not new infrastructure code

Get started with the [Getting Started guide](https://api4.pro/getting-started/) for a full walkthrough.

---

## FAQ

### How do I generate REST APIs from Java models?

Use a framework like [JsonApi4j](https://api4.pro/) that scans your annotated model classes and generates REST endpoints automatically. Define your models with `@JsonApiResource`, implement `ResourceOperations` for data access, and the framework handles routing and JSON serialization.

### Do I still need to write controllers and DTOs manually?

No. With JsonApi4j, you define your resources and operations declaratively. The framework generates the endpoints and handles JSON:API response formatting. You focus on the data access logic in your `ResourceOperations` implementation.

### Are relationships between resources generated automatically?

Yes. Declare relationships using `@JsonApiRelationship` and implement a resolver method. The framework automatically includes relationship links in responses and supports compound documents with the `?include` parameter.

### Can I customize which fields and endpoints are generated?

Yes. The `resolveAttributes` method controls which fields appear in the JSON output. Pagination, sorting, sparse fieldsets, and access control are configurable through annotations and application properties.

### Is this approach suitable for large projects?

Absolutely. Each resource is defined independently, so adding new resources does not require modifying existing code. The plugin architecture supports cross-cutting concerns like access control and OpenAPI generation without changing individual resource definitions.

<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "FAQPage",
  "mainEntity": [
    {
      "@type": "Question",
      "name": "How do I generate REST APIs from Java models?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Use a framework like JsonApi4j that scans your annotated model classes and generates REST endpoints automatically. Define your models with @JsonApiResource, implement ResourceOperations for data access, and the framework handles routing and JSON serialization."
      }
    },
    {
      "@type": "Question",
      "name": "Do I still need to write controllers and DTOs manually?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "No. With JsonApi4j, you define your resources and operations declaratively. The framework generates the endpoints and handles JSON:API response formatting. You focus on the data access logic in your ResourceOperations implementation."
      }
    },
    {
      "@type": "Question",
      "name": "Are relationships between resources generated automatically?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Yes. Declare relationships using @JsonApiRelationship and implement a resolver method. The framework automatically includes relationship links in responses and supports compound documents with the ?include parameter."
      }
    },
    {
      "@type": "Question",
      "name": "Can I customize which fields and endpoints are generated?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "The resolveAttributes method controls which fields appear in the JSON output. Pagination, sorting, sparse fieldsets, and access control are configurable through annotations and application properties."
      }
    },
    {
      "@type": "Question",
      "name": "Is this approach suitable for large projects?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Absolutely. Each resource is defined independently, so adding new resources does not require modifying existing code. The plugin architecture supports cross-cutting concerns like access control and OpenAPI generation without changing individual resource definitions."
      }
    }
  ]
}
</script>
