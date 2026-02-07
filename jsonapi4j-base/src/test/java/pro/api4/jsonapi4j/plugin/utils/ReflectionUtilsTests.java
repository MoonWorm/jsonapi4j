package pro.api4.jsonapi4j.plugin.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReflectionUtilsTests {

    @Test
    public void fetchAnnotationForMethod_overridesAnnotation_checkAnnotationIsResolvedProperly() {
        // given
        IBar bar = new Bar();

        // when
        Deprecated actual = ReflectionUtils.fetchAnnotationForMethod(
                bar.getClass(),
                "foo",
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

}
