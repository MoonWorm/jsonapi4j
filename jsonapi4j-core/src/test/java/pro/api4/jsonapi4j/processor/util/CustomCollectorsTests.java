package pro.api4.jsonapi4j.processor.util;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomCollectorsTests {

    @Test
    public void toMapThatSupportsNullValues() {
        Map<Integer, Integer> result = Stream.of(1, 2, 3, 4)
                .collect(
                        CustomCollectors.toMapThatSupportsNullValues(
                                v -> v,
                                v -> {
                                    if (v % 2 == 0) {
                                        return null;
                                    } else {
                                        return v;
                                    }
                                }
                        )
                );
        assertThat(result).isNotNull().hasSize(4);
        assertThat(result.get(1)).isEqualTo(1);
        assertThat(result.get(2)).isEqualTo(null);
        assertThat(result.get(3)).isEqualTo(3);
        assertThat(result.get(4)).isEqualTo(null);
    }

}
