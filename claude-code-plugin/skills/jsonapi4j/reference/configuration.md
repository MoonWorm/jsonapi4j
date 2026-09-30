# Configuration reference (`jsonapi4j.*`)

Bind via Spring Boot properties / Quarkus config. Plugins are opt-in dependencies; their config blocks
apply only when the plugin is on the classpath.

```yaml
jsonapi4j:
  rootPath: /jsonapi            # base path all resources are served under

  cd:                           # Compound Documents (?include=)
    enabled: true
    maxHops: 3                  # deepest ?include path supported (a.b.c = 3)
    unsupportedIncludes: FAIL   # deeper path: FAIL -> 400 UNSUPPORTED_INCLUDE (spec);
                                #   IGNORE -> resolved to maxHops, listed in meta.includedIncomplete
    maxIncludedResources: 100   # no further hops once reached (last hop may overshoot);
                                #   unresolved paths listed as MAX_INCLUDED_RESOURCES
    errorStrategy: IGNORE       # IGNORE -> failed includes are left out of `included` (response marked no-store,
                                #   gaps listed in meta.includedIncomplete: FETCH_FAILED | NO_ROUTE per type);
                                # FAIL -> JSON:API error: 504 timeout, 502 other downstream failure, 500 no route
    propagation: [FIELDS, CUSTOM_QUERY_PARAMS, HEADERS]   # what to forward to downstream self-HTTP calls
    deduplication: DATA_AND_INCLUDED   # or INCLUDED_ONLY (repeat reached primary resources in included) | NONE
    defaultMaxBatchSize: 20
    batchSizeMapping:           # per-type override of the filter[id] batch size
      countries: 20
    mapping:                    # ONLY for types another service serves; same-app types need no entry
      orders: https://orders.internal/jsonapi
      default: https://api.internal:8443/jsonapi   # optional: same-app types when loopback isn't reachable
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
  against loopback (`127.0.0.1`, or `[::1]` over IPv6) at the local port the request arrived on (never the
  `Host` header), so there is no base URL or port to keep in sync. When the app isn't reachable on loopback
  as-is (TLS terminated in the app, a server bound to one specific non-loopback address), set
  `cd.mapping.default`, e.g. via `JSONAPI4J_CD_MAPPING_DEFAULT`. `default` is reserved: a resource type
  named `default` fails startup.
- The exact set of keys can grow between versions — confirm against your version's property classes /
  the docs below.

---

**Canonical reference**
- Sample app `application.yml` files under `examples/jsonapi4j-*-sampleapp/src/main/resources/`
- Docs: https://api4.pro/configuration/
