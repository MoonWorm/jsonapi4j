package pro.api4.jsonapi4j.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToOneRefRelationshipTests {

    private final ToOneRefRelationship<ResourceRef> sut = new ToOneRefRelationship<>() {
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
        ToOneRefRelationship<RelativeRef> customSut = new ToOneRefRelationship<>() {
        };

        assertThat(customSut.resolveResourceIdentifierId(new RelativeRef("2", "HUSBAND"))).isEqualTo("2");
    }

    @Test
    void resolveResourceIdentifierType_customResourceRef_returnsRefType() {
        ToOneRefRelationship<RelativeRef> customSut = new ToOneRefRelationship<>() {
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
