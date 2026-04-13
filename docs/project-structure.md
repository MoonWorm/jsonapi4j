---
title: "Project Structure"
permalink: /project-structure/
---

**JsonApi4j** is designed to be **modular and embeddable**, allowing you to use only the parts you need depending on your application context.
Each module is published as a separate artifact in Maven Central.

- 🌀 [jsonapi4j-core](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-core) — a lightweight JSON:API request processor, ideal for embedding into non-web services (e.g., CLI tools) that need to handle JSON:API input/output without bringing in HTTP-related dependencies.
- 🔌 [jsonapi4j-rest](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-rest) — the Servlet API–based HTTP layer for integration with any Java web framework. Can be used directly in plain Servlet applications or as a foundation for building native integrations for frameworks like Spring Boot, Quarkus, etc.
- 🌱 [jsonapi4j-rest-springboot](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-rest-springboot) — [Spring Boot](https://spring.io/projects/spring-boot) auto-configuration module that integrates **JsonApi4j** seamlessly into a Spring environment.
- 🚀 [jsonapi4j-rest-quarkus](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-rest-quarkus-parent/runtime) — [Quarkus](https://quarkus.io/) auto-configuration Quarkus Extension that integrates **JsonApi4j** seamlessly into a Quarkus app.
- 🌐 [jsonapi4j-compound-docs-resolver](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-compound-docs-resolver) — a standalone **compound documents resolver** that automatically fetches and populates the `included` section of JSON:API responses. Perfect for API Gateway-level use or microservice response composition layers.

Here's how transitive dependencies between modules are structured in the framework:

```text
├──jsonapi4j-core
│
├── jsonapi4j-compound-docs-resolver
│
└── jsonapi4j-rest
    ├── depends on → jsonapi4j-core
    └── depends on → jsonapi4j-compound-docs-resolver
        │
        ├── jsonapi4j-rest-springboot
        │    └── depends on → jsonapi4j-rest
        └── jsonapi4j-rest-quarkus
             └── depends on → jsonapi4j-rest
```

There are other modules, for example `jsonapi4j-base`, but apps never need them to use an explicit dependency.

In short:
* if you're integrating **JsonApi4j** with a Spring Boot application, you only need to include a single dependency: `jsonapi4j-rest-springboot`
* if you're integrating **JsonApi4j** with a Quarkus application - just use `jsonapi4j-rest-quarkus`
* if you want to build a **JsonApi4j** integration with some other Java Web Frameworks or build an App on top of Servlet API - just use `jsonapi4j-rest`
* if you want to use **JsonApi4j** for an app that is not relying on Servlet API - for example, Desktop app - just use `jsonapi4j-core`
* if you only want to use **JsonApi4j** Compound Docs Resolver module for your App or API Gateway - use `jsonapi4j-compound-docs-resolver`
