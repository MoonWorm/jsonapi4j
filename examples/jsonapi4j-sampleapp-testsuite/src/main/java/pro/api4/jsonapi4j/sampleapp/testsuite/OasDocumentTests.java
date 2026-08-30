package pro.api4.jsonapi4j.sampleapp.testsuite;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Golden-file tests for the document served by the OAS plugin. The whole document is compared against
 * {@code oas/expected-oas.json}, so any change to the generated paths, schemas, parameters, responses or
 * error examples surfaces as a reviewable diff rather than going unnoticed.
 * <p>
 * All three sample apps run under an {@code oasTest} profile that enables the OAS plugin and nothing else,
 * and that pins the one OAS setting which otherwise differs between them ({@code info.description}). The
 * apps therefore share a single golden file: a diff between them is itself a defect.
 * <p>
 * To regenerate the golden file after an intentional change, run any of the concrete subclasses with
 * {@code -Djsonapi4j.oas.golden.update=true}. The run rewrites the file and then fails, so a regeneration
 * can never be mistaken for a passing build.
 */
public abstract class OasDocumentTests {

    private static final String GOLDEN_RESOURCE = "/oas/expected-oas.json";
    private static final String TESTSUITE_MODULE = "examples/jsonapi4j-sampleapp-testsuite";
    private static final String GOLDEN_SOURCE = TESTSUITE_MODULE + "/src/main/resources" + GOLDEN_RESOURCE;
    private static final String UPDATE_GOLDEN_PROPERTY = "jsonapi4j.oas.golden.update";

    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String YAML_CONTENT_TYPE = "application/yaml";

    private final String oasRootPath;
    private final int appPort;

    protected OasDocumentTests(String oasRootPath, int appPort) {
        this.oasRootPath = oasRootPath;
        this.appPort = appPort;
    }

    private String oasUrl() {
        return String.format("http://localhost:%d%s", appPort, oasRootPath);
    }

    @Test
    public void get_oasDocument_matchesGoldenFile() {
        Response response = given().get(oasUrl()).thenReturn();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).startsWith(JSON_CONTENT_TYPE);
        assertMatchesGoldenFile(normalize(readJson(response.asString())));
    }

    @Test
    public void get_oasDocumentInYamlFormat_matchesGoldenFile() {
        assumeFalse(isUpdatingGoldenFile(), "The golden file is written from the JSON document only");

        Response response = given().queryParam("format", "yaml").get(oasUrl()).thenReturn();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).startsWith(YAML_CONTENT_TYPE);
        assertJsonEquals(readGoldenFile(), normalize(readYaml(response.asString())));
    }

    @Test
    public void get_oasDocumentTwice_returnsIdenticalDocument() {
        String first = given().get(oasUrl()).thenReturn().asString();
        String second = given().get(oasUrl()).thenReturn().asString();

        assertJsonEquals(first, second);
    }

    private boolean isUpdatingGoldenFile() {
        return Boolean.getBoolean(UPDATE_GOLDEN_PROPERTY);
    }

    private void assertMatchesGoldenFile(String actual) {
        if (isUpdatingGoldenFile()) {
            Path goldenSourceFile = resolveGoldenSourceFile();
            writeGoldenFile(goldenSourceFile, actual);
            fail(String.format(
                    "Golden OAS file regenerated at %s. Review the diff, then re-run without -D%s=true.",
                    goldenSourceFile, UPDATE_GOLDEN_PROPERTY
            ));
        }
        assertJsonEquals(readGoldenFile(), actual);
    }

    private String readGoldenFile() {
        try (InputStream in = OasDocumentTests.class.getResourceAsStream(GOLDEN_RESOURCE)) {
            if (in == null) {
                return fail(String.format(
                        "Golden OAS file %s is missing from the classpath. Generate it with -D%s=true.",
                        GOLDEN_RESOURCE, UPDATE_GOLDEN_PROPERTY
                ));
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeGoldenFile(Path goldenSourceFile, String content) {
        try {
            Files.createDirectories(goldenSourceFile.getParent());
            Files.writeString(goldenSourceFile, content + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Tests run from a sample app module, while the golden file lives in the shared testsuite module. Walk
     * up from the working directory until the repository root - the first ancestor holding the testsuite
     * source tree - comes into view.
     */
    private Path resolveGoldenSourceFile() {
        for (Path candidate = Paths.get("").toAbsolutePath(); candidate != null; candidate = candidate.getParent()) {
            if (Files.isRegularFile(candidate.resolve(TESTSUITE_MODULE).resolve("pom.xml"))) {
                return candidate.resolve(GOLDEN_SOURCE);
            }
        }
        return fail(String.format(
                "Cannot locate %s above the working directory %s. Run the test from within the repository.",
                GOLDEN_SOURCE, Paths.get("").toAbsolutePath()
        ));
    }

    private Map<String, Object> readJson(String body) {
        return read(new ObjectMapper(), body);
    }

    private Map<String, Object> readYaml(String body) {
        return read(new ObjectMapper(new YAMLFactory()), body);
    }

    private Map<String, Object> read(ObjectMapper objectMapper, String body) {
        try {
            return objectMapper.readValue(body, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Renders the document with map keys sorted and one property per line. Sorting keeps the golden file
     * stable no matter what order the registries hand out resources and operations in; the line-per-property
     * layout keeps a regenerated file reviewable as a diff.
     */
    private String normalize(Map<String, Object> document) {
        ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(document);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
