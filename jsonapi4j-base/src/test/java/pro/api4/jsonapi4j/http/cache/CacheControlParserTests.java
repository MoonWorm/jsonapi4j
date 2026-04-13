package pro.api4.jsonapi4j.http.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CacheControlParserTests {

    @Test
    public void parse_nullInput_returnsNonCacheable() {
        CacheControlDirectives result = CacheControlParser.parse(null);

        assertThat(result).isSameAs(CacheControlDirectives.NON_CACHEABLE);
        assertThat(result.isCacheable()).isFalse();
    }

    @Test
    public void parse_emptyString_returnsNonCacheable() {
        CacheControlDirectives result = CacheControlParser.parse("");

        assertThat(result).isSameAs(CacheControlDirectives.NON_CACHEABLE);
        assertThat(result.isCacheable()).isFalse();
    }

    @Test
    public void parse_blankString_returnsNonCacheable() {
        CacheControlDirectives result = CacheControlParser.parse("   ");

        assertThat(result).isSameAs(CacheControlDirectives.NON_CACHEABLE);
        assertThat(result.isCacheable()).isFalse();
    }

    @Test
    public void parse_maxAgeOnly_returnsCacheableWithTtl() {
        CacheControlDirectives result = CacheControlParser.parse("max-age=300");

        assertThat(result.isCacheable()).isTrue();
        assertThat(result.getMaxAge()).isEqualTo(300L);
        assertThat(result.getEffectiveTtlSeconds()).isEqualTo(300L);
    }

    @Test
    public void parse_sMaxAgeOnly_returnsCacheableWithTtl() {
        CacheControlDirectives result = CacheControlParser.parse("s-maxage=120");

        assertThat(result.isCacheable()).isTrue();
        assertThat(result.getSMaxAge()).isEqualTo(120L);
        assertThat(result.getMaxAge()).isNull();
        assertThat(result.getEffectiveTtlSeconds()).isEqualTo(120L);
    }

    @Test
    public void parse_maxAgeAndSMaxAge_sMaxAgeTakesPrecedence() {
        CacheControlDirectives result = CacheControlParser.parse("max-age=300, s-maxage=60");

        assertThat(result.isCacheable()).isTrue();
        assertThat(result.getMaxAge()).isEqualTo(300L);
        assertThat(result.getSMaxAge()).isEqualTo(60L);
        assertThat(result.getEffectiveTtlSeconds()).isEqualTo(60L);
    }

    @Test
    public void parse_noStore_returnsNonCacheable() {
        CacheControlDirectives result = CacheControlParser.parse("no-store");

        assertThat(result.isCacheable()).isFalse();
        assertThat(result.isNoStore()).isTrue();
    }

    @Test
    public void parse_noCache_returnsNonCacheable() {
        CacheControlDirectives result = CacheControlParser.parse("no-cache");

        assertThat(result.isCacheable()).isFalse();
        assertThat(result.isNoCache()).isTrue();
    }

    @Test
    public void parse_privateDirective_returnsNonCacheable() {
        CacheControlDirectives result = CacheControlParser.parse("private");

        assertThat(result.isCacheable()).isFalse();
        assertThat(result.isPrivateCacheControl()).isTrue();
    }

    @Test
    public void parse_noStoreWithMaxAge_returnsNonCacheable() {
        CacheControlDirectives result = CacheControlParser.parse("no-store, max-age=300");

        assertThat(result.isCacheable()).isFalse();
        assertThat(result.isNoStore()).isTrue();
        assertThat(result.getMaxAge()).isEqualTo(300L);
    }

    @Test
    public void parse_privateWithMaxAge_returnsNonCacheable() {
        CacheControlDirectives result = CacheControlParser.parse("private, max-age=300");

        assertThat(result.isCacheable()).isFalse();
        assertThat(result.isPrivateCacheControl()).isTrue();
        assertThat(result.getMaxAge()).isEqualTo(300L);
    }

    @Test
    public void parse_caseInsensitive_parsesCorrectly() {
        CacheControlDirectives result = CacheControlParser.parse("Max-Age=300, No-Store");

        assertThat(result.isCacheable()).isFalse();
        assertThat(result.isNoStore()).isTrue();
        assertThat(result.getMaxAge()).isEqualTo(300L);
    }

    @Test
    public void parse_invalidMaxAgeValue_returnsNonCacheable() {
        CacheControlDirectives result = CacheControlParser.parse("max-age=abc");

        assertThat(result.isCacheable()).isFalse();
        assertThat(result.getMaxAge()).isNull();
    }

    @Test
    public void parse_negativeMaxAge_returnsNonCacheable() {
        CacheControlDirectives result = CacheControlParser.parse("max-age=-1");

        assertThat(result.isCacheable()).isFalse();
        assertThat(result.getMaxAge()).isNull();
    }

    @Test
    public void parse_zeroMaxAge_returnsNonCacheable() {
        CacheControlDirectives result = CacheControlParser.parse("max-age=0");

        assertThat(result.isCacheable()).isFalse();
        assertThat(result.getMaxAge()).isEqualTo(0L);
        assertThat(result.getEffectiveTtlSeconds()).isEqualTo(0L);
    }

    @Test
    public void parse_unknownDirectivesIgnored_parsesKnownOnes() {
        CacheControlDirectives result = CacheControlParser.parse("public, max-age=300, must-revalidate");

        assertThat(result.isCacheable()).isTrue();
        assertThat(result.getMaxAge()).isEqualTo(300L);
        assertThat(result.getEffectiveTtlSeconds()).isEqualTo(300L);
    }

    @Test
    public void parse_duplicateMaxAge_lastValueWins() {
        CacheControlDirectives result = CacheControlParser.parse("max-age=300, max-age=60");

        assertThat(result.isCacheable()).isTrue();
        assertThat(result.getMaxAge()).isEqualTo(60L);
        assertThat(result.getEffectiveTtlSeconds()).isEqualTo(60L);
    }

    @Test
    public void parse_directivesWithExtraWhitespace_parsesCorrectly() {
        CacheControlDirectives result = CacheControlParser.parse("  max-age=300 ,  no-cache  ");

        assertThat(result.isCacheable()).isFalse();
        assertThat(result.isNoCache()).isTrue();
        assertThat(result.getMaxAge()).isEqualTo(300L);
    }

}
