---
title: "JsonApi4j"
layout: splash
permalink: /
header:
  overlay_color: "#1a1a2e"
  overlay_filter: "linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%)"
  actions:
    - label: "Get Started"
      url: /getting-started/
    - label: "View on GitHub"
      url: "https://github.com/MoonWorm/jsonapi4j"
excerpt: "A modern, lightweight Java framework for building production-ready REST APIs compliant with the JSON:API specification. Define your domain — the framework handles the rest."

feature_row_main:
  - title: "JSON:API Compliant"
    excerpt: "Purpose-built around the [JSON:API specification](https://jsonapi.org) — predictable request/response format and processing rules."
  - title: "Framework Agnostic"
    excerpt: "Works with **Spring Boot**, **Quarkus**, and plain **Jakarta Servlet API**. Add one dependency and the framework auto-configures itself in your environment."
  - title: "Pluggable Architecture"
    excerpt: "Hook into the request pipeline with the visitor-based Plugin System. Ships with **Access Control**, **Sparse Fieldsets**, **OpenAPI**, and **Compound Documents** plugins."

feature_row_advanced:
  - title: "Compound Documents"
    excerpt: "Multi-level `include` queries with parallel batch resolution, built-in caching, and Cache-Control aggregation. Deployable at the application or API Gateway level."
  - title: "Fine-Grained Access Control"
    excerpt: "Declarative, annotation-driven authorization — per-field anonymization based on access tier, OAuth2 scopes, and resource ownership. No changes to core logic."
  - title: "Parallel Execution"
    excerpt: "Relationship resolution, compound document fetching, and batch operations run concurrently. Supports virtual threads (Project Loom) for maximum throughput."

feature_row_dx:
  - title: "Minimal Boilerplate"
    excerpt: "Define resources, relationships, and operations — the framework automatically generates JSON:API documents, pagination links, error responses, and many more."
  - title: "Persistence/Data Source Agnostic"
    excerpt: "No JPA or Hibernate required. Works with SQL, NoSQL, REST clients, in-memory stores, or any data source you bring."
  - title: "Auto-Generated OpenAPI"
    excerpt: "Always-in-sync API documentation derived from the same metadata used at runtime. Powerful customization capabilities. Exposed as JSON or YAML via a dedicated endpoint."
---

{% include feature_row id="feature_row_main" %}

## Built for Scale

Whether you're standardizing your organization's API layer across dozens of services or building a new microservice from scratch, **JsonApi4j** provides a consistent foundation that eliminates API design debates and reduces boilerplate.

![Request Processing Pipeline](/assets/images/request-processing-pipeline.png "JsonApi4j Request Processing Pipeline"){: .align-center}

{% include feature_row id="feature_row_advanced" %}

## Developer Experience

{% include feature_row id="feature_row_dx" %}

## Quick Example

Define a resource and an operation — that's all it takes to expose a fully compliant JSON:API endpoint:

```java
@JsonApiResource(resourceType = "users")
public class UserResource implements Resource<UserDbEntity> {

    @Override
    public String resolveResourceId(UserDbEntity dto) {
        return dto.getId();
    }

    @Override
    public UserAttributes resolveAttributes(UserDbEntity dto) {
        return new UserAttributes(dto.getFullName(), dto.getEmail());
    }
}
```

```java
@JsonApiResourceOperation(resource = UserResource.class)
public class UserOperations implements ResourceOperations<UserDbEntity> {

    @Override
    public CursorPageableResponse<UserDbEntity> readPage(JsonApiRequest request) {
        return CursorPageableResponse.fromItemsAndCursor(
                userDb.readAll(request.getCursor()),
                userDb.nextCursor()
        );
    }
}
```

This produces paginated, JSON:API-compliant responses at `GET /users` — with links, resource identifiers, and error handling included automatically.

[Read the full Getting Started guide](/getting-started/){: .btn .btn--primary .btn--large}
{: .text-center}

## Works With

<div class="works-with-grid" markdown="0">
  <div class="works-with-item">
    <strong>Spring Boot</strong>
    <span>Auto-configuration</span>
  </div>
  <div class="works-with-item">
    <strong>Quarkus</strong>
    <span>Quarkus Extension</span>
  </div>
  <div class="works-with-item">
    <strong>Jakarta Servlet API</strong>
    <span>Direct integration</span>
  </div>
  <div class="works-with-item">
    <strong>Maven Central</strong>
    <span>Published artifacts</span>
  </div>
</div>

[Browse Sample Apps](https://github.com/MoonWorm/jsonapi4j/tree/main/examples){: .btn .btn--info}
[Read the Documentation](/introduction/){: .btn .btn--info}
[View on GitHub](https://github.com/MoonWorm/jsonapi4j){: .btn .btn--info}
{: .text-center}
