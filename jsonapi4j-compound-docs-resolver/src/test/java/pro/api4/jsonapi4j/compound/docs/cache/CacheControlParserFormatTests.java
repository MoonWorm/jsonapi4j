package pro.api4.jsonapi4j.compound.docs.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CacheControlParserFormatTests {

    @Test
    void format_maxAgeOnly_returnsMaxAgeString() {
        CacheControlDirectives directives = CacheControlParser.parse("max-age=300");
        assertThat(CacheControlParser.format(directives)).isEqualTo("max-age=300");
    }

    @Test
    void format_sMaxAgeOnly_returnsSMaxAgeString() {
        CacheControlDirectives directives = CacheControlParser.parse("s-maxage=120");
        assertThat(CacheControlParser.format(directives)).isEqualTo("s-maxage=120");
    }

    @Test
    void format_maxAgeAndSMaxAge_returnsBoth() {
        CacheControlDirectives directives = CacheControlParser.parse("max-age=300, s-maxage=60");
        String formatted = CacheControlParser.format(directives);
        assertThat(formatted).isEqualTo("s-maxage=60, max-age=300");
    }

    @Test
    void format_noStore_returnsNoStore() {
        CacheControlDirectives directives = CacheControlParser.parse("no-store");
        assertThat(CacheControlParser.format(directives)).isEqualTo("no-store");
    }

    @Test
    void format_noCache_returnsNoCache() {
        CacheControlDirectives directives = CacheControlParser.parse("no-cache");
        assertThat(CacheControlParser.format(directives)).isEqualTo("no-cache");
    }

    @Test
    void format_private_returnsPrivate() {
        CacheControlDirectives directives = CacheControlParser.parse("private");
        assertThat(CacheControlParser.format(directives)).isEqualTo("private");
    }

    @Test
    void format_privateWithMaxAge_returnsBoth() {
        CacheControlDirectives directives = CacheControlParser.parse("private, max-age=60");
        String formatted = CacheControlParser.format(directives);
        assertThat(formatted).contains("private");
        assertThat(formatted).contains("max-age=60");
    }

    @Test
    void format_allFlags_returnsAll() {
        CacheControlDirectives directives = CacheControlParser.parse("no-store, no-cache, private, max-age=60, s-maxage=30");
        String formatted = CacheControlParser.format(directives);
        assertThat(formatted).contains("no-store");
        assertThat(formatted).contains("no-cache");
        assertThat(formatted).contains("private");
        assertThat(formatted).contains("max-age=60");
        assertThat(formatted).contains("s-maxage=30");
    }

    @Test
    void format_nonCacheable_returnsNull() {
        assertThat(CacheControlParser.format(CacheControlDirectives.NON_CACHEABLE)).isNull();
    }

    @Test
    void format_roundTrip_parseAndFormatPreservesValues() {
        String original = "max-age=300";
        CacheControlDirectives parsed = CacheControlParser.parse(original);
        String formatted = CacheControlParser.format(parsed);
        CacheControlDirectives reparsed = CacheControlParser.parse(formatted);

        assertThat(reparsed.getMaxAge()).isEqualTo(parsed.getMaxAge());
        assertThat(reparsed.getSMaxAge()).isEqualTo(parsed.getSMaxAge());
        assertThat(reparsed.isNoStore()).isEqualTo(parsed.isNoStore());
        assertThat(reparsed.isNoCache()).isEqualTo(parsed.isNoCache());
        assertThat(reparsed.isPrivateCacheControl()).isEqualTo(parsed.isPrivateCacheControl());
    }
}
