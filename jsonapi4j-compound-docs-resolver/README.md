# jsonapi4j-compound-docs-resolver

Standalone compound documents resolver. Fetches and populates the `included` section of JSON:API responses with multi-hop relationship traversal, parallel batch resolution, and built-in caching.

## Features

- Multi-level `include` traversal (e.g. `include=placeOfBirth.currencies`)
- Parallel batch fetching via `filter[id]=x,y,z`
- Built-in in-memory cache with Cache-Control header support
- Cache-Control aggregation across included resources
- Deployable within the application or at the API Gateway level

## Usage

```xml
<dependency>
  <groupId>pro.api4</groupId>
  <artifactId>jsonapi4j-compound-docs-resolver</artifactId>
  <version>${jsonapi4j.version}</version>
</dependency>
```
