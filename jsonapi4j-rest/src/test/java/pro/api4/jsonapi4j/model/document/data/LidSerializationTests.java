package pro.api4.jsonapi4j.model.document.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class LidSerializationTests {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Test
    public void resourceIdentifierObject_withLid_isSerializedAndDeserialized() throws Exception {
        ResourceIdentifierObject identifierObject = new ResourceIdentifierObject(
                null,
                "tmp-user-1",
                "users",
                null
        );

        String json = OBJECT_MAPPER.writeValueAsString(identifierObject);
        assertThat(json).contains("\"lid\":\"tmp-user-1\"");
        assertThat(json).contains("\"type\":\"users\"");
        assertThat(json).doesNotContain("\"id\"");

        ResourceIdentifierObject deserialized = OBJECT_MAPPER.readValue(json, ResourceIdentifierObject.class);
        assertThat(deserialized.getId()).isNull();
        assertThat(deserialized.getLid()).isEqualTo("tmp-user-1");
        assertThat(deserialized.getType()).isEqualTo("users");
    }

    @Test
    public void resourceObject_withLid_isSerializedAndDeserialized() throws Exception {
        ResourceObject<LinkedHashMap<String, Object>, LinkedHashMap<String, Object>> resourceObject =
                new ResourceObject<>(
                        null,
                        "tmp-user-1",
                        "users",
                        new LinkedHashMap<>(),
                        new LinkedHashMap<>(),
                        null,
                        null
                );

        String json = OBJECT_MAPPER.writeValueAsString(resourceObject);
        assertThat(json).contains("\"lid\":\"tmp-user-1\"");
        assertThat(json).contains("\"type\":\"users\"");
        assertThat(json).doesNotContain("\"id\"");

        ResourceObject<?, ?> deserialized = OBJECT_MAPPER.readValue(json, ResourceObject.class);
        assertThat(deserialized.getId()).isNull();
        assertThat(deserialized.getLid()).isEqualTo("tmp-user-1");
        assertThat(deserialized.getType()).isEqualTo("users");
    }
}
