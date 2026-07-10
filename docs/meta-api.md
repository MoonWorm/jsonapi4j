---
title: "Meta API"
permalink: /meta-api/
---

### Overview

The **Meta API** is a built-in, opt-in **runtime introspection** endpoint. When enabled, JsonApi4j exposes a
machine-readable, JSON:API-formatted view of the application it is currently running: the registered resources,
relationships, and operations, the active plugins, and the effective configuration — all served under your
existing `rootPath`.

Because the description is generated from the same metadata the framework uses at request time, it is always in
sync with the running service. That makes it useful for **self-documentation**, **API governance** across many
services, tooling/service catalogs, and **debugging** ("is this plugin actually enabled? what's the effective
`cd` config in this environment?").

The Meta API is **disabled by default** — it exposes your application's internal structure, so it is opt-in.

### Enabling the Meta API

Set `jsonapi4j.meta.enabled` to `true`. Like every other property, the format follows your stack (see
[Configuration](/configuration/)):

<div class="tabs" markdown="0">
  <div class="tab-buttons">
    <button class="tab-btn active" data-tab="meta-springboot">Spring Boot</button>
    <button class="tab-btn" data-tab="meta-quarkus">Quarkus</button>
    <button class="tab-btn" data-tab="meta-servlet">Servlet API</button>
  </div>

  <div id="meta-springboot" class="tab-panel active">
    <div class="language-yaml highlighter-rouge"><div class="highlight"><pre class="highlight"><code><span class="na">jsonapi4j</span><span class="pi">:</span>
  <span class="na">meta</span><span class="pi">:</span>
    <span class="na">enabled</span><span class="pi">:</span> <span class="no">true</span></code></pre></div></div>
  </div>

  <div id="meta-quarkus" class="tab-panel">
    <div class="language-properties highlighter-rouge"><div class="highlight"><pre class="highlight"><code><span class="py">jsonapi4j.meta.enabled</span>=<span class="s">true</span></code></pre></div></div>
  </div>

  <div id="meta-servlet" class="tab-panel">
    <div class="language-yaml highlighter-rouge"><div class="highlight"><pre class="highlight"><code><span class="na">meta</span><span class="pi">:</span>
  <span class="na">enabled</span><span class="pi">:</span> <span class="no">true</span></code></pre></div></div>
  </div>
</div>

When enabled, the framework prints a **Live introspection** URL in the startup banner so you can find the entry
point immediately:

```
Live introspection: /jsonapi/state/this?include=plugins,resources,relationships,operations,config
```

### Endpoints

All Meta API resources are served under your configured `rootPath` (the examples below assume the default
`/jsonapi`). The domain is reserved: its resource types (`state`, `config`, `plugins`, `resources`,
`relationships`, `operations`) cannot collide with your own, and are excluded from the regular `/resources`
listing.

| Method & path | Returns |
|---|---|
| `GET /state/this` | The singleton `state` resource — a snapshot of the running application. |
| `GET /state` | The same `state` resource as a single-element collection. |
| `GET /config/this` | The effective, non-secret configuration (`config` resource). |
| `GET /plugins` | All registered plugins with their enabled/precedence status. |
| `GET /resources` | Descriptors for every registered resource type. |
| `GET /resources/{type}` | The descriptor for one resource type (e.g. `/resources/orders`). |
| `GET /relationships` | Descriptors for every registered relationship. |
| `GET /operations` | Descriptors for every registered operation (filterable by id). |

The reserved singleton id is `this`; requesting any other id (e.g. `GET /state/bogus`) returns a `404`.

### The `state` resource

`GET /jsonapi/state/this` returns a snapshot of the running application and links to the four descriptor
collections plus the config resource.

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
      "pluginsCount": 4,
      "resourcesCount": 3,
      "relationshipsCount": 4,
      "operationsCount": 8
    },
    "relationships": {
      "plugins": { "links": { "self": "/state/this/relationships/plugins" } },
      "resources": { "links": { "self": "/state/this/relationships/resources" } },
      "relationships": { "links": { "self": "/state/this/relationships/relationships" } },
      "operations": { "links": { "self": "/state/this/relationships/operations" } },
      "config": { "links": { "self": "/state/this/relationships/config" } }
    }
  }
}
```

`integration` is one of `SPRING`, `QUARKUS`, or `SERVLET`.

#### One-request snapshot with `?include`

Like any JSON:API resource, `state` supports `include` to pull the linked resources into a single response:

```bash
curl -H 'Accept: application/vnd.api+json' \
  'http://localhost:8080/jsonapi/state/this?include=plugins,resources,relationships,operations,config'
```

The full descriptors land in the top-level `included` array. This one-request form is resolved through the
[Compound Documents plugin](/compound-docs-plugin/) (`jsonapi4j.cd.enabled=true`) — the same mechanism as any
other `include`. Without it, query the per-collection endpoints (`/plugins`, `/resources`, …) directly.

### Descriptor collections

Each collection is a normal JSON:API resource you can `GET` or filter.

**Plugins** — `GET /jsonapi/plugins`

```json
{
  "data": [
    {
      "type": "plugins",
      "id": "JsonApiCompoundDocsPlugin",
      "attributes": {
        "name": "JsonApiCompoundDocsPlugin",
        "enabled": true,
        "precedence": 100,
        "className": "pro.api4.jsonapi4j.plugin.cd.JsonApiCompoundDocsPlugin"
      }
    }
  ]
}
```

**Resources** — `GET /jsonapi/resources` (or `/resources/{type}`): each descriptor carries `type` and
`className`.

**Relationships** — `GET /jsonapi/relationships`: each carries `name`, `parentResourceType`,
`relationshipType`, and `className`.

**Operations** — `GET /jsonapi/operations`: each carries `operationType`, `httpMethod`, `pathTemplate`,
`resourceType`, `relationshipName`, and `className`. Operation ids combine the resource type and operation, so
you can filter to a single one:

```bash
curl -H 'Accept: application/vnd.api+json' \
  'http://localhost:8080/jsonapi/operations?filter[id]=orders.READ_RESOURCE_BY_ID'
```

```json
{
  "data": [
    {
      "type": "operations",
      "id": "orders.READ_RESOURCE_BY_ID",
      "attributes": {
        "operationType": "READ_RESOURCE_BY_ID",
        "httpMethod": "GET",
        "pathTemplate": "/jsonapi/orders/{id}"
      }
    }
  ]
}
```

### The `config` resource

`GET /jsonapi/config/this` returns the **effective, non-secret** configuration under `settings`. The tree is
composed from the framework's already-bound, strictly-typed config objects — the root properties plus each
enabled plugin's own section (`cd`, `oas`, `ac`, `sf`). Two consequences worth knowing:

- **Effective defaults are always shown.** Every value reflects what the framework is actually using, including
  defaults you never set explicitly.
- **The output is host-consistent and structurally faithful.** List properties render as JSON arrays regardless
  of how you wrote them in configuration — `jsonapi4j.cd.propagation=FIELDS,CUSTOM_QUERY_PARAMS,HEADERS` and the
  indexed `jsonapi4j.cd.propagation[0]=FIELDS` form produce the identical array below, on every stack.

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
        "validation": {
          "maxNumberFilterParams": 5,
          "maxElementsInFilterParam": 20,
          "resourceIdMaxLength": 64,
          "limitMaxValue": 100,
          "maxElementsInIncludeParam": 10,
          "maxElementsInSortByParam": 5
        },
        "meta": { "enabled": true },
        "cd": {
          "enabled": true,
          "maxHops": 3,
          "propagation": ["FIELDS", "CUSTOM_QUERY_PARAMS", "HEADERS"],
          "deduplicateResources": true,
          "defaultMaxBatchSize": 20
        }
      }
    }
  }
}
```

Only configuration backed by a typed model appears here. A plugin that wants to surface its settings exposes them
through the framework's plugin-config contract, so third-party plugins can contribute their own section too.

### Security considerations

The Meta API describes your application's internal structure — resource types, operation routes, active plugins,
and effective (non-secret) configuration. Keep it **disabled by default** and enable it deliberately:

- In production, gate it behind authentication/network policy, or expose it only on an internal management port.
- It never emits secrets (credentials are not part of the typed config it serializes), but the structural
  detail is still best kept away from untrusted callers.

### Meta domain at a glance

<div class="mermaid">
graph LR
  S["state / this"] --> P["plugins"]
  S --> R["resources"]
  S --> RL["relationships"]
  S --> O["operations"]
  S --> C["config / this"]
</div>
