package pro.api4.jsonapi4j.compound.docs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.config.Deduplication;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.config.UnsupportedIncludeStrategy;
import pro.api4.jsonapi4j.compound.docs.exception.UnsupportedIncludeException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncludesCheckerTests {

    @Nested
    class Fail {

        private final IncludesChecker sut = checker(UnsupportedIncludeStrategy.FAIL);

        @Test
        void check_includeDeeperThanMaxHops_throwsNamingThePath() {
            assertThatThrownBy(() -> sut.check(request("/users/1", "placeOfBirth", "placeOfBirth.currencies")))
                    .isInstanceOf(UnsupportedIncludeException.class)
                    .hasMessage("Include path 'placeOfBirth.currencies' spans 2 relationships, more than the supported 1");
        }

        @Test
        void check_includesWithinMaxHops_returnsThemWithoutGaps() {
            CheckedIncludes result = sut.check(request("/users/1", "relatives", "placeOfBirth"));

            assertThat(result.paths()).containsExactly("relatives", "placeOfBirth");
            assertThat(result.gaps()).isEmpty();
        }

        @Test
        void check_deepIncludeOutsideRequestedRelationship_dropsItWithoutFailing() {
            CheckedIncludes result = sut.check(
                    request("/users/1/relationships/citizenships", "citizenships", "relatives.relatives", "citizenshipsX")
            );

            assertThat(result.paths()).containsExactly("citizenships");
            assertThat(result.gaps()).isEmpty();
        }

        @Test
        void check_requestNotProcessable_returnsNothing() {
            CheckedIncludes result = sut.check(
                    new CompoundDocsRequest("GET", null, Map.of(), Map.of(), "/users/1", Map.of())
            );

            assertThat(result.paths()).isEmpty();
            assertThat(result.gaps()).isEmpty();
        }

    }

    @Nested
    class Ignore {

        private final IncludesChecker sut = checker(UnsupportedIncludeStrategy.IGNORE);

        @Test
        void check_includeDeeperThanMaxHops_cutsItAndReportsGap() {
            CheckedIncludes result = sut.check(request("/users/1", "relatives", "placeOfBirth.currencies"));

            assertThat(result.paths()).containsExactly("relatives", "placeOfBirth");
            assertThat(result.gaps()).containsExactly(
                    IncludedGap.forPath(IncompleteReason.UNSUPPORTED_INCLUDE, "placeOfBirth.currencies")
            );
        }

    }

    private static IncludesChecker checker(UnsupportedIncludeStrategy unsupportedIncludes) {
        return IncludesChecker.from(new CompoundDocsResolverConfig(
                true, 1, unsupportedIncludes, 100, ErrorStrategy.FAIL, List.of(), Deduplication.DATA_AND_INCLUDED,
                1000, 1000, false, 1
        ));
    }

    private static CompoundDocsRequest request(String relativePath, String... includes) {
        return new CompoundDocsRequest("GET", List.of(includes), Map.of(), Map.of(), relativePath, Map.of());
    }

}
