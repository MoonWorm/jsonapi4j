# jsonapi4j-rest-springboot

Spring Boot auto-configuration module. Automatically registers the JsonApi4j servlet, filters, domain scanning, and plugin integration into a Spring Boot application.

## Features

- Auto-discovers `Resource`, `Relationship`, and operation beans via Spring context
- Registers `JsonApi4jDispatcherServlet` and filters
- Binds `jsonapi4j.*` properties via `@ConfigurationProperties`
- Auto-configures plugins when present on the classpath

## Usage

```xml
<dependency>
  <groupId>pro.api4</groupId>
  <artifactId>jsonapi4j-rest-springboot</artifactId>
  <version>${jsonapi4j.version}</version>
</dependency>
```
