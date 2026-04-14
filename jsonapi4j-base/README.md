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

## Dependencies

Minimal: slf4j, commons-lang3, commons-collections4, lombok.
