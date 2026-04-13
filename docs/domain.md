---
title: "Designing the Domain"
permalink: /domain/
---

As highlighted earlier in the **Getting Started** guide, designing your domain model is one of the most important steps - and typically the first one - when building APIs with **JsonApi4j**. A well-structured domain design ensures clear resource boundaries, consistent data representation, and smoother integration with the JSON:API specification.

There are a few extension points that are important to understand when working with **JsonApi4j**.
In most cases, you'll simply implement one or more predefined interfaces that allow the framework to recognize and apply your domain configuration automatically.

All domain-related interfaces are located in the `jsonapi4j-core` module under the `pro.api4.jsonapi4j.domain` package.

Here are the most essential ones:

* `Resource<RESOURCE_DTO>` - implement this interface to declare a new **JSON:API resource**
* `ToOneRelationship<RELATIONSHIP_DTO>` - implement this interface to declare a new **JSON:API to-one relationship**
* `ToManyRelationship<RELATIONSHIP_DTO>` - implement this interface to declare a new **JSON:API to-many relationship**

### Resource<RESOURCE_DTO>

This is the primary interface for defining a JSON:API resource. It describes how your internal model is going to be represented by JSON:API documents.

Think about resources as of vertices (or nodes) in a graph.

Type parameter:
* `RESOURCE_DTO` - the internal data object or DTO from your domain or persistence layer (`UserDbEntity`, `DownstreamCountry`, etc.).

Mandatory / Key Responsibilities:
* Provide a unique resource ID. Implement `resolveResourceId(RESOURCE_DTO dataSourceDto)`). This is mandatory for every resource and ensures each object can be uniquely identified.
* Define the resource type. Implement `resolveResourceId()`). This is mandatory to differentiate resource types across your APIs.
* Map internal objects to API-facing attributes. Implement `resolveAttributes(RESOURCE_DTO dataSourceDto)`). By default, **attributes** are `null`, but most resources should define this as it represents the core domain information.

Optional / Advanced Capabilities:
* Top-level **links** for single resource documents. Implement `resolveTopLevelLinksForSingleResourceDoc(JsonApiRequest request, RESOURCE_DTO dataSourceDto)`). By default, generates "self" member only.
* Top-level **links** for multi-resource documents. Implement `resolveTopLevelLinksForMultiResourcesDoc(JsonApiRequest request, List<RESOURCE_DTO> dataSourceDtos, PaginationContext paginationContext)`). By default, generates "self" and "next" members if applicable.
* Top-level **meta** for single resource documents. Implement `resolveTopLevelMetaForSingleResourceDoc(JsonApiRequest request, RESOURCE_DTO dataSourceDto)`. By default, generates `null`.
* Top-level **meta** for multi-resource documents. Implement `resolveTopLevelMetaForMultiResourcesDoc(JsonApiRequest request, List<RESOURCE_DTO> dataSourceDtos)`. By default, generates `null`.
* Resource-level **links**. Implement `resolveResourceLinks(JsonApiRequest request, RESOURCE_DTO dataSourceDto)`. By default, generates a "self" link.
* Resource-level **meta**. Implement `resolveResourceMeta(JsonApiRequest request, RESOURCE_DTO dataSourceDto)`). By default, generates `null`.

### ToOneRelationship<RELATIONSHIP_DTO>

This interface is used to define a **To-One relationship** between a JSON:API resource and another related resource. It allows the framework to map and expose single-valued relationships in a JSON:API-compliant response.

Think of this relationship as a 1-to-1 edge in a graph, where one parent resource can reference a single related resource.

Type parameter:
* `RELATIONSHIP_DTO` - the internal data object or DTO representing the related resource (e.g., `DownstreamCountry`).

Mandatory / Key Responsibilities:
* Define the relationship name. Implement `relationshipName()`. This identifies the relationship field in the JSON:API document.
* Specify the parent resource type. Implement `resourceType()`. This tells the framework which resource the relationship belongs to.
* Resolve the related resource type. Implement `resolveResourceIdentifierType(RELATIONSHIP_DTO relationshipDto)`. This defines the type of the related resource in the JSON:API document.
* Resolve the related resource ID. Implement `resolveResourceIdentifierId(RELATIONSHIP_DTO relationshipDto)`. This should return a unique identifier for the related resource.

Optional / Advanced Capabilities:
* Customize relationship links. Implement `resolveRelationshipLinks(JsonApiRequest request, RELATIONSHIP_DTO relationshipDto)`. By default, generates "self" and "related" links for the relationship.
* Customize relationship meta. Implement `resolveRelationshipMeta(JsonApiRequest request, RELATIONSHIP_DTO relationshipDto)`. By default, generates `null`.

Notes:
* A To-One relationship always resolves to a single resource identifier object (or `null`) in the JSON:API response.
* Multiple relationships can be defined for the same resource by implementing multiple `ToOneRelationship` instances.

### ToManyRelationship<RELATIONSHIP_DTO>

This interface is used to define a **To-Many relationship** between a JSON:API resource and another related resource. It allows the framework to map and expose multivalued relationships in a JSON:API-compliant response.

Think of this relationship as a 1-to-N edge in a graph, where one parent resource can reference multiple related resources.

Refer to the **ToOneRelationship** section for additional details, as the key concepts and advanced capabilities are largely the same.
