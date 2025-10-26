[![Build](https://github.com/moonworm/jsonapi4j/actions/workflows/build.yml/badge.svg)](https://github.com/moonworm/jsonapi4j/actions/workflows/build.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/pro.api4/jsonapi4j.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/pro.api4/jsonapi4j)
[![Issues](https://img.shields.io/github/issues/moonworm/jsonapi4j)](https://github.com/moonworm/jsonapi4j/issues)
[![License](https://img.shields.io/github/license/moonworm/jsonapi4j)](LICENSE)

![Logo](/docs/jsonapi4j-logo-medium.png)

Welcome to **JsonApi4j** — a lightweight API framework for Java for building [JSON:API](https://jsonapi.org/format/)-compliant web services with minimal configuration.

## Features

- 🔌 JSON:API-compliant request/response handling. Automatic error handling according to the JSON:API spec
- ⚙️ Servlet-level architecture. Natively integrates with [Spring Boot](https://spring.io/projects/spring-boot) but works with any Java web framework thanks to its foundation on the Servlet API.
- 📦 Compound Documents. Supports multi-level includes (e.g., `include=comments.authors.followers`) for complex client-driven requests. Available as an embedded module that can also run elsewhere (f.e. at the API Gateway level), using a shared resource cache to reduce latency and improve performance.
- 📘 [OpenAPI Specification](https://swagger.io/specification/) generation out of the box. Comprehensive enough by default, but can be configured if needed. 
- 🔐 Flexible Auth Model. Extensive support for authentication and authorization customization, including per-field data anonymization based on client access tier, user scopes, and resource ownership.
- 🚀 Optimized for Concurrency. Everything that can be parallelized is parallelized. You can configure execution using virtual threads (Java Loom) or any [ExecutorService](https://download.java.net/java/early_access/loom/docs/api/java.base/java/util/concurrent/ExecutorService.html) implementation.
- 🧠 Declarative approach with minimal boilerplate. Just describe your domain models (resources and relationships), supported operations, and authorization rules — the framework handles the rest for you.
- 🔧 Modular & Embeddable — use parts independently depending on the context:
    - 🌀 [jsonapi4j-core](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-core) — a lightweight JSON:API request processor ideal for embedding into non-web services, f.e. CLI tools that need to handle JSON:API input/output but without a need to carry all HTTP dependencies and specifics.
    - 🔌 [jsonapi4j-rest](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-rest) — Servlet API HTTP base for integration with other popular Web Frameworks. Can also be used for a plain Servlet API web application.
    - 🌱 [jsonapi4j-rest-springboot](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-rest-springboot) — [Spring Boot](https://spring.io/projects/spring-boot) auto configurable integration.
    - 🌐 [jsonapi4j-compound-docs-resolver](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-compound-docs-resolver) — a standalone compound documents resolver that automatically fetches and populates the `included` section of a JSON:API response — perfect for API Gateway-level use or microservice response composition layers.


## Getting Started

Here is an example how can you integrate JsonApi4j framework into your [Spring Boot](https://spring.io/projects/spring-boot) application.

### 1. Add Dependency (Maven)
```xml
<dependency>
  <groupId>pro.api4</groupId>
  <artifactId>jsonapi4j-rest-springboot</artifactId>
  <version>${jsonapi4j.version}</version>
</dependency>
```

It's supposed to be available in Maven Central. Check for the latest versions [here](https://central.sonatype.com/artifact/pro.api4/jsonapi4j-rest-springboot).

### 2. Declare your first JSON:API resource and related classes

Let's start describing your domain first by specifying the first resource.

```java
@Component
public class UserJsonApiResource implements Resource<UserAttributes, UserDbEntity> {

    @Override
    public String resolveResourceId(UserDbEntity userDbEntity) {
      return userDbEntity.getId();
    }
  
    @Override
    public ResourceType resourceType() {
      return () -> "users";
    }
  
    @Override
    public UserAttributes resolveAttributes(UserDbEntity userDbEntity) {
      return new UserAttributes(
              userDbEntity.getFullName().split("\\s+")[0],
              userDbEntity.getFullName().split("\\s+")[1],
              userDbEntity.getEmail(),
              userDbEntity.getCreditCardNumber()
      );
    }
}
```

`String resourceId(UserDbEntity userDbEntity)` method should return a resource identifier (must be unique across all resources of this type).

`ResourceType resourceType()` returns a unique resource type (must be unique across all resource types of all domains).

This class has 2 types it parametrized with: `UserAttributes` and `UserDbEntity`

`UserAttributes` - User's JSON:API resource specific data. This is what we expose via API. 

```java
public class UserAttributes {
    
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String creditCardNumber;
    
    // constructors, getters and setters

}
```

`UserDbEntity` - Internal user's model, usually has differences comparing to `UserAttributes`. Could encapsulate some DB specifics, for example Hibernate's entity or JOOQ's record. Can be also a DTO model of a 3rd party service. Or even an aggregation DTO of multiple sources.
```java
public class UserDbEntity {

    private final String id;
    private final String fullName;
    private final String email;
    private final String creditCardNumber;
    
    // constructors, getters and setters

}
```

`UserAttributes resolveAttributes(UserDbEntity userDbEntity)` - optional, the mapping logic that converts data that is fetched from a resource data source to an API-facing `attributes` of the JSON:API resource object

### 3. Declare your first JSON:API operation (read all users): 

Let's implement the first operation for reading multiple users (available by accessing `GET /users`)

```java
@Component
public class ReadAllUsersOperation implements ReadMultipleResourcesOperation<UserDbEntity> {

    private final UserDb userDb;
    
    public ReadAllUsersOperation(UserDb userDb) {
        this.userDb = userDb;
    }

    @Override
    public ResourceType resourceType() {
        return () -> "users";
    }

    @Override
    public CursorPageableResponse<UserDbEntity> readPage(JsonApiRequest request) {
        UserDb.DbPage<UserDbEntity> pagedResult = userDb.readAllUsers(request.getCursor());
        return new CursorPageableResponse.fromItemsAndCursor(
                pagedResult.getEntities(),
                pagedResult.getCursor()
        );
    }

}
```

and the corresponding Users Data Source provider:

```java
@Component
public class UserDb {

    private Map<String, UserDbEntity> users = new ConcurrentHashMap<>();
    {
        users.put("1", new UserDbEntity("1", "John Doe", "john@doe.com", "123456789"));
        users.put("2", new UserDbEntity("2", "Jane Doe", "jane@doe.com", "222456789"));
        users.put("3", new UserDbEntity("3", "Jack Doe", "jack@doe.com", "333456789"));
        users.put("4", new UserDbEntity("4", "Jessy Doe", "jessy@doe.com", "444456789"));
        users.put("5", new UserDbEntity("5", "Jared Doe", "jared@doe.com", "555456789"));
    }

    public DbPage<UserDbEntity> readAllUsers(String cursor) {
        LimitOffsetToCursorAdapter adapter = new LimitOffsetToCursorAdapter(cursor).withDefaultLimit(2); // let's say our page size is 2
        LimitOffsetToCursorAdapter.LimitAndOffset limitAndOffset = adapter.decodeLimitAndOffset();

        int effectiveFrom = limitAndOffset.getOffset() < users.size() ? limitAndOffset.getOffset() : users.size() - 1;
        int effectiveTo = Math.min(effectiveFrom + limitAndOffset.getLimit(), users.size());

        List<UserDbEntity> result = new ArrayList<>(users.values()).subList(effectiveFrom, effectiveTo);
        String nextCursor = adapter.nextCursor(users.size());
        return new DbPage<>(nextCursor, result);
    }

    public static class DbPage<E> {

        private final String cursor;
        private final List<E> entities;

        public DbPage(String cursor, List<E> entities) {
            this.cursor = cursor;
            this.entities = entities;
        }

        public String getCursor() {
            return cursor;
        }

        public List<E> getEntities() {
            return entities;
        }
    }
}
```

You can now run your app (for example, on port `8080` by setting Spring Boot's property to `server.port=8080`) and send the next HTTP request: [/users?page[cursor]=DoJu](http://localhost:8080/jsonapi/users?page[cursor]=DoJu)

And then you should get a JSON:API compatible response like that: 
```json
{
  "data": [
    {
      "attributes": {
        "firstName": "Jack",
        "lastName": "Doe",
        "email": "jack@doe.com",
        "creditCardNumber": "333456789"
      },
      "links": {
        "self": "/users/3"
      },
      "id": "3",
      "type": "users"
    },
    {
      "attributes": {
        "firstName": "Jessy",
        "lastName": "Doe",
        "email": "jessy@doe.com",
        "creditCardNumber": "444456789"
      },
      "links": {
        "self": "/users/4"
      },
      "id": "4",
      "type": "users"
    }
  ],
  "links": {
    "self": "/users?page%5Bcursor%5D=DoJu",
    "next": "/users?page%5Bcursor%5D=DoJw"
  }
}
```

Many things are covered for free, for example resource's `self` links and link for the `next` page.

You can also remove `page[cursor]=xxx` from the request URL - it will just start reading users from the very beginning.

### 4. Adding your first JSON:API relationship

Let's declare our first relationship to our JSON:API.  

We can introduce a new relationship like `relatives` that be a self-pointing relationship (each relationship item will be represented by a `users` resource itself). But let's better introduce more representative relationship, for example user's `citizenships`. 

First, we need to declare a dedicated JSON:API resource that represents a citizenship. In our case it's a `country`. 

```java
@Component
public class CountryJsonApiResource implements Resource<CountryAttributes, DownstreamCountry> {

    @Override
    public String resolveResourceId(DownstreamCountry downstreamCountry) {
        return downstreamCountry.getCca2(); // let's use CCA2 errorCode as a unique country identifier
    }

    @Override
    public ResourceType resourceType() {
        return () -> "countries";
    }

    @Override
    public CountryAttributes map(DownstreamCountry downstreamCountry) {
        return new CountryAttributes(
                downstreamCountry.getName().getCommon(),
                downstreamCountry.getRegion()
        );
    }
  
}
```

Similar to what we've done for the User resource declaration here are `CountryAttributes` and `DownstreamCountry`:

```java
public class CountryAttributes {
    
    private final String name;
    private final String region;
  
    // constructors, getters and setters

}
```

let's say we want to expose only `name` and `region` in our API. And let's use `.getName().getCommon()` for a name.

```java
public class DownstreamCountry {

    private final String cca2;
    private final Name name;
    private final String region;
    
    // constructors, getters and setters

    public static class Name {
  
        private final String common;
        private final String official;

        // constructors, getters and setters
  
    }

}
```

Now we're finally can set a relationship between `UserJsonApiResource` and `CountryJsonApiResource` resources. User might have multiple `citizenships` which means the relationship should have a to-many nature (represented by an array of resource identifier objects). That means we need to implement `ToManyRelationship` interface: 

```java
@Component
public class UserCitizenshipsJsonApiRelationship implements ToManyRelationship<UserDbEntity, DownstreamCountry> {

    @Override
    public Relationship relationshipName() {
        return () -> "citizenships";
    }
  
    @Override
    public ResourceType parentResourceType() {
        return () -> "users";
    }
  
    @Override
    public ResourceType resolveResourceIdentifierType(DownstreamCountry downstreamCountry) {
        return () -> "countries";
    }
  
    @Override
    public String resolveResourceIdentifierId(DownstreamCountry downstreamCountry) {
        return downstreamCountry.getCca2();
    }

}
```

`Relationship relationshipName()` - returns the name of the relationship

`ResourceType parentResourceType()` - returns the name of the resource this relationship belongs to

`ResourceType resolveResourceIdentifierType(DownstreamCountry downstreamCountry)` - resolves the relationship resource type. There might be cases where one relationship might consist a mix of different resource types. For example, `userProperty` might be a mix of resources like `cars`, `apartment`, `yachts` etc. 

`String resolveResourceIdentifierId(DownstreamCountry downstreamCountry)` - resolves relationship resource id

So now we have the domain graph that looks like: 

![Simple Domain Graph](docs/simple-domain-graph.png "Simple Domain Graph")

The only missing piece of puzzle is to teach the framework how to resolve the declared relationship data. That usually requires two things:
1. Implement `ReadToManyRelationshipOperation<DownstreamCountry>` to tell the framework how to find the corresponding country ids for a user where they basically have passports of
2. Optional. Implement `ReadMultipleResourcesOperation<DownstreamCountry>` for the `id` filter so the framework will know how to resolve [Compound Documents](https://jsonapi.org/format/#document-compound-documents) when it's requested in the `include` parameter. It's also possible to implement `ReadByIdOperation<DownstreamCountry>` but this would be less efficient because in that case compound docs are resolved sequentially one by one instead of a single batch request using `filter[id]=x,y,z` JSON:API query parameter.

`ReadMultiDataRelationshipOperation` to resolve the relationship between a user and a country:
```java
@Component
public class ReadUserCitizenshipsRelationshipOperation implements ReadToManyRelationshipOperation<DownstreamCountry> {

    private final RestCountriesFeignClient client;
    private final UserDb userDb;
    
    public ReadUserCitizenshipsRelationshipOperation(RestCountriesFeignClient client,
                                                     UserDb userDb) {
        this.client = client;
        this.userDb = userDb;
    }
    

    @Override
    public CursorAwareResponse<DownstreamCountry> read(JsonApiRequest request) {
        return CursorPageableResponse.fromItemsPageable(
                client.readCountriesByIds(userDb.getUserCitizenships(request.getResourceId())),
                request.getCursor(), 
                2 // set limit to 2
        );
    }

    @Override
    public RelationshipName relationshipName() {
        return () -> "citizenships";
    }

    @Override
    public ResourceType parentResourceType() {
        return () -> "users";
    }
    
}
```

`RestCountriesFeignClient` could be a FeignClient that represents some 3rd party API, for example [restcountries](https://restcountries.com/). But let's keep it simple at this moment:
```java
@Component
public class RestCountriesFeignClient {

  private static final Map<String, DownstreamCountry> COUNTRIES = Map.of(
          "NO", new DownstreamCountry("NO", new Name("Norway", "Kingdom of Norway"), "Europe"),
          "FI", new DownstreamCountry("FI", new Name("Finland", "Republic of Finland"), "Europe"),
          "US", new DownstreamCountry("US", new Name("United States", "United States of America"), "Americas")
  );

  public List<DownstreamCountry> readCountriesByIds(List<String> countryIds) {
    return countryIds.stream().map(COUNTRIES::get).toList();
  }

}
```

We also need to extend our existing `UserDb` to let it know which are these cca2 country codes the user has passports from.
`UserDb` (updated):
```java

public class UserDb {
    
    //  ...
    
    private Map<String, List<String>> userIdToCountryCca2 = new ConcurrentHashMap<>();
    {
        userIdToCountryCca2.put("1", List.of("NO", "FI", "US"));
        userIdToCountryCca2.put("2", List.of("US"));
        userIdToCountryCca2.put("3", List.of("US", "FI"));
        userIdToCountryCca2.put("4", List.of("NO", "US"));
        userIdToCountryCca2.put("5", List.of("US"));
    }

    public List<String> getUserCitizenships(String userId) {
        return userIdToCountryCca2.get(userId);
    }

    // ...

}
```

Optional. If we want to use Compound Documents feature of the JSON:API we also need to implement `ReadMultipleResourcesOperation<DownstreamCountry>` operation that can read countries by their ids.  
`ReadMultipleResourcesOperation<DownstreamCountry>`:
```java
@Component
public class ReadCountriesOperation implements ReadMultipleResourcesOperation<DownstreamCountry> {

    private final RestCountriesFeignClient client;
    
    public ReadAllCountriesOperation(RestCountriesFeignClient client) {
        this.client = client;
    }

    @Override
    public ResourceType resourceType() {
        return () -> "countries";
    }

    @Override
    public CursorPageableResponse<DownstreamCountry> readPage(JsonApiRequest request) {
        if (request.getFilters().containsKey(ID_FILTER_NAME)) {
            return CursorPageableResponse.byItems(client.readCountriesByIds(request.getFilters().get(ID_FILTER_NAME)));
        } else {
            throw new JsonApi4jException(400, CommonCodes.MISSING_REQUIRED_PARAMETER, "Operation supports 'id' filter only");
        }
    }

}
```

Now we can finally play around with some more exciting HTTP requests. Check out the next section for some examples!

## Request/response examples

### Fetch a user citizenships linkages

Request: [/users/1/relationships/citizenships](http://localhost:8080/jsonapi/users/1/relationships/citizenships)

<details>
  <summary>Response</summary>

  ```json
  {
    "data": [
      {
        "id": "NO",
        "type": "countries"
      },
      {
        "id": "FI",
        "type": "countries"
      }
    ],
    "links": {
      "self": "/users/1/relationships/citizenships",
      "related": {
        "countries": {
          "href": "/countries?filter[id]=FI,NO", 
          "describedby": "https://github.com/MoonWorm/jsonapi4j/tree/main/schemas/oas-schema-to-many-relationships-related-link.yaml", 
          "meta": {
            "ids": ["FI", "NO"]
          }
        }
      },
      "next": "/users/1/relationships/citizenships?page%5Bcursor%5D=DoJu"
    }
  }
  ```
</details>

It's worth noticing that relationshipName section has its own pagination. You can find the link pointing to the next page in `links` -> `next` field in the response. So try [/users/1/relationships
/citizenships?page[cursor]=DoJu](http://localhost:8080/jsonapi/users/1/relationships/citizenships?page%5Bcursor%5D=DoJu) to read the second page.

### Fetch a user citizenships linkages with the corresponding Country resources

Request: [/users/1/relationships/citizenships?include=citizenships](http://localhost:8080/jsonapi/users/1/relationships/citizenships?include=citizenships)

<details>
  <summary>Response</summary>

  ```json
  {
    "data": [
      {
        "id": "NO",
        "type": "countries"
      },
      {
        "id": "FI",
        "type": "countries"
      }
    ],
    "links": {
      "self": "/users/1/relationships/citizenships?include=citizenships",
      "related": {
        "countries": {
          "href": "/countries?filter[id]=FI,NO",
          "describedby": "https://github.com/MoonWorm/jsonapi4j/tree/main/schemas/oas-schema-to-many-relationships-related-link.yaml",
          "meta": {
            "ids": ["FI", "NO"]
          }  
        }
      },
      "next": "/users/1/relationships/citizenships?include=citizenships&page%5Bcursor%5D=DoJu"
    },
    "included": [
      {
        "attributes": {
          "name": "Norway",
          "region": "Europe"
        },
        "links": {
          "self": "/countries/NO"
        },
        "id": "NO",
        "type": "countries"
      },
      {
        "attributes": {
          "name": "Finland",
          "region": "Europe"
        },
        "links": {
          "self": "/countries/FI"
        },
        "id": "FI",
        "type": "countries"
      }
    ]
  }
  ```
</details>

### Fetch multiple Countries by ids 

Request: [/countries?filter[id]=US,NO](http://localhost:8080/jsonapi/countries?filter[id]=US,NO)

<details>
  <summary>Response</summary>

  ```json
  {
  "data": [
      {
        "attributes": {
          "name": "Norway",
          "region": "Europe"
        },
        "links": {
          "self": "/countries/NO"
        },
        "id": "NO",
        "type": "countries"
      },
      {
        "attributes": {
          "name": "United States",
          "region": "Americas"
        },
        "links": {
          "self": "/countries/US"
        },
        "id": "US",
        "type": "countries"
      }
    ],
    "links": {
      "self": "/countries?filter%5Bid%5D=US%2CNO"
    }
  }
  ```
</details>

### Fetch a particular page of users with their citizenships linkage objects

Request: [/users?page[cursor]=DoJu](http://localhost:8080/jsonapi/users?page[cursor]=DoJu)

<details>
  <summary>Response</summary>

  ```json
  {
    "data": [
      {
        "attributes": {
          "firstName": "Jack",
          "lastName": "Doe",
          "email": "jack@doe.com"
        },
        "relationships": {
          "citizenships": {
            "links": {
              "self": "/users/3/relationships/citizenships"
            }
          }
        },
        "links": {
          "self": "/users/3"
        },
        "id": "3",
        "type": "users"
      },
      {
        "attributes": {
          "firstName": "Jessy",
          "lastName": "Doe",
          "email": "jessy@doe.com"
        },
        "relationships": {
          "citizenships": {
            "links": {
              "self": "/users/4/relationships/citizenships"
            }
          }
        },
        "links": {
          "self": "/users/4"
        },
        "id": "4",
        "type": "users"
      }
    ],
    "links": {
      "self": "/users?page%5Bcursor%5D=DoJu",
      "next": "/users?page%5Bcursor%5D=DoJw"
    }
  }
  ```
</details>

### Fetch a particular page of users with their citizenships linkage objects and resolved Country resources

Request: [/users?page[cursor]=DoJu&include=citizenships](http://localhost:8080/jsonapi/users?page[cursor]=DoJu&include=citizenships)

<details>
  <summary>Response</summary>

  ```json
  {
    "data": [
      {
        "attributes": {
          "firstName": "Jack",
          "lastName": "Doe",
          "email": "jack@doe.com"
        },
        "relationships": {
          "citizenships": {
            "data": [
              {
                "id": "US",
                "type": "countries"
              },
              {
                "id": "FI",
                "type": "countries"
              }
            ],
            "links": {
              "self": "/users/3/relationships/citizenships",
              "related": {
                "countries": {
                  "href": "/countries?filter[id]=FI,US",
                  "describedby": "https://github.com/MoonWorm/jsonapi4j/tree/main/schemas/oas-schema-to-many-relationships-related-link.yaml",
                  "meta": {
                    "ids": ["FI", "US"]
                  }
                }
              }
            }
          }
        },
        "links": {
          "self": "/users/3"
        },
        "id": "3",
        "type": "users"
      },
      {
        "attributes": {
          "firstName": "Jessy",
          "lastName": "Doe",
          "email": "jessy@doe.com"
        },
        "relationships": {
          "citizenships": {
            "data": [
              {
                "id": "NO",
                "type": "countries"
              },
              {
                "id": "US",
                "type": "countries"
              }
            ],
            "links": {
              "self": "/users/4/relationships/citizenships",
              "related": {
                "countries": {
                  "href": "/countries?filter[id]=NO,US",
                  "describedby": "https://github.com/MoonWorm/jsonapi4j/tree/main/schemas/oas-schema-to-many-relationships-related-link.yaml",
                  "meta": {
                    "ids": ["NO", "US"]
                  }
                }
              }
            }
          }
        },
        "links": {
          "self": "/users/4"
        },
        "id": "4",
        "type": "users"
      }
    ],
    "links": {
      "self": "/users?include=citizenships&page%5Bcursor%5D=DoJu",
      "next": "/users?include=citizenships&page%5Bcursor%5D=DoJw"
    },
    "included": [
      {
        "attributes": {
          "name": "Norway",
          "region": "Europe"
        },
        "links": {
          "self": "/countries/NO"
        },
        "id": "NO",
        "type": "countries"
      },
      {
        "attributes": {
          "name": "Finland",
          "region": "Europe"
        },
        "links": {
          "self": "/countries/FI"
        },
        "id": "FI",
        "type": "countries"
      },
      {
        "attributes": {
          "name": "United States",
          "region": "Americas"
        },
        "links": {
          "self": "/countries/US"
        },
        "id": "US",
        "type": "countries"
      }
    ]
  }
  ```
</details>

## What's next
- Refer [jsonapi4j-sampleapp](https://github.com/MoonWorm/jsonapi4j-sampleapp) to get more insights and inspiration
- Implement `placeOfBirth` relationship that connects a particular 'user' with a 'country'. Unlike `citizenships` - this relationship must implement `ToOneRelationship` since every person can have only one place of birth. 
- Explore more operations by implementing: 
  - Resource operations: 
    - `CreateResourceOperation` e.g. `POST /users (/w payload)` for creating a new resources
    - `UpdateResourceOperation` e.g. `PATCH /users/123 (/w payload)` for updating the existing resources
    - `DeleteResourceOperation`, e.g. `DELETE /uesrs/123` for deletion of the existing resources
  - Relationship operations:
    - `UpdateToOneRelationshipOperation` e.g. `PATCH /users/123/citizenships/placeOfBirth` for updating/removal of the existing to-one relationship linkages
    - `UpdateToManyRelationshipOperation` e.g. `PATCH /users/123/citizenships/citizenships` for updating/removal of the existing to-many relationship linkages
- Implement some other filters for `ReadMultipleResourcesOperation` and soring options operations, for example 'read countries by region' 
- [Explore](https://github.com/MoonWorm/jsonapi4j#access-control) authentication, authorization, and anonymization capabilities if you need a fain grained mechanism of which data is visible based on access tier, OAuth2 scopes, and resource ownership
- [Explore](https://github.com/MoonWorm/jsonapi4j#openapi-specification) how to tune your [OpenAPI Specification](https://swagger.io/specification/)
- Find out how multi-level-includes work for the [Compound Documents](https://github.com/MoonWorm/jsonapi4j#compound-documents)
- Add more validations
- Tune performance by using batch read relationship operations, custom executor service, tuning some jsonApi4j properties 
- Try to fork, submit a PR or create a ticket if you've found any issues or just have any recommendations

## Access Control

### Evaluation stages 

Access control evaluation is executed twice for request lifecycle - for **inbound** and **outbound** stage. 

![Access Control Evaluation Stages](/docs/access-control-evaluation-stages-medium.png)

During the **inbound** stage JsonApi4j application just received a request, but hasn't triggered data fetching from a downstream data source. Access control rules are evaluated for `JsonApiRequest` since there no other data available yet. 
If access control requirements are not met there will be no any further data fetching stages and **data** field will be fully anonymized. 

**Outbound** stage is executed after gathering data from a data source, composing response document, and right before sending it to the client. Access control rules are evaluated for each resource/resource identifier withing a generated JSON:API Document. Resource documents usually contain full [JSON:API Resource Objects](https://jsonapi.org/format/#document-resource-objects) while Relationship documents consist of [Resource Identifier Objects](https://jsonapi.org/format/#document-resource-identifier-objects) only.
In case of **Resource Documents** access control requirements can be set for either: 
- Entire JSON:API Resource. If access control requirements are not met - entire resource will be anonymized.
- Any member of the JSON:API Resource (e.g. 'attributes', 'meta'). If access control requirements are not met - only this particular field will be anonymized.
- Entire 'attributes' member of the JSON:API Resource. If access control requirements are not met - entire 'attributes' section will be anonymized.
- Any member of the 'attributes' abject. If access control requirements are not met - only this particular field will be anonymized.
- Any relationship. If access control requirements are not met for the relationship - relationship data fetching process will not be triggered and the relationship data will be anonymized.

In case of **Relationship Documents** access control requirements can be set for either:
- Entire JSON:API Resource Identifier object. If access control requirements are not met - entire resource identifier will be anonymized.
- Any member of the JSON:API Resource Identifier (e.g. 'meta'). If access control requirements are not met - only this particular field will be anonymized.

By default, JsonApi4j allows everything (no Access Control evaluations), but it's always possible to enforce rules for either both or just one of these stage. 

### Access Control Requirements

There are four requirements that can be assigned in any combination:
- **Authentication requirement** - checks if request is sent on behalf of authenticated client/user. Can be used to restrict anonymous access.
- **Access tier requirement** - checks whether the client/user that originated the request belongs to a particular group e.g. 'Admin', 'Internal API consumers', 'Public API consumers'. This helps to organize access to your APIs based on so-called tiers.
- **OAuth2 Scope(s) requirement** - checks if request was authorised to access user data protected by a certain OAuth2 scope(s). Usually, this information is carried within JWT Access Token.
- **Ownership requirement** - checks if requested resource belongs to a client/user that triggered this request. This is used for those APIs where user can view only its own data, but not others data.

If any of specified requirements are not met - the marked section or the entire object will be anonymized.

### Setting Principal Context

By default, the framework uses `DefaultPrincipalResolver` which relies on the next HTTP headers in order to resolve the current auth context: 

1. `X-Authenticated-User-Id` - to check if request is sent on behalf of authenticated client/user, considers as true if not null/blank. Is also used for ownership checks.
2. `X-Authenticated-Client-Access-Tier` - for principal's Access Tier. By default, supports the next values: 'NO_ACCESS', 'PUBLIC', 'PARTNER', 'ADMIN', 'ROOT_ADMIN'. It's possible to declare your own tiers by implementing `AccessTierRegistry`.  
3. `X-Authenticated-User-Granted-Scopes` - for getting OAuth2 Scopes which user has granted the client, space-separated string

It is also possible to implement your own `PrincipalResolver` that tells the framework how to retrieve Principal-related info from an incoming HTTP request. 

Later, the framework will use this info for Inbound/Outbound evaluations.

### Setting Access Requirements

How and where to declare your Access Control requirements? 

There are two main approaches: 
1. Via Java annotations. If you are working with **jsonapi4j-core** it's possible to place Access Control annotations on either a custom `ResourceObject`, or a custom `Attributes` object. Annotations can be placed both on class and field levels. If you're working with modules that operates higher abstractions - **jsonapi4j-rest** or **jsonapi4j-rest-springboot** - you can place annotations only for an Attributes Object. Here is the list of annotations that can be used: `@AccessControlAuthenticated`, `@AccessControlScopes`, `@AccessControlAccessTier`, `@AccessControlOwnership`. This approach is preferable for setting Access Control requirements for Attributes.
2. Via **JsonApi4j** plugin system. You can use `OperationInboundAccessControlPlugin` plugin for your Operations - that will be used for the Inbound Access Control evaluations. `ResourceOutboundAccessControlPlugin` can be used for the `Resource` implementations and be applied for JSON:API Resource Objects during the Outbound Access Control evaluations. `RelationshipsOutboundAccessControlPlugin` can be used for the `Relationship` implementations and be applied for JSON:API Resource Identifier Objects during the Outbound Access Control evaluations. This is approach is preferable for all other cases.

If the system detects a mix of settings it merges them giving priority to ones that were set programmatically via Plugins.  

### Examples

Example 1: Outbound Access Control

Let's hide user's credit card number for everyone but the owner. By achieving that `@AccessControlOwnership(ownerIdFieldPath = "id")` must be placed on top of `creditCardNumber` field.
We can also put `@AccessControlAuthenticated` to ensure the user is authenticated and `@AccessControlScopes(requiredScopes = {"users.sensitive.read"})` if we want to protect access to this field by checking whether the client has gotten a user grant for this data.  

```java
public class UserAttributes {
    
    private final String firstName;
    private final String lastName;
    private final String email;
    
    @AccessControlAuthenticated
    @AccessControlScopes(requiredScopes = {"users.sensitive.read"})
    @AccessControlOwnership(ownerIdFieldPath = "id")
    private final String creditCardNumber;
    
    // constructors, getters and setters

}
```

Example 2: Inbound Access Control

Let's only allow a new user creation for the admin clients. 

```java
@Component
public class CreateUserOperation implements CreateResourcesOperation<UserDbEntity> {

    // methods implementations

    @Override
    public List<OperationPlugin<?>> plugins() {
      return List.of(
        OperationInboundAccessControlPlugin.builder()
          .requestAccessControl(
            AccessControlRequirements.builder()
              .requiredAccessTier(
                AccessControlAccessTierModel.builder()
                  .requiredAccessTier(TierAdmin.ADMIN_ACCESS_TIER)
                  .build()
              )
              .build()
          )
          .build()
      );
    }

}
```

## OpenAPI Specification

Since JSON:API has predetermined list of operations and schemas Open API Spec generation can be fully automated. 

JsonApi4j can generate an instance of `io.swagger.v3.oas.models.OpenApi` model and then expose it either through a Maven Exec Plugin or via dedicated endpoint.

Here is two ways of how to generate an Open API Specification for you APIs:

1. Access via HTTP endpoint. By default, you can access either JSON or YAML version of the Open API Specification by accessing [/jsonapi/oas](http://localhost:8080/jsonapi/oas) endpoint. It supports 'format' query parameter that can be either 'json' or 'yaml'. Always fallbacks to JSON format. 
2. Via Maven Exec Plugin. TBD

By default, JsonApi4j generate all schemas and operations for you. But if you need to enrich it with more data e.g. 'info', 'components' -> 'securitySchemes' or custom HTTP headers you need to explicitly configure that in `JsonApi4jProperties` ('oas' section) via `application.yaml` if you're using 'jsonapi4j-rest-springboot' or via proper `JsonApi4jServletContainerInitializer` bootstrapping if you're relying on Servlet API only from 'jsonapi4j-rest'.

## Compound documents

[Compound Documents](https://jsonapi.org/format/#document-compound-documents) is a part of JSON:API specification that describes the way to include related resources in one request. For example, if you want to request some 'users' you can also ask the server to include related resources to these users. It's worth mentioning that you can only ask for those resources that enabled via relationships. All resolved resourced are placed as a flat structure into a top-level "included" field. In order to request related resources "include" query parameter must be used, for example `/users?page[cursor]=xxx&include=citizenships`. 

It is allowed to request multiple relationships in one go - just specify relationship names using comma ',' as a separator, for example `include=citizenships,placeOfBirth`

Compound Documents feature also supports multi-level relationship resolution. That means that client can request a chain of relationships, f.e. `include=placeOfBirth.currency`. The relationships sequence is a dot-separated string that must be a valid chain of relationships - meaning they must exist for the resources on each stage. This particular example would trigger the process that resolves related resources in two stages - firstly, JsonApi4j will resolve 'placeOfBirth' relationship which is represented by Country resource. Then, as a second stage, the framework will resolve 'currency' of the previously resolved countries. 'currency' relationship must exist for Country resource. 

Since every level generates a new wave of requests it's important to remember that and use these feature carefully. JsonApi4j relies on batch operations (e.g. `filter[id]=1,2,3,4,5`) that's why it's important to implement this operation for all resources that can be requested as someone's relationship. If the operation is not implemented the framework tries to fallback on sequential 'read by id' operation if it exists. 

Let's define what does resolution stage means in terms of how framework resolves Compound Documents. For example, `include=citizenships,placeOfBirth.currency` would be parsed into two stages - first stage includes 'citizenships' and 'placeOfBirth' relationships. The second stage includes 'currency' relationship. Within each stage the framework groups all related resources by their types and associated list of identifiers and sends as many parallel request as many resource types were detected. 

In order to be able to control the amount of these extra requests the framework provides some settings and guardrails to control the limits. Refer `CompoundDocsProperties` for more details, for example `maxHops` settings allows to define how many levels your system supposed to support. 

Compound Documents resolver is part of a dedicated module 'jsonapi4j-compound-docs-resolver'. By default, this feature is disabled on the application server, but it can be enabled by setting `enabled` property to `true`. Since the logic is part of a separate independent module it opens multiple options where to host this logic. There are at least two the most obvious options: on the same application server or on the API Gateway level. 

- Compound docs works as a post processor. First main request is executed.
- Sequence diagram - stages
- Point the difference in 'includes' for Primary Resources and Relationship requests (how relationship request refers self).
- CacheControlPropagator examples, how to configure an external Cache that relies on HTTP Cache Control headers

## Register custom error handlers
- Example of how to declare a custom error handler

## JSON:API Specification deviations

1. JsonApi4j encourages flat resource structure e.g. '/users' and '/articles' instead of '/users/{userId}/articles'. This approach fully automates default 'links' generation and enables the gates for automatic Compound Documents resolution.
2. No support for [Sparse Fieldsets](https://jsonapi.org/format/#fetching-sparse-fieldsets) (maybe later)
3. No support for [client generated ids](https://jsonapi.org/format/#document-resource-object-identification) ('lid') -> use 'id' field and set client-generated id there.
4. JSON:API spec is agnostic about the pagination strategy (e.g. 'page[number]' and 'page[size]' for limit-offset), while the framework encourages Cursor pagination ('page[cursor]')
5. Doesn't support JSON:API Profiles and Extensions (maybe later)
6. Default relationships concept, no 'relationships'->'{relName}'->'data' resolution by default. This is done to have more control under extra +N requests per each existing relationship
7. The framework enforces the requirement for implementing either 'Filter By Id' ('/users?filter[id]=123') operation or 'Read By Id' ('/users/123') operation because Compound Docs Resolver uses them to compose 'included' section.

## Contributing 

I welcome issues and pull requests! See [CONTRIBUTING.md](./CONTRIBUTING.md) for details.

## License 

This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details.