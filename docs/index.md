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
    - label: "Maven Central"
      url: "https://central.sonatype.com/namespace/pro.api4"
excerpt: "A modern, lightweight Java framework for building production-ready REST APIs compliant with the JSON:API specification. Define your domain — the framework handles the rest."

feature_row_main:
  - title: "JSON:API Compliant"
    excerpt: "Purpose-built around the [JSON:API specification](https://jsonapi.org) — predictable request/response format and processing rules."
  - title: "Auto-Generated OpenAPI"
    excerpt: "Always-in-sync API documentation derived from the same metadata used at runtime. Powerful customization capabilities. Exposed as JSON or YAML via a dedicated endpoint."  
  - title: "Framework Agnostic"
    excerpt: "Works with **Spring Boot**, **Quarkus**, and plain **Jakarta Servlet API**. Add one dependency and the framework auto-configures itself in your environment."
  
feature_row_advanced:
  - title: "Pluggable Architecture"
    excerpt: "Hook into the request pipeline with the visitor-based Plugin System. Ships with **Access Control**, **Sparse Fieldsets**, **OpenAPI**, and **Compound Documents** plugins."
  - title: "Compound Documents"
    excerpt: "Multi-level `include` queries with parallel batch resolution, built-in caching, and Cache-Control aggregation. Deployable at the application or API Gateway level."
  - title: "Fine-Grained Access Control"
    excerpt: "Declarative, annotation-driven authorization — per-field anonymization based on access tier, OAuth2 scopes, and resource ownership. No changes to core logic."

feature_row_dx:
  - title: "Minimal Boilerplate"
    excerpt: "Define resources, relationships, and operations — the framework automatically generates JSON:API documents, pagination links, error responses, and many more."
  - title: "Persistence/Data Source Agnostic"
    excerpt: "No JPA or Hibernate required. Works with SQL, NoSQL, REST clients, in-memory stores, or any data source you bring."
  - title: "Parallel Execution"
    excerpt: "Relationship resolution, compound document fetching, and batch operations run concurrently. Supports virtual threads (Project Loom) for maximum throughput."  
---

## Choose Your Stack

<div class="stack-grid" markdown="0">
  <a href="/getting-started/#spring-boot" class="stack-card stack-card--spring">
    <img src="/assets/images/spring-icon.svg" alt="Spring" class="stack-card__icon">
    <strong>Spring Boot</strong>
    <span>Auto-configuration, zero boilerplate. Add one dependency and go.</span>
  </a>
  <a href="/getting-started/#quarkus" class="stack-card stack-card--quarkus">
    <img src="/assets/images/quarkus-icon.svg" alt="Quarkus" class="stack-card__icon">
    <strong>Quarkus</strong>
    <span>Native Quarkus extension with CDI integration and build-time processing.</span>
  </a>
  <a href="/getting-started/#jakarta-servlet" class="stack-card stack-card--jakarta">
    <img src="/assets/images/jakarta-icon.svg" alt="Jakarta EE" class="stack-card__icon">
    <strong>Jakarta Servlet API</strong>
    <span>Direct integration — no framework required, just the Servlet API.</span>
  </a>
</div>

<div class="dark-band" markdown="0">
  <h2>Why JsonApi4j</h2>
  <p class="dark-band__subtitle">One standard. Every service. No more API design debates — just consistent, spec-compliant endpoints out of the box.</p>
</div>

{% include feature_row id="feature_row_main" %}

<div class="dark-band" markdown="0">
  <h2>Under the Hood</h2>
  <p class="dark-band__subtitle">Built for engineers who care about what happens between the request and the response.</p>
</div>

{% include feature_row id="feature_row_advanced" %}

{% include feature_row id="feature_row_dx" %}

<div class="pipeline-band" markdown="0">
  <h3>Request Processing Pipeline</h3>
  <div style="display: flex; align-items: center; gap: 2rem; flex-wrap: wrap;">
    <div style="flex: 1; min-width: 280px;">
      <p>Every incoming API request flows through a well-defined <strong>processing pipeline</strong> — from data retrieval through relationship resolution to final document composition.</p>
      <p>At each stage, registered <a href="/plugins/">plugins</a> can inspect, mutate, or short-circuit the request using the Visitor pattern. This gives you full control over cross-cutting concerns like access control, sparse fieldsets, and other features — without touching core logic.</p>
    </div>
    <div style="flex: 0 0 auto;">
      <img src="/assets/images/request-processing-pipeline.png" alt="Request Processing Pipeline" style="max-height: 620px; width: auto;">
    </div>
  </div>
</div>

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
        return PaginationAwareResponse.limitOffsetAware(
                userDb.readAll(request.getLimit(), request.getOffset()),
                userDb.total()
        );
    }
}
```

`GET /users` →

```json
{
  "data": [
    {
      "type": "users",
      "id": "1",
      "attributes": {
        "fullName": "John Doe",
        "email": "john@example.com"
      },
      "links": {
        "self": "/users/1"
      }
    }
  ],
  "links": {
    "self": "/users?page[offset]=0&page[limit]=20",
    "next": "/users?page[offset]=20&page[limit]=20"
  }
}
```

<div class="site-cta-footer" markdown="0">
  <div class="site-cta-footer__cta">
    <h2>Start Building Today</h2>
    <p>Add one dependency. Define your domain. Ship a spec-compliant API.</p>
    <div class="site-cta-footer__buttons">
      <a href="/getting-started/" class="cta-btn cta-btn--ghost">Get Started</a>
      <a href="/introduction/" class="cta-btn cta-btn--ghost">Documentation</a>
      <a href="https://github.com/MoonWorm/jsonapi4j/tree/main/examples" class="cta-btn cta-btn--ghost">Sample Apps</a>
      <a href="https://github.com/MoonWorm/jsonapi4j" class="cta-btn cta-btn--ghost">GitHub</a>
      <a href="https://central.sonatype.com/namespace/pro.api4" class="cta-btn cta-btn--ghost">Maven Central</a>
    </div>
  </div>
  <div class="site-cta-footer__bottom">
    <span>JsonApi4j</span>
    <span class="site-cta-footer__sep">&middot;</span>
    <span>JSON:API framework for Java</span>
    <span class="site-cta-footer__sep">&middot;</span>
    <a href="https://github.com/MoonWorm/jsonapi4j">GitHub</a>
  </div>
</div>
