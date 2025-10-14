package pro.api4.jsonapi4j.processor.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class CustomCollectors {

    private CustomCollectors() {

    }

    public static <T, K, U>
    Collector<T, ?, LinkedHashMap<K, U>> toOrderedMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper
    ) {
        return Collectors.toMap(
                keyMapper,
                valueMapper,
                (x, y) -> y,
                LinkedHashMap::new
        );
    }

    public static <T, K, U>
    Collector<T, ?, Map<K, U>> toMapThatSupportsNullValues(Function<? super T, ? extends K> keyMapper,
                                                           Function<? super T, ? extends U> valueMapper) {
        return Collector.of(
                HashMap::new,
                uniqKeysMapAccumulator(keyMapper, valueMapper),
                uniqKeysMapMerger(),
                Collector.Characteristics.IDENTITY_FINISH);
    }

    private static <T, K, V>
    BiConsumer<Map<K, V>, T> uniqKeysMapAccumulator(Function<? super T, ? extends K> keyMapper,
                                                    Function<? super T, ? extends V> valueMapper) {
        return (map, element) -> {
            K k = keyMapper.apply(element);
            V v = valueMapper.apply(element);
            V u = map.putIfAbsent(k, v);
            if (u != null) throw duplicateKeyException(k, u, v);
        };
    }

    private static <K, V, M extends Map<K, V>>
    BinaryOperator<M> uniqKeysMapMerger() {
        return (m1, m2) -> {
            for (Map.Entry<K, V> e : m2.entrySet()) {
                K k = e.getKey();
                V v = e.getValue();
                V u = m1.putIfAbsent(k, v);
                if (u != null) throw duplicateKeyException(k, u, v);
            }
            return m1;
        };
    }

    private static IllegalStateException duplicateKeyException(
            Object k, Object u, Object v) {
        return new IllegalStateException(String.format(
                "Duplicate key %s (attempted merging values %s and %s)",
                k, u, v));
    }

}
