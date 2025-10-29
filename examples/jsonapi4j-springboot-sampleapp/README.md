# JsonApi4j Spring Boot Sample App

This is a sample app of how to use JsonApi4j with Spring Boot

## Domain

The app implements an imaginable and very simple domain:

![Domain graph](../docs/domain-graph.png)

## How to run

`mvn -f examples/jsonapi4j-springboot-sampleapp spring-boot:run`

## Examples

1. Reads first page of users: http://localhost:8080/jsonapi/users 
2. Reads the next page of users: http://localhost:8080/jsonapi/users?page[cursor]=DoJu
3. Reads a single user by id: http://localhost:8080/jsonapi/users/3
4. Reads user's 'citizenships' (to-many) resource linkages:  http://localhost:8080/jsonapi/users/1/relationships/citizenships
5. Reads user's 'citizenships' (to-many) resource linkages (next page):  http://localhost:8080/jsonapi/users/1/relationships/citizenships?page[cursor]=DoJu
6. Reads user's 'placeOfBirth' (to-one) resource linkage:  http://localhost:8080/jsonapi/users/3/relationships/placeOfBirth
7. Reads user's 'relatives' (to-many) resource linkages:  http://localhost:8080/jsonapi/users/5/relationships/relatives
8. Reads two users with related resources through multiple relationships: 'citizenships', 'placeOfBirth', and 'relatives': http://localhost:8080/jsonapi/users?filter[id]=1,4&include=citizenships,placeOfBirth,relatives
9. Reads a single user with related resources through multi-level relationships: 'citizenships' and 'citizenships.currencies': http://localhost:8080/jsonapi/users/1?include=citizenships.currencies
