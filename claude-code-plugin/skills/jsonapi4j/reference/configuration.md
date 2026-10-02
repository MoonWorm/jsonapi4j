# Configuration reference (`jsonapi4j.*`)

Bind via Spring Boot properties / Quarkus config. Plugins are opt-in dependencies; their config blocks
apply only when the plugin is on the classpath.

```yaml
jsonapi4j:
  rootPath: /jsonapi            # base path all resources are served under

  cd:                           # Compound Documents (?include=)
    enabled: true
    maxHops: 3                  # deepest ?include path supported (a.b.c = 3)
    unsupportedIncludes: FAIL   # path deeper than maxHops or naming an unknown relationship:
                                #   FAIL -> 400 UNSUPPORTED_INCLUDE with meta.path (spec);
                                #   IGNORE -> resolved as far as supported, listed in meta.includedIncomplete
    maxIncludedResources: 100   # no further hops once reached (last hop may overshoot);
                                #   unresolved paths listed as MAX_INCLUDED_RESOURCES
    errorStrategy: IGNORE       # IGNORE -> failed includes are left out of `included` (response marked no-store,
                                #   gaps listed in meta.includedIncomplete: FETCH_FAILED | NO_ROUTE per type);
                                # FAIL -> JSON:API error: 504 timeout, 502 other downstream failure, 500 no route
    propagation: [FIELDS, CUSTOM_QUERY_PARAMS, HEADERS]   # what include reads carry over from the request
                                # (never: Accept-Encoding, conditional/Range, hop-by-hop, X-Forwarded-For & co.)
    credentialHeaders: [Authorization, Cookie, Proxy-Authorization, X-Authenticated-User-Id, ...]   # the default
    deduplication: DATA_AND_INCLUDED   # or INCLUDED_ONLY (repeat reached primary resources in included) | NONE
    defaultMaxBatchSize: 20
    mapping:                    # one entry per resource type
      orders:
        url: https://orders.internal/jsonapi   # ONLY for types another service serves
        maxBatchSize: 50                       # optional filter[id] batch size (else defaultMaxBatchSize)
        propagateCredentials: true             # send credentialHeaders there (default false)
      countries:
        maxBatchSize: 20                       # same-app type: no url
        transport: HTTP                        # optional: over HTTP from default.url (else default.transport)
      default:
        url: https://api.internal:8443/jsonapi # this app's own address, for same-app types over HTTP
        transport: IN_PROCESS                  # default transport of same-app types (IN_PROCESS | HTTP)
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
- `cd.mapping.<type>.url` is for cross-service types only. Same-app includes (and the built-in meta types) are
  read in-process, so there is no base URL or port to keep in sync. To fetch them over HTTP instead — when
  servlet filters such as URL-based security rules must see include requests — set `cd.mapping.default.url`
  plus `transport: HTTP` per type or on `default` (e.g. `JSONAPI4J_CD_MAPPING_DEFAULT_URL` and
  `JSONAPI4J_CD_MAPPING_DEFAULT_TRANSPORT=HTTP`). Startup fails on `HTTP` without `default.url`, and on
  `IN_PROCESS` with a `url`. `default` is reserved: a resource type named `default` fails startup.
- The client's credentials (`cd.credentialHeaders`) always reach same-app types, but a cross-service type
  only with `propagateCredentials: true`.
- The exact set of keys can grow between versions — confirm against your version's property classes /
  the docs below.

---

**Canonical reference**
- Sample app `application.yml` files under `examples/jsonapi4j-*-sampleapp/src/main/resources/`
- Docs: https://api4.pro/configuration/
