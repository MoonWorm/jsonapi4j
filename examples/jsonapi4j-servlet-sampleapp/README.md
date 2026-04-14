# JsonApi4j Servlet Sample App

Demonstrates JsonApi4j integration with plain Jakarta Servlet API using an embedded Jetty server.

## Dependencies

- `jsonapi4j-rest` — framework + Servlet API integration
- `jsonapi4j-all-plugins` — all plugins (Access Control, OpenAPI, Sparse Fieldsets, Compound Docs)
- `jsonapi4j-sampleapp-domain` — shared domain model

## Run

```bash
mvn exec:java
```

App starts on `http://localhost:8080`. See [examples/README.md](../README.md) for API request examples.
