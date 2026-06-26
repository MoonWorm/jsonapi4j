# Relationships

A `Relationship<REF>` only ever produces a **resource identifier** (`{type, id}` + optional identifier
meta) — it has no `resolveAttributes`. The full target resource is materialized **separately** by the
target's own `Resource`, and (on `?include=`) by the Compound-Docs plugin fetching it over HTTP. `REF`
can be **any** type, so the framework leaves the choice to you — it's flexible by design.

The **recommended default** is a lightweight, purpose-built ref rather than a heavy DTO:

```java
public record ShowRef(UUID id) {}      // identity only — what a linkage needs
public record SeasonRef(UUID id) {}
```

- Back `readOne` with a thin query (`SELECT show_id FROM seasons WHERE id = ?`), not a full `findById`
  that computes joins/aggregates just to throw everything but one UUID away.
- Avoids the partially-populated-DTO trap (a `new ShowDto()` with only `id` set is a silent-bug magnet).
- If the identifier needs **meta** (edge data — see below), the ref carries those fields too — still far
  lighter than the full DTO.

That said, it's a guideline, not a rule — weigh it against your situation. If you don't control the DTO
you're handed (a downstream model, a shared entity), or you already hold the full object and reusing it
keeps the code simpler with fewer models to maintain, passing that type as `REF` is perfectly valid —
only its `id`/`type` (and any identifier meta) are read. Optimize for a purpose-built ref when the
linkage path would otherwise do real work just to discard it; reach for the existing type when simplicity
wins.

## To-one

`ToOneRelationshipOperations<PARENT_DTO, CHILD_DTO>` — implement `readOne` (+ `validateReadToOne`), and
override `readOneForResource` for N+1 avoidance.

```java
@Component @JsonApiRelationship(relationshipName = "show", parentResource = SeasonResource.class)
public class SeasonShowRelationship implements ToOneRelationship<ShowRef> { /* type "shows", id */ }

@Component @JsonApiRelationshipOperation(relationship = SeasonShowRelationship.class)
public class SeasonShowOperations implements ToOneRelationshipOperations<SeasonDto, ShowRef> {

    public ShowRef readOne(JsonApiRequest req) {              // standalone /seasons/{id}/relationships/show
        UUID seasonId = UUID.fromString(req.getResourceId()); // path id is the PARENT (season), not the show
        UUID showId = seasonRepo.findShowIdById(seasonId);
        if (showId == null) throw new ResourceNotFoundException(req.getResourceId(), new ResourceType("seasons"));
        return new ShowRef(showId);
    }

    public ShowRef readOneForResource(JsonApiRequest req, SeasonDto season) { // embedding / include path
        return new ShowRef(season.getShowId());               // straight off the parent FK — no DB hit
    }
}
```

## To-many

`ToManyRelationshipOperations<PARENT_DTO, CHILD_DTO>` — implement `readMany` (+ `validateReadToMany`),
optionally `add`/`update`/`delete` for mutable relationships, and `readManyForResource` for the embedded
path.

```java
@Component @JsonApiRelationship(relationshipName = "seasons", parentResource = ShowResource.class)
public class ShowSeasonsRelationship implements ToManyRelationship<SeasonRef> { /* type "seasons", id */ }

@Component @JsonApiRelationshipOperation(relationship = ShowSeasonsRelationship.class)
public class ShowSeasonsOperations implements ToManyRelationshipOperations<ShowDto, SeasonRef> {
    public PaginationAwareResponse<SeasonRef> readMany(JsonApiRequest req) {
        UUID showId = UUID.fromString(req.getResourceId());   // parent id from URL
        long limit = req.getLimit() != null ? req.getLimit() : DEFAULT_LIMIT, offset = ...;
        return PaginationAwareResponse.limitOffsetAware(repo.findRefsByShowId(showId, limit, offset), total);
    }
}
```

## `readOne` vs `readOneForResource` (N+1 avoidance)

- `readOne(request)` — standalone `GET /{type}/{id}/relationships/{rel}`. Path id is the **parent**.
- `readOneForResource(relReq, parentDto)` — called when **embedding** the relationship in a primary
  resource (incl. `?include=`). **Defaults to calling `readOne`** (a per-parent lookup = N+1 on lists).
  **Override it** to build the ref from the parent DTO's FK with zero extra queries — which is why DTOs
  carry FKs. Fall back to `readOne` if a given projection didn't populate the FK.
- `readManyForResource(relReq, parentDto)` — the to-many equivalent: serve the linkage list straight
  from the parent DTO when it already holds the child refs.

## Batch operations (collection includes)

When a **collection primary** includes a relationship, implement the batch interface to resolve all
parents in one or two queries instead of N:

- `BatchReadToOneRelationshipOperation.readBatches(originalReq, parentDtos)` →
  `Map<RESOURCE_DTO, RELATIONSHIP_DTO>`.
- `BatchReadToManyRelationshipOperation.readBatches(originalReq, parentDtos)` →
  `Map<RESOURCE_DTO, PaginationAwareResponse<RELATIONSHIP_DTO>>` — each value is a **page** (first page +
  total). A window-function query (`ROW_NUMBER()/COUNT(*) OVER (PARTITION BY parent_id)`) keeps it to one
  round trip and matches the single-parent include's first-page behavior.

Practical notes:
- The op implements **both** the composite (`ToOneRelationshipOperations<P,C>`) **and** the batch
  interface — they share the `ReadTo*RelationshipOperation` supertype, so you add `readBatches` alongside
  `readMany`/`readOne`. The composite still serves the standalone `/relationships/x` endpoint; batch
  serves the collection-primary include. (Real example: `UserPlaceOfBirthOperations` implements
  `ToOneRelationshipOperations<UserDbEntity, CountryRef>` + `BatchReadToOneRelationshipOperation<…>`.)
- Batch only fires for a **collection primary + include** — so the parent needs a `filter[id]`
  collection endpoint for it to trigger (a resource only ever read by id can never batch).
- Don't bother batching a relationship that already resolves **in-house** (`readOneForResource` off the
  parent FK) — it's already zero-query.
- Key the returned map by the exact parent DTO instances passed in; include **every** parent (empty page
  for those with no children).
- JdbcTemplate gotcha: `jdbcTemplate.query(sql, rs -> {…}, args)` is ambiguous between
  `ResultSetExtractor` and `RowCallbackHandler` — use a **block-body** lambda (no return) to bind it to
  `RowCallbackHandler` for the accumulate-into-a-Map pattern.

## Edge / association data → identifier `meta`

If a value exists **only on the association** between two resources — belongs to neither alone — put it
in the relationship's **resource-identifier `meta`**, never in either resource's attributes. Litmus
test: *does the value change depending on which pairing you look at?* If yes, it's edge data.

```java
public record TrackRef(UUID id, int position) {}   // ref carries the edge data

public class AlbumTracksRelationship implements ToManyRelationship<TrackRef> {
    public String resolveResourceIdentifierType(TrackRef t) { return "tracks"; }
    public String resolveResourceIdentifierId(TrackRef t)   { return t.id().toString(); }
    @Override public Object resolveResourceIdentifierMeta(JsonApiRequest req, TrackRef t) {
        return Map.of("position", t.position());   // -> relationships.tracks.data[i].meta.position
    }
}
```

Promote the edge to a **first-class resource** only when the association grows its own identity,
lifecycle, or many attributes.

---

**Canonical examples in the framework**
- `examples/jsonapi4j-sampleapp-domain/.../operations/user/UserPlaceOfBirthOperations.java` (to-one + batch),
  `.../UserCitizenshipsOperations.java` / `.../UserRelativesOperations.java` (to-many),
  `.../domain/country/CountryCurrenciesRelationship.java`
- Docs: https://api4.pro/domain/ · https://api4.pro/performance/
