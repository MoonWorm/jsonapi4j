# Compound documents (`?include=`)

The Compound-Docs (CD) plugin resolves includes by calling the API **back over HTTP**, per resource
type. Consequences:

- **Same-app types need no `cd.mapping` at all.** With no entry, the base URL is taken from the request
  currently being served — its scheme, host, port and context path — so it is always right for the port
  actually in use. `jsonapi4j.cd.mapping.<type>` is for types served by *another* service
  (`orders: https://orders.internal/jsonapi`). A `batchSizeMapping` entry is still worth setting per type.
- **Multi-hop works**: `?include=season.show` (bounded by `cd.maxHops`). Each hop is a fresh HTTP
  fetch, so each intermediate resource must expose the next relationship's linkage. A multi-hop path
  **implies the intermediate** — `?include=a.b` pulls in both `a` and `b`; no need to write `a,a.b`.
- **A synthetic / non-persisted primary can't have its relationships included.** If an operation
  fabricates a resource that isn't backed by a real row (e.g. a composed item with a hashed id and no FK
  for some relationship), `?include=<that-rel>` can't resolve — `readOneForResource` reads a null FK and
  the linkage breaks. Such a relationship works via include only on the *persisted* read paths. Confirm
  **every** path that produces the resource can resolve the relationship before moving a field behind
  `include` (see `separation-of-concerns.md`).
- Because it's self-HTTP, **the app must be able to reach itself** on the address the client used — a
  container or proxy that rewrites host/port can break includes that work locally.
- `cd.propagation: [FIELDS, CUSTOM_QUERY_PARAMS, HEADERS]` controls what's forwarded downstream (e.g.
  the `Authorization` header, so an authenticated parent can pull included resources).

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
- The sample apps keep `cd.mapping` entries for `users`, `countries`, `currencies` purely as an
  illustration — they are equivalent to the automatic default and can be removed; integration tests:
  `examples/jsonapi4j-springboot-sampleapp/.../operations/SpringCompoundDocsOperationsTests.java`
  (and the Quarkus / Servlet equivalents, all built on the shared
  `examples/jsonapi4j-sampleapp-testsuite/.../CompoundDocsOperationsTests.java`).
- Docs: https://api4.pro/compound-docs/ · https://api4.pro/compound-docs-plugin/
