# Compound documents (`?include=`)

The Compound-Docs (CD) plugin resolves includes by calling the API **back over HTTP**, per resource
type, using `jsonapi4j.cd.mapping.<type> = http://host:port/<rootPath>`. Consequences:

- **Every includable resource type needs a `cd.mapping` entry** (and usually a `batchSizeMapping`
  entry). A missing entry → `included` silently empty (with `cd.errorStrategy: IGNORE`).
- **Multi-hop works**: `?include=season.show` (bounded by `cd.maxHops`). Each hop re-fetches via the
  mapping, so each intermediate resource must expose the next relationship's linkage. A multi-hop path
  **implies the intermediate** — `?include=a.b` pulls in both `a` and `b`; no need to write `a,a.b`.
- **A synthetic / non-persisted primary can't have its relationships included.** If an operation
  fabricates a resource that isn't backed by a real row (e.g. a composed item with a hashed id and no FK
  for some relationship), `?include=<that-rel>` can't resolve — `readOneForResource` reads a null FK and
  the linkage breaks. Such a relationship works via include only on the *persisted* read paths. Confirm
  **every** path that produces the resource can resolve the relationship before moving a field behind
  `include` (see `separation-of-concerns.md`).
- Because it's self-HTTP, **the port must be fixed and reachable** — matters for tests (see
  `testing.md`).
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
- The sample apps wire `cd.mapping` for `users`, `countries`, `currencies`; integration tests:
  `examples/jsonapi4j-springboot-sampleapp/.../operations/SpringCompoundDocsOperationsTests.java`
  (and the Quarkus / Servlet equivalents, all built on the shared
  `examples/jsonapi4j-sampleapp-testsuite/.../CompoundDocsOperationsTests.java`).
- Docs: https://api4.pro/compound-docs/ · https://api4.pro/compound-docs-plugin/
