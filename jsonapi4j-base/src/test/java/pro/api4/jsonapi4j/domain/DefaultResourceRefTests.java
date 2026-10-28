package pro.api4.jsonapi4j.domain;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultResourceRefTests {

    @Test
    void of_typeAndId_createsDefaultResourceRef() {
        assertThat(DefaultResourceRef.of("countries", "NO")).isEqualTo(new DefaultResourceRef("countries", "NO"));
    }

    @Test
    void from_resourceIdentifierObject_copiesTypeAndId() {
        ResourceIdentifierObject resourceIdentifier = new ResourceIdentifierObject("NO", null, "countries", "meta");

        assertThat(DefaultResourceRef.from(resourceIdentifier)).isEqualTo(new DefaultResourceRef("countries", "NO"));
    }

}
