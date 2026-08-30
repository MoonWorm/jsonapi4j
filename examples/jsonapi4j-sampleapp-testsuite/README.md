# JsonApi4j Sample App Test Suite

Reusable integration tests for the shared domain model. Runs against all sample apps (Spring Boot, Quarkus, Servlet) to verify consistent behavior across frameworks.

## Coverage

- User, Country, Currency CRUD operations
- Relationship operations (to-one, to-many)
- Access Control enforcement
- Sparse Fieldsets
- Compound Documents with multi-level includes
- Generated OpenAPI document, compared against a golden file

## Golden OpenAPI document

`OasDocumentTests` fetches `/jsonapi/oas` (JSON and YAML) and compares the whole document against
`src/main/resources/oas/expected-oas.json`. All three apps run an `oasTest` profile that enables the OAS plugin
and nothing else, and they share the one golden file — a difference between frameworks is itself a defect.

After an intentional change to the generated document, regenerate the file from any sample app:

```bash
mvn -pl jsonapi4j-servlet-sampleapp test -Dtest=ServletOasDocumentTests -Djsonapi4j.oas.golden.update=true
```

The run rewrites the file and then fails on purpose, so a regeneration is never mistaken for a passing build.
Review the diff, then re-run without the flag.

## Tech

JUnit 5, REST Assured, AssertJ. Tests expect the app running on `localhost:8080`.
