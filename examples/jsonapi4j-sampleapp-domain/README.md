# JsonApi4j Sample App Domain

Shared domain model and operations used by all sample apps (Spring Boot, Quarkus, Servlet).

## Contents

- **Resources:** `UserResource`, `CountryResource`, `CurrencyResource`
- **Relationships:** `UserCitizenshipsRelationship`, `UserPlaceOfBirthRelationship`, `UserRelativesRelationship`, `CountryCurrenciesRelationship` — a relationship only emits a resource identifier (`{type, id}`), so `UserCitizenshipsRelationship`, `UserPlaceOfBirthRelationship` and `CountryCurrenciesRelationship` just implement `ToManyRefRelationship<ResourceRef>` / `ToOneRefRelationship<ResourceRef>`; `UserRelativesRelationship` implements `ToManyRefRelationship<RelativeRef>`, where `RelativeRef` is a record implementing `ResourceRef` that also carries the relation kind for the identifier meta
- **Operations:** `UserOperations`, `CountryOperations`, `CurrencyOperations`, and relationship operations
- **Data sources:** In-memory stores (`UserDb`, `CountriesClient`, `CurrenciesClient`)

## Usage

Not runnable on its own — used as a dependency by the sample apps.
