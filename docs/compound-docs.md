---
title: "Compound Documents"
permalink: /compound-docs/
---

### Overview

[Compound Documents](https://jsonapi.org/format/#document-compound-documents) is a core feature of the JSON:API specification that enable clients to include related resources within a single request.
For example, when fetching users, you can ask the server to include each user's related `citizenships` by calling:
`GET /users?page[cursor]=xxx&include=citizenships`.
Only relationships explicitly exposed through your resource definitions can be included.
All resolved related resources are placed in the top-level `included` array.

There are two distinct components involved in compound document support, and it's important to understand the difference:

- **`jsonapi4j-cd-plugin`** — the **plugin** for JsonApi4j applications. It hooks into the JsonApi4j request processing pipeline and automatically handles compound document resolution as part of the normal request lifecycle. For plugin setup, configuration properties, and cache settings, see the [Compound Documents Plugin](/compound-docs-plugin/) page.
- **`jsonapi4j-compound-docs-resolver`** — the standalone **resolver module**. It has no dependency on the JsonApi4j plugin system or Servlet API and can be embedded anywhere — for example, at an **API Gateway** level for centralized response composition across multiple downstream services.

In short: the **plugin** is for JsonApi4j apps, the **resolver** is for anything else.

### Multiple and Nested Includes

You can request multiple relationships in a single call using commas - e.g. `include=relatives,placeOfBirth`.

JSON:API defines that relationship endpoints themselves (`/users/1/relationships/...`) return only linkage objects (type + id), not the related resources.
If you also want to include the full related resources, use the `include` parameter: `GET /users/1/relationships/placeOfBirth?include=placeOfBirth`.

Compound documents also support multi-level includes, allowing chained relationships such as `include=placeOfBirth.currencies`.
Each level in the chain must represent a valid relationship on the corresponding resource.
For instance, this example first resolves each user's `placeOfBirth` (a Country resource), and then resolves each country's `currencies`.

The same applies to relationship endpoints - e.g. a relationship request may include nested relationships that start from the relationship name itself, f.e. `/users/{id}/relationships/relatives?include=relatives.relatives` will resolve user's relatives and relatives of his relatives in one go.

### Resolution Process

The Compound Documents Resolver operates as a post-processor: it inspects the original response and, if necessary, enriches it with the `included` section.

**JsonApi4j** resolves includes in stages.
For example, `/users/{id}?include=relatives,placeOfBirth.currencies,placeOfBirth.economy` is parsed into:
* **Stage 1**: resolve list of `relatives` and a country that is a `placeOfBirth` for the requested user
* **Stage 2**: resolve `currencies` and `economy` for a country resolved in Stage 1

Within each stage, resources are grouped by type and their IDs; then, parallel batch reads (e.g. using `filter[id]=1,2,3,4,5`) are made for each resource type.
A type served by another service is read over HTTP; within a JsonApi4j app, a type the app serves itself is read in-process, through the framework directly — see [How includes are fetched](/compound-docs-plugin/#how-includes-are-fetched).
If a bulk operation isn't implemented, the framework falls back to sequential "read-by-id" calls.
That's why it's important to implement either "filter[id]" or "read-by-id" operations giving the priority to the first one.

Since each additional level may trigger new batches of requests, it's important to use this feature judiciously.
You can control and limit the depth and breadth of includes using the `CompoundDocsProperties` configuration - for example, the `maxHops` property defines the maximum allowed relationship depth.

### Per-Domain Batch Size Limits

Downstream services typically impose a hard cap on the number of values accepted in a single `filter[id]=...` parameter — 20, 50, 100 are all common in practice.
To respect those limits, the resolver supports a configurable max batch size per resource type. When the IDs to fetch for a given type exceed that limit, the resolver splits the request into chunks of that size and fetches them **in parallel** through the configured `ExecutorService`. Cache lookups still happen against the full ID set, so only the cache **misses** are chunked.

Configure the fallback batch size and per-resource-type overrides via plugin properties:

```yaml
jsonapi4j:
  cd:
    mapping:
      default:
        maxBatchSize: 20                 # fallback for every type (default: 20)
      users:
        maxBatchSize: 50                 # per-resource override
      countries:
        maxBatchSize: 20
```

When using the standalone resolver, the same setting lives on `DomainSettings`:

```java
DomainSettingsResolver resolver = new DefaultDomainSettingsResolver(Map.of(
    "users",     DomainSettings.overHttp(URI.create("https://users.example.com"), 50),  // per-type batch size
    "countries", DomainSettings.overHttp(URI.create("https://countries.example.com"))    // the default batch size
));
```

`DomainSettingsResolver` is a strict functional interface — `Optional<DomainSettings> resolveDomainSettings(String resourceType)` — so any custom implementation has full control over the URL, the per-type batch size, and whether the domain is trusted with the client's credentials (`propagateCredentials` of `DomainSettings.overHttp(...)`; credential headers such as `Authorization` and `Cookie` are sent only to trusted domains). An empty result means there is no route for the type: used standalone (e.g. in an API gateway), resolution then fails with `Resource type '<type>' has no mapping`, so every type your API can include must be mapped. The [Compound Documents Plugin](/compound-docs-plugin/) instead treats such a type as served by the app itself.

### Standalone Resolver

The resolver is provided by a separate module: `jsonapi4j-compound-docs-resolver`. It can be used independently of the JsonApi4j plugin system — for example, to add compound document support at the API Gateway level:

```xml
<dependency>
  <groupId>pro.api4</groupId>
  <artifactId>jsonapi4j-compound-docs-resolver</artifactId>
  <version>${jsonapi4j.version}</version>
</dependency>
```

It handles multi-hop traversal, parallel batch fetching, resource deduplication, caching, and Cache-Control aggregation — all without requiring the JsonApi4j framework or Servlet API.

Build the resolver and its routing once at startup. Every resource type your API can include must be mapped:

```java
URI geoService = URI.create("http://geo-service/jsonapi");
DomainSettingsResolver routing = new DefaultDomainSettingsResolver(Map.of(
    // url, filter[id] batch size, whether it gets the client's credentials
    "users",      DomainSettings.overHttp(URI.create("http://users-service/jsonapi"), 50, true),
    "countries",  DomainSettings.overHttp(geoService, DomainSettings.DEFAULT_MAX_BATCH_SIZE, true),
    "currencies", DomainSettings.overHttp(geoService, DomainSettings.DEFAULT_MAX_BATCH_SIZE, true)
));

CompoundDocsResolverConfig config = new CompoundDocsResolverConfig(
    true,                                     // enabled
    2,                                        // maxHops
    UnsupportedIncludeStrategy.FAIL,          // unsupportedIncludes: 400 for a deeper include path
    100,                                      // maxIncludedResources
    ErrorStrategy.IGNORE,
    List.of(Propagation.FIELDS, Propagation.HEADERS),
    Set.of("Authorization", "Cookie"),        // credentialHeaders: sent only to trusted domains
    Deduplication.DATA_AND_INCLUDED,
    5000,                                     // httpConnectTimeoutMs
    10000,                                    // httpTotalTimeoutMs
    true,                                     // cacheEnabled
    1000                                      // cacheMaxSize
);

CompoundDocsResolver resolver = new CompoundDocsResolver(
    config,
    new ObjectMapper(),
    Executors.newCachedThreadPool(),
    new InMemoryCompoundDocsResourceCache(1000)   // or null to disable caching
);
```

Then, for every proxied `GET` that carries an `include` parameter and got a `2xx` from the backend:

```java
CompoundDocsRequest request = new CompoundDocsRequest(
    "GET",
    List.of("relatives", "placeOfBirth.currencies"), // the split `include` parameter
    Map.of("users", List.of("fullName")),            // fields[type], or Map.of()
    headers,                                         // incoming headers with all their values, propagated per `propagation`
    "/users/1",                                      // request path, e.g. /users/1/relationships/relatives
    Map.of()                                         // custom query params
);

CompoundDocsResult result = resolver.resolveCompoundDocs(backendResponseBody, request, routing);
// respond with result.responseBody(); when result.cacheControlDirectives() isn't null,
// set Cache-Control to CacheControlParser.format(result.cacheControlDirectives())
```

Under `UnsupportedIncludeStrategy.FAIL`, an include path deeper than `maxHops` makes `resolveCompoundDocs` throw
`UnsupportedIncludeException`, to be answered with `400 Bad Request`. To reject such a request without calling the
backend at all, run the same check before proxying it, with a checker built from the same config as the resolver:

```java
IncludesChecker.from(config).check(request);
```

How each type is fetched is up to `DomainSettings`, which is one of two kinds: `DomainSettings.overHttp(...)` is fetched over HTTP from its `url`; `DomainSettings.inProcess(...)` is fetched by the in-process `BatchFetcher<DomainSettings.InProcess>` passed to the resolver's constructor — the JsonApi4j plugin uses it for the types an app serves itself, and an embedding with in-process data sources of its own can implement it the same way. Each fetcher is typed by the kind of route it serves, so the two can't be mixed up. Resources fetched in-process are never cached.

Each backend must serve `GET /{type}?filter[id]=a,b,c` and emit relationship linkage for the relationships named in `include`. A backend that answers an unknown relationship name with `400 UNSUPPORTED_INCLUDE`, naming it in `meta.path` — as every JsonApi4j app does — lets the resolver report the full include path back to the client, or leave it out under `UnsupportedIncludeStrategy.IGNORE`. It doesn't resolve includes itself: the resolver's calls carry `X-Disable-Compound-Docs: true` and the gateway assembles `included`.

### Caching

Since JSON:API defines a clear way to uniquely identify resources using the "type" + "id" pair, a cache layer can be integrated to store resolved resources and avoid redundant downstream requests.

The Compound Documents Resolver includes a built-in in-memory cache that stores individual resources keyed by type, id, requested includes, and sparse fieldsets.
Cache entries respect `Cache-Control` headers from downstream HTTP responses: the `max-age` (or `s-maxage`) directive determines TTL, while `no-store`, `no-cache`, and `private` directives prevent caching entirely.

When a compound document request arrives, the resolver checks the cache for each required resource.
Only cache misses trigger downstream HTTP calls. Cached and freshly fetched resources are merged transparently.

The final compound document response carries an aggregated `Cache-Control` header reflecting the most restrictive directive across all included resources.
For example, if `countries` returns `max-age=300` and `currencies` returns `max-age=60`, the compound document response will contain `max-age=60`.

**`public` means the body is identical for every caller.** A shared cache — a CDN, a reverse proxy — is
entitled to store one response and serve it to everyone who asks for the same URL, without ever consulting
your application again. That is the whole point of the directive, and it is your declaration to make: the
framework forwards what your operations return and never second-guesses it.

Use `private` when the body varies by caller — the [Access Control plugin](/access-control-plugin/) hiding
fields the current principal may not see, per-principal links, or a resource that simply belongs to one
user. Browsers and other private caches still store it; shared caches do not. Use `no-store` when it should
not be retained at all. HTTP's own answer for "varies by caller" is `Vary`, but the input here is a
principal derived from a token, so it degenerates to `Vary: Authorization` — correct in principle and
destructive to hit rate in practice, which is why `private` is usually the honest answer.

The built-in cache uses a `ConcurrentHashMap` with lazy expiration and LRU eviction when the soft capacity is exceeded.
For distributed deployments or custom eviction policies, implement the `CompoundDocsResourceCache` SPI and register your own bean - the framework will use it instead of the default in-memory cache.

**Cache-Control propagation for primary resources**

To propagate downstream cache settings from your primary resource operations upstream, use: `ResponseHeaders#propagateCacheControl(CacheControlDirectives cacheControlDirectives)`.
This method forwards cache headers so that the Compound Documents Resolver can aggregate them with the included resources' directives.

### Sequence Overview

The **Compound Documents Resolver**, when incorporated into a JsonApi4j application as a plugin, functions as a post-processing filter. The sequence diagram is shown below.

![Compound Docs Sequence Diagram](/assets/images/compound-docs-sequence-diagram-embedded.svg "Compound Docs Sequence Diagram")

As mentioned earlier, the **Compound Documents Resolver** can also be used as a standalone module. A common approach is to integrate it into an existing API Gateway that orchestrates the entire process. Below is a high-level sequence diagram illustrating this scenario:

![Compound Docs Sequence Diagram](/assets/images/compound-docs-sequence-diagram-standalone.svg "Compound Docs Sequence Diagram")