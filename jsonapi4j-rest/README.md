# jsonapi4j-rest

Servlet API integration layer. Provides the HTTP dispatcher, request parsing, error handling, and response writing for JSON:API endpoints. Can be used directly with any Servlet container or as a foundation for framework-specific integrations.

## Key Classes

- `JsonApi4jDispatcherServlet` — main servlet that routes JSON:API requests
- `PrincipalResolvingFilter` — servlet filter for authentication context
- `ErrorHandlerFactoriesRegistry` — pluggable error handling
- `ResponseStatus`, `ResponseHeaders` — response customization from operation logic

## Usage

```xml
<dependency>
  <groupId>pro.api4</groupId>
  <artifactId>jsonapi4j-rest</artifactId>
  <version>${jsonapi4j.version}</version>
</dependency>
```
