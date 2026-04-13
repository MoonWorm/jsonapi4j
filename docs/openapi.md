---
title: "OpenAPI Plugin"
permalink: /openapi/
---

The OpenAPI Specification Plugin (OAS) builds on top of the JsonApi4j plugin system to provide automatic, always-in-sync API documentation.
It observes registered resources, relationships, and operations and translates them into an OpenAPI-compliant model.
Because the specification is derived directly from the same metadata used at runtime, it accurately reflects the actual behavior of your JSON:API endpoints without requiring manual maintenance.

In order to enable JsonApi4j OpenAPI Specification (OAS) plugin - add the next dependency:

```xml
<dependency>
  <groupId>pro.api4</groupId>
  <artifactId>jsonapi4j-oas-plugin</artifactId>
  <version>${jsonapi4j.version}</version>
</dependency>
```

If you're using JsonApi4j in the scope of Spring Boot or Quarkus App - everything will be autoconfigured using default values.

**JsonApi4j** can generate an instance of the `io.swagger.v3.oas.models.OpenAPI` model and expose it through a dedicated endpoint.

By default, you can access both the JSON and YAML versions of the generated specification via the [/jsonapi/oas](http://localhost:8080/jsonapi/oas) endpoint.
It supports an optional `format` query parameter (`json` or `yaml`) - defaulting to `json` if not provided.

Out of the box, **JsonApi4j** generates all schemas and operations automatically.

However, if you want to enrich the document with additional metadata (e.g., `info`, `components.securitySchemes`, custom HTTP headers, etc.), you can do so via your `application.yaml`/`application.properties` configuration files. Please refer [application.properties](https://github.com/MoonWorm/jsonapi4j/blob/main/examples/jsonapi4j-quarkus-sampleapp/src/main/resources/application.properties#L12) or [application.yaml](https://github.com/MoonWorm/jsonapi4j/blob/main/examples/jsonapi4j-springboot-sampleapp/src/main/resources/application.yaml#L21) from Spring Boot / Quarkus Sample apps as a reference for available OAS settings.

There are more tunings available by placing the next annotations:
* `@OasResourceInfo` annotation on top of JSON:API resource declaration
* `@OasRelationshipInfo` annotation on top of JSON:API To-One or To-Many Relationship declaration
* `@OasOperationInfo` annotation on top of operation class or any of its methods that represents some particular operation

**Available properties**

| Property name                               | Default value | Description                                                                                                             |
|---------------------------------------------|---------------|-------------------------------------------------------------------------------------------------------------------------|
| `jsonapi4j.oas.enabled` | `true` | Enables/disables OAS plugin and OAS endpoint exposure. |
| `jsonapi4j.oas.oasRootPath` | `/jsonapi/oas` | Root path for generated OpenAPI spec endpoint. |
| `jsonapi4j.oas.info.title` | `JsonApi4j API Sample Title` | OpenAPI info.title. |
| `jsonapi4j.oas.info.description` | not set | OpenAPI info.description. |
| `jsonapi4j.oas.info.version` | `1.0.0` | OpenAPI info.version. |
| `jsonapi4j.oas.info.termsOfService` | not set | OpenAPI info.termsOfService URL. |
| `jsonapi4j.oas.info.contact.name` | not set | OpenAPI info.contact.name. |
| `jsonapi4j.oas.info.contact.url` | not set | OpenAPI info.contact.url. |
| `jsonapi4j.oas.info.contact.email` | not set | OpenAPI info.contact.email. |
| `jsonapi4j.oas.info.license.name` | not set | OpenAPI info.license.name. |
| `jsonapi4j.oas.info.license.url` | not set | OpenAPI info.license.url. |
| `jsonapi4j.oas.info.license.identifier` | not set | OpenAPI info.license.identifier (SPDX). |
| `jsonapi4j.oas.externalDocumentation.url` | not set | OpenAPI external docs URL. |
| `jsonapi4j.oas.externalDocumentation.description` | not set | OpenAPI external docs description. |
| `jsonapi4j.oas.oauth2.clientCredentials.name` | not set | OAuth2 client credentials scheme name. |
| `jsonapi4j.oas.oauth2.clientCredentials.description` | not set | OAuth2 client credentials description. |
| `jsonapi4j.oas.oauth2.clientCredentials.tokenUrl` | not set | OAuth2 client credentials token URL. |
| `jsonapi4j.oas.oauth2.authorizationCodeWithPkce.name` | not set | OAuth2 authorization code + PKCE scheme name. |
| `jsonapi4j.oas.oauth2.authorizationCodeWithPkce.description` | not set | OAuth2 authorization code + PKCE description. |
| `jsonapi4j.oas.oauth2.authorizationCodeWithPkce.tokenUrl` | not set | OAuth2 authorization code + PKCE token URL. |
| `jsonapi4j.oas.oauth2.authorizationCodeWithPkce.authorizationUrl` | not set | OAuth2 authorization URL (PKCE flow). |
| `jsonapi4j.oas.oauth2.authorizationCodeWithPkce.scopes[*].name` | not set | OAuth2 scope name. |
| `jsonapi4j.oas.oauth2.authorizationCodeWithPkce.scopes[*].description` | not set | OAuth2 scope description. |
| `jsonapi4j.oas.servers[*].name` | not set | OpenAPI server display name. |
| `jsonapi4j.oas.servers[*].url` | not set | OpenAPI server URL. |
| `jsonapi4j.oas.servers[*].enabled` | false | Include server in generated spec or not. |
| `jsonapi4j.oas.customResponseHeaders[*].httpStatusCode` | not set | HTTP status code this custom-header group applies to. |
| `jsonapi4j.oas.customResponseHeaders[*].headers[*].name` | not set | Header name. |
| `jsonapi4j.oas.customResponseHeaders[*].headers[*].description` | not set | Header description. |
| `jsonapi4j.oas.customResponseHeaders[*].headers[*].required` | false | Whether header is required. |
| `jsonapi4j.oas.customResponseHeaders[*].headers[*].schema` | string | Header schema type. |
| `jsonapi4j.oas.customResponseHeaders[*].headers[*].example` | not set | Header example value. |
