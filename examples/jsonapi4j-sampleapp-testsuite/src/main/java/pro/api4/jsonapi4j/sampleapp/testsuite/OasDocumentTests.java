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

    /**
     * The document endpoint serves one thing and only reads: anything but a GET or a HEAD is a mistake worth saying
     * out loud, with {@code Allow} so the caller learns what is on offer.
     */
    @Test
    public void request_methodOtherThanGetOrHead_isRefusedWithAllow() {
        Response response = given().post(oasUrl()).thenReturn();

        assertThat(response.statusCode()).isEqualTo(405);
        assertThat(response.header("Allow")).isEqualTo("GET, HEAD");
    }

    @Test
    public void get_acceptYaml_servesYamlWithoutAFormatParameter() {
        Response response = given().header("Accept", YAML_CONTENT_TYPE).get(oasUrl()).thenReturn();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).startsWith(YAML_CONTENT_TYPE);
    }

    /**
     * An explicit {@code format} is the caller being specific, so it outranks whatever the client library happened
     * to put in {@code Accept}.
     */
    @Test
    public void get_formatParameter_winsOverTheAcceptHeader() {
        Response response = given()
                .header("Accept", YAML_CONTENT_TYPE)
                .queryParam("format", "json")
                .get(oasUrl())
                .thenReturn();

        assertThat(response.contentType()).startsWith(JSON_CONTENT_TYPE);
    }

    /**
     * The document is built once and served from a cache, so a caller holding its {@code ETag} should not have to
     * download it again - these endpoints get polled.
     */
    @Test
    public void get_eTagTheCallerAlreadyHolds_answersNotModifiedWithNoBody() {
        String eTag = given().get(oasUrl()).thenReturn().header("ETag");
        assertThat(eTag).isNotBlank();

        Response response = given().header("If-None-Match", eTag).get(oasUrl()).thenReturn();

        assertThat(response.statusCode()).isEqualTo(304);
        assertThat(response.asString()).isEmpty();
    }

    @Test
    public void get_eTagThatNoLongerMatches_servesTheDocumentAgain() {
        Response response = given().header("If-None-Match", "\"stale\"").get(oasUrl()).thenReturn();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.asString()).isNotEmpty();
    }

    private boolean isUpdatingGoldenFile() {
        return GoldenOasDocument.isBeingRegenerated();
    }

    private void assertMatchesGoldenFile(String actual) {
        if (isUpdatingGoldenFile()) {
            Path goldenSourceFile = GoldenOasDocument.repositoryFile(GoldenOasDocument.GOLDEN_SOURCE);
            Path publishedCopy = GoldenOasDocument.repositoryFile(GoldenOasDocument.PUBLISHED_COPY);
            GoldenOasDocument.write(goldenSourceFile, actual);
            GoldenOasDocument.write(publishedCopy, actual);
            fail(String.format(
                    "Golden OAS file regenerated at %s and %s. Review the diff, then re-run without -D%s=true.",
                    goldenSourceFile, publishedCopy, GoldenOasDocument.UPDATE_GOLDEN_PROPERTY
            ));
        }
        assertJsonEquals(readGoldenFile(), actual);
    }

    private String readGoldenFile() {
        return GoldenOasDocument.readGolden();
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
