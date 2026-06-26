# jsonapi4j-base

Foundation module containing domain interfaces, annotations, model classes, and the plugin SPI. Can generate JSON:API-compliant Java models without bringing any HTTP-layer dependencies.

## Key Interfaces

- `Resource<T>`, `ToOneRelationship<T>`, `ToManyRelationship<T>` — domain model contracts
- `JsonApiRequest` — request representation
- `JsonApi4jPlugin` — plugin SPI with visitor-based extension points
- `Principal`, `AuthenticatedPrincipalContextHolder` — security context

## Key Models

- `SingleResourceDoc`, `MultipleResourcesDoc`, `ToOneRelationshipDoc`, `ToManyRelationshipsDoc`, `ErrorsDoc` — JSON:API document types
- `ResourceObject`, `ResourceIdentifierObject` — JSON:API resource representations
- `JsonApi4jException` — exception hierarchy root

## Validation

A fluent, JSON:API-compliant request validation framework. Violations surface as spec-compliant error objects — with `source` pointers/parameters — either individually or aggregated into a single error document.

- `JsonApiRequestValidator` — request-aware fluent API: `forRequest(req).path(...).parameters(...).headers(...).validate()`. Covers resource id/type, relationship name, filters, `include`, `sort`, cursor, limit/offset, sparse fieldsets, custom query params, and create/update payloads.
- `Validate.assertThat(value)` — AssertJ-style standalone assertions for arbitrary values.
- Typed assertions: `StringValidationAssert`, `NumberValidationAssert`, `CollectionValidationAssert`, `MapValidationAssert`, `ObjectValidationAssert`, plus JSON:API-specific `ResourceTypeValidationAssert` and `RelationshipNameValidationAssert` — a wide variety of checks (e.g. `isNotBlank`, `isUUID`, `isEmail`, `matches`, `hasLengthBetween`, `isOneOf`).
- `JsonApiRequestValidationException`, `CompositeJsonApiRequestValidationException` — thrown on violations and mapped to JSON:API error responses.

## Dependencies

Minimal: slf4j, commons-lang3, commons-collections4, lombok.
