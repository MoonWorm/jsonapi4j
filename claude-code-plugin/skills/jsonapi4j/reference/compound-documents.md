# Compound documents (`?include=`)

The Compound-Docs (CD) plugin resolves includes hop by hop, per resource type: types the app serves itself
**in-process** (through the framework directly, no HTTP), types of another service over HTTP. Consequences:

- **Same-app types need no `url` in `cd.mapping`.** Without one, they're read in-process, as the client's
  principal — validation and the access-control plugin apply, servlet filters (URL-based security rules,
  logging, metrics) don't. `jsonapi4j.cd.mapping.<type>.url` is for types served by *another* service;
  such a type gets the client's credentials only with `propagateCredentials: true`. A `maxBatchSize` is
  still worth setting per type, same-app ones included. `transport: HTTP` (per type, or on `cd.mapping.default`)
  fetches same-app types over HTTP from `cd.mapping.default.url` instead, when filters must see include requests.
- **Multi-hop works**: `?include=season.show` (bounded by `cd.maxHops`). Each hop is a fresh batch
  read, so each intermediate resource must expose the next relationship's linkage. A multi-hop path
  **implies the intermediate** — `?include=a.b` pulls in both `a` and `b`; no need to write `a,a.b`.
- **A synthetic / non-persisted primary can't have its relationships included.** If an operation
  fabricates a resource that isn't backed by a real row (e.g. a composed item with a hashed id and no FK
  for some relationship), `?include=<that-rel>` can't resolve — `readOneForResource` reads a null FK and
  the linkage breaks. Such a relationship works via include only on the *persisted* read paths. Confirm
  **every** path that produces the resource can resolve the relationship before moving a field behind
  `include` (see `separation-of-concerns.md`).
- **Put access rules for includable types in the access-control plugin**, not only in URL-based security
  rules: in-process includes don't pass through servlet filters.
- `cd.propagation: [FIELDS, CUSTOM_QUERY_PARAMS, HEADERS]` controls what's carried over to include reads.
  Credential headers (`Authorization`, cookies, principal headers) go to same-app types always, to another
  service only when its mapping has `propagateCredentials: true`.

## Resolution order, fastest first

1. **In-house** — `readOneForResource` / `readManyForResource` off the parent DTO's FK: zero downstream
   calls. Prefer this whenever the linkage is already on the parent.
2. **Batched** — `readBatches` (see `relationships.md`): one/two queries for a whole collection's
   relationship.
3. **`filter[id]` batch fetch** — the CD resolver batches included fetches into one
   `GET /type?filter[id]=a,b,c`. Requires the target's `readPage` to support `filter[id]` (see
   `performance.md`). Without it, the resolver falls back to N sequential read-by-id calls.

The standalone related-linkage endpoint is `/{type}/{id}/relationships/{rel}`; the convenience
related-resource URL `/{type}/{id}/{rel}` is **not** served (see `known-behaviors.md`).

---

**Canonical examples in the framework**
- The sample apps keep `cd.mapping` entries for `users`, `countries`, `currencies` with only a
  `maxBatchSize` (no `url`, so they're still fetched from the app itself); integration tests:
  `examples/jsonapi4j-springboot-sampleapp/.../operations/SpringCompoundDocsOperationsTests.java`
  (and the Quarkus / Servlet equivalents, all built on the shared
  `examples/jsonapi4j-sampleapp-testsuite/.../CompoundDocsOperationsTests.java`).
- Docs: https://api4.pro/compound-docs/ · https://api4.pro/compound-docs-plugin/
