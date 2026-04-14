# JsonApi4j Spring Boot Sample App

Demonstrates JsonApi4j integration with Spring Boot using auto-configuration.

## Dependencies

- `jsonapi4j-rest-springboot` — framework + Spring Boot auto-configuration
- `jsonapi4j-all-plugins` — all plugins (Access Control, OpenAPI, Sparse Fieldsets, Compound Docs)
- `jsonapi4j-sampleapp-domain` — shared domain model

## Run

```bash
mvn spring-boot:run
```

App starts on `http://localhost:8080`. See [examples/README.md](../README.md) for API request examples.
