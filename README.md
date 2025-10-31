[![Build](https://github.com/moonworm/jsonapi4j/actions/workflows/build.yml/badge.svg)](https://github.com/moonworm/jsonapi4j/actions/workflows/build.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/pro.api4/jsonapi4j.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/pro.api4/jsonapi4j)
[![Last Commit](https://img.shields.io/github/last-commit/moonworm/jsonapi4j)](https://img.shields.io/github/last-commit/moonworm/jsonapi4j)
[![codecov](https://codecov.io/gh/moonworm/jsonapi4j/branch/main/graph/badge.svg)](https://codecov.io/gh/moonworm/jsonapi4j)
[![Issues](https://img.shields.io/github/issues/moonworm/jsonapi4j)](https://github.com/moonworm/jsonapi4j/issues)
[![License](https://img.shields.io/github/license/moonworm/jsonapi4j)](LICENSE)

![Logo](/docs/jsonapi4j-logo-medium.png)

# Introduction

Welcome to **JsonApi4j** — a lightweight API framework for Java for building [JSON:API](https://jsonapi.org/format/)-compliant APIs with minimal configuration.

There are some **application examples** available in [examples/](https://github.com/MoonWorm/jsonapi4j/tree/main/examples) folder. Please check them out for more insights on how to use the framework.

Detailed **documentation** is available [here](https://moonworm.github.io/jsonapi4j/).

# Quick start

Here is a step-by-step guide of how to integrate JsonApi4j framework into your [Spring Boot](https://spring.io/projects/spring-boot) application.

## 1. Add Dependency (Maven)
```xml
<dependency>
  <groupId>pro.api4</groupId>
  <artifactId>jsonapi4j-rest-springboot</artifactId>
  <version>${jsonapi4j.version}</version>
</dependency>
```

It's supposed to be available in Maven Central. Check for the latest versions [here](https://central.sonatype.com/artifact/pro.api4/jsonapi4j-rest-springboot).

## 2. Declare your first JSON:API resource and related classes

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

## 3. Declare your first JSON:API operation (read all users): 

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

## 4. Adding your first JSON:API relationship

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

## 5. Add missing relationship operations

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

## 6. Request/response examples

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

# Contributing 

I welcome issues and pull requests! See [CONTRIBUTING.md](./CONTRIBUTING.md) for details.

# License 

This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details.