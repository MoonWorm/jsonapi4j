---
title: "Project Structure"
permalink: /project-structure/
---

**JsonApi4j** is designed to be **modular and embeddable**, allowing you to use only the parts you need depending on your application context.
Each module is published as a separate artifact in Maven Central.

### Core Modules

- [jsonapi4j-base](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-base) — foundation module: domain interfaces (`Resource`, `Relationship`), annotations, JSON:API model classes, plugin SPI, and exception hierarchy. Minimal dependencies — no HTTP layer.
- [jsonapi4j-core](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-core) — processing engine: `JsonApi4j` entry point, `DomainRegistry`, `OperationsRegistry`, resource/relationship processors. Ideal for embedding into non-web services (e.g., CLI tools) that need JSON:API processing without HTTP dependencies.
- [jsonapi4j-compound-docs-resolver](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-compound-docs-resolver) — standalone compound documents resolver with multi-hop `include` traversal, parallel batch fetching, built-in caching, and Cache-Control aggregation. Can run within the application or at the API Gateway level.
- [jsonapi4j-rest](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-rest) — Servlet API integration: `JsonApi4jDispatcherServlet`, request parsing, error handling, response customization. Can be used directly in plain Servlet applications or as a foundation for framework-specific integrations.
- [jsonapi4j-rest-springboot](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-rest-springboot) — [Spring Boot](https://spring.io/projects/spring-boot) auto-configuration module. Automatically registers servlets, filters, domain scanning, and plugin integration.
- [jsonapi4j-rest-quarkus](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-rest-quarkus-parent/runtime) — [Quarkus](https://quarkus.io/) extension following the standard two-module pattern (`runtime/` for CDI beans + `deployment/` for build-time registration).

### Plugin Modules

- [jsonapi4j-ac-plugin](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-plugins/jsonapi4j-ac-plugin) — **Access Control** — fine-grained, annotation-driven authorization with per-field anonymization based on authentication, access tier, OAuth2 scopes, and resource ownership.
- [jsonapi4j-cd-plugin](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-plugins/jsonapi4j-cd-plugin) — **Compound Documents** — integrates the compound docs resolver into the plugin pipeline, enabling `include` query parameter support.
- [jsonapi4j-sf-plugin](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-plugins/jsonapi4j-sf-plugin) — **Sparse Fieldsets** — supports `fields[TYPE]=field1,field2` to return only requested attributes per resource type.
- [jsonapi4j-oas-plugin](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-plugins/jsonapi4j-oas-plugin) — **OpenAPI Specification** — automatically generates an OpenAPI spec from registered resources, relationships, and operations.
- [jsonapi4j-all-plugins](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-plugins/jsonapi4j-all-plugins) — convenience aggregator that includes all plugins as a single dependency.

### Module Dependency Graph

```text
jsonapi4j-base
    │
jsonapi4j-core
    │
jsonapi4j-compound-docs-resolver
    │
jsonapi4j-rest
    └── depends on → jsonapi4j-core
        │
        ├── jsonapi4j-rest-springboot
        │    └── depends on → jsonapi4j-rest
        └── jsonapi4j-rest-quarkus
             └── depends on → jsonapi4j-rest

plugins (optional, depend on jsonapi4j-base or jsonapi4j-rest)
    ├── jsonapi4j-ac-plugin
    ├── jsonapi4j-cd-plugin
    ├── jsonapi4j-sf-plugin
    ├── jsonapi4j-oas-plugin
    └── jsonapi4j-all-plugins (aggregator)
```

### Which dependency do I need?

* **Spring Boot** application → `jsonapi4j-rest-springboot`
* **Quarkus** application → `jsonapi4j-rest-quarkus`
* **Other Java Web Frameworks** or plain Servlet API → `jsonapi4j-rest`
* **Non-web** application (e.g., CLI, desktop) → `jsonapi4j-core`
* **Compound Docs only** (e.g., API Gateway) → `jsonapi4j-compound-docs-resolver`
* **All plugins** → add `jsonapi4j-all-plugins` alongside your framework dependency
