## Cookbook application to use JsonApi4J with Servlet Container

# Running application

Application starts embedded Jetty on port 8080 with command:
```
$ mvn exec:java
```

Query data:
```
$ curl -s -X GET 'http://localhost:8080/jsonapi/recipes' | jq .
{
  "links": {
    "self": "/recipes"
  },
  "data": [
    {
      "id": "cheese-sandwich",
      "type": "recipes",
      "attributes": {
        "name": "cheese-sandwich",
        "instructions": [
          "Spread evenly butter on bread",
          "Put slice of cheese on top"
        ]
      },
      "links": {
        "self": "/recipes/cheese-sandwich"
      }
    },
    {
      "id": "pelmeni",
      "type": "recipes",
      "attributes": {
        "name": "pelmeni",
        "instructions": [
          "Boil water in a pot",
          "Add pelmeni into boiling water",
          "Add salt",
          "Cook for 10 minutes",
          "Serve pelmeni with butter or smetana"
        ]
      },
      "links": {
        "self": "/recipes/pelmeni"
      }
    }
  ]
}
```
