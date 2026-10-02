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
| `jsonapi4j.cd.unsupportedIncludes`    | `FAIL`                               | What a request with an unsupported include path gets — one deeper than `maxHops`, or naming a relationship its resource type doesn't have, see [Unsupported include paths](#unsupported-include-paths). `FAIL`: `400 Bad Request` naming the path, as the JSON:API spec requires. `IGNORE`: the path is resolved only as far as supported and listed in `meta.includedIncomplete` as `UNSUPPORTED_INCLUDE`. |
| `jsonapi4j.cd.maxIncludedResources`   | `100`                                | When `included` reaches this many resources, resolution fetches nothing further, and the include paths left unresolved are listed in `meta.includedIncomplete` as `MAX_INCLUDED_RESOURCES`. Checked between hops, so the last hop can take `included` past it. |
| `jsonapi4j.cd.errorStrategy`          | `IGNORE`                             | What a failed include does — see [Error handling](#error-handling). `IGNORE`: the failed resources are left out of `included`, listed in `meta.includedIncomplete`, and the response is marked `no-store`. `FAIL`: the request is answered with a JSON:API error document instead. |
| `jsonapi4j.cd.propagation`            | `FIELDS,CUSTOM_QUERY_PARAMS,HEADERS` | Request parts propagated to the include calls: `FIELDS`, `CUSTOM_QUERY_PARAMS`, `HEADERS` — see [Header propagation](#header-propagation) for which headers go where. |
| `jsonapi4j.cd.credentialHeaders`      | `Authorization,Cookie,Proxy-Authorization,X-Authenticated-User-Id,X-Authenticated-User-Granted-Scopes,X-Authenticated-Client-Entitlements` | Headers carrying the client's identity, sent only to same-app types and to mappings with `propagateCredentials: true`. Add your own, e.g. `X-Api-Key`, or the header names of a custom `PrincipalResolver`. |
| `jsonapi4j.cd.deduplication`         | `DATA_AND_INCLUDED`                  | How resource objects repeat (by `type` / `id`). `DATA_AND_INCLUDED`: each resource appears once across `data` and `included` — spec-compliant. `INCLUDED_ONLY`: once within `included`, and a primary resource an include path reaches is repeated there, so a client can resolve every related resource from `included` alone. `NONE`: no deduplication. The last two go beyond the spec's one-resource-object-per-`type`/`id` rule. |
| `jsonapi4j.cd.httpConnectTimeoutMs`   | `5000`                               | Controls how long to wait when establishing TCP connection (in millisecond). Applied to each generated HTTP request.                                                              |
| `jsonapi4j.cd.httpTotalTimeoutMs`     | `10000`                              | Controls total request timeout (in millisecond). Applied to each generated HTTP request.                                                                                          |
| `jsonapi4j.cd.mapping.<resourceType>.url` | not set                     | Base URL of the service serving the resource type. Set it **only** for a type served by a different service; same-app types (including the built-in meta types) need none — see [Resolving base URLs](#how-includes-are-fetched). |
| `jsonapi4j.cd.mapping.<resourceType>.maxBatchSize` | `defaultMaxBatchSize` | Max `filter[id]=...` batch size for the type, e.g. when its service enforces a stricter cap. Also applies to a same-app type, with no `url`. |
| `jsonapi4j.cd.mapping.<resourceType>.propagateCredentials` | `false` | Whether the client's credentials (`credentialHeaders`) are sent to the service at `url` — see [Header propagation](#header-propagation). Same-app types always get them, so it's rejected without a `url`. |
| `jsonapi4j.cd.mapping.<resourceType>.transport` | `mapping.default.transport` | How a same-app type is fetched: `IN_PROCESS`, or `HTTP` from `mapping.default.url` — see [Fetching same-app types over HTTP](#fetching-same-app-types-over-http). A type with a `url` is always fetched over HTTP, so `IN_PROCESS` is rejected there, and `HTTP` without a `url` needs `mapping.default.url`. |
| `jsonapi4j.cd.mapping.default.url` | not set                             | This app's own base URL, where same-app types with transport `HTTP` are fetched from. |
| `jsonapi4j.cd.mapping.default.transport` | `IN_PROCESS`                  | How same-app types without a `transport` of their own are fetched. `HTTP` needs `mapping.default.url`. `default` is a reserved key that takes only `url` and `transport`: a resource type named `default` fails startup. |
| `jsonapi4j.cd.defaultMaxBatchSize`    | `20`                                 | Fallback max number of resource IDs per downstream `filter[id]=...` request. Larger ID sets are split into parallel chunks of this size.                                          |

Each resource type has one `mapping` entry holding all its settings:

```yaml
jsonapi4j:
  cd:
    mapping:
      orders:                                    # served by another service
        url: https://orders.internal/jsonapi
        maxBatchSize: 50
        propagateCredentials: true               # the orders service gets the client's Authorization and cookies
      rates:
        url: https://partner.example.com/jsonapi # no credentials for a partner API
      users:
        maxBatchSize: 100                        # served by this app, fetched in-process
```

**Cache properties**

| Property name                | Default value | Description                                                                 |
|------------------------------|---------------|-----------------------------------------------------------------------------|
| `jsonapi4j.cd.cache.enabled` | `true`        | Enables/disables the built-in resource cache for compound docs resolution.  |
| `jsonapi4j.cd.cache.maxSize` | `1000`        | Soft maximum number of cached entries. Eviction uses LRU + TTL expiration.  |

### How includes are fetched

To assemble the `included` array, the resolver fetches each included resource type, hop by hop, in batches of
`filter[id]`. Where each type is fetched from:

1. **A `jsonapi4j.cd.mapping.<type>.url`: over HTTP from that service.** Set it **only** for resource types served by
   a *different* service (a distributed / microservice setup), e.g.
   `jsonapi4j.cd.mapping.orders.url=https://orders.internal/jsonapi`.
2. **Otherwise the type is served by this app, and is fetched in-process** — the read runs through the framework
   directly, as an HTTP request to the app would, but without one: no socket, no servlet filters, no JSON sent and
   parsed again, and no second worker thread held while the original request waits.
3. **Unless its transport is `HTTP`:** then it's fetched over HTTP from `jsonapi4j.cd.mapping.default.url`, this
   app's own address — see [Fetching same-app types over HTTP](#fetching-same-app-types-over-http).

This means **same-app includes need no mapping and no base URL anywhere in config** — including the built-in meta
types (`state`, `plugins`, `resources`, `relationships`, `operations`, `config`), which always resolve once
`jsonapi4j.cd.enabled=true`. A same-app `mapping` entry without a `url` sets only the type's `maxBatchSize` and
`transport`; the app
validates the size of `filter[id]` of in-process reads the same way as of any other request.

An in-process read goes through what an HTTP request to the app goes through inside the framework: request
validation, plugins such as [access control](/access-control-plugin/), the operation itself. It runs as the client's
principal — resolved by the app's `PrincipalResolver` — so access control decides exactly as for the client, and
`fields[...]`, custom query parameters and headers are carried over per `jsonapi4j.cd.propagation`. Resources read
in-process are never stored in the resource cache: reading them is cheap, and they are always fresh.

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

### Unsupported include paths

The JSON:API spec requires `400 Bad Request` for an include path a server can't identify or doesn't support. A path is
unsupported when it is deeper than `maxHops`, or when it names a relationship its resource type doesn't have — at any
position in the path. Under `unsupportedIncludes: FAIL` (default) the request is answered with one error per path,
naming it in `meta.path`:

```json
{ "errors": [ {
  "status": "400",
  "code": "UNSUPPORTED_INCLUDE",
  "detail": "Resource type 'countries' has no relationship 'economy'",
  "source": { "parameter": "include" },
  "meta": { "path": "placeOfBirth.economy" }
} ] }
```

Who finds out:

- **Depth** is known from the request alone, so a path deeper than `maxHops` is rejected before the primary data is
  even fetched.
- **An unknown relationship** is rejected by the server of its resource type. The framework checks every request —
  with or without this plugin — against the relationships of the requested type, so the first relationship of a path
  is checked by the app serving the primary data. The following ones are checked by the services the plugin fetches
  them from, over the same `include` parameter: their `400 UNSUPPORTED_INCLUDE` is mapped back to the full path of
  the original request, `placeOfBirth.economy` above. A downstream service that isn't built on JsonApi4j is recognized
  only if it answers the same way — an unknown relationship it ignores goes unnoticed.

Under `IGNORE` an unsupported path is resolved only as far as supported and listed as `UNSUPPORTED_INCLUDE` — up to
`maxHops`, or up to the unknown relationship. The request is then served again without the rejected path (by this app,
or by the downstream service that rejected it), so one mistyped include doesn't cost the rest of the document.

### Limits

**`maxHops`** is the deepest include path supported — see [Unsupported include paths](#unsupported-include-paths).

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
| `UNSUPPORTED_INCLUDE`    | a path   | Fix the path: it is deeper than `maxHops`, or names an unknown relationship. The unsupported part is missing. |

A type-level gap comes from a failed fetch, which is batched per type across paths; a path-level gap comes from a
limit, which cuts the include tree at a depth. Which resources exactly are missing follows from the document: those
linked in `relationships` with no matching resource object in `included`. Only failures forbid storing the response —
the limits give the same result on every request, so it may be cached like any other.

### Limitations

**Servlet filters don't see same-app includes.** An in-process read never passes through the servlet container, so
URL-based security rules (e.g. Spring Security restricting `/jsonapi/countries`), and logging, metrics or rate limiting
done in filters don't apply to it — only the framework itself does, including the
[access control plugin](/access-control-plugin/). Put access rules for resource types that can be included there; or,
when filters have to see include requests, fetch same-app types over HTTP as below.

**The executor is used re-entrantly.** Included resources are fetched on the framework's executor, and an in-process
read resolves relationships on that same executor. The default — a cached thread pool — handles that; a **bounded**
executor you provide has to be sized for both, or a burst of includes can wait on the very threads it occupies.

#### Fetching same-app types over HTTP

A same-app type with transport `HTTP` is fetched over HTTP from `jsonapi4j.cd.mapping.default.url` — this app's own
address — like any other service: through every servlet filter, and with the client's credentials. The transport is
set per type, or for all of them on the reserved `default` entry:

```yaml
jsonapi4j:
  cd:
    mapping:
      default:
        url: http://127.0.0.1:8080/jsonapi
        transport: HTTP           # same-app types over HTTP...
      users:
        transport: IN_PROCESS     # ...except users
```

or the other way round: `HTTP` only on the types whose includes servlet filters must see. Set the address where the
deployment is configured — in Spring Boot and Quarkus e.g. through environment variables:

```bash
JSONAPI4J_CD_MAPPING_DEFAULT_URL=http://127.0.0.1:8080/jsonapi
JSONAPI4J_CD_MAPPING_DEFAULT_TRANSPORT=HTTP
```

A plain servlet deployment reads its settings from the config file only, so point `JSONAPI4J_CONFIG` at a
per-environment file that sets `mapping.default.url`.

Startup fails on a transport that can't work: `HTTP` without `mapping.default.url`, or `IN_PROCESS` on a type mapped
to another service's `url`.

Over HTTP every include is a request of its own, so it brings what an HTTP call costs:

- each include holds a worker thread of its own while the original request waits, so under heavy load a bounded
  thread pool can run out and includes time out after `jsonapi4j.cd.httpTotalTimeoutMs`;
- the URL has to be reachable from the app itself — e.g. one its TLS certificate covers;
- during graceful shutdown the connector stops accepting connections, so includes of in-flight requests fail;
- rate limiters keyed by client address see include calls as coming from the app itself, and so does any rule that
  grants trust by source address — which then also matches include calls carrying the original caller's credentials.
  The client's `Forwarded`, `X-Forwarded-For` and `X-Real-IP` headers are never propagated, so a caller can't choose
  the address the app sees on an include call.

`default` is a reserved key: with the plugin enabled, a resource type named `default` fails startup with a message
naming the clashing resource — whether or not the key is set, so the clash surfaces in development rather than in the
first deployment that sets it.

For finer control, map types individually with `jsonapi4j.cd.mapping.<type>.url`, or replace the routing entirely with
a custom `DomainSettingsResolver` bean. Whatever it returns `Optional.empty()` for is fetched per its `transport`. A route of its own can be in-process too, with `DomainSettings.inProcess(...)`:

```java
@Bean
public DomainSettingsResolver domainSettingsResolver() {
    URI orders = URI.create("https://orders.internal/jsonapi");
    return resourceType -> "orders".equals(resourceType)
            ? Optional.of(DomainSettings.overHttp(orders, 50, true))   // another service, trusted with credentials
            : Optional.of(DomainSettings.inProcess(DomainSettings.DEFAULT_MAX_BATCH_SIZE));
}
```

> The sample apps keep `mapping` entries for their same-app types (`users`, `countries`, `currencies`) only to set a
> batch size; they have no `url`, so those types are fetched in-process.

### Header propagation

With `HEADERS` in `jsonapi4j.cd.propagation` (default), the client's headers are sent along with every include call
over HTTP, with every value a repeated header has. A few never are, whatever the configuration:

- headers that would change the response away from a complete JSON:API document the resolver can read —
  `Accept-Encoding` (the response would come back compressed), the conditional `If-None-Match`, `If-Modified-Since`,
  `If-Match`, `If-Unmodified-Since`, `If-Range` and `Range` (a `304` or a `206`), and `Accept` and `Content-Type`.
  Each include call asks for `application/vnd.api+json` itself;
- hop-by-hop headers, which describe the client's own connection: `Keep-Alive`, `Proxy-Connection`, `TE`, `Trailer`,
  `Transfer-Encoding`;
- `Forwarded`, `X-Forwarded-For` and `X-Real-IP` — see [Fetching same-app types over HTTP](#fetching-same-app-types-over-http).

**Credentials go only where they are trusted.** The headers in `jsonapi4j.cd.credentialHeaders` — `Authorization`,
cookies and the principal headers by default — reach a resource type served by this app itself, which is the same
trust boundary (an in-process read runs as the client's principal directly), and a type mapped to another service only
with `propagateCredentials: true`. Any other service is
called without the client's identity, so a mapping to a partner or third-party API doesn't leak it:

```yaml
jsonapi4j:
  cd:
    mapping:
      users:
        url: http://users-service/jsonapi
        propagateCredentials: true            # users-service gets Authorization and cookies
      rates:
        url: https://partner.example.com/jsonapi   # the partner API doesn't
```

Cookies are sent as one `Cookie` header, as HTTP/1.1 requires, even when an HTTP/2 client split them across several.

### Further Reading

For deeper details on how compound document resolution works — including the multi-stage resolution process, caching internals, Cache-Control aggregation, and the standalone resolver for API Gateway deployments — see the [Compound Documents](/compound-docs/) section.
