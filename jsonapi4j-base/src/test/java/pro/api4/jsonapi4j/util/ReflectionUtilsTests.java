package pro.api4.jsonapi4j.util;

import lombok.Data;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReflectionUtilsTests {

    @Test
    public void getAllFieldPaths_testNotFallingIntoInfiniteLoop_checkResult() {
        // given - when
        Set<String> paths = ReflectionUtils.getAllFieldPaths(SelfRef.class);

        // then
        assertThat(paths).hasSize(1).isEqualTo(Set.of("nested"));
    }

    @Test
    public void getAllFieldPaths_happyPath_checkResult() {
        // given - when
        Set<String> paths = ReflectionUtils.getAllFieldPaths(PathsTraversal.class);

        // then
        assertThat(paths).hasSize(11).isEqualTo(
                Set.of(
                        "a",
                        "a.nested.nested",
                        "s",
                        "a.b",
                        "a.b.s",
                        "a.b.c",
                        "a.b.c.s",
                        "i",
                        "a.i",
                        "a.b.i",
                        "a.nested"
                )
        );
    }

    @Test
    public void getAllFieldPaths_jdkValueTypeAttributes_treatedAsLeavesAndNotTraversed() {
        // given - when
        Set<String> paths = ReflectionUtils.getAllFieldPaths(JdkTypes.class);

        // then - JDK value types are leaf paths; traversal must not descend into their internals
        // (descending into e.g. java.time.LocalDate would throw InaccessibleObjectException)
        assertThat(paths).isEqualTo(
                Set.of(
                        "localDate",
                        "localDateTime",
                        "instant",
                        "uuid",
                        "amount"
                )
        );
    }

    @Test
    public void isJdkType_jdkPackages_returnTrue() {
        assertThat(ReflectionUtils.isJdkType(String.class)).isTrue();
        assertThat(ReflectionUtils.isJdkType(java.time.LocalDate.class)).isTrue();
        assertThat(ReflectionUtils.isJdkType(java.util.UUID.class)).isTrue();
        assertThat(ReflectionUtils.isJdkType(java.math.BigDecimal.class)).isTrue();
        assertThat(ReflectionUtils.isJdkType(javax.security.auth.Subject.class)).isTrue();
    }

    @Test
    public void isJdkType_customClass_returnsFalse() {
        assertThat(ReflectionUtils.isJdkType(SelfRef.class)).isFalse();
    }

    @Test
    public void isJdkType_arrays_returnFalse() {
        assertThat(ReflectionUtils.isJdkType(String[].class)).isFalse();
        assertThat(ReflectionUtils.isJdkType(int[].class)).isFalse();
        assertThat(ReflectionUtils.isJdkType(SelfRef[].class)).isFalse();
    }

    @Test
    public void isJdkType_nullPackage_returnsFalse() {
        // primitives have no package
        assertThat(ReflectionUtils.isJdkType(int.class)).isFalse();
    }

    @Test
    public void setFieldPathValueThrowing_nullObject_checkResult() {
        // given
        Object object = null;

        // when - then
        assertThatThrownBy(() -> ReflectionUtils.setFieldPathValueThrowing(object, "nested.nested", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void setFieldPathValueThrowing_nullPath_checkResult() {
        // given
        SelfRef object = new SelfRef();

        // when - then
        assertThatThrownBy(() -> ReflectionUtils.setFieldPathValueThrowing(object, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void setFieldPathValueThrowing_emptyPath_checkResult() {
        // given
        SelfRef object = new SelfRef();

        // when - then
        assertThatThrownBy(() -> ReflectionUtils.setFieldPathValueThrowing(object, "", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void setFieldPathValueThrowing_nonExistingPath_checkResult() {
        // given
        SelfRef object = new SelfRef();

        // when - then
        assertThatThrownBy(() -> ReflectionUtils.setFieldPathValueThrowing(object, "foo", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void setFieldPathValueThrowing_happyPath_checkResult() {
        // given
        SelfRef nestedLevel1 = new SelfRef();
        SelfRef nestedLevel2 = new SelfRef();
        SelfRef nestedLevel3 = new SelfRef();
        nestedLevel2.setNested(nestedLevel3);
        nestedLevel1.setNested(nestedLevel2);

        // when
        ReflectionUtils.setFieldPathValueThrowing(nestedLevel1, "nested.nested", null);

        // then
        assertThat(nestedLevel1)
                .isNotNull()
                .extracting(SelfRef::getNested)
                .isNotNull()
                .extracting(SelfRef::getNested)
                .isNull();
    }

    @Test
    public void setFieldPathValue_nullObject_checkResult() {
        // given
        Object object = null;

        // when
        ReflectionUtils.setFieldPathValueSilent(object, "nested.nested", null);

        // then nothing happens
    }

    @Test
    public void setFieldPathValue_nullPath_checkResult() {
        // given
        SelfRef object = new SelfRef();

        // when
        ReflectionUtils.setFieldPathValueSilent(object, null, null);

        // then nothing happens
    }

    @Test
    public void setFieldPathValue_emptyPath_checkResult() {
        // given
        SelfRef object = new SelfRef();

        // when
        ReflectionUtils.setFieldPathValueSilent(object, "", null);

        // then nothing happens
    }

    @Test
    public void setFieldPathValue_nonExistingPath_checkResult() {
        // given
        SelfRef object = new SelfRef();

        // when
        ReflectionUtils.setFieldPathValueSilent(object, "foo", null);

        // then nothing happens
    }

    @Test
    public void setFieldPathValue_happyPath_checkResult() {
        // given
        SelfRef nestedLevel1 = new SelfRef();
        SelfRef nestedLevel2 = new SelfRef();
        SelfRef nestedLevel3 = new SelfRef();
        nestedLevel2.setNested(nestedLevel3);
        nestedLevel1.setNested(nestedLevel2);

        // when
        ReflectionUtils.setFieldPathValueSilent(nestedLevel1, "nested.nested", null);

        // then
        assertThat(nestedLevel1)
                .isNotNull()
                .extracting(SelfRef::getNested)
                .isNotNull()
                .extracting(SelfRef::getNested)
                .isNull();
    }

    @Test
    public void fetchAnnotationForMethod_overridesAnnotation_checkAnnotationIsResolvedProperly() {
        // given
        IBar bar = new Bar();

        // when
        Deprecated actual = ReflectionUtils.fetchAnnotationForMethod(
                bar.getClass(),
                "foo",
                new Class<?>[]{String.class},
                Deprecated.class
        );

        // then
        assertThat(actual).isNotNull();
        assertThat(actual.since()).isEqualTo("Bar-foo");
    }

    @Test
    public void fetchAnnotationForMethod_methodDoesNotExist_shouldReturnNull() {
        // given
        IBar bar = new Bar();

        // when
        Deprecated actual = ReflectionUtils.fetchAnnotationForMethod(
                bar.getClass(),
                "not-exist",
                new Class<?>[]{},
                Deprecated.class
        );

        // then
        assertThat(actual).isNull();
    }

    public interface IBar {

        @Deprecated(since = "IBar-foo")
        void foo(String s);

    }

    public class Bar implements IBar {

        @Deprecated(since = "Bar-foo")
        @Override
        public void foo(String s) {

        }

    }

    @Data
    public class SelfRef {
        SelfRef nested;
    }

    public class JdkTypes {
        java.time.LocalDate localDate;
        java.time.LocalDateTime localDateTime;
        java.time.Instant instant;
        java.util.UUID uuid;
        java.math.BigDecimal amount;
    }

    public class PathsTraversal {
        A a;
        String s;
        int i;

        class A {
            int i;
            B b;
            SelfRef nested;
        }

        class B {
            String s;
            int i;
            C c;
        }

        class C {
            String s;
        }
    }


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
    class AllFieldPaths {

        @Test
        void getAllFieldPaths_inheritedField_isIncluded() {
            assertThat(ReflectionUtils.getAllFieldPaths(Child.class)).contains("inheritedOnly");
        }

        @Test
        void getAllFieldPaths_declaredField_isIncluded() {
            assertThat(ReflectionUtils.getAllFieldPaths(Child.class)).contains("value");
        }

        @Test
        void getAllFieldPaths_inheritedNestedObject_isTraversed() {
            assertThat(ReflectionUtils.getAllFieldPaths(HoldingChild.class))
                    .contains("inheritedNested", "inheritedNested.leaf");
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

    private static class Nested1 {

        private String leaf = "leaf";

    }

    private static class HoldingBase {

        private Nested1 inheritedNested = new Nested1();

    }

    private static class HoldingChild extends HoldingBase {

        private String own = "own";

    }

    private static class Child extends Base {

        @Marker
        private String value = "child";

        String childValue() {
            return value;
        }

    }
}
