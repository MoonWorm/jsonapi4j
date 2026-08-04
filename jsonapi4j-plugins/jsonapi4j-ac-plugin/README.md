# jsonapi4j-ac-plugin

Access Control plugin. Enforces fine-grained security rules during JSON:API request processing — per-field anonymization based on authentication, access tier, OAuth2 scopes, and resource ownership.

## Features

- Inbound evaluation (before data fetch) and outbound evaluation (before response)
- Declarative rules via `@AccessControl` annotation on operations, resources, relationships, and fields
- Pluggable `PrincipalResolver` for authentication context extraction — header-based by default, with
  JWT resolvers for Spring Security and Quarkus OIDC

## Usage

```xml
<dependency>
  <groupId>pro.api4</groupId>
  <artifactId>jsonapi4j-ac-plugin</artifactId>
  <version>${jsonapi4j.version}</version>
</dependency>
```

See [Access Control Plugin docs](https://api4.pro/access-control-plugin/) for details, and
[Principal Resolution](https://api4.pro/principal-resolution/) for how the principal is resolved from
headers or a JWT.

> **Using tier requirements with a JWT?** JWT has no standard access tier claim — name yours explicitly, or
> every `@AccessControl(tier = …)` operation is denied.
