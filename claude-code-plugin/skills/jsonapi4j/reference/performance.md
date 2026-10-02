# Performance

In rough order of impact:

1. **Support `filter[id]` on every includable resource's `readPage`** — *the single most impactful
   compound-doc optimization*. The CD resolver batches included fetches into one
   `GET /type?filter[id]=a,b,c`; without it, it falls back to N sequential read-by-id calls (20
   resources = 20 HTTP requests). Cap the batch with `cd.defaultMaxBatchSize` / per-type
   `cd.mapping.<type>.maxBatchSize` (e.g. 20) so you never exceed a downstream's limit.
2. **Keep same-app includes in-process** (`cd.mapping.<type>.transport: IN_PROCESS`, the default) — no
   socket, servlet filters or JSON round trip per include, and no second worker thread held (~1.8× faster
   on a 3-hop include in a rough benchmark). Use `transport: HTTP` only for types whose include requests
   servlet filters (URL-based security, logging, metrics) must see.
3. **Resolve linkage in-house** via `readOneForResource` / `readManyForResource` (off the parent DTO's
   FK) and **batch ops** (`readBatches`) — eliminate downstream calls entirely when the linkage is
   already in the parent (see `relationships.md`).
4. **Parallel relationship resolution** — register an `ExecutorService` bean to resolve a resource's
   multiple relationships concurrently. Default is synchronous (`Runnable::run`). Options:
   `Executors.newFixedThreadPool(N)` (bounded), `newCachedThreadPool()` (dynamic),
   `newVirtualThreadPerTaskExecutor()` (Java 21+, ideal for I/O-bound downstream calls). Override the
   default bean (Spring `@ConditionalOnMissingBean`, Quarkus `@DefaultBean`) to supply your own.
5. **Bound the blast radius** — `cd.maxHops` (default 2) caps `?include=a.b.c` depth;
   `cd.maxIncludedResources` (default 100) caps total resolved resources per response.
6. **Compound-doc cache** — built-in (`cd.cache.enabled`, `cd.cache.maxSize` default 1000). It
   **respects `Cache-Control`** (TTL from `max-age`/`s-maxage`), so set those headers on your operations
   to make the cache effective. For distributed/Redis caching, provide your own
   `CompoundDocsResourceCache` bean (the in-memory default is overridable).
7. `cd.httpConnectTimeoutMs` / `cd.httpTotalTimeoutMs` bound include calls to other services (same-app
   types are read in-process);
   `cd.deduplication` (default `DATA_AND_INCLUDED`) avoids refetching an already-resolved resource within one response; `NONE` refetches every time.

---

**Canonical reference**
- Docs: https://api4.pro/performance/ · https://api4.pro/compound-docs/
- Config keys: `reference/configuration.md`.
