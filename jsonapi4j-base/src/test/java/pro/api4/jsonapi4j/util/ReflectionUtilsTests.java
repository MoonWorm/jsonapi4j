package pro.api4.jsonapi4j.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReflectionUtilsTests {

    @Nested
    class MostDerivedDeclarationWins {

        @Test
        void fetchFields_shadowedName_resolvesToTheSubclassField() {
            assertThat(ReflectionUtils.fetchFields(Child.class).get("value").getDeclaringClass())
                    .isEqualTo(Child.class);
        }

        @Test
        void getFieldValueThrowing_shadowedName_readsTheSubclassValue() {
            assertThat(ReflectionUtils.getFieldValueThrowing(new Child(), "value")).isEqualTo("child");
        }

        @Test
        void setFieldValueThrowing_shadowedName_writesTheSubclassField() {
            Child target = new Child();

            ReflectionUtils.setFieldValueThrowing(target, "value", null);

            assertThat(target.childValue()).isNull();
            assertThat(target.inheritedValue()).isEqualTo("base");
        }

        @Test
        void fetchAnnotationForFields_annotationOnTheSubclassField_isFound() {
            assertThat(ReflectionUtils.fetchAnnotationForFields(Child.class, Marker.class))
                    .containsKey("value");
        }

        @Test
        void fetchFields_unshadowedInheritedField_isStillReachable() {
            assertThat(ReflectionUtils.fetchFields(Child.class)).containsKey("inheritedOnly");
        }

    }

    @Nested
    class ShadowedFieldNames {

        @Test
        void shadowedFieldNames_shadowedField_isReported() {
            assertThat(ReflectionUtils.shadowedFieldNames(Child.class)).isEqualTo(Set.of("value"));
        }

        @Test
        void shadowedFieldNames_noShadowing_isEmpty() {
            assertThat(ReflectionUtils.shadowedFieldNames(Base.class)).isEmpty();
        }

    }

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @interface Marker {
    }

    private static class Base {

        private String value = "base";
        private String inheritedOnly = "inherited";

        String inheritedValue() {
            return value;
        }

    }

    private static class Child extends Base {

        @Marker
        private String value = "child";

        String childValue() {
            return value;
        }

    }

}
