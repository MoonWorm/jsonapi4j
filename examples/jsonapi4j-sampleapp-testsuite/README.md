# JsonApi4j Sample App Test Suite

Reusable integration tests for the shared domain model. Runs against all sample apps (Spring Boot, Quarkus, Servlet) to verify consistent behavior across frameworks.

## Coverage

- User, Country, Currency CRUD operations
- Relationship operations (to-one, to-many)
- Access Control enforcement
- Sparse Fieldsets
- Compound Documents with multi-level includes

## Tech

JUnit 5, REST Assured, AssertJ. Tests expect the app running on `localhost:8080`.
