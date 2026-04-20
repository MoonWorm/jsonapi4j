---
title: "Compound Documents Plugin"
permalink: /compound-docs-plugin/
---

### Overview

The [Compound Documents](https://jsonapi.org/format/#document-compound-documents) Plugin integrates the compound documents resolver into the JsonApi4j request processing pipeline, automatically enriching responses with the `included` section when the `include` query parameter is present.

There are two distinct components involved in compound document support, and it's important to understand the difference:

- **`jsonapi4j-cd-plugin`** — the **plugin** for JsonApi4j applications. It hooks into the plugin pipeline and handles compound document resolution as part of the normal request lifecycle. If you're building a JsonApi4j-based service, this is what you need.
- **`jsonapi4j-compound-docs-resolver`** — the standalone **resolver module**. It has no dependency on the JsonApi4j plugin system or Servlet API and can be embedded anywhere — for example, at an API Gateway level. See the [Compound Documents](/compound-docs/) section for details.

In short: the **plugin** is for JsonApi4j apps, the **resolver** is for anything else.

### Getting Started

Add the plugin dependency:

```xml
<dependency>
  <groupId>pro.api4</groupId>
  <artifactId>jsonapi4j-cd-plugin</artifactId>
  <version>${jsonapi4j.version}</version>
</dependency>
```

If you're using Spring Boot or Quarkus with `jsonapi4j-all-plugins` — the plugin is already on the classpath and will be auto-configured.

Enable it via configuration:

```yaml
jsonapi4j:
  cd:
    enabled: true
```

### Available Properties

| Property name                         | Default value                        | Description                                                                                                                                                                       |
|---------------------------------------|--------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `jsonapi4j.cd.enabled`                | `false`                              | Enables/disables Compound Documents post-processing.                                                                                                                              |
| `jsonapi4j.cd.maxHops`                | `2`                                  | Max include traversal depth for compound document resolution.                                                                                                                     |
| `jsonapi4j.cd.maxIncludedResources`   | `100`                                | Maximum amount of included resources. Doesn't guarantee the exact gap - can be more if fact. Checks before moving to down to the next depth level and adds all resolved resource. |
| `jsonapi4j.cd.errorStrategy`          | `IGNORE`                             | Error handling strategy in compound docs resolver. Available options: `IGNORE`, `FAIL`                                                                                            |
| `jsonapi4j.cd.propagation`            | `FIELDS,CUSTOM_QUERY_PARAMS,HEADERS` | List of request parts that must be propagated during Compound Docs resolution loop. Available options: `FIELDS`, `CUSTOM_QUERY_PARAMS`, `HEADERS`                                 |
| `jsonapi4j.cd.deduplicateResources`   | `true`                               | Defines if Compound Docs plugin should deduplicate resources in the 'included' section (by 'type' / 'id')                                                                         |
| `jsonapi4j.cd.httpConnectTimeoutMs`   | `5000`                               | Controls how long to wait when establishing TCP connection (in millisecond). Applied to each generated HTTP request.                                                              |
| `jsonapi4j.cd.httpTotalTimeoutMs`     | `10000`                              | Controls total request timeout (in millisecond). Applied to each generated HTTP request.                                                                                          |
| `jsonapi4j.cd.mapping.<resourceType>` | empty map                            | Per-resource-type base URL mapping used by compound docs resolver.                                                                                                                |

**Cache properties**

| Property name                | Default value | Description                                                                 |
|------------------------------|---------------|-----------------------------------------------------------------------------|
| `jsonapi4j.cd.cache.enabled` | `true`        | Enables/disables the built-in resource cache for compound docs resolution.  |
| `jsonapi4j.cd.cache.maxSize` | `1000`        | Soft maximum number of cached entries. Eviction uses LRU + TTL expiration.  |

### Further Reading

For deeper details on how compound document resolution works — including the multi-stage resolution process, caching internals, Cache-Control aggregation, and the standalone resolver for API Gateway deployments — see the [Compound Documents](/compound-docs/) section.
