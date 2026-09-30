---
title: "Compound Documents Plugin"
permalink: /compound-docs-plugin/
---

### Overview

The [Compound Documents](https://jsonapi.org/format/#document-compound-documents) Plugin integrates the compound documents resolver into the JsonApi4j [request processing pipeline](/request-processing-pipeline/), automatically enriching responses with the `included` section when the `include` query parameter is present.

There are two distinct components involved in compound document support, and it's important to understand the difference:

- **`jsonapi4j-cd-plugin`** — the **plugin** for JsonApi4j applications. It hooks into the plugin pipeline and handles compound document resolution as part of the normal request lifecycle. If you're building a JsonApi4j-based service, this is what you need.
- **`jsonapi4j-compound-docs-resolver`** — the standalone **resolver module**. It has no dependency on the JsonApi4j plugin system or Servlet API and can be embedded anywhere — for example, at an API Gateway level. See the [Compound Documents](/compound-docs/) section for details.

In short: the **plugin** is for JsonApi4j apps, the **resolver** is for anything else.

### Getting Started

To enable the plugin, add the following dependency:

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
| `jsonapi4j.cd.maxHops`                | `2`                                  | The deepest include path supported, in relationships: `a.b` is 2. A deeper one is handled per `unsupportedIncludes`. |
| `jsonapi4j.cd.unsupportedIncludes`    | `FAIL`                               | What a request with an include path deeper than `maxHops` gets. `FAIL`: `400 Bad Request` naming the path, as the JSON:API spec requires. `IGNORE`: the path is resolved only as deep as supported and listed in `meta.includedIncomplete` as `UNSUPPORTED_INCLUDE`. |
| `jsonapi4j.cd.maxIncludedResources`   | `100`                                | When `included` reaches this many resources, resolution fetches nothing further, and the include paths left unresolved are listed in `meta.includedIncomplete` as `MAX_INCLUDED_RESOURCES`. Checked between hops, so the last hop can take `included` past it. |
| `jsonapi4j.cd.errorStrategy`          | `IGNORE`                             | What a failed include does — see [Error handling](#error-handling). `IGNORE`: the failed resources are left out of `included`, listed in `meta.includedIncomplete`, and the response is marked `no-store`. `FAIL`: the request is answered with a JSON:API error document instead. |
| `jsonapi4j.cd.propagation`            | `FIELDS,CUSTOM_QUERY_PARAMS,HEADERS` | List of request parts that must be propagated during Compound Docs resolution loop. Available options: `FIELDS`, `CUSTOM_QUERY_PARAMS`, `HEADERS`. `Forwarded`, `X-Forwarded-For` and `X-Real-IP` are never propagated. |
| `jsonapi4j.cd.deduplication`         | `DATA_AND_INCLUDED`                  | How resource objects repeat (by `type` / `id`). `DATA_AND_INCLUDED`: each resource appears once across `data` and `included` — spec-compliant. `INCLUDED_ONLY`: once within `included`, and a primary resource an include path reaches is repeated there, so a client can resolve every related resource from `included` alone. `NONE`: no deduplication. The last two go beyond the spec's one-resource-object-per-`type`/`id` rule. |
| `jsonapi4j.cd.httpConnectTimeoutMs`   | `5000`                               | Controls how long to wait when establishing TCP connection (in millisecond). Applied to each generated HTTP request.                                                              |
| `jsonapi4j.cd.httpTotalTimeoutMs`     | `10000`                              | Controls total request timeout (in millisecond). Applied to each generated HTTP request.                                                                                          |
| `jsonapi4j.cd.mapping.<resourceType>` | empty map                            | Base URL for a resource type **served by a different service**. Same-app types (including the built-in meta types) need no entry — see [Resolving base URLs](#resolving-base-urls). |
| `jsonapi4j.cd.mapping.default`       | not set                              | Base URL for every same-app type without an entry of its own, used instead of loopback — see [Limitations](#limitations). `default` is therefore a reserved key: a resource type named `default` fails startup. |
| `jsonapi4j.cd.batchSizeMapping.<resourceType>` | empty map                   | Per-resource override for the max `filter[id]=...` batch size. Use when a downstream service enforces a stricter cap than the global default.                                     |
| `jsonapi4j.cd.defaultMaxBatchSize`    | `20`                                 | Fallback max number of resource IDs per downstream `filter[id]=...` request. Larger ID sets are split into parallel chunks of this size.                                          |

**Cache properties**

| Property name                | Default value | Description                                                                 |
|------------------------------|---------------|-----------------------------------------------------------------------------|
| `jsonapi4j.cd.cache.enabled` | `true`        | Enables/disables the built-in resource cache for compound docs resolution.  |
| `jsonapi4j.cd.cache.maxSize` | `1000`        | Soft maximum number of cached entries. Eviction uses LRU + TTL expiration.  |

### Resolving base URLs

To assemble the `included` array, the resolver fetches each included resource type over HTTP, so it needs a base URL
per type. That base URL is resolved as follows:

1. **An explicit `jsonapi4j.cd.mapping.<type>` entry wins.** Use this **only** for resource types served by a
   *different* service (a distributed / microservice setup), e.g. `jsonapi4j.cd.mapping.orders=https://orders.internal/jsonapi`.
2. **Otherwise the type is treated as same-app.** If `jsonapi4j.cd.mapping.default` is set, it is fetched from there.
3. **If not, the app calls itself on loopback** — `127.0.0.1`, or `[::1]` when
   the request arrived over IPv6, at the local port the request arrived on, `https` when that connection is TLS, plus
   the servlet context path and the configured `jsonapi4j.rootPath`. For example `http://127.0.0.1:8080/jsonapi`.

This means **same-app includes need no mapping and no base URL anywhere in config** — including the built-in meta
types (`state`, `plugins`, `resources`, `relationships`, `operations`, `config`), which always resolve automatically
once `jsonapi4j.cd.enabled=true`. Because the port comes from the live connection, it is always correct for the actual
port in use (random ports, test ports, a context path), with nothing to keep in sync. The call never leaves the
machine, and the loopback address always matches a family the server is already accepting connections on.

The base URL is deliberately **never taken from the `Host` or `X-Forwarded-*` headers**. The client controls those,
so trusting them would let any caller point the server's include requests — and the shared resource cache — at a
host of its choosing.

### Error handling

Resolving includes means fetching other resources, and those fetches can fail: a non-`2xx` response, a timeout, a
connection error, a response that isn't a JSON:API document, or a resource type with no route to fetch it from.
`jsonapi4j.cd.errorStrategy` decides what happens then.

**`IGNORE` (default)** — a failed include never fails the request. The resources that couldn't be fetched are left out
of `included`, everything that did resolve is returned with the primary data's status, and the failure is logged
(`WARN`; `ERROR` for a type with no route, which is a configuration mistake rather than a transient failure). Failures
are isolated per `filter[id]` batch, so one failed batch doesn't drop its siblings. A response with an incomplete
`included` carries `Cache-Control: no-store`, so no shared cache keeps a partial document for the primary resource's
full `max-age`.

The document also says what is missing — see [Incomplete `included`](#incomplete-included).

**`FAIL`** — the request is answered with a JSON:API error document instead of the primary data:

| Failure                                             | Status | Error `code`       |
|-----------------------------------------------------|--------|--------------------|
| a downstream call timed out                         | `504`  | `GATEWAY_TIMEOUT`  |
| any other downstream failure                        | `502`  | `BAD_GATEWAY`      |
| a resource type with no route to fetch it from      | `500`  | internal error     |

Error details never name downstream URLs. The documents are rendered through the same error handler registry as every
other error the API returns, so they share its format — and an application that maps these exceptions itself
(`DownstreamTimeoutException`, `ErrorJsonApiResponseException`, `DomainResolutionException`) keeps its own mapping.

### Limits

**`maxHops`** is the deepest include path supported. The depth of every path is known from the request alone, so a
deeper one is handled before the primary data is even fetched. Under `unsupportedIncludes: FAIL` (default) the request
is answered with `400 Bad Request`, as the JSON:API spec requires for include paths a server doesn't support:

```json
{ "errors": [ {
  "status": "400",
  "code": "UNSUPPORTED_INCLUDE",
  "detail": "Include path 'relatives.relatives.relatives' spans 3 relationships, more than the supported 2",
  "source": { "parameter": "include" }
} ] }
```

Under `IGNORE` the path is resolved as deep as supported — `relatives.relatives` here — and listed as
`UNSUPPORTED_INCLUDE`.

**`maxIncludedResources`** bounds the size of `included`, which depends on the data, not the request. Once `included`
has reached it, nothing further is fetched and the include paths left unresolved are listed as
`MAX_INCLUDED_RESOURCES`. A hop that has started always finishes, so the last one can take `included` past the limit —
nothing requested is dropped for it, and no gap is reported when nothing was left to fetch.

### Incomplete `included`

Whenever `included` misses something the request asked for, the document says what and why. Its top-level `meta` lists
each gap — merged into the document's own `meta`, and present only when `included` is incomplete:

```json
"meta": {
  "includedIncomplete": [
    { "reason": "FETCH_FAILED", "type": "currencies" },
    { "reason": "NO_ROUTE", "type": "regions" },
    { "reason": "MAX_INCLUDED_RESOURCES", "path": "relatives.relatives" },
    { "reason": "UNSUPPORTED_INCLUDE", "path": "relatives.relatives.relatives" }
  ]
}
```

| Reason                   | Names    | What the client can do                                                      |
|--------------------------|----------|-----------------------------------------------------------------------------|
| `FETCH_FAILED`           | a type   | Retry — likely transient. The response is marked `no-store`.                |
| `NO_ROUTE`               | a type   | Nothing — a server configuration issue. The response is marked `no-store`.  |
| `MAX_INCLUDED_RESOURCES` | a path   | Ask for less: fewer or shallower includes, smaller pages. Everything below the path is missing too. |
| `UNSUPPORTED_INCLUDE`    | a path   | Ask for a shallower path. The part beyond `maxHops` is missing.             |

A type-level gap comes from a failed fetch, which is batched per type across paths; a path-level gap comes from a
limit, which cuts the include tree at a depth. Which resources exactly are missing follows from the document: those
linked in `relationships` with no matching resource object in `included`. Only failures forbid storing the response —
the limits give the same result on every request, so it may be cached like any other.

### Limitations

Same-app includes are ordinary HTTP calls from the app to itself over loopback, so a few setups need attention.

**The loopback call can't connect** when:

- TLS is terminated by the app itself — `https://127.0.0.1:8443` fails certificate validation unless the certificate
  covers that address;
- the server is bound to one specific non-loopback address (e.g. `server.address=10.0.0.5`);
- the connector requires the PROXY protocol, client certificates (mTLS), or listens on a Unix domain socket.

Point same-app types at an address that works instead with `jsonapi4j.cd.mapping.default`, e.g. a name the
certificate covers or a plain-HTTP internal port. It is meant for exactly this deployment-specific override, so set it
where the deployment is configured — in Spring Boot and Quarkus through an environment variable:

```bash
JSONAPI4J_CD_MAPPING_DEFAULT=https://api.internal:8443/jsonapi
```

A plain servlet deployment reads its settings from the config file only, so point `JSONAPI4J_CONFIG` at a
per-environment file that sets `mapping.default`.

`default` is a reserved key: with the plugin enabled, a resource type named `default` fails startup with a message
naming the clashing resource — whether or not the key is set, so the clash surfaces in development rather than in the
first deployment that sets it.

For finer control, map types individually with `jsonapi4j.cd.mapping.<type>`, or replace the routing entirely with a
custom `DomainSettingsResolver` bean. Whatever it returns `Optional.empty()` for falls back to `mapping.default`, or to
loopback when that isn't set:

```java
@Bean
public DomainSettingsResolver domainSettingsResolver() {
    URI internal = URI.create("http://api.internal:8081/jsonapi");
    return resourceType -> Optional.of(DomainSettings.of(internal));
}
```

**Operational effects** of calling the app over HTTP:

- each include is a separate request that needs its own worker thread while the original one waits, so under heavy load
  a bounded thread pool can run out and includes time out after `jsonapi4j.cd.httpTotalTimeoutMs`;
- rate limiters keyed by client address see include calls as coming from loopback;
- during graceful shutdown the connector stops accepting connections, so includes of in-flight requests fail;
- tests without a real server (e.g. MockMvc) can't resolve includes.

**Because include calls arrive from loopback:**

- The client's `Forwarded`, `X-Forwarded-For` and `X-Real-IP` headers are never propagated, even with `HEADERS`
  propagation on. Containers commonly trust loopback as a proxy (e.g. Tomcat's `RemoteIpValve`), so forwarding them
  would let a caller choose the remote address the app sees on the include call. Other headers, including
  `X-Forwarded-Proto` and identity headers such as `X-Forwarded-User`, are propagated as usual.
- Any rule that grants trust by source address to loopback (e.g. `hasIpAddress("127.0.0.1")` or `::1`) also matches include
  calls, which carry the original caller's credentials. Don't grant extra privileges to loopback on JSON:API paths.

> The sample apps keep same-app `mapping` entries (e.g. `users`, `countries`) purely as an illustrative example; they
> are equivalent to the automatic default and can be removed.

### Further Reading

For deeper details on how compound document resolution works — including the multi-stage resolution process, caching internals, Cache-Control aggregation, and the standalone resolver for API Gateway deployments — see the [Compound Documents](/compound-docs/) section.
