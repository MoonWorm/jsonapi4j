package pro.api4.jsonapi4j.ac;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.Authenticated;
import pro.api4.jsonapi4j.plugin.utils.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ReflectionUtilsTests {

    @Test
    public void fetchAnnotationForMethod_overridesAnnotation_checkAnnotationIsResolvedProperly() {
        // given
        IBar bar = new Bar();

        // when
        AccessControl actual = ReflectionUtils.fetchAnnotationForMethod(
                bar.getClass(),
                "foo",
                AccessControl.class
        );

        // then
        assertThat(actual).isNotNull();
        assertThat(actual.authenticated()).isEqualTo(Authenticated.AUTHENTICATED);
    }

    @Test
    public void fetchAnnotationForMethod_methodDoesNotExist_shouldReturnNull() {
        // given
        IBar bar = new Bar();

        // when
        AccessControl actual = ReflectionUtils.fetchAnnotationForMethod(
                bar.getClass(),
                "not-exist",
                AccessControl.class
        );

        // then
        assertThat(actual).isNull();
    }

    @Test
    public void fetchAnnotationForMethod_annotationDoesNotExist_shouldReturnNull() {
        // given
        IBar bar = new Bar();

        // when
        SuppressWarnings actual = ReflectionUtils.fetchAnnotationForMethod(
                bar.getClass(),
                "foo",
                SuppressWarnings.class
        );

        // then
        assertThat(actual).isNull();
    }

    public interface IBar {

        @AccessControl(authenticated = Authenticated.ANONYMOUS)
        void foo(String s);

    }

    public class Bar implements IBar {

        @AccessControl(authenticated = Authenticated.AUTHENTICATED)
        @Override
        public void foo(String s) {

        }

    }

}
