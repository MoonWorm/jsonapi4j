package pro.api4.jsonapi4j.plugin.utils;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.util.ReflectionUtils;

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
                new Class<?>[]{ String.class },
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

}
