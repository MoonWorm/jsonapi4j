package pro.api4.jsonapi4j.operation.validation;

import java.util.Collection;
import java.util.Map;

public final class Validate {

    private Validate() {

    }

    public static StringValidationAssert assertThat(String value) {
        return new StringValidationAssert(value, null);
    }

    public static NumberValidationAssert<Integer> assertThat(Integer value) {
        return new NumberValidationAssert<>(value, null);
    }

    public static NumberValidationAssert<Long> assertThat(Long value) {
        return new NumberValidationAssert<>(value, null);
    }

    public static <E> CollectionValidationAssert<E> assertThat(Collection<E> value) {
        return new CollectionValidationAssert<>(value, null);
    }

    public static <K, V> MapValidationAssert<K, V> assertThat(Map<K, V> value) {
        return new MapValidationAssert<>(value, null);
    }

    public static <T> ObjectValidationAssert<?, T> assertThat(T value) {
        return new ObjectValidationAssert<>(value, null);
    }

}
