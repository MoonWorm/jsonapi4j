---
title: "Getting Started"
permalink: /getting-started/
---

Let's take a quick look at what a typical **JsonApi4j**-based service looks like in code.

### 1. Add Dependency

<div class="tabs" markdown="0">
  <div class="tab-buttons">
    <button class="tab-btn active" data-tab="tab-springboot">Spring Boot</button>
    <button class="tab-btn" data-tab="tab-quarkus">Quarkus</button>
    <button class="tab-btn" data-tab="tab-servlet">Servlet API</button>
  </div>
  <div id="tab-springboot" class="tab-panel active">
    <p>If you want to integrate <strong>JsonApi4j</strong> into a clean or existing <a href="https://spring.io/projects/spring-boot">Spring Boot</a> application, add:</p>
    <div class="language-xml highlighter-rouge"><div class="highlight"><pre class="highlight"><code><span class="nt">&lt;dependency&gt;</span>
  <span class="nt">&lt;groupId&gt;</span>pro.api4<span class="nt">&lt;/groupId&gt;</span>
  <span class="nt">&lt;artifactId&gt;</span>jsonapi4j-rest-springboot<span class="nt">&lt;/artifactId&gt;</span>
  <span class="nt">&lt;version&gt;</span>${jsonapi4j.version}<span class="nt">&lt;/version&gt;</span>
<span class="nt">&lt;/dependency&gt;</span></code></pre></div></div>
  </div>
  <div id="tab-quarkus" class="tab-panel">
    <p>For <a href="https://quarkus.io/">Quarkus</a> app — use:</p>
    <div class="language-xml highlighter-rouge"><div class="highlight"><pre class="highlight"><code><span class="nt">&lt;dependency&gt;</span>
  <span class="nt">&lt;groupId&gt;</span>pro.api4<span class="nt">&lt;/groupId&gt;</span>
  <span class="nt">&lt;artifactId&gt;</span>jsonapi4j-rest-quarkus<span class="nt">&lt;/artifactId&gt;</span>
  <span class="nt">&lt;version&gt;</span>${jsonapi4j.version}<span class="nt">&lt;/version&gt;</span>
<span class="nt">&lt;/dependency&gt;</span></code></pre></div></div>
  </div>
  <div id="tab-servlet" class="tab-panel">
    <p>For custom web integrations or apps that run on Servlet API:</p>
    <div class="language-xml highlighter-rouge"><div class="highlight"><pre class="highlight"><code><span class="nt">&lt;dependency&gt;</span>
  <span class="nt">&lt;groupId&gt;</span>pro.api4<span class="nt">&lt;/groupId&gt;</span>
  <span class="nt">&lt;artifactId&gt;</span>jsonapi4j-rest<span class="nt">&lt;/artifactId&gt;</span>
  <span class="nt">&lt;version&gt;</span>${jsonapi4j.version}<span class="nt">&lt;/version&gt;</span>
<span class="nt">&lt;/dependency&gt;</span></code></pre></div></div>
  </div>
</div>

The framework modules are published to Maven Central. You can find the latest available versions [here](https://mvnrepository.com/artifact/pro.api4).

### 2. Declare the Domain

Let's implement a simple application that exposes two resources - `users` and `countries` - and defines a relationship between them, representing which `citizenships` (or passports) each user holds.

<div class="mermaid">
graph LR
    users((users)) -- "citizenships (1-N)" --> countries((countries))
</div>

Then, let's implement a few operations for these resources - reading multiple users and countries by their IDs, and retrieving which citizenships each user has.

### 3. Define the JSON:API Resource for Users

As mentioned above, let's start by defining our first JSON:API resource - `user` resource.

```java
@JsonApiResource(resourceType = "users")
public class UserResource implements Resource<UserDbEntity> {

    @Override
    public String resolveResourceId(UserDbEntity userDbEntity) {
        return userDbEntity.getId();
    }

    @Override
    public UserAttributes resolveAttributes(UserDbEntity userDbEntity) {
        return new UserAttributes(
                userDbEntity.getFirstName() + " " + userDbEntity.getLastName(),
                userDbEntity.getEmail(),
                userDbEntity.getCreditCardNumber()
        );
    }
}
```

What's happening here:
* `@JsonApiResource(resourceType = "users")` defines a unique resource type name (`users` in this case). Each resource in your API must have a distinct type.
* `String resolveResourceId(UserDbEntity userDbEntity)` returns the unique identifier for this resource, must be unique across all resources of this type.
* `UserAttributes resolveAttributes(UserDbEntity userDbEntity)` - (optional) maps internal domain data (`UserDbEntity`) to the public API-facing representation (`UserAttributes`)

Each resource is parametrized with a type:
* `UserDbEntity` - is represented internally.

While `UserAttributes` represents what is exposed via API.

Here's a draft implementation of both classes:

```java
public class UserAttributes {

    private final String firstName;
    private final String lastName;
    private final String email;
    private final String creditCardNumber;

    // constructors, getters and setters

}
```

```java
public class UserDbEntity {

    private final String id;
    private final String fullName;
    private final String email;
    private final String creditCardNumber;

    // constructors, getters and setters

}
```

Internal models (like `UserDbEntity` in this case) often differ from `UserAttributes`. They may encapsulate database-specific details (for example, a Hibernate entity or a JOOQ record), represent a DTO from an external service, or even aggregate data from multiple sources.

### 4. Declare the JSON:API Operation — Read Multiple Users

Now that we've defined our resource and attributes, let's implement the first operation to read all users.
This operation will be available under `GET /users`.

```java
@JsonApiResourceOperation(resource = UserResource.class)
public class UserOperations implements ResourceOperations<UserDbEntity> {

    private final UserDb userDb;

    public UserOperations(UserDb userDb) {
        this.userDb = userDb;
    }

    @Override
    public PaginationAwareResponse<UserDbEntity> readPage(JsonApiRequest request) {
        UserDb.DbPage<UserDbEntity> pagedResult = userDb.readAllUsers(request.getCursor());
        return PaginationAwareResponse.cursorAware(
                pagedResult.getEntities(),
                pagedResult.getCursor()
        );
    }

}
```

* `@JsonApiResourceOperation(resource = UserResource.class)` - identify which resource this operation belongs to (`users`).

`PaginationAwareResponse.cursorAware()` wraps the result with cursor-based pagination metadata. The framework uses this to generate pagination links in the response. For more on pagination strategies (cursor vs limit-offset), see the [Pagination](/pagination/) page.

The `UserDb` class doesn't depend on any **JsonApi4j**-specific interfaces or components — it simply represents your data source.
In a real application, this could be an ORM entity manager, a JOOQ repository, a REST client, or any other persistence mechanism.
For the sake of this demo, here's a simple in-memory implementation to support the operation above:

```java
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

You can now run your application (for example, on port `8080` by setting Spring Boot's property to `server.port=8080`) and send the following HTTP request: [/users?page[cursor]=DoJu](http://localhost:8080/jsonapi/users?page[cursor]=DoJu).

And then you should receive a paginated, JSON:API-compliant response such as:
```json
{
  "data": [
    {
      "attributes": {
        "fullName": "Jack Doe",
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
        "fullName": "Jessy Doe",
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

Try to remove `page[cursor]=xxx` query parameter - it will just start reading user resources from the very beginning.

### 5. Define the JSON:API Resource for Countries

Similar to the `users` resource, we need to declare a dedicated JSON:API resource representing a `citizenship` - in this case, a resource of type `country`.

```java
@JsonApiResource(resourceType = "countries")
public class CountryResource implements Resource<DownstreamCountry> {

    @Override
    public String resolveResourceId(DownstreamCountry downstreamCountry) {
        return downstreamCountry.getCca2();
    }

    @Override
    public CountryAttributes resolveAttributes(DownstreamCountry downstreamCountry) {
        return new CountryAttributes(
                downstreamCountry.getName().getCommon(),
                downstreamCountry.getRegion()
        );
    }

}
```

This resource is parametrized with a type: `DownstreamCountry`.

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

And here is a custom `CountryAttributes` that represents an API-facing version of a country:
```java
public class CountryAttributes {

    private final String name;
    private final String region;

    // constructors, getters and setters

}
```

In this example, we expose only the `name` and `region` fields through the **attributes**, using `.getName().getCommon()` for the country name. While `cca2` is used as a country ID.

### 6. Add a JSON:API Relationship - User Citizenships

Now that we've defined our first resources, let's establish a relationship between them.

We'll define a relationship called `citizenships` between the `UserJsonApiResource` and `CountryJsonApiResource`.
Each user can have multiple `citizenships`, which makes this a **to-many** relationship (represented by an array of resource identifier objects).

To implement this, we'll create a class that implements the `ToManyRelationship` interface:

```java
@JsonApiRelationship(relationshipName = "citizenships", parentResource = UserResource.class)
public class UserCitizenshipsRelationship implements ToManyRelationship<DownstreamCountry> {

    @Override
    public String resolveResourceIdentifierType(DownstreamCountry downstreamCountry) {
        return "countries";
    }

    @Override
    public String resolveResourceIdentifierId(DownstreamCountry downstreamCountry) {
        return downstreamCountry.getCca2();
    }

}
```

* `@JsonApiRelationship(relationshipName = "citizenships", parentResource = UserResource.class)` -  defines the name of the relationship (`citizenships`). Also identifies which resource this relationship belongs to (`users`).
* `ResourceType resolveResourceIdentifierType(DownstreamCountry downstreamCountry)` - determines the type of the related resource (`countries`). In some cases, a relationship may include multiple resource types - for example, a `userProperty` relationship could contain a mix of `cars`, `apartments`, or `yachts`.
* `String resolveResourceIdentifierId(DownstreamCountry downstreamCountry)` - resolves the unique identifier of each related resource (e.g., the country's CCA2 code).

### 7. Add the Missing Relationship Operation

The final piece of the puzzle is teaching the framework how to **resolve the declared relationship data**.

To do this, implement `ReadToManyRelationshipOperation<DownstreamCountry>` - this tells **JsonApi4j** how to find the related country resources (i.e., which passports or `citizenships` each user has).

```java
@JsonApiRelationshipOperation(relationship = UserCitizenshipsRelationship.class)
public class UserCitizenshipsOperations implements ToManyRelationshipOperations<UserDbEntity, DownstreamCountry> {

    private final CountriesClient client;
    private final UserDb userDb;

    public UserCitizenshipsOperations(CountriesClient client,
                                      UserDb userDb) {
        this.client = client;
        this.userDb = userDb;
    }


    @Override
    public PaginationAwareResponse<DownstreamCountry> readMany(JsonApiRequest request) {
        return PaginationAwareResponse.inMemoryCursorAware(
                client.readCountriesByIds(userDb.getUserCitizenships(request.getResourceId())),
                request.getCursor(),
                2 // set limit to 2
        );
    }

}
```

* `@JsonApiRelationshipOperation(relationship = UserCitizenshipsRelationship.class)` uniquely identify which resource and relationship this operation belongs to (`users` and `citizenships` accordingly).

* `CountriesClient` could be a Feign client representing a third-party API - for example, the [restcountries](https://restcountries.com/) service.
For simplicity, let's keep it local for now and simulate its behavior with an in-memory implementation:

```java
public class CountriesClient {

  private static final Map<String, DownstreamCountry> COUNTRIES = Map.of(
          "NO", new DownstreamCountry("NO", new Name("Norway", "Kingdom of Norway"), "Europe"),
          "FI", new DownstreamCountry("FI", new Name("Finland", "Republic of Finland"), "Europe"),
          "US", new DownstreamCountry("US", new Name("United States", "United States of America"), "Americas")
  );

  public List<DownstreamCountry> readCountriesByIds(List<String> countryIds) {
    return countryIds.stream().filter(COUNTRIES::containsKey).map(COUNTRIES::get).toList();
  }

}
```

We also need to extend our existing `UserDb` to include information about which countries each user holds passports from (identified by their CCA2 codes).
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

Finally, this operation will be available under [/users/1/relationships/citizenships](http://localhost:8080/jsonapi/users/1/relationships/citizenships).

### 8. Enable Compound Documents (Optional)

In order to support [JSON:API Compound Documents feature](https://jsonapi.org/format/#document-compound-documents) we must implement an operation that tells the framework how to read multiple resources by `id`. This allows the framework to resolve included resources efficiently when requested via the include query parameter.

While you could also implement an operation that reads a single resource by its `id`, this approach is less efficient because compound documents would be resolved sequentially, one by one, instead of using a single batch request via `filter[id]=x,y,z`.

```java
@JsonApiResourceOperation(resource = CountryResource.class)
public class CountryOperations implements ResourceOperations<DownstreamCountry> {

    private final CountriesClient client;

    public CountryOperations(CountriesClient client) {
        this.client = client;
    }

    @Override
    public PaginationAwareResponse<UserDbEntity> readPage(JsonApiRequest request) {
            return PaginationAwareResponse.fromItemsNotPageable(client.readCountriesByIds(request.getFilters().get(ID_FILTER_NAME)));
    }

}
```

* `@JsonApiResourceOperation(resource = CountryResource.class)` - identify which resource this operation belongs to (`countries`).

* `readPage(JsonApiRequest request)` - delegates to the already implemented `readCountriesByIds(...)`. For now, this operation only supports requests using `filter[id]=x,y,z`. Support for **read all** or additional filters (e.g., by **region**) can be added later if needed.

This operation will be available under [/countries?filter[id]=NO,FI,US](http://localhost:8080/jsonapi/countries?filter[id]=NO,FI,US).

Also, ensure Compound Docs feature is enabled:
```yaml
jsonapi4j:
  compound-docs:
    enabled: true
    maxHops: 3
```

Now we can finally start exploring some more exciting HTTP requests. Check out the [Request/Response Examples](/request-response-examples/) for hands-on examples!
