package pro.api4.jsonapi4j.servlet.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class SparseFieldsetsResponseFilterTests {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void apply_filtersPrimaryAndIncludedResourcesByType() throws Exception {
        String responseBody = """
                {
                  "data": {
                    "id": "1",
                    "type": "users",
                    "attributes": {
                      "fullName": "John Doe",
                      "email": "john@doe.com"
                    },
                    "relationships": {
                      "placeOfBirth": {
                        "data": {
                          "id": "US",
                          "type": "countries"
                        }
                      },
                      "relatives": {
                        "data": []
                      }
                    }
                  },
                  "included": [
                    {
                      "id": "US",
                      "type": "countries",
                      "attributes": {
                        "name": "United States",
                        "region": "Americas"
                      },
                      "relationships": {
                        "currencies": {
                          "data": [
                            {
                              "id": "USD",
                              "type": "currencies"
                            }
                          ]
                        }
                      }
                    }
                  ]
                }
                """;

        String filtered = SparseFieldsetsResponseFilter.apply(
                responseBody,
                Map.of(
                        "users", Set.of("fullName", "placeOfBirth", "unknownUserField"),
                        "countries", Set.of("name", "currencies"),
                        "currencies", Set.of("symbol")
                ),
                OBJECT_MAPPER
        );

        JsonNode filteredNode = OBJECT_MAPPER.readTree(filtered);
        JsonNode primaryData = filteredNode.get("data");
        assertThat(primaryData.path("attributes").has("fullName")).isTrue();
        assertThat(primaryData.path("attributes").has("email")).isFalse();
        assertThat(primaryData.path("relationships").has("placeOfBirth")).isTrue();
        assertThat(primaryData.path("relationships").has("relatives")).isFalse();

        JsonNode includedCountry = filteredNode.path("included").get(0);
        assertThat(includedCountry.path("attributes").has("name")).isTrue();
        assertThat(includedCountry.path("attributes").has("region")).isFalse();
        assertThat(includedCountry.path("relationships").has("currencies")).isTrue();
    }

    @Test
    public void apply_withNoSparseFieldsets_returnsOriginal() {
        String responseBody = "{\"data\":{\"id\":\"1\",\"type\":\"users\"}}";

        String filtered = SparseFieldsetsResponseFilter.apply(
                responseBody,
                Map.of(),
                OBJECT_MAPPER
        );

        assertThat(filtered).isEqualTo(responseBody);
    }
}
