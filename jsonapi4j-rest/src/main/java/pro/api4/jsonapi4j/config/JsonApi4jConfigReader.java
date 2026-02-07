package pro.api4.jsonapi4j.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class JsonApi4jConfigReader {

    private final static ObjectMapper JSON_MAPPER = createJsonMapper();
    private final static ObjectMapper YAML_MAPPER = createYamlMapper();

    private static ObjectMapper createYamlMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        configureObjectMapper(mapper);
        return mapper;
    }

    private static ObjectMapper createJsonMapper() {
        ObjectMapper mapper = new ObjectMapper();
        configureObjectMapper(mapper);
        return mapper;
    }

    private static void configureObjectMapper(ObjectMapper mapper) {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public static ObjectMapper getYamlObjectMapper() {
        return YAML_MAPPER;
    }

    public static ObjectMapper getJsonObjectMapper() {
        return JSON_MAPPER;
    }

    public static JsonApi4jProperties readConfig(String path) throws IOException {
        try (InputStream is = JsonApi4jProperties.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Resource not found: " + path);
            }
            return getObjectMapper(path).readValue(is, JsonApi4jProperties.class);
        }
    }

    public static Map<String, Object> readConfigAsMap(String path) throws IOException {
        try (InputStream is = JsonApi4jProperties.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Resource not found: " + path);
            }
            return getObjectMapper(path).readValue(is, new TypeReference<>() {});
        }
    }

    public static <T> T convertToConfig(Map<String, Object> rawConfig,
                                        Class<T> targetConfigType) {
        return JsonApi4jConfigReader.getJsonObjectMapper().convertValue(rawConfig, targetConfigType);
    }

    public static JsonApi4jProperties readConfigFromClasspath(String configNameYaml, String configNameJson) throws IOException {
        try (InputStream is = JsonApi4jConfigReader.class.getResourceAsStream("/" + configNameYaml)) {
            if (is != null) {
                return JsonApi4jConfigReader.getYamlObjectMapper().readValue(is, JsonApi4jProperties.class);
            }
        }
        try (InputStream in = JsonApi4jConfigReader.class.getResourceAsStream("/" + configNameJson)) {
            if (in == null) {
                throw new IllegalStateException("No configuration file found");
            }
            return JsonApi4jConfigReader.getJsonObjectMapper().readValue(in, JsonApi4jProperties.class);
        }
    }

    public static Map<String, Object> readConfigFromClasspathAsMap(String configNameYaml,
                                                                   String configNameJson) throws IOException {
        try (InputStream is = JsonApi4jConfigReader.class.getResourceAsStream("/" + configNameYaml)) {
            if (is != null) {

                return JsonApi4jConfigReader.getYamlObjectMapper().readValue(is, new TypeReference<>() {});
            }
        }
        try (InputStream is = JsonApi4jConfigReader.class.getResourceAsStream("/" + configNameJson)) {
            if (is == null) {
                throw new IllegalStateException("No configuration file found");
            }
            return JsonApi4jConfigReader.getJsonObjectMapper().readValue(is, new TypeReference<>() {});
        }
    }

    private static ObjectMapper getObjectMapper(String path) {
        if (path != null && path.endsWith(".json")) {
            return JsonApi4jConfigReader.getJsonObjectMapper();
        } else {
            return JsonApi4jConfigReader.getYamlObjectMapper();
        }
    }

}
