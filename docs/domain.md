---
title: "Designing the Domain"
permalink: /domain/
---

As highlighted earlier in the **[Getting Started](/getting-started/)** guide, designing your domain model is one of the most important steps - and typically the first one - when building APIs with **JsonApi4j**. A well-structured domain design ensures clear resource boundaries, consistent data representation, and smoother integration with the JSON:API specification.

There are a few extension points that are important to understand when working with **JsonApi4j**.
In most cases, you'll simply implement one or more predefined interfaces that allow the framework to recognize and apply your domain configuration automatically.

All domain-related interfaces are located in the `jsonapi4j-base` module under the `pro.api4.jsonapi4j.domain` package.

Here are the most essential ones:

* `Resource<RESOURCE_DTO>` - implement this interface to declare a new **JSON:API resource**
* `ToOneRelationship<RELATIONSHIP_DTO>` - implement this interface to declare a new **JSON:API to-one relationship**
* `ToManyRelationship<RELATIONSHIP_DTO>` - implement this interface to declare a new **JSON:API to-many relationship**
* `ToOneRefRelationship<REF extends ResourceRef>` / `ToManyRefRelationship<REF extends ResourceRef>` - shortcuts for relationships whose DTO is a `ResourceRef` (`{type, id}`, or your own record implementing it) - the most common case

### Resource\<RESOURCE_DTO\>

This is the primary interface for defining a JSON:API resource. It describes how your internal model is going to be represented by JSON:API documents.

Think about resources as of vertices (or nodes) in a graph.

Type parameter:
* `RESOURCE_DTO` - the internal data object or DTO from your domain or persistence layer (`UserDbEntity`, `DownstreamCountry`, etc.).

#### Annotation

Every resource must be annotated with `@JsonApiResource` to register it in the framework:

```java
@JsonApiResource(resourceType = "users")
public class UserResource implements Resource<UserDbEntity> {
    // ...
}
```

The `resourceType` attribute defines the unique type name for this resource. Each resource in your API must have a distinct type.

#### Mandatory

* `resolveResourceId(RESOURCE_DTO dataSourceDto)` - returns the unique identifier for this resource ("id" member). Must be unique across all resources of this type.

#### Optional Capabilities

**Attributes:**

| Method | Default | Description |
|--------|---------|-------------|
| `resolveAttributes(RESOURCE_DTO dataSourceDto)` | `null` | Maps internal objects to API-facing attributes ("attributes" member). Most resources should define this as it represents the core domain information. |

**Top-level document links and meta** — override these methods to customize the top-level "links" and "meta" members of the JSON:API document. The defaults are sufficient for most cases:

| Method | Default | Description |
|--------|---------|-------------|
| `resolveTopLevelLinksForSingleResourceDoc(request, dataSourceDto)` | "self" link | Links for single-resource documents (e.g., `GET /users/5`). By default, generates a "self" link. |
| `resolveTopLevelLinksForMultiResourcesDoc(request, dataSourceDtos, paginationContext)` | "self" + "next" links | Links for multi-resource documents (e.g., `GET /users`). By default, generates "self" and "next" links where applicable. |
| `resolveTopLevelMetaForSingleResourceDoc(request, dataSourceDto)` | `null` | Meta for single-resource documents |
| `resolveTopLevelMetaForMultiResourcesDoc(request, dataSourceDtos)` | `null` | Meta for multi-resource documents |

**Resource-level links and meta** — customize the "links" and "meta" members inside each resource object:

| Method | Default | Description |
|--------|---------|-------------|
| `resolveResourceLinks(request, dataSourceDto)` | "self" link | Links within each resource object |
| `resolveResourceMeta(request, dataSourceDto)` | `null` | Meta within each resource object |

### ToOneRelationship\<RELATIONSHIP_DTO\>

This interface is used to define a **To-One relationship** between a JSON:API resource and another related resource. It allows the framework to map and expose single-valued relationships in a JSON:API-compliant response.

Think of this relationship as a 1-to-1 edge in a graph, where one parent resource can reference a single related resource.

Type parameter:
* `RELATIONSHIP_DTO` - a reference to the related resource. A relationship only ever emits a resource identifier (`{type, id}` + optional identifier meta) — it has no `resolveAttributes` — and the full target resource is materialized separately by its own `Resource` (and, on `?include=`, by the Compound-Docs plugin). Because only the `id`/`type` (and optional meta) are read, `RELATIONSHIP_DTO` can be **any** type. The **recommended default** is the built-in `ResourceRef` (see [ResourceRef](#resourceref)) — it keeps the linkage path cheap and avoids partially-populated DTOs. Use your own type when the identifier needs more than `{type, id}` (e.g. identifier meta), when you don't control the type, or when you already hold a fuller object and reusing it is simpler.

#### Annotation

Every relationship must be annotated with `@JsonApiRelationship` to register it in the framework:

```java
@JsonApiRelationship(relationshipName = "placeOfBirth", parentResource = UserResource.class)
public class UserPlaceOfBirthRelationship implements ToOneRefRelationship<ResourceRef> {
}
```

The `relationshipName` attribute defines the name of the relationship field in the JSON:API document. The `parentResource` attribute identifies which resource this relationship belongs to.

#### Mandatory

These methods are inherited from the `Relationship` base interface (`ToOneRefRelationship` already implements both):

* `resolveResourceIdentifierType(RELATIONSHIP_DTO relationshipDto)` - returns the type of the related resource ("type" member of the resource linkage object). Can return different types if the relationship is polymorphic — for example, a `userProperty` relationship might return `"apartments"`, `"cars"`, or `"yachts"` depending on the DTO.
* `resolveResourceIdentifierId(RELATIONSHIP_DTO relationshipDto)` - returns the unique identifier of the related resource ("id" member of the resource linkage object).

#### Optional Capabilities

| Method | Default | Description |
|--------|---------|-------------|
| `resolveRelationshipLinks(request, relationshipDto)` | "self" + "related" links | Customize the "links" member of the relationship object |
| `resolveRelationshipMeta(request, relationshipDto)` | `null` | Customize the "meta" member of the relationship object |
| `resolveResourceIdentifierMeta(request, relationshipDto)` | `null` | Customize the "meta" member of the resource identifier object within the relationship's "data" member |

Notes:
* A To-One relationship always resolves to a single resource identifier object (or `null`) in the JSON:API response.
* Multiple relationships can be defined for the same resource by implementing multiple `ToOneRelationship` instances.

### ToManyRelationship\<RELATIONSHIP_DTO\>

This interface is used to define a **To-Many relationship** between a JSON:API resource and another related resource. It allows the framework to map and expose multivalued relationships in a JSON:API-compliant response.

Think of this relationship as a 1-to-N edge in a graph, where one parent resource can reference multiple related resources.

Type parameter:
* `RELATIONSHIP_DTO` - a reference to each related resource — `ResourceRef` by default, same as for `ToOneRelationship` (any type works; `ResourceRef` is the default, not a requirement).

#### Annotation

Same as ToOneRelationship — annotate with `@JsonApiRelationship`:

```java
@JsonApiRelationship(relationshipName = "citizenships", parentResource = UserResource.class)
public class UserCitizenshipsRelationship implements ToManyRefRelationship<ResourceRef> {
}
```

#### Mandatory

Same as ToOneRelationship — inherited from the `Relationship` base interface (`ToManyRefRelationship` already implements both):

* `resolveResourceIdentifierType(RELATIONSHIP_DTO relationshipDto)` - returns the type of each related resource.
* `resolveResourceIdentifierId(RELATIONSHIP_DTO relationshipDto)` - returns the unique identifier of each related resource.

#### Optional Capabilities

The key difference from ToOneRelationship: `resolveRelationshipLinks` and `resolveRelationshipMeta` receive a `List<RELATIONSHIP_DTO>` (all items in the relationship) instead of a single DTO. `resolveRelationshipLinks` also receives a `PaginationContext` since to-many relationships support pagination.

| Method | Default | Description |
|--------|---------|-------------|
| `resolveRelationshipLinks(request, relationshipDtos, paginationContext)` | "self" + "related" links | Customize the "links" member. Receives the full list of relationship DTOs and pagination context for generating "next" links. |
| `resolveRelationshipMeta(request, relationshipDtos)` | `null` | Customize the "meta" member. Receives the full list of relationship DTOs. |
| `resolveResourceIdentifierMeta(request, relationshipDto)` | `null` | Customize the "meta" member of each individual resource identifier object within the "data" array. Called per item. |

Notes:
* A To-Many relationship resolves to an array of resource identifier objects in the JSON:API response.
* Multiple relationships can be defined for the same resource by implementing multiple `ToManyRelationship` instances.

### ResourceRef

`ResourceRef` is the built-in, recommended relationship DTO: an interface with just `type()` and `id()`. `DefaultResourceRef` is its default implementation — create one with `DefaultResourceRef.of(type, id)`, which returns `ResourceRef` so that streams infer `List<ResourceRef>`. It carries exactly what a resource identifier needs, so pairing it with `ToOneRefRelationship<ResourceRef>` / `ToManyRefRelationship<ResourceRef>` leaves nothing to implement — both `resolveResourceIdentifierType` and `resolveResourceIdentifierId` are resolved from the ref:

```java
@JsonApiRelationship(relationshipName = "citizenships", parentResource = UserResource.class)
public class UserCitizenshipsRelationship implements ToManyRefRelationship<ResourceRef> {
}

@JsonApiRelationshipOperation(relationship = UserCitizenshipsRelationship.class)
public class UserCitizenshipsOperations implements ToManyRelationshipOperations<UserDbEntity, ResourceRef> {

    @Override
    public PaginationAwareResponse<ResourceRef> readMany(JsonApiRequest request) {
        return PaginationAwareResponse.inMemoryCursorAware(
                userDb.getUserCitizenships(request.getResourceId()).stream()
                        .map(countryId -> DefaultResourceRef.of("countries", countryId))
                        .toList(),
                request.getCursor()
        );
    }
}
```

Use `DefaultResourceRef.from(resourceIdentifierObject)` to map identifiers received in write payloads.

When the identifier needs more than `{type, id}` — typically for identifier `meta` — make your own DTO implement `ResourceRef` (records work well) and use it as the type argument: `ToOneRefRelationship<YourRef>` / `ToManyRefRelationship<YourRef>` still resolve the type and id, and you override only what you need. For example, the sample app's `relatives` relationship carries the kind of family relation on each linkage:

```java
public record RelativeRef(String id, RelationshipType relationshipType) implements ResourceRef {

    @Override
    public String type() {
        return "users";
    }
}

@JsonApiRelationship(relationshipName = "relatives", parentResource = UserResource.class)
public class UserRelativesRelationship implements ToManyRefRelationship<RelativeRef> {

    @Override
    public RelativeLinkageMeta resolveResourceIdentifierMeta(JsonApiRequest request, RelativeRef relativeRef) {
        return new RelativeLinkageMeta(relativeRef.relationshipType());
    }
}
```

Because each ref reports its own `type()`, polymorphic relationships work the same way — e.g. a `userProperty` relationship whose refs return `"cars"`, `"apartments"`, or `"yachts"`.

Implement `ToOneRelationship<YourDto>` / `ToManyRelationship<YourDto>` directly (with your own `resolveResourceIdentifierType` / `resolveResourceIdentifierId`) only when the DTO can't implement `ResourceRef`, e.g. a type you don't control.
