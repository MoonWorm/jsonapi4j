package pro.api4.jsonapi4j.util;

import org.junit.jupiter.api.Test;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CustomCollectorsTests {

    // --- toOrderedMap ---

    @Test
    public void toOrderedMap_preservesInsertionOrder() {
        // given
        List<String> input = List.of("b", "a", "c");

        // when
        LinkedHashMap<String, String> result = input.stream()
                .collect(CustomCollectors.toOrderedMap(s -> s, String::toUpperCase));

        // then
        assertThat(result.keySet()).containsExactly("b", "a", "c");
        assertThat(result.values()).containsExactly("B", "A", "C");
    }

    @Test
    public void toOrderedMap_duplicateKeys_lastValueWins() {
        // given
        List<Map.Entry<String, Integer>> input = List.of(
                Map.entry("a", 1),
                Map.entry("b", 2),
                Map.entry("a", 3)
        );

        // when
        LinkedHashMap<String, Integer> result = input.stream()
                .collect(CustomCollectors.toOrderedMap(Map.Entry::getKey, Map.Entry::getValue));

        // then
        assertThat(result).containsEntry("a", 3);
        assertThat(result).hasSize(2);
    }

    @Test
    public void toOrderedMap_emptyStream_returnsEmptyMap() {
        // when
        LinkedHashMap<String, String> result = Stream.<String>empty()
                .collect(CustomCollectors.toOrderedMap(s -> s, s -> s));

        // then
        assertThat(result).isEmpty();
        assertThat(result).isInstanceOf(LinkedHashMap.class);
    }

    // --- toMapThatSupportsNullValues ---

    @Test
    public void toMapThatSupportsNullValues_nullValueAllowed() {
        // given
        List<Map.Entry<String, String>> input = List.of(
                Map.entry("a", "value"),
                new AbstractMap.SimpleEntry<>("b", null)
        );

        // when
        Map<String, String> result = input.stream()
                .collect(CustomCollectors.toMapThatSupportsNullValues(Map.Entry::getKey, Map.Entry::getValue));

        // then
        assertThat(result).containsEntry("a", "value");
        assertThat(result).containsKey("b");
        assertThat(result.get("b")).isNull();
    }

    @Test
    public void toMapThatSupportsNullValues_duplicateKey_throwsException() {
        // given
        List<Map.Entry<String, String>> input = List.of(
                Map.entry("a", "first"),
                Map.entry("a", "second")
        );

        // when / then
        assertThatThrownBy(() -> input.stream()
                .collect(CustomCollectors.toMapThatSupportsNullValues(Map.Entry::getKey, Map.Entry::getValue)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate key");
    }

    // --- toOrderedMapThatSupportsNullValues ---

    @Test
    public void toOrderedMapThatSupportsNullValues_preservesOrderAndAllowsNulls() {
        // given
        List<Map.Entry<String, String>> input = List.of(
                Map.entry("b", "value-b"),
                new AbstractMap.SimpleEntry<>("a", null),
                Map.entry("c", "value-c")
        );

        // when
        Map<String, String> result = input.stream()
                .collect(CustomCollectors.toOrderedMapThatSupportsNullValues(Map.Entry::getKey, Map.Entry::getValue));

        // then
        assertThat(result).isInstanceOf(LinkedHashMap.class);
        assertThat(result.keySet()).containsExactly("b", "a", "c");
        assertThat(result.get("a")).isNull();
        assertThat(result).containsEntry("b", "value-b");
    }

    @Test
    public void toOrderedMapThatSupportsNullValues_duplicateKey_throwsException() {
        // given
        List<Map.Entry<String, String>> input = List.of(
                Map.entry("x", "first"),
                Map.entry("x", "second")
        );

        // when / then
        assertThatThrownBy(() -> input.stream()
                .collect(CustomCollectors.toOrderedMapThatSupportsNullValues(Map.Entry::getKey, Map.Entry::getValue)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate key");
    }

}
