package pro.api4.jsonapi4j.sampleapp.testsuite;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Where the generated OpenAPI document is kept, and how to reach it from a test.
 * <p>
 * Two copies exist, and both are generated: the golden file the sample apps assert their endpoint against, and the
 * copy published on the documentation site. Sharing their locations here is what keeps the two tests that care - one
 * writing them, one checking they agree - from drifting apart over a hard-coded path.
 */
final class GoldenOasDocument {

    /** The golden file, as a classpath resource of this module. */
    static final String GOLDEN_RESOURCE = "/oas/expected-oas.json";

    /** The golden file, relative to the repository root, for the run that rewrites it. */
    static final String GOLDEN_SOURCE =
            "examples/jsonapi4j-sampleapp-testsuite/src/main/resources" + GOLDEN_RESOURCE;

    /**
     * The same document, published on the documentation site so a reader can explore it without running anything.
     * Generated rather than maintained, so something has to check it: see {@code PublishedOasDocumentTests}.
     */
    static final String PUBLISHED_COPY = "docs/assets/oas/sample-app.json";

    static final String UPDATE_GOLDEN_PROPERTY = "jsonapi4j.oas.golden.update";

    private static final String TESTSUITE_MODULE = "examples/jsonapi4j-sampleapp-testsuite";

    private GoldenOasDocument() {
    }

    static boolean isBeingRegenerated() {
        return Boolean.getBoolean(UPDATE_GOLDEN_PROPERTY);
    }

    static String readGolden() {
        try (InputStream in = GoldenOasDocument.class.getResourceAsStream(GOLDEN_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(String.format(
                        "Golden OAS file %s is missing from the classpath. Generate it with -D%s=true.",
                        GOLDEN_RESOURCE, UPDATE_GOLDEN_PROPERTY
                ));
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Tests run from a sample app module, while the files they write live elsewhere in the repository. Walk up from
     * the working directory until the repository root - the first ancestor holding this module - comes into view.
     */
    static Path repositoryFile(String repositoryRelativePath) {
        for (Path candidate = Paths.get("").toAbsolutePath(); candidate != null; candidate = candidate.getParent()) {
            if (Files.isRegularFile(candidate.resolve(TESTSUITE_MODULE).resolve("pom.xml"))) {
                return candidate.resolve(repositoryRelativePath);
            }
        }
        throw new IllegalStateException(String.format(
                "Cannot locate %s above the working directory %s. Run the test from within the repository.",
                repositoryRelativePath, Paths.get("").toAbsolutePath()
        ));
    }

    static void write(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
