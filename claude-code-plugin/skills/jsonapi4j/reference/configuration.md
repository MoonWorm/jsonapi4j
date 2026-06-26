# Configuration reference (`jsonapi4j.*`)

Bind via Spring Boot properties / Quarkus config. Plugins are opt-in dependencies; their config blocks
apply only when the plugin is on the classpath.

```yaml
jsonapi4j:
  rootPath: /jsonapi            # base path all resources are served under

  cd:                           # Compound Documents (?include=)
    enabled: true
    maxHops: 3                  # caps ?include=a.b.c depth
    maxIncludedResources: 100   # caps total resolved resources per response
    errorStrategy: IGNORE       # IGNORE -> a failed include leaves `included` empty rather than erroring
    propagation: [FIELDS, CUSTOM_QUERY_PARAMS, HEADERS]   # what to forward to downstream self-HTTP calls
    deduplicateResources: true
    defaultMaxBatchSize: 20
    batchSizeMapping:           # per-type override of the filter[id] batch size
      countries: 20
    mapping:                    # REQUIRED per includable type: where to self-fetch it over HTTP
      users:     http://localhost:${server.port}${jsonapi4j.rootPath}
      countries: http://localhost:${server.port}${jsonapi4j.rootPath}
    httpConnectTimeoutMs: 1000
    httpTotalTimeoutMs: 5000
    cache:
      enabled: true
      maxSize: 1000             # in-memory LRU; respects Cache-Control TTL

  ac:  { enabled: true }        # Access Control plugin (@AccessControl)
  sf:  { enabled: true, requestedFieldsDontExistMode: ... }   # Sparse Fieldsets (?fields[type]=a,b)
  oas: { enabled: true }        # OpenAPI/Swagger generation (served at <rootPath>/oas)

  validation:
    maxNumberFilterParams: ...
    maxElementsInFilterParam: ...
    resourceIdMaxLength: ...
    limitMaxValue: ...
    maxElementsInIncludeParam: ...
    maxElementsInSortByParam: ...
```

Notes:
- `cd.mapping` points the resolver back at your own service (self-HTTP). Keep the port resolvable —
  `${server.port}` works at runtime; pin it for `?include=` tests (see `testing.md`).
- The exact set of keys can grow between versions — confirm against your version's property classes /
  the docs below.

---

**Canonical reference**
- Sample app `application.yml` files under `examples/jsonapi4j-*-sampleapp/src/main/resources/`
- Docs: https://api4.pro/configuration/
