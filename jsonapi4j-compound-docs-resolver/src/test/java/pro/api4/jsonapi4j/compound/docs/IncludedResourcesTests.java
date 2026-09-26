package pro.api4.jsonapi4j.compound.docs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.api4.jsonapi4j.compound.docs.IdAndTypeFixtures.idAndType;

public class IncludedResourcesTests {

    @Nested
    class Deduplicating {

        private final IncludedResources sut = new IncludedResources(true);

        @Test
        public void add_sameResourceTwice_keepsLatestCopy() {
            sut.add(idAndType("users", "1"), "first");
            sut.add(idAndType("users", "1"), "second");

            assertThat(sut.toList()).containsExactly("second");
            assertThat(sut.size()).isEqualTo(1);
        }

        @Test
        public void add_differentResources_keepsInsertionOrder() {
            sut.add(idAndType("users", "1"), "user-1");
            sut.add(idAndType("countries", "US"), "country-US");
            sut.add(idAndType("users", "1"), "user-1-refetched");

            assertThat(sut.toList()).containsExactly("user-1-refetched", "country-US");
            assertThat(sut.size()).isEqualTo(2);
        }

    }

    @Nested
    class NotDeduplicating {

        private final IncludedResources sut = new IncludedResources(false);

        @Test
        public void add_sameResourceTwice_keepsBothCopies() {
            sut.add(idAndType("users", "1"), "first");
            sut.add(idAndType("users", "1"), "second");

            assertThat(sut.toList()).containsExactly("first", "second");
            assertThat(sut.size()).isEqualTo(2);
        }

    }

    @Test
    public void toList_nothingAdded_returnsEmptyList() {
        IncludedResources sut = new IncludedResources(true);

        assertThat(sut.toList()).isEmpty();
        assertThat(sut.size()).isZero();
    }

}
