---
title: "JsonApi4j"
layout: splash
permalink: /
header:
  overlay_color: "#0e0f20"
  actions:
    - label: "Get Started"
      url: /getting-started/
    - label: "Browse Docs"
      url: /documentation/
    - label: "View on GitHub"
      url: "https://github.com/MoonWorm/jsonapi4j"
    - label: "Maven Central"
      url: "https://central.sonatype.com/namespace/pro.api4"
excerpt: "Define your domain — the framework handles the rest."

feature_row_main:
  - title: "JSON:API Compliant"
    excerpt: "Purpose-built around the [JSON:API specification](https://jsonapi.org) — predictable request/response format and processing rules."
  - title: "Auto-Generated OpenAPI"
    excerpt: "Always-in-sync API documentation derived from the same metadata used at runtime via the [OpenAPI plugin](/openapi-plugin/). Powerful customization capabilities. Exposed as JSON or YAML via a dedicated endpoint."  
  - title: "Framework Agnostic"
    excerpt: "Works with [**Spring Boot**](/getting-started/#spring-boot), [**Quarkus**](/getting-started/#quarkus), and plain [**Jakarta Servlet API**](/getting-started/#jakarta-servlet). Add one dependency and the framework auto-configures itself in your environment."
  
feature_row_advanced:
  - title: "Pluggable Architecture"
    excerpt: "Hook into the request pipeline with the visitor-based [Plugin System](/plugins/). Ships with [Access Control](/access-control-plugin/), [Sparse Fieldsets](/sparse-fieldsets-plugin/), [OpenAPI](/openapi-plugin/), and [Compound Documents](/compound-docs-plugin/) plugins."
  - title: "Compound Documents"
    excerpt: "Multi-level [`include`](/compound-docs/) queries with parallel batch resolution, built-in caching, and Cache-Control aggregation. Deployable at the application or API Gateway level via the [Compound Documents plugin](/compound-docs-plugin/)."
  - title: "Fine-Grained Access Control"
    excerpt: "Declarative, annotation-driven authorization via the [Access Control plugin](/access-control-plugin/) — per-field anonymization based on access tier, OAuth2 scopes, and resource ownership. No changes to core logic."
  - title: "Runtime Introspection"
    excerpt: "An opt-in [Meta API](/meta-api/) exposes your live API's resources, relationships, operations, plugins, and effective configuration as machine-readable JSON:API — always in sync with the running service."
  - title: "Built-in Validation"
    excerpt: "Structural [request validation](/validation/) out of the box — `filter`, `include`, `sort`, and pagination limits — plus a fluent Validation API for your own domain rules. No JSR-380 dependency."
  - title: "Pagination, Filtering & Sorting"
    excerpt: "Cursor or limit/offset [pagination](/pagination/) with automatic `first`/`prev`/`next`/`last` links, plus [filtering and sorting](/filtering-and-sorting/) through standard `filter[...]` and `sort` parameters — parsed and validated for you."

feature_row_dx:
  - title: "Minimal Boilerplate"
    excerpt: "Define [resources, relationships, and operations](/domain/) — the framework automatically generates JSON:API documents, [pagination links](/pagination/), [error responses](/error-handling/), and many more."
  - title: "Persistence/Data Source Agnostic"
    excerpt: "No JPA or Hibernate required. Your [operations](/operations/) work with SQL, NoSQL, REST clients, in-memory stores, or any data source you bring."
  - title: "Parallel Execution"
    excerpt: "Relationship resolution, compound document fetching, and batch operations run concurrently. Supports virtual threads (Project Loom) for [maximum throughput](/performance/)."  

feature_row_ai:
  - title: "Official Claude Code Plugin"
    excerpt: "Install the plugin and your AI agent instantly knows how to design resources, relationships, operations, compound documents, validation, and tests — right in your own project."
    url: /ai-assisted-development/
    btn_label: "Learn More"
    btn_class: "btn--primary"
  - title: "Works With Any AI Tool"
    excerpt: "A committed `AGENTS.md` gives Claude Code, Cursor, GitHub Copilot, and Codex the same project context — build commands, conventions, and module map."
  - title: "Knows the Idioms"
    excerpt: "Not generic Java scaffolding — agents follow framework patterns: lightweight relationship refs, N+1-safe includes, `cd.mapping`, and the right testing setup."
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

<div class="dark-band" markdown="0">
  <h2>Built to Pair With Your AI Agent</h2>
  <p class="dark-band__subtitle">An official Claude Code plugin and a cross-tool <code>AGENTS.md</code> — so your AI assistant builds spec-compliant APIs the framework's way. <a href="/ai-assisted-development/">Learn more →</a></p>
</div>

{% include feature_row id="feature_row_ai" %}

<div class="pipeline-band" markdown="0">
  <h3>Request Processing Pipeline</h3>
  <p>Every incoming API request flows through a well-defined <strong>processing pipeline</strong> — from data retrieval through relationship resolution to final document composition. At each stage, registered <a href="/plugins/">plugins</a> can inspect, mutate, or short-circuit the request using the Visitor pattern.</p>
  <img src="/assets/images/request-processing-pipeline.svg" alt="Request Processing Pipeline" style="width: 100%; margin-top: 1em;">
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
    public PaginationAwareResponse<UserDbEntity> readPage(JsonApiRequest request) {
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
  },
  "meta": {
    "pagination.totalItems": 26
  }
}
```
