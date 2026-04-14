# JsonApi4j Sample App Domain

Shared domain model and operations used by all sample apps (Spring Boot, Quarkus, Servlet).

## Contents

- **Resources:** `UserResource`, `CountryResource`, `CurrencyResource`
- **Relationships:** `UserCitizenshipsRelationship`, `UserPlaceOfBirthRelationship`, `UserRelativesRelationship`, `CountryCurrenciesRelationship`
- **Operations:** `UserOperations`, `CountryOperations`, `CurrencyOperations`, and relationship operations
- **Data sources:** In-memory stores (`UserDb`, `CountriesClient`, `CurrenciesClient`)

## Usage

Not runnable on its own — used as a dependency by the sample apps.
