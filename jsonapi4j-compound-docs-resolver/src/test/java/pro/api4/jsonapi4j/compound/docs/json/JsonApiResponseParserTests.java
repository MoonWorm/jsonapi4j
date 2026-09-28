package pro.api4.jsonapi4j.compound.docs.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.compound.docs.ResourceUtil;
import pro.api4.jsonapi4j.compound.docs.exception.InvalidJsonApiResponseException;
import pro.api4.jsonapi4j.processor.IdAndType;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pro.api4.jsonapi4j.compound.docs.IdAndTypeFixtures.idAndType;

public class JsonApiResponseParserTests {

    private final JsonApiResponseParser sut = new JsonApiResponseParser(new ObjectMapper());

    @Nested
    class ParsePrimaryResourceDoc {

        @Test
        public void parsePrimaryResourceDoc_multipleCountriesWithOneTwoEmptyOrNullCurrencies_mergesLinkagePerRelationship() {
            String originalResponse = ResourceUtil.readResourceFile("pro/api4/jsonapi4j/compound/docs/multiple-countries-response.json");

            Map<String, Set<IdAndType>> actualResult = sut.parsePrimaryResourceDoc(originalResponse).relationships();

            assertThat(actualResult).isEqualTo(Map.of(
                    "currencies", Set.of(
                            idAndType("currencies", "XOF"),
                            idAndType("currencies", "EUR"),
                            idAndType("currencies", "USD")
                    )
            ));
        }

        @Test
        public void parsePrimaryResourceDoc_resourceObjects_returnsEachPrimaryResourceWithItsLinkage() {
            String response = """
                    {"data":[
                      {"type":"users","id":"1","relationships":{"relatives":{"data":[{"type":"users","id":"2"}]}}},
                      {"type":"users","id":"2"}]}""";

            List<PrimaryResource> actualResult = sut.parsePrimaryResourceDoc(response).primaryResources();

            assertThat(actualResult).extracting(r -> r.linkage().idAndType())
                    .containsExactly(idAndType("users", "1"), idAndType("users", "2"));
            assertThat(actualResult.get(0).linkage().relationships())
                    .isEqualTo(Map.of("relatives", Set.of(idAndType("users", "2"))));
            assertThat(actualResult.get(1).json()).isEqualTo("{\"type\":\"users\",\"id\":\"2\"}");
        }

        @Test
        public void parsePrimaryResourceDoc_nullData_returnsNoRelationships() {
            String originalResponse = ResourceUtil.readResourceFile("pro/api4/jsonapi4j/compound/docs/null-data-response.json");

            assertThat(sut.parsePrimaryResourceDoc(originalResponse).relationships()).isEmpty();
        }

        @Test
        public void parsePrimaryResourceDoc_missingData_returnsNoRelationships() {
            String originalResponse = ResourceUtil.readResourceFile("pro/api4/jsonapi4j/compound/docs/missing-data-response.json");

            assertThat(sut.parsePrimaryResourceDoc(originalResponse).relationships()).isEmpty();
        }

        @Test
        public void parsePrimaryResourceDoc_emptyResponse_throwsInvalidJsonApiResponseException() {
            assertThatThrownBy(() -> sut.parsePrimaryResourceDoc(""))
                    .isInstanceOf(InvalidJsonApiResponseException.class);
        }

        @Test
        public void parsePrimaryResourceDoc_nullResponse_throwsInvalidJsonApiResponseException() {
            assertThatThrownBy(() -> sut.parsePrimaryResourceDoc(null))
                    .isInstanceOf(InvalidJsonApiResponseException.class);
        }

    }

    @Nested
    class ParseRelationshipDoc {

        @Test
        public void parseRelationshipDoc_toManyLinkage_returnsLinkageUnderRelationshipName() {
            String response = """
                    {"data":[{"type":"countries","id":"NO"},{"type":"countries","id":"FI"}]}""";

            Map<String, Set<IdAndType>> actualResult = sut.parseRelationshipDoc(response, "citizenships").relationships();

            assertThat(actualResult).isEqualTo(Map.of(
                    "citizenships", Set.of(idAndType("countries", "NO"), idAndType("countries", "FI"))
            ));
        }

        @Test
        public void parseRelationshipDoc_identifiers_returnsNoPrimaryResources() {
            String response = """
                    {"data":[{"type":"countries","id":"NO"}]}""";

            assertThat(sut.parseRelationshipDoc(response, "citizenships").primaryResources()).isEmpty();
        }

        @Test
        public void parseRelationshipDoc_nullData_returnsNoRelationships() {
            assertThat(sut.parseRelationshipDoc("{\"data\":null}", "placeOfBirth").relationships()).isEmpty();
        }

    }

    @Nested
    class ParseResource {

        @Test
        public void parseResource_resourceWithRelationships_returnsIdentityAndLinkage() {
            String resource = """
                    {"type":"users","id":"1","relationships":{
                      "placeOfBirth":{"data":{"type":"countries","id":"US"}},
                      "relatives":{"data":[]},
                      "citizenships":{"links":{"self":"/users/1/relationships/citizenships"}}}}""";

            ResourceLinkage actualResult = sut.parseResource(resource);

            assertThat(actualResult.idAndType()).isEqualTo(idAndType("users", "1"));
            assertThat(actualResult.relationships()).isEqualTo(Map.of(
                    "placeOfBirth", Set.of(idAndType("countries", "US"))
            ));
        }

        @Test
        public void parseResource_missingId_returnsNullIdentity() {
            assertThat(sut.parseResource("{\"type\":\"users\"}").idAndType()).isNull();
        }

    }

}
