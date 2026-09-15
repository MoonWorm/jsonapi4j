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
    mapping:                    # ONLY for types another service serves; same-app types need no entry
      orders: https://orders.internal/jsonapi
    httpConnectTimeoutMs: 1000
    httpTotalTimeoutMs: 5000
    cache:
      enabled: true
      maxSize: 1000             # in-memory LRU; respects Cache-Control TTL

  ac:                           # Access Control plugin (@AccessControl)
    enabled: true
    failOnMisconfiguration: false   # reject AC that cannot take effect instead of only logging it
    anonymizationReport: NONE       # NONE | INDICATOR | FIELDS | FIELDS_AND_REASONS — what a response
                                    # says about data withheld; off by default, since saying something
                                    # was hidden confirms it exists

  sf:                           # Sparse Fieldsets (?fields[type]=a,b)
    enabled: true
    requestedFieldsDontExistMode: SPARSE_ALL_FIELDS   # or RETURN_ALL_FIELDS

  oas:                          # OpenAPI generation
    enabled: true
    oasRootPath: /jsonapi/oas       # must differ from rootPath; ?format=json|yaml
    diagnostics: WARN               # DISABLED | WARN | FAIL_ON_REQUEST | FAIL_ON_STARTUP —
                                    # what a loose end in the generated document costs you

  validation:
    maxNumberFilterParams: ...
    maxElementsInFilterParam: ...
    resourceIdMaxLength: ...
    limitMaxValue: ...
    maxElementsInIncludeParam: ...
    maxElementsInSortByParam: ...
```

Notes:
- `cd.mapping` is for cross-service types only. Same-app includes (and the built-in meta types) resolve
  against the endpoint the request arrived on, so there is no base URL or port to keep in sync.
- The exact set of keys can grow between versions — confirm against your version's property classes /
  the docs below.

---

**Canonical reference**
- Sample app `application.yml` files under `examples/jsonapi4j-*-sampleapp/src/main/resources/`
- Docs: https://api4.pro/configuration/
