# Resources & operations

## The `Resource<DTO>` contract

A `Resource<RESOURCE_DTO>` maps your backend DTO to a JSON:API resource object. Two methods carry the
weight; the rest are optional customization points with sensible defaults.

```java
@Component
@JsonApiResource(resourceType = "seasons")
public class SeasonResource implements Resource<SeasonDto> {
    public String resolveResourceId(SeasonDto e) { return e.getId().toString(); }
    public SeasonAttributes resolveAttributes(SeasonDto e) {
        return new SeasonAttributes(e.getSeasonNumber(), e.getEpisodeCount(), e.getAirDate());
    }
}
```

- `resolveResourceId(dto)` — the `id` member; must be unique within the resource type.
- `resolveAttributes(dto)` — the `attributes` member. Return a small attributes object holding **only
  this resource's own data** (see `separation-of-concerns.md`). Defaults to `null` (no attributes).

Optional overrides (default to a `self`-only links object / no meta — usually leave them alone):
`resolveResourceLinks`, `resolveResourceMeta`, `resolveTopLevelLinksForSingleResourceDoc`,
`resolveTopLevelLinksForMultiResourcesDoc`, `resolveTopLevelMetaForSingleResourceDoc`,
`resolveTopLevelMetaForMultiResourcesDoc`. Use these only when you need custom `links`/`meta` beyond the
framework defaults.

## The composite operations

Implement `ResourceOperations<DTO>` to group a resource's CRUD in one class. Every method defaults to
throwing `OperationNotFoundException`, so **override only what you support**. `validate(request)`
auto-dispatches to the matching `validateXxx` hook by operation type.

- `readById(request)` — `request.getResourceId()` is the path id.
- `readPage(request)` — branch on filters via `request.getFilters().get("id" | "slug" | …)`. Return a
  `PaginationAwareResponse` (factories below). With no recognized filter, decide deliberately: enumerate
  the whole collection via a keyset cursor, or return `PaginationAwareResponse.empty()`.
- `create` / `update` / `delete` — override for write operations (see the `write-operations` docs page).
- Validation hooks: `validateReadById`, `validateReadMultiple`, `validateCreate`, `validateUpdate`,
  `validateDelete` (see `validation-and-security.md`).

You may instead implement the narrow single-purpose interfaces (`ReadResourceByIdOperation`,
`ReadMultipleResourcesOperation`, `CreateResourceOperation`, …) if you prefer one class per operation —
the sample apps do this for `countries` (`ReadCountryByIdOperation`, `ReadMultipleCountriesOperation`).

## Pagination & `PaginationAwareResponse`

`readPage` returns a `PaginationAwareResponse<DTO>`. Pick the factory that matches your data source:

| Factory | Use when |
|---------|----------|
| `empty()` | Zero results — query ran, returned nothing. Pipeline runs **all** phases (incl. relationship visitors) on an empty array. |
| `fromItemsNotPageable(items)` | A small, bounded set that is never paged by nature. |
| `limitOffsetAware(items, totalItems)` | Server-side limit/offset paging; you know the total. |
| `inMemoryLimitOffsetAware(items, limit, offset)` | Small set; let the framework slice + count in memory. |
| `cursorAware(items, nextCursor)` | Server-side cursor paging; you produce the opaque `nextCursor`. |
| `inMemoryCursorAware(items[, cursor][, limit])` | Small set; framework computes the cursor in memory. |

**`null` vs `empty()` — important:** returning `null` from an operation signals "no data available" and
**short-circuits the pipeline before relationship resolution**; `empty()` is "zero results" and runs all
phases normally. Return `empty()` for an empty collection; reserve `null` for genuine absence.

**Cursor pagination:** encode an opaque `page[cursor]` (e.g. base64-url of the last row's key); the
server fixes the page size — the client only follows `page[cursor]`. A malformed cursor → throw
`InvalidCursorException` (400). The next-page cursor surfaces at top-level
`meta."pagination.nextCursor"` (absent/null = last page).

## Data layer: one canonical mapper per resource

Keep **exactly one `ResultSet`→DTO mapper per resource** (and one ref mapper per relationship). Do NOT
spin up per-query "lighter" projections that populate a different subset of fields — they drift, and a
field that's null on one path but set on another causes silent, hard-to-trace bugs (e.g. a relationship
FK present via the by-id query but null via a list query, breaking `readOneForResource`).

- Standardize column **aliases** across every query that feeds the resource so one mapper covers them all;
  extract the shared `SELECT … FROM … JOIN …` into a constant and append per-query `WHERE`/`ORDER`.
- Loading a few extra columns you don't always need is fine — predictability beats micro-optimizing.
- Want a lighter **response**? Use **Sparse Fieldsets** (`?fields[type]=…`) — it trims serialized output;
  it does NOT reduce DB columns and does NOT apply to relationship linkage (that's what refs are for).
- The DTO may carry **more** than the resource exposes (internal digest/notification/sort logic) — fine;
  `resolveAttributes` exposes only the subset.

---

**Canonical examples in the framework**
- `examples/jsonapi4j-sampleapp-domain/.../domain/user/UserResource.java`,
  `.../operations/user/UserOperations.java`
- `.../operations/country/ReadCountryByIdOperation.java`, `.../ReadMultipleCountriesOperation.java`
- Docs: https://api4.pro/domain/ · https://api4.pro/operations/ · https://api4.pro/pagination/
