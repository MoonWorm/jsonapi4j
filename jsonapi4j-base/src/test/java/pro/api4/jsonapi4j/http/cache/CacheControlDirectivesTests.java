package pro.api4.jsonapi4j.http.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CacheControlDirectivesTests {

    @Test
    public void nonCacheableConstant_isCacheableReturnsFalse() {
        assertThat(CacheControlDirectives.NON_CACHEABLE.isCacheable()).isFalse();
    }

    @Test
    public void nonCacheableConstant_effectiveTtlReturnsNull() {
        assertThat(CacheControlDirectives.NON_CACHEABLE.getEffectiveTtlSeconds()).isNull();
    }

    @Test
    public void isCacheable_withNoStore_returnsFalse() {
        CacheControlDirectives directives = new CacheControlDirectives(300L, null, true, false, false);

        assertThat(directives.isCacheable()).isFalse();
    }

    @Test
    public void isCacheable_withNoCache_returnsFalse() {
        CacheControlDirectives directives = new CacheControlDirectives(300L, null, false, true, false);

        assertThat(directives.isCacheable()).isFalse();
    }

    @Test
    public void isCacheable_withPrivate_returnsFalse() {
        CacheControlDirectives directives = new CacheControlDirectives(300L, null, false, false, true);

        assertThat(directives.isCacheable()).isFalse();
    }

    @Test
    public void isCacheable_withNullTtl_returnsFalse() {
        CacheControlDirectives directives = new CacheControlDirectives(null, null, false, false, false);

        assertThat(directives.isCacheable()).isFalse();
    }

    @Test
    public void isCacheable_withZeroTtl_returnsFalse() {
        CacheControlDirectives directives = new CacheControlDirectives(0L, null, false, false, false);

        assertThat(directives.isCacheable()).isFalse();
    }

    @Test
    public void isCacheable_withPositiveTtl_returnsTrue() {
        CacheControlDirectives directives = new CacheControlDirectives(300L, null, false, false, false);

        assertThat(directives.isCacheable()).isTrue();
    }

    @Test
    public void effectiveTtlSeconds_sMaxAgePrecedence() {
        CacheControlDirectives directives = new CacheControlDirectives(300L, 60L, false, false, false);

        assertThat(directives.getEffectiveTtlSeconds()).isEqualTo(60L);
    }

    @Test
    public void effectiveTtlSeconds_fallsBackToMaxAge() {
        CacheControlDirectives directives = new CacheControlDirectives(300L, null, false, false, false);

        assertThat(directives.getEffectiveTtlSeconds()).isEqualTo(300L);
    }

    @Test
    public void equals_sameValues_areEqual() {
        CacheControlDirectives a = new CacheControlDirectives(300L, 60L, true, false, true);
        CacheControlDirectives b = new CacheControlDirectives(300L, 60L, true, false, true);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    public void equals_differentValues_areNotEqual() {
        CacheControlDirectives a = new CacheControlDirectives(300L, null, false, false, false);
        CacheControlDirectives b = new CacheControlDirectives(60L, null, false, false, false);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    public void toString_containsAllFields() {
        CacheControlDirectives directives = new CacheControlDirectives(300L, 60L, true, false, true);

        String result = directives.toString();

        assertThat(result).contains("maxAge=300");
        assertThat(result).contains("sMaxAge=60");
        assertThat(result).contains("noStore=true");
        assertThat(result).contains("noCache=false");
        assertThat(result).contains("privateCacheControl=true");
    }

}
