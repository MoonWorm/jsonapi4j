package pro.api4.jsonapi4j.plugin.ac.anonymization;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlScopes;
import pro.api4.jsonapi4j.plugin.ac.annotation.ScopesGroup;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AnonymizationPlanTests {

    private static List<String> descendableNamesOf(Class<?> clazz) {
        return AnonymizationPlan.of(clazz).descendable().stream().map(Field::getName).toList();
    }

    @Nested
    class Descendable {

        @Test
        void descendable_finalJdkTypedFields_areSkipped() {
            assertThat(descendableNamesOf(Fields.class))
                    .doesNotContain("text", "boxed", "date");
        }

        @Test
        void descendable_primitiveAndStaticFields_areSkipped() {
            assertThat(descendableNamesOf(Fields.class)).doesNotContain("count", "SHARED");
        }

        @Test
        void descendable_applicationTypedField_isKept() {
            assertThat(descendableNamesOf(Fields.class)).contains("nested");
        }

        @Test
        void descendable_fieldsWhoseRuntimeTypeCannotBePredicted_areKept() {
            assertThat(descendableNamesOf(Fields.class)).contains("loose", "iface", "supertype");
        }

        @Test
        void descendable_containerFields_areKeptEvenWhenTheirTypeIsFinal() {
            assertThat(descendableNamesOf(Fields.class)).contains("items", "byName", "maybe", "array");
        }

    }

    @Nested
    class Leaves {

        @Test
        void isLeaf_jdkType_isTrue() {
            assertThat(AnonymizationPlan.of(String.class).isLeaf()).isTrue();
        }

        @Test
        void isLeaf_enum_isTrue() {
            assertThat(AnonymizationPlan.of(Status.class).isLeaf()).isTrue();
        }

        @Test
        void isLeaf_frameworkEnvelopeType_isTrue() {
            assertThat(AnonymizationPlan.of(ResourceIdentifierObject.class).isLeaf()).isTrue();
            assertThat(AnonymizationPlan.of(LinksObject.class).isLeaf()).isTrue();
        }

        @Test
        void isLeaf_applicationType_isFalse() {
            assertThat(AnonymizationPlan.of(Nested1.class).isLeaf()).isFalse();
        }

        @Test
        void descendable_jdkType_isEmpty() {
            assertThat(AnonymizationPlan.of(String.class).descendable()).isEmpty();
        }

        @Test
        void descendable_frameworkEnvelopeType_isNotEmpty() {
            assertThat(AnonymizationPlan.of(ResourceIdentifierObject.class).descendable()).isNotEmpty();
        }

    }

    @Nested
    class Requirements {

        @Test
        void declaresRequirements_annotatedClass_isTrue() {
            assertThat(AnonymizationPlan.of(Nested1.class).declaresRequirements()).isTrue();
        }

        @Test
        void declaresRequirements_plainClass_isFalse() {
            assertThat(AnonymizationPlan.of(Fields.class).declaresRequirements()).isFalse();
        }

        @Test
        void of_sameClassTwice_returnsTheSameInstance() {
            assertThat(AnonymizationPlan.of(Fields.class)).isSameAs(AnonymizationPlan.of(Fields.class));
        }

    }

    private enum Status {
        ACTIVE
    }

    public static class Nested1 {
        @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("secret.read")))
        public String secret = "s";
    }

    public interface Marker {
    }

    public static class Base {
    }

    public static class Fields {

        public static String SHARED;

        public String text = "t";
        public Integer boxed = 1;
        public LocalDate date = LocalDate.EPOCH;
        public int count = 3;

        public Nested1 nested = new Nested1();
        public Object loose = new Nested1();
        public Marker iface;
        public Base supertype = new Base();

        public List<Nested1> items = List.of();
        public Map<String, Nested1> byName = Map.of();
        public Optional<Nested1> maybe = Optional.empty();
        public Nested1[] array = {};

    }

}
