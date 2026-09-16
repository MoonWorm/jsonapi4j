package pro.api4.jsonapi4j.sampleapp.testsuite;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * The OpenAPI document published on the documentation site must be the one the framework currently generates.
 * <p>
 * It is a copy of the golden file, written by the same run that regenerates it, and nothing but this stops it going
 * quietly stale - a documentation site showing a document the framework no longer produces is worse than showing
 * none, because a reader has no way to tell.
 * <p>
 * Deliberately not part of {@link OasDocumentTests}: that suite describes what the endpoint serves and runs once per
 * sample app, while this is a property of the repository. Checking it here runs it once instead of three times, and
 * runs it before the sample apps are built at all - a stale copy is worth learning about in seconds rather than
 * after three applications have started.
 */
class PublishedOasDocumentTests {

    @Test
    void publishedDocument_matchesTheGoldenFile() {
        assumeFalse(GoldenOasDocument.isBeingRegenerated(), "Both copies are being rewritten by this run");

        assertJsonEquals(GoldenOasDocument.readGolden(), read(publishedDocument()));
    }

    /**
     * The site loads it by a path baked into the page, so its location is part of the contract too - moving it
     * silently would leave a viewer pointed at nothing.
     */
    @Test
    void publishedDocument_isWhereTheDocumentationSiteLooksForIt() {
        Path published = publishedDocument();

        assertThat(published).as("published OpenAPI document").isRegularFile();
        assertThat(published.toString().replace('\\', '/')).endsWith("docs/assets/oas/sample-app.json");
    }

    private Path publishedDocument() {
        Path published = GoldenOasDocument.repositoryFile(GoldenOasDocument.PUBLISHED_COPY);
        assertThat(published).as(
                "published OpenAPI document - regenerate it with -D%s=true",
                GoldenOasDocument.UPDATE_GOLDEN_PROPERTY).isRegularFile();
        return published;
    }

    private String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
