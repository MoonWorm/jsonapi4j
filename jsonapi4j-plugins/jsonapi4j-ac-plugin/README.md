# jsonapi4j-ac-plugin

Access Control plugin. Enforces fine-grained security rules during JSON:API request processing — per-field anonymization based on authentication, access tier, OAuth2 scopes, and resource ownership.

## Features

- Inbound evaluation (before data fetch) and outbound evaluation (before response)
- Declarative rules via `@AccessControl` annotation on operations, resources, relationships, and fields
- Pluggable `PrincipalResolver` for custom authentication context extraction

## Usage

```xml
<dependency>
  <groupId>pro.api4</groupId>
  <artifactId>jsonapi4j-ac-plugin</artifactId>
  <version>${jsonapi4j.version}</version>
</dependency>
```

See [Access Control Plugin docs](https://api4.pro/access-control/) for details.
