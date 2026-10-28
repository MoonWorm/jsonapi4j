package pro.api4.jsonapi4j.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToManyRefRelationshipTests {

    private final ToManyRefRelationship<ResourceRef> sut = new ToManyRefRelationship<>() {
    };

    @Test
    void resolveResourceIdentifierType_resourceRef_returnsRefType() {
        assertThat(sut.resolveResourceIdentifierType(new DefaultResourceRef("countries", "NO"))).isEqualTo("countries");
    }

    @Test
    void resolveResourceIdentifierId_resourceRef_returnsRefId() {
        assertThat(sut.resolveResourceIdentifierId(new DefaultResourceRef("countries", "NO"))).isEqualTo("NO");
    }

    @Test
    void resolveResourceIdentifierId_customResourceRef_returnsRefId() {
        ToManyRefRelationship<RelativeRef> customSut = new ToManyRefRelationship<>() {
        };

        assertThat(customSut.resolveResourceIdentifierId(new RelativeRef("2", "HUSBAND"))).isEqualTo("2");
    }

    @Test
    void resolveResourceIdentifierType_customResourceRef_returnsRefType() {
        ToManyRefRelationship<RelativeRef> customSut = new ToManyRefRelationship<>() {
        };

        assertThat(customSut.resolveResourceIdentifierType(new RelativeRef("2", "HUSBAND"))).isEqualTo("users");
    }

    private record RelativeRef(String id, String relation) implements ResourceRef {

        @Override
        public String type() {
            return "users";
        }
    }

}
