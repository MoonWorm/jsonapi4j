# Performance

In rough order of impact:

1. **Support `filter[id]` on every includable resource's `readPage`** — *the single most impactful
   compound-doc optimization*. The CD resolver batches included fetches into one
   `GET /type?filter[id]=a,b,c`; without it, it falls back to N sequential read-by-id calls (20
   resources = 20 HTTP requests). Cap the batch with `cd.defaultMaxBatchSize` / per-type
   `cd.batchSizeMapping` (e.g. 20) so you never exceed a downstream's limit.
2. **Resolve linkage in-house** via `readOneForResource` / `readManyForResource` (off the parent DTO's
   FK) and **batch ops** (`readBatches`) — eliminate downstream calls entirely when the linkage is
   already in the parent (see `relationships.md`).
3. **Parallel relationship resolution** — register an `ExecutorService` bean to resolve a resource's
   multiple relationships concurrently. Default is synchronous (`Runnable::run`). Options:
   `Executors.newFixedThreadPool(N)` (bounded), `newCachedThreadPool()` (dynamic),
   `newVirtualThreadPerTaskExecutor()` (Java 21+, ideal for I/O-bound downstream calls). Override the
   default bean (Spring `@ConditionalOnMissingBean`, Quarkus `@DefaultBean`) to supply your own.
4. **Bound the blast radius** — `cd.maxHops` (default 3) caps `?include=a.b.c` depth;
   `cd.maxIncludedResources` (default 100) caps total resolved resources per response.
5. **Compound-doc cache** — built-in (`cd.cache.enabled`, `cd.cache.maxSize` default 1000). It
   **respects `Cache-Control`** (TTL from `max-age`/`s-maxage`), so set those headers on your operations
   to make the cache effective. For distributed/Redis caching, provide your own
   `CompoundDocsResourceCache` bean (the in-memory default is overridable).
6. `cd.httpConnectTimeoutMs` / `cd.httpTotalTimeoutMs` bound the self-HTTP downstream calls;
   `cd.deduplicateResources` avoids refetching an already-resolved resource within one response.

---

**Canonical reference**
- Docs: https://api4.pro/performance/ · https://api4.pro/compound-docs/
- Config keys: `reference/configuration.md`.
