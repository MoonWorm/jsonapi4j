package pro.api4.jsonapi4j.compound.docs.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import pro.api4.jsonapi4j.compound.docs.ResourceUtil;
import pro.api4.jsonapi4j.compound.docs.exception.InvalidJsonApiResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JsonApiResponseParserTests {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    static {
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private JsonApiResponseParser sut = new JsonApiResponseParser(MAPPER);

    @Test
    public void parsePrimaryResourceDoc_multipleCountriesWithOneTwoEmptyOrNullCurrencies_checkGroupingIsDoneProperly() {
        // given
        String originalResponse = ResourceUtil.readResourceFile("pro/api4/jsonapi4j/compound/docs/multiple-countries-response.json");

        // when
        Map<String, Set<String>> actualResult = sut.parsePrimaryResourceDoc(originalResponse).typeToIdsMap();

        // then
        assertThat(actualResult).isNotNull().isNotEmpty().isEqualTo(Map.of(
                "currencies", Set.of("XOF", "EUR", "USD")
        ));
    }

    @Test
    public void parsePrimaryResourceDoc_nullData_checkResultIsEmpty() {
        // given
        String originalResponse = ResourceUtil.readResourceFile("pro/api4/jsonapi4j/compound/docs/null-data-response.json");

        // when
        Map<String, Set<String>> actualResult = sut.parsePrimaryResourceDoc(originalResponse).typeToIdsMap();

        // then
        assertThat(actualResult).isNotNull().isEmpty();
    }

    @Test
    public void parsePrimaryResourceDoc_missingData_checkExceptionIsThrown() {
        // given
        String originalResponse = ResourceUtil.readResourceFile("pro/api4/jsonapi4j/compound/docs/missing-data-response.json");

        // when - then
        assertThatThrownBy(() -> sut.parsePrimaryResourceDoc(originalResponse)).isInstanceOf(InvalidJsonApiResponse.class);
    }

    @Test
    public void parsePrimaryResourceDoc_emptyResponse_checkExceptionIsThrown() {
        // given
        String originalResponse = "";

        // when - then
        assertThatThrownBy(() -> sut.parsePrimaryResourceDoc(originalResponse)).isInstanceOf(InvalidJsonApiResponse.class);
    }

    @Test
    public void parsePrimaryResourceDoc_nullResponse_checkExceptionIsThrown() {
        // given
        String originalResponse = null;

        // when - then
        assertThatThrownBy(() -> sut.parsePrimaryResourceDoc(originalResponse)).isInstanceOf(InvalidJsonApiResponse.class);
    }

}
