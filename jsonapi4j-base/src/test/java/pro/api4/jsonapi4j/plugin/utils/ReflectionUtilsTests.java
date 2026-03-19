package pro.api4.jsonapi4j.plugin.utils;

import lombok.Data;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.util.ReflectionUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReflectionUtilsTests {

    @Test
    public void getAllFieldPaths_testNotFallingIntoInfiniteLoop_checkResult() {
        // given - when
        Set<String> paths = ReflectionUtils.getAllFieldPaths(Nested.class);

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
        Nested object = new Nested();

        // when - then
        assertThatThrownBy(() -> ReflectionUtils.setFieldPathValueThrowing(object, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void setFieldPathValueThrowing_emptyPath_checkResult() {
        // given
        Nested object = new Nested();

        // when - then
        assertThatThrownBy(() -> ReflectionUtils.setFieldPathValueThrowing(object, "", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void setFieldPathValueThrowing_nonExistingPath_checkResult() {
        // given
        Nested object = new Nested();

        // when - then
        assertThatThrownBy(() -> ReflectionUtils.setFieldPathValueThrowing(object, "foo", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void setFieldPathValueThrowing_happyPath_checkResult() {
        // given
        Nested nestedLevel1 = new Nested();
        Nested nestedLevel2 = new Nested();
        Nested nestedLevel3 = new Nested();
        nestedLevel2.setNested(nestedLevel3);
        nestedLevel1.setNested(nestedLevel2);

        // when
        ReflectionUtils.setFieldPathValueThrowing(nestedLevel1, "nested.nested", null);

        // then
        assertThat(nestedLevel1)
                .isNotNull()
                .extracting(Nested::getNested)
                .isNotNull()
                .extracting(Nested::getNested)
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
        Nested object = new Nested();

        // when
        ReflectionUtils.setFieldPathValueSilent(object, null, null);

        // then nothing happens
    }

    @Test
    public void setFieldPathValue_emptyPath_checkResult() {
        // given
        Nested object = new Nested();

        // when
        ReflectionUtils.setFieldPathValueSilent(object, "", null);

        // then nothing happens
    }

    @Test
    public void setFieldPathValue_nonExistingPath_checkResult() {
        // given
        Nested object = new Nested();

        // when
        ReflectionUtils.setFieldPathValueSilent(object, "foo", null);

        // then nothing happens
    }

    @Test
    public void setFieldPathValue_happyPath_checkResult() {
        // given
        Nested nestedLevel1 = new Nested();
        Nested nestedLevel2 = new Nested();
        Nested nestedLevel3 = new Nested();
        nestedLevel2.setNested(nestedLevel3);
        nestedLevel1.setNested(nestedLevel2);

        // when
        ReflectionUtils.setFieldPathValueSilent(nestedLevel1, "nested.nested", null);

        // then
        assertThat(nestedLevel1)
                .isNotNull()
                .extracting(Nested::getNested)
                .isNotNull()
                .extracting(Nested::getNested)
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
    public class Nested {
        Nested nested;
    }

    public class PathsTraversal {
        A a;
        String s;
        int i;

        class A {
            int i;
            B b;
            Nested nested;
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

}
