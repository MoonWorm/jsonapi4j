# jsonapi4j-rest-quarkus

Native Quarkus extension for JsonApi4j. Follows the standard Quarkus two-module extension pattern.

## Modules

- **`runtime/`** (`jsonapi4j-rest-quarkus`) — CDI beans, configuration properties, plugin integration
- **`deployment/`** (`jsonapi4j-rest-quarkus-deployment`) — build-time servlet and filter registration via `@BuildStep`

## Usage

```xml
<dependency>
  <groupId>pro.api4</groupId>
  <artifactId>jsonapi4j-rest-quarkus</artifactId>
  <version>${jsonapi4j.version}</version>
</dependency>
```
