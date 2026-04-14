# jsonapi4j-core

Core processing engine. Handles JSON:API request execution, resource/relationship processing, and registry management. Suitable for embedding into non-web services that need JSON:API processing without HTTP dependencies.

## Key Classes

- `JsonApi4j` — main entry point for request processing
- `DomainRegistry` — central registry of resources and relationships
- `OperationsRegistry` — central registry of operations
- `SingleResourceProcessor`, `MultipleResourcesProcessor` — resource document builders
- `ToOneRelationshipProcessor`, `ToManyRelationshipsProcessor` — relationship document builders

## Usage

```xml
<dependency>
  <groupId>pro.api4</groupId>
  <artifactId>jsonapi4j-core</artifactId>
  <version>${jsonapi4j.version}</version>
</dependency>
```
