---
title: "Self-Documenting REST APIs in Java: Runtime Introspection with JSON:API"
date: 2026-07-10
permalink: /self-documenting-rest-api-java-runtime-introspection/
categories:
  - tutorials
tags:
  - java
  - rest-api
  - json-api
  - jsonapi4j
  - api-documentation
excerpt: "Learn how to build self-documenting REST APIs in Java. A runtime introspection endpoint exposes a live API's resources, operations, plugins, and effective configuration as machine-readable JSON — always in sync with the running service."
---

Most REST APIs describe themselves in two places that drift apart over time: a hand-written spec and the actual
running code. A **self-documenting API** closes that gap by exposing its own structure at runtime — so anything
that queries the service gets an accurate, always-current answer to "what does this API expose, and how is it
configured right now?"

In this guide you will see how **runtime introspection** works, why a machine-readable metadata endpoint beats a
static document, and how [JsonApi4j](https://api4.pro/) ships one built in. Examples use a small product-catalog
domain (`products`, `orders`) rather than the usual `users` example.

## What "self-documenting" actually means

A self-documenting API answers questions about *itself* over HTTP:

- Which resource types and relationships are registered?
- Which operations exist, and at which routes/HTTP methods?
- Which plugins or middleware are active in this environment?
- What is the **effective configuration** — including defaults nobody set explicitly?

The key word is **effective**. A YAML file tells you what someone *wrote*; a runtime endpoint tells you what the
service is *actually running with* after defaults, profiles, and environment overrides are applied. That
distinction is what makes introspection valuable for debugging and governance.

## Why not just a static OpenAPI file?

OpenAPI is great for describing request/response shapes, and you should still generate it (JsonApi4j does, via
its [OpenAPI plugin](https://api4.pro/openapi-plugin/)). But a static spec has blind spots that a live
introspection endpoint fills:

| Question | Static spec | Runtime introspection |
|---|---|---|
| What routes exist? | Yes | Yes |
| Is this feature/plugin enabled **in prod**? | No | Yes |
| What is the effective config right now? | No | Yes |
| Which framework/runtime version is deployed? | No | Yes |
| Always in sync with the deployed build? | Only if regenerated | Always |

The two are complementary: OpenAPI for consumers integrating against your contract, runtime introspection for
operators, platform tooling, and anyone debugging a live service.

## Enabling runtime introspection in JsonApi4j

JsonApi4j exposes a built-in **Meta API**. It is **disabled by default** — it reveals internal structure, so it
is opt-in. Turn it on with a single property:

```yaml
jsonapi4j:
  meta:
    enabled: true
```

That is the whole setup. The metadata resources are served under your existing root path (e.g. `/jsonapi`),
using the same JSON:API format as the rest of your API. See the [Meta API reference](https://api4.pro/meta-api/)
for the full endpoint list.

## Walking a live API over HTTP

### A snapshot of the running service

```bash
curl -H 'Accept: application/vnd.api+json' \
  http://localhost:8080/jsonapi/state/this
```

```json
{
  "data": {
    "type": "state",
    "id": "this",
    "attributes": {
      "frameworkVersion": "1.8.6",
      "javaVersion": "23.0.2",
      "integration": "SPRING",
      "pluginsCount": 2,
      "resourcesCount": 2,
      "relationshipsCount": 1,
      "operationsCount": 5
    }
  }
}
```

In one call you learn the deployed framework and Java versions, which stack the app runs on
(`SPRING`/`QUARKUS`/`SERVLET`), and how many resources, relationships, and operations are registered.

### Listing operations and their routes

```bash
curl -H 'Accept: application/vnd.api+json' \
  http://localhost:8080/jsonapi/operations
```

```json
{
  "data": [
    {
      "type": "operations",
      "id": "products.READ_RESOURCE_BY_ID",
      "attributes": {
        "operationType": "READ_RESOURCE_BY_ID",
        "httpMethod": "GET",
        "pathTemplate": "/jsonapi/products/{id}"
      }
    },
    {
      "type": "operations",
      "id": "orders.READ_MULTIPLE_RESOURCES",
      "attributes": {
        "operationType": "READ_MULTIPLE_RESOURCES",
        "httpMethod": "GET",
        "pathTemplate": "/jsonapi/orders"
      }
    }
  ]
}
```

This is a machine-readable route map generated from the live registry — no annotations to scrape, no build step.

### Checking which plugins are active

```bash
curl -H 'Accept: application/vnd.api+json' \
  http://localhost:8080/jsonapi/plugins
```

Each entry reports the plugin's `name`, whether it is `enabled`, and its `precedence` — so "is compound-document
resolution actually on in staging?" becomes a one-line query instead of a config archaeology session.

## The most useful part: effective configuration

```bash
curl -H 'Accept: application/vnd.api+json' \
  http://localhost:8080/jsonapi/config/this
```

```json
{
  "data": {
    "type": "config",
    "id": "this",
    "attributes": {
      "settings": {
        "rootPath": "/jsonapi",
        "meta": { "enabled": true },
        "cd": {
          "enabled": true,
          "maxHops": 3,
          "propagation": ["FIELDS", "CUSTOM_QUERY_PARAMS", "HEADERS"]
        }
      }
    }
  }
}
```

Two properties make this trustworthy. First, it shows **effective defaults** — values the framework is actually
using, even ones you never set. Second, the output is **structurally faithful and host-consistent**: a list like
`propagation` renders as a JSON array whether you wrote it comma-separated
(`jsonapi4j.cd.propagation=FIELDS,CUSTOM_QUERY_PARAMS,HEADERS`) or with indexed keys, on Spring Boot, Quarkus, or
plain Servlet. What you read back is exactly what the service runs with.

## One-request view with compound documents

Because the Meta API is just JSON:API, you can pull everything into a single response with `include`:

```bash
curl -H 'Accept: application/vnd.api+json' \
  'http://localhost:8080/jsonapi/state/this?include=plugins,resources,operations,config'
```

The full descriptors land in the top-level `included` array. This one-shot form uses the
[Compound Documents plugin](https://api4.pro/compound-docs/), the same include machinery the rest of your API
uses.

## Keep it locked down

Runtime introspection describes your internals, so treat it like any management surface:

- Leave it **disabled by default**; enable it deliberately per environment.
- In production, gate it behind authentication or a network policy, or expose it on an internal port only.
- It does not emit secrets, but the structural detail is still best kept away from untrusted callers.

## Conclusion

A self-documenting API turns "read the (possibly stale) docs" into "ask the running service." In this guide you
saw:

- Why runtime introspection complements a static OpenAPI spec
- How to expose a live API's resources, operations, and plugins over HTTP
- How an effective-configuration endpoint removes guesswork when debugging environments

If you are building JSON:API services in Java, the [Getting Started guide](https://api4.pro/getting-started/)
walks through defining resources and operations, and the [Meta API reference](https://api4.pro/meta-api/) covers
every introspection endpoint in detail.

---

## FAQ

### What is a self-documenting REST API?

A self-documenting REST API exposes its own structure and configuration at runtime over HTTP, so clients and
tools can query what resources, operations, and settings the running service currently has instead of relying on
a separate, potentially outdated document.

### How is runtime introspection different from OpenAPI?

OpenAPI is a static description of request/response shapes, typically generated at build time. Runtime
introspection reflects the actual deployed service — which features are enabled, the effective configuration, and
the runtime versions — and is always in sync with the running build. The two are complementary.

### Does exposing API metadata at runtime leak secrets?

A well-designed introspection endpoint serializes only structural, non-secret configuration — credentials are not
part of it. Still, because it reveals internal structure, keep it opt-in and protect it behind authentication or
network policy in production. In JsonApi4j the [Meta API](https://api4.pro/meta-api/) is disabled by default.

### How do I enable the Meta API in JsonApi4j?

Set `jsonapi4j.meta.enabled=true`. The introspection resources are then served under your configured root path
using standard JSON:API, on Spring Boot, Quarkus, or plain Servlet.

### Can I fetch the whole API description in one request?

Yes. Because the Meta API is JSON:API, `GET /jsonapi/state/this?include=plugins,resources,operations,config`
returns the full picture in a single compound document via the
[Compound Documents plugin](https://api4.pro/compound-docs/).

<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "FAQPage",
  "mainEntity": [
    {
      "@type": "Question",
      "name": "What is a self-documenting REST API?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "A self-documenting REST API exposes its own structure and configuration at runtime over HTTP, so clients and tools can query what resources, operations, and settings the running service currently has instead of relying on a separate, potentially outdated document."
      }
    },
    {
      "@type": "Question",
      "name": "How is runtime introspection different from OpenAPI?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "OpenAPI is a static description of request/response shapes, typically generated at build time. Runtime introspection reflects the actual deployed service — which features are enabled, the effective configuration, and the runtime versions — and is always in sync with the running build. The two are complementary."
      }
    },
    {
      "@type": "Question",
      "name": "Does exposing API metadata at runtime leak secrets?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "A well-designed introspection endpoint serializes only structural, non-secret configuration — credentials are not part of it. Still, because it reveals internal structure, keep it opt-in and protect it behind authentication or network policy in production. In JsonApi4j the Meta API is disabled by default."
      }
    },
    {
      "@type": "Question",
      "name": "How do I enable the Meta API in JsonApi4j?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Set jsonapi4j.meta.enabled=true. The introspection resources are then served under your configured root path using standard JSON:API, on Spring Boot, Quarkus, or plain Servlet."
      }
    },
    {
      "@type": "Question",
      "name": "Can I fetch the whole API description in one request?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Yes. Because the Meta API is JSON:API, GET /jsonapi/state/this?include=plugins,resources,operations,config returns the full picture in a single compound document via the Compound Documents plugin."
      }
    }
  ]
}
</script>
