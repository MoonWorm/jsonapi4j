package pro.api4.jsonapi4j.compound.docs.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CacheResultTests {

    @Test
    void constructor_nullResourceJson_throwsNullPointerException() {
        assertThatThrownBy(() -> new CacheResult(null, 300))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("resourceJson");
    }

    @Test
    void constructor_negativeTtl_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new CacheResult("{}", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remainingTtlSeconds");
    }

    @Test
    void constructor_zeroTtl_succeeds() {
        CacheResult result = new CacheResult("{}", 0);

        assertThat(result.getRemainingTtlSeconds()).isZero();
    }

    @Test
    void constructor_positiveTtl_succeeds() {
        CacheResult result = new CacheResult("{}", 300);

        assertThat(result.getRemainingTtlSeconds()).isEqualTo(300);
    }

    @Test
    void getResourceJson_returnsStoredValue() {
        String json = "{\"type\":\"countries\",\"id\":\"FI\"}";
        CacheResult result = new CacheResult(json, 120);

        assertThat(result.getResourceJson()).isEqualTo(json);
    }

    @Test
    void getRemainingTtlSeconds_returnsStoredValue() {
        CacheResult result = new CacheResult("{}", 120);

        assertThat(result.getRemainingTtlSeconds()).isEqualTo(120);
    }

    @Test
    void equals_sameValues_areEqual() {
        CacheResult result1 = new CacheResult("{\"id\":\"1\"}", 300);
        CacheResult result2 = new CacheResult("{\"id\":\"1\"}", 300);

        assertThat(result1).isEqualTo(result2);
    }

    @Test
    void equals_differentJson_areNotEqual() {
        CacheResult result1 = new CacheResult("{\"id\":\"1\"}", 300);
        CacheResult result2 = new CacheResult("{\"id\":\"2\"}", 300);

        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void equals_differentTtl_areNotEqual() {
        CacheResult result1 = new CacheResult("{\"id\":\"1\"}", 300);
        CacheResult result2 = new CacheResult("{\"id\":\"1\"}", 60);

        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void hashCode_sameValues_sameHash() {
        CacheResult result1 = new CacheResult("{\"id\":\"1\"}", 300);
        CacheResult result2 = new CacheResult("{\"id\":\"1\"}", 300);

        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    void toString_containsTtl() {
        CacheResult result = new CacheResult("{}", 120);

        assertThat(result.toString()).contains("120");
    }

}
