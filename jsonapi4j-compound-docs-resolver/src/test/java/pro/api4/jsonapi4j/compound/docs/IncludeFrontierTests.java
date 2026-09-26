package pro.api4.jsonapi4j.compound.docs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.compound.docs.json.ResourceLinkage;
import pro.api4.jsonapi4j.processor.IdAndType;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.api4.jsonapi4j.compound.docs.IdAndTypeFixtures.idAndType;

public class IncludeFrontierTests {

    private static final IdAndType NORWAY = idAndType("countries", "NO");
    private static final IdAndType USA = idAndType("countries", "US");
    private static final IdAndType USER_2 = idAndType("users", "2");
    private static final IdAndType NOK = idAndType("currencies", "NOK");
    private static final IdAndType USD = idAndType("currencies", "USD");

    private static final Map<String, Set<IdAndType>> ROOT_RELATIONSHIPS = Map.of(
            "citizenships", Set.of(NORWAY, USA),
            "placeOfBirth", Set.of(USA),
            "relatives", Set.of(USER_2)
    );

    @Nested
    class Start {

        @Test
        public void start_rootLinkage_followsOnlyRequestedRelationships() {
            IncludeFrontier sut = IncludeFrontier.start(
                    IncludeTree.of(List.of("citizenships", "placeOfBirth.currencies")),
                    ROOT_RELATIONSHIPS
            );

            assertThat(sut.resources()).containsExactlyInAnyOrder(NORWAY, USA);
        }

        @Test
        public void start_noRequestedRelationshipLinked_isEmpty() {
            IncludeFrontier sut = IncludeFrontier.start(IncludeTree.of(List.of("friends")), ROOT_RELATIONSHIPS);

            assertThat(sut.isEmpty()).isTrue();
        }

    }

    @Nested
    class RequiredIncludes {

        @Test
        public void requiredIncludes_resourceReachedThroughSeveralPaths_unionsTheirContinuations() {
            IncludeFrontier sut = IncludeFrontier.start(
                    IncludeTree.of(List.of("citizenships", "placeOfBirth.currencies")),
                    ROOT_RELATIONSHIPS
            );

            assertThat(sut.requiredIncludes(USA)).containsExactly("currencies");
            assertThat(sut.requiredIncludes(NORWAY)).isEmpty();
        }

        @Test
        public void requiredIncludes_resourceNotInFrontier_isEmpty() {
            IncludeFrontier sut = IncludeFrontier.start(IncludeTree.of(List.of("citizenships")), ROOT_RELATIONSHIPS);

            assertThat(sut.requiredIncludes(USER_2)).isEmpty();
        }

    }

    @Nested
    class Next {

        @Test
        public void next_resourcesSharingType_followsOnlyTheirOwnPaths() {
            IncludeFrontier sut = IncludeFrontier.start(
                    IncludeTree.of(List.of("citizenships", "placeOfBirth.currencies")),
                    ROOT_RELATIONSHIPS
            );

            IncludeFrontier next = sut.next(Map.of(
                    NORWAY, new ResourceLinkage(NORWAY, Map.of("currencies", Set.of(NOK))),
                    USA, new ResourceLinkage(USA, Map.of("currencies", Set.of(USD)))
            ));

            assertThat(next.resources()).containsExactly(USD);
            assertThat(next.requiredIncludes(USD)).isEmpty();
        }

        @Test
        public void next_resourceWithoutLinkage_isNotFollowed() {
            IncludeFrontier sut = IncludeFrontier.start(
                    IncludeTree.of(List.of("placeOfBirth.currencies")),
                    ROOT_RELATIONSHIPS
            );

            assertThat(sut.next(Map.of()).isEmpty()).isTrue();
        }

        @Test
        public void next_isImmutable_leavesCurrentFrontierUnchanged() {
            IncludeFrontier sut = IncludeFrontier.start(
                    IncludeTree.of(List.of("placeOfBirth.currencies")),
                    ROOT_RELATIONSHIPS
            );

            sut.next(Map.of(USA, new ResourceLinkage(USA, Map.of("currencies", Set.of(USD)))));

            assertThat(sut.resources()).containsExactly(USA);
        }

    }

}
