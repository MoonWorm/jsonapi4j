---
title: "Sparse Fieldsets Plugin"
permalink: /sparse-fieldsets-plugin/
---

In order to enable JsonApi4j Sparse Fieldsets (SF) plugin - add the next dependency:

```xml
<dependency>
  <groupId>pro.api4</groupId>
  <artifactId>jsonapi4j-sf-plugin</artifactId>
  <version>${jsonapi4j.version}</version>
</dependency>
```
Please refer [JSON:API Sparse Fieldsets](https://jsonapi.org/format/#fetching-sparse-fieldsets) for more details.

Then, you can use `fields[TYPE]=field1,field2` query parameter to fetch only requested fields from the attributes object.

Even though the functionality looks straightforward at first glance, there are a number of specific edge cases to consider:
* If a response consists of resources of different type - for example, if you requested `users` and some other related resources of other types via Compound Docs - you can control which fields to request for each resource type by adding multiple query parameters like that.
* If none fields requested - all fields are returned by default for a particular resource type.
* If empty fields is explicitly requested, e.g. `fields[users]=` - no fields are returned for this resource type.
* If empty/non-existing type or field is requested - it's just ignored.
* If `fields[TYPE]=a.b.c` field is requested, and `a.b` segment exists, but `a.b.c` doesn't exist - entire path will be ignored.
* If all requested fields don't exist - this scenario is treated as empty fields (`fields[users]=`) by default - no fields are returned for this resource type. This behavior can be changed by using `jsonapi4j.sf.requestedFieldsDontExistMode` application property.
* Sparse fieldsets can only be applied to object types. For example, if `users` has a field `ageYears` of type `java.lang.Integer`, it can be excluded (if not explicitly requested) by setting its value to `null`. However, primitive types cannot be excluded if other fields at the same level are requested. Therefore, if you want full support for sparse fieldsets across all fields, it is recommended to use object types instead of primitives.

**Example**:
In order to request `users` with `email` and `lastName` fields and related `countries` resources with a `name` field only (via `citizenships` relationship):
[/users?include=citizenships&fields[users]=email,lastName&fields[countries]=name](http://localhost:8080/jsonapi/users?include=citizenships&fields[users]=email,lastName&fields[countries]=name)

### Available Properties

| Property name                               | Default value | Description                                                                                                             |
|---------------------------------------------|---------------|-------------------------------------------------------------------------------------------------------------------------|
| `jsonapi4j.sf.enabled`                      | `true`          | Enables/Disables Sparse Fieldsets plugin                                                                                |
| `jsonapi4j.sf.requestedFieldsDontExistMode` | `SPARSE_ALL_FIELDS`  | Defines the behavior when all requested fields don't exist. Available options: `SPARSE_ALL_FIELDS`, `RETURN_ALL_FIELDS` |
