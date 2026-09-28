package pro.api4.jsonapi4j.compound.docs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.compound.docs.config.Deduplication;
import pro.api4.jsonapi4j.processor.IdAndType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.api4.jsonapi4j.compound.docs.IdAndTypeFixtures.idAndType;

public class IncludedResourcesTests {

    private static final IdAndType PRIMARY = idAndType("users", "1");
    private static final IdAndType OTHER = idAndType("users", "2");
    private static final Map<IdAndType, String> PRIMARY_RESOURCE_JSONS = Map.of(PRIMARY, "primary-from-data");

    @Nested
    class DataAndIncluded {

        private final IncludedResources sut = new IncludedResources(Deduplication.DATA_AND_INCLUDED, PRIMARY_RESOURCE_JSONS);

        @Test
        public void add_sameResourceTwice_keepsLatestCopy() {
            sut.add(OTHER, "first");
            sut.add(OTHER, "second");

            assertThat(sut.toList()).containsExactly("second");
            assertThat(sut.size()).isEqualTo(1);
        }

        @Test
        public void add_primaryResource_ignoresIt() {
            sut.add(PRIMARY, "fetched");

            assertThat(sut.toList()).isEmpty();
            assertThat(sut.size()).isZero();
        }

        @Test
        public void reached_primaryResource_doesNotIncludeIt() {
            sut.reached(PRIMARY);

            assertThat(sut.toList()).isEmpty();
        }

    }

    @Nested
    class IncludedOnly {

        private final IncludedResources sut = new IncludedResources(Deduplication.INCLUDED_ONLY, PRIMARY_RESOURCE_JSONS);

        @Test
        public void reached_primaryResource_includesItFromPrimaryData() {
            sut.reached(PRIMARY);

            assertThat(sut.toList()).containsExactly("primary-from-data");
            assertThat(sut.size()).isEqualTo(1);
        }

        @Test
        public void reached_primaryResourceAlreadyFetched_keepsFetchedCopy() {
            sut.add(PRIMARY, "fetched");
            sut.reached(PRIMARY);

            assertThat(sut.toList()).containsExactly("fetched");
        }

        @Test
        public void add_primaryResourceAfterReached_replacesCopyFromPrimaryData() {
            sut.reached(PRIMARY);
            sut.add(PRIMARY, "fetched-with-more-linkage");

            assertThat(sut.toList()).containsExactly("fetched-with-more-linkage");
            assertThat(sut.size()).isEqualTo(1);
        }

        @Test
        public void reached_otherResource_doesNothing() {
            sut.reached(OTHER);

            assertThat(sut.toList()).isEmpty();
        }

    }

    @Nested
    class None {

        private final IncludedResources sut = new IncludedResources(Deduplication.NONE, PRIMARY_RESOURCE_JSONS);

        @Test
        public void add_sameResourceTwice_keepsBothCopies() {
            sut.add(OTHER, "first");
            sut.add(OTHER, "second");

            assertThat(sut.toList()).containsExactly("first", "second");
            assertThat(sut.size()).isEqualTo(2);
        }

        @Test
        public void add_primaryResource_includesIt() {
            sut.add(PRIMARY, "fetched");

            assertThat(sut.toList()).containsExactly("fetched");
        }

    }

    @Test
    public void toList_nothingAdded_returnsEmptyList() {
        IncludedResources sut = new IncludedResources(Deduplication.DATA_AND_INCLUDED, Map.of());

        assertThat(sut.toList()).isEmpty();
        assertThat(sut.size()).isZero();
    }

}
