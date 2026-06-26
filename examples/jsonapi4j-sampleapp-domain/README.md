# JsonApi4j Sample App Domain

Shared domain model and operations used by all sample apps (Spring Boot, Quarkus, Servlet).

## Contents

- **Resources:** `UserResource`, `CountryResource`, `CurrencyResource`
- **Relationships:** `UserCitizenshipsRelationship`, `UserPlaceOfBirthRelationship`, `UserRelativesRelationship`, `CountryCurrenciesRelationship` — each is parametrized on a **lightweight ref** (`CountryRef`, `CurrencyRef`, `RelativeRef`); a relationship only emits a resource identifier (`{type, id}`), so a ref is all it needs (the recommended default — any type works, since only the `id`/`type`/meta are read)
- **Operations:** `UserOperations`, `CountryOperations`, `CurrencyOperations`, and relationship operations
- **Data sources:** In-memory stores (`UserDb`, `CountriesClient`, `CurrenciesClient`)

## Usage

Not runnable on its own — used as a dependency by the sample apps.
