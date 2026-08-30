---
title: "OpenAPI Plugin"
permalink: /openapi-plugin/
---

The OpenAPI Specification Plugin (OAS) builds on top of the JsonApi4j plugin system to provide automatic, always-in-sync API documentation.
It observes registered resources, relationships, and operations and translates them into an OpenAPI-compliant model.
Because the specification is derived directly from the same metadata used at runtime, it accurately reflects the actual behavior of your JSON:API endpoints without requiring manual maintenance.

To enable the plugin, add the following dependency:

```xml
<dependency>
  <groupId>pro.api4</groupId>
  <artifactId>jsonapi4j-oas-plugin</artifactId>
  <version>${jsonapi4j.version}</version>
</dependency>
```

If you're using Spring Boot or Quarkus, the plugin is auto-configured with default values.

### Accessing the Specification

**JsonApi4j** generates an `io.swagger.v3.oas.models.OpenAPI` model and exposes it through a dedicated endpoint.

By default, the specification is available at `/jsonapi/oas`. It supports an optional `format` query parameter (`json` or `yaml`) — defaulting to `json` if not provided.

![Swagger UI](/assets/images/swagger-ui-screenshot.png)

Out of the box, **JsonApi4j** generates all schemas and operations automatically. JSON:API parameters, request/response schemas, and error models are all included.

### Enriching the Specification

To add metadata beyond what the framework generates automatically (e.g., `info`, `securitySchemes`, custom headers), you have two options:

**Via configuration properties** — set OpenAPI metadata in `application.yaml` / `application.properties`. See the [Spring Boot sample config](https://github.com/MoonWorm/jsonapi4j/blob/main/examples/jsonapi4j-springboot-sampleapp/src/main/resources/application.yaml#L21) or [Quarkus sample config](https://github.com/MoonWorm/jsonapi4j/blob/main/examples/jsonapi4j-quarkus-sampleapp/src/main/resources/application.properties#L12) for reference.

**Via annotations** — place these on your domain and operation classes for fine-grained control:

| Annotation | Placement | Purpose |
|-----------|-----------|---------|
| `@OasResourceInfo` | On `Resource` class | Customizes the resource's OpenAPI schema (description, example values) |
| `@OasRelationshipInfo` | On `ToOneRelationship` or `ToManyRelationship` class | Declares the resource types the relationship links to, and the type of the `meta` its resource linkage carries |
| `@OasOperationInfo` | On operation class or individual operation methods | Overrides the generated operation summary, description and request body type, and declares extra query/path parameters and OAuth2 security requirements |

Example:

```java
@OasResourceInfo(description = "Represents a registered user in the system")
@JsonApiResource(resourceType = "users")
public class UserResource implements Resource<UserDbEntity> {
    // ...
}
```

```java
@JsonApiResourceOperation(resource = UserResource.class)
public class UserOperations implements ResourceOperations<UserDbEntity> {

    @OasOperationInfo(summary = "List all users", description = "Returns a paginated list of users")
    @Override
    public PaginationAwareResponse<UserDbEntity> readPage(JsonApiRequest request) {
        // ...
    }
}
```

### Request Bodies

Every write operation gets a request body derived from what JSON:API prescribes for it — nothing to declare:

| Operation | Request body schema |
|-----------|---------------------|
| `POST /{type}` | `<Type>CreateRequestDoc` — `id` optional, so the client may generate it |
| `PATCH /{type}/{id}` | `<Type>UpdateRequestDoc` — `id` required |
| `PATCH /{type}/{id}/relationships/{name}` (to-one) | `ToOneRelationshipRequestDoc` — linkage, nullable to clear |
| `POST`/`PATCH`/`DELETE` `/{type}/{id}/relationships/{name}` (to-many) | `ToManyRelationshipsRequestDoc` — array of linkage |
| `DELETE /{type}/{id}` | none |

`<Type>` is the resource type capitalized, so `users` yields `UsersCreateRequestDoc`. The relationship bodies carry no
`<Type>` prefix because they are pure resource linkage — `{"data": {"id": …, "type": …}}` — which looks the same
whatever resource it points at, so one schema serves them all. The exception is a relationship declaring
`@OasRelationshipInfo(resourceLinkageMetaType = …)`: its linkage carries a typed `meta`, which makes the body specific
to that relationship, so it gets `<Type><Relationship>ToOneRelationshipRequestDoc` /
`<Type><Relationship>ToManyRelationshipsRequestDoc` instead. The same typed identifier is reused in responses, so a
client sees one shape for the linkage whichever direction it travels.

These are separate from the response documents on purpose: a request carries no `links` or `included`, and `id` is
optional on create but mandatory on update. To document a body the framework cannot derive, set
`@OasOperationInfo(payloadType = YourPayload.class)` — the declared type replaces the derived one and its schema is
registered automatically.

### Operation Ids

Every operation gets an `operationId`, derived from what it does and what it acts on:

```
get-all-users                        create-single-user
get-single-user                      update-single-user
get-user-relatives-relationship      delete-user-relatives-relationship
```

Client generators name their methods after it, so it is part of your published contract — treat a rename as a
breaking change.

Your `resourceType` stays exactly as you declared it, plural as JSON:API asks: it is what paths, tags, schema names
and the `type` member are built from, and `get-all-users` uses it as-is. A singular form appears only where the plural
would read wrong — in the ids of operations acting on one resource (`get-single-user`) and in the generated summaries
and descriptions ("Retrieves user details by resource id."). The plugin guesses it from the resource type
(`countries` → `country`, `addresses` → `address`, `status` → `status`, left alone when unrecognised). Set
`@OasResourceInfo(resourceNameSingle = …)` on the resource whenever that guess reads wrong. There is no override for
the plural — that is the resource type itself.

### Responses

Every operation documents the status it answers with, including the writes that return no body:

| Operation | Success response |
|-----------|------------------|
| `GET` (resource, collection, relationship) | `200` with the corresponding document |
| `POST /{type}` | `201` with `<Type>SingleResourceDoc` |
| `PATCH` / `DELETE`, and every relationship write | `204`, no body |

Error responses carry a ready example per status code, embedded as JSON so that spec linters and mock servers read
them as documents rather than as strings.

### Available Properties

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
| `jsonapi4j.oas.oauth2.clientCredentials.name` | not set | OAuth2 client credentials scheme name. Also the name operations reference in their `security` requirements — leave it unset and no operation requires this flow. |
| `jsonapi4j.oas.oauth2.clientCredentials.description` | not set | OAuth2 client credentials description. |
| `jsonapi4j.oas.oauth2.clientCredentials.tokenUrl` | not set | OAuth2 client credentials token URL. |
| `jsonapi4j.oas.oauth2.clientCredentials.scopes[*].name` | not set | OAuth2 scope name. |
| `jsonapi4j.oas.oauth2.clientCredentials.scopes[*].description` | not set | OAuth2 scope description. |
| `jsonapi4j.oas.oauth2.authorizationCodeWithPkce.name` | not set | OAuth2 authorization code + PKCE scheme name. Also the name operations reference in their `security` requirements — leave it unset and no operation requires this flow. |
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
