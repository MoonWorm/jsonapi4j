package pro.api4.jsonapi4j.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipObject;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultResourceRefTests {

    private static ResourceIdentifierObject identifier(String type, String id) {
        return new ResourceIdentifierObject(id, null, type, null);
    }

    @Nested
    class Construction {

        @Test
        void of_typeAndId_createsDefaultResourceRef() {
            assertThat(DefaultResourceRef.of("countries", "NO")).isEqualTo(new DefaultResourceRef("countries", "NO"));
        }

        @Test
        void constructor_nullType_throwsException() {
            assertThatThrownBy(() -> new DefaultResourceRef(null, "NO"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("type must not be null");
        }

        @Test
        void constructor_nullId_throwsException() {
            assertThatThrownBy(() -> new DefaultResourceRef("countries", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("id must not be null");
        }

    }

    @Nested
    class FromResourceIdentifier {

        @Test
        void from_resourceIdentifierObject_copiesTypeAndId() {
            ResourceIdentifierObject resourceIdentifier = new ResourceIdentifierObject("NO", null, "countries", "meta");

            assertThat(DefaultResourceRef.from(resourceIdentifier)).isEqualTo(new DefaultResourceRef("countries", "NO"));
        }

    }

    @Nested
    class FromToOneRelationship {

        @Test
        void from_toOneRelationshipWithData_copiesTypeAndId() {
            ToOneRelationshipObject relationship = new ToOneRelationshipObject(identifier("countries", "NO"), null, null);

            assertThat(DefaultResourceRef.from(relationship)).isEqualTo(new DefaultResourceRef("countries", "NO"));
        }

        @Test
        void from_toOneRelationshipWithNullData_returnsNull() {
            assertThat(DefaultResourceRef.from(new ToOneRelationshipObject(null, null, null))).isNull();
        }

    }

    @Nested
    class ListFromToManyRelationship {

        @Test
        void listFrom_toManyRelationshipWithData_copiesTypesAndIdsInOrder() {
            ToManyRelationshipObject relationship = new ToManyRelationshipObject(
                    List.of(identifier("countries", "NO"), identifier("countries", "FI")),
                    null,
                    null
            );

            assertThat(DefaultResourceRef.listFrom(relationship)).containsExactly(
                    new DefaultResourceRef("countries", "NO"),
                    new DefaultResourceRef("countries", "FI")
            );
        }

        @Test
        void listFrom_toManyRelationshipWithNullData_returnsEmptyList() {
            assertThat(DefaultResourceRef.listFrom(new ToManyRelationshipObject(null, null, null))).isEmpty();
        }

    }

}
