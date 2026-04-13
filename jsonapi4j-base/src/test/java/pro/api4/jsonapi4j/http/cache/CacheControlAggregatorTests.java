package pro.api4.jsonapi4j.http.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CacheControlAggregatorTests {

    // --- Empty aggregator ---

    @Test
    void getResult_noDirectivesAdded_returnsNull() {
        var aggregator = new CacheControlAggregator();
        assertThat(aggregator.getResult()).isNull();
    }

    @Test
    void getResult_onlyNullDirectivesAdded_returnsNull() {
        var aggregator = new CacheControlAggregator();
        aggregator.add(null);
        aggregator.add(null);
        assertThat(aggregator.getResult()).isNull();
    }

    // --- Single directive ---

    @Test
    void add_singleMaxAge_returnsMaxAge() {
        var aggregator = new CacheControlAggregator();
        aggregator.add(CacheControlParser.parse("max-age=300"));

        CacheControlDirectives result = aggregator.getResult();

        assertThat(result).isNotNull();
        assertThat(result.getMaxAge()).isEqualTo(300L);
        assertThat(result.isNoStore()).isFalse();
        assertThat(result.isNoCache()).isFalse();
        assertThat(result.isPrivateCacheControl()).isFalse();
    }

    @Test
    void add_singleNoStore_returnsNoStore() {
        var aggregator = new CacheControlAggregator();
        aggregator.add(CacheControlParser.parse("no-store"));

        CacheControlDirectives result = aggregator.getResult();

        assertThat(result).isNotNull();
        assertThat(result.isNoStore()).isTrue();
    }

    @Test
    void add_singlePrivate_returnsPrivate() {
        var aggregator = new CacheControlAggregator();
        aggregator.add(CacheControlParser.parse("private, max-age=60"));

        CacheControlDirectives result = aggregator.getResult();

        assertThat(result).isNotNull();
        assertThat(result.isPrivateCacheControl()).isTrue();
        assertThat(result.getMaxAge()).isEqualTo(60L);
    }

    @Test
    void add_singleNoCache_returnsNoCache() {
        var aggregator = new CacheControlAggregator();
        aggregator.add(CacheControlParser.parse("no-cache"));

        assertThat(aggregator.getResult().isNoCache()).isTrue();
    }

    @Test
    void add_singleSMaxAge_returnsSMaxAge() {
        var aggregator = new CacheControlAggregator();
        aggregator.add(CacheControlParser.parse("s-maxage=120"));

        assertThat(aggregator.getResult().getSMaxAge()).isEqualTo(120L);
    }

    // --- Multiple max-age values ---

    @Test
    void add_multipleMaxAge_returnsMinimum() {
        var aggregator = new CacheControlAggregator();
        aggregator.add(CacheControlParser.parse("max-age=300"));
        aggregator.add(CacheControlParser.parse("max-age=60"));

        CacheControlDirectives result = aggregator.getResult();

        assertThat(result).isNotNull();
        assertThat(result.getMaxAge()).isEqualTo(60L);
    }

    @Test
    void add_multipleSMaxAge_returnsMinimum() {
        var aggregator = new CacheControlAggregator();
        aggregator.add(CacheControlParser.parse("s-maxage=300"));
        aggregator.add(CacheControlParser.parse("s-maxage=120"));

        assertThat(aggregator.getResult().getSMaxAge()).isEqualTo(120L);
    }

    @Test
    void add_mixedMaxAgeAndSMaxAge_returnsMinimumsOfEach() {
        var aggregator = new CacheControlAggregator();
        aggregator.add(CacheControlParser.parse("max-age=300, s-maxage=200"));
        aggregator.add(CacheControlParser.parse("max-age=60, s-maxage=400"));

        CacheControlDirectives result = aggregator.getResult();

        assertThat(result.getMaxAge()).isEqualTo(60L);
        assertThat(result.getSMaxAge()).isEqualTo(200L);
    }

    // --- Sticky restrictive flags ---

    @Test
    void add_oneNoStoreAmongMany_resultHasNoStore() {
        var aggregator = new CacheControlAggregator();
        aggregator.add(CacheControlParser.parse("max-age=300"));
        aggregator.add(CacheControlParser.parse("no-store"));
        aggregator.add(CacheControlParser.parse("max-age=60"));

        CacheControlDirectives result = aggregator.getResult();

        assertThat(result.isNoStore()).isTrue();
        assertThat(result.getMaxAge()).isEqualTo(60L);
    }

    @Test
    void add_oneNoCacheAmongMany_resultHasNoCache() {
        var aggregator = new CacheControlAggregator();
        aggregator.add(CacheControlParser.parse("max-age=300"));
        aggregator.add(CacheControlParser.parse("no-cache"));

        assertThat(aggregator.getResult().isNoCache()).isTrue();
    }

    @Test
    void add_onePrivateAmongMany_resultHasPrivate() {
        var aggregator = new CacheControlAggregator();
        aggregator.add(CacheControlParser.parse("max-age=300"));
        aggregator.add(CacheControlParser.parse("private"));

        assertThat(aggregator.getResult().isPrivateCacheControl()).isTrue();
    }

    @Test
    void add_allRestrictiveFlags_resultHasAll() {
        var aggregator = new CacheControlAggregator();
        aggregator.add(CacheControlParser.parse("no-store"));
        aggregator.add(CacheControlParser.parse("no-cache"));
        aggregator.add(CacheControlParser.parse("private"));

        CacheControlDirectives result = aggregator.getResult();

        assertThat(result.isNoStore()).isTrue();
        assertThat(result.isNoCache()).isTrue();
        assertThat(result.isPrivateCacheControl()).isTrue();
    }

    // --- Mixed scenarios ---

    @Test
    void add_noStoreWithMaxAge_resultHasBoth() {
        var aggregator = new CacheControlAggregator();
        aggregator.add(CacheControlParser.parse("no-store, max-age=60"));

        CacheControlDirectives result = aggregator.getResult();

        assertThat(result.isNoStore()).isTrue();
        assertThat(result.getMaxAge()).isEqualTo(60L);
    }

    @Test
    void add_nonCacheableDirective_setsHasDirectivesButNoFlags() {
        var aggregator = new CacheControlAggregator();
        aggregator.add(CacheControlDirectives.NON_CACHEABLE);

        CacheControlDirectives result = aggregator.getResult();

        assertThat(result).isNotNull();
        assertThat(result.getMaxAge()).isNull();
        assertThat(result.isNoStore()).isFalse();
        assertThat(result.isNoCache()).isFalse();
        assertThat(result.isPrivateCacheControl()).isFalse();
    }

    // --- Edge cases ---

    @Test
    void add_zeroMaxAge_returnsZeroMaxAge() {
        var aggregator = new CacheControlAggregator();
        aggregator.add(CacheControlParser.parse("max-age=300"));
        aggregator.add(CacheControlParser.parse("max-age=0"));

        assertThat(aggregator.getResult().getMaxAge()).isEqualTo(0L);
    }

    @Test
    void add_nullDirective_ignored() {
        var aggregator = new CacheControlAggregator();
        aggregator.add(CacheControlParser.parse("max-age=300"));
        aggregator.add(null);

        CacheControlDirectives result = aggregator.getResult();

        assertThat(result).isNotNull();
        assertThat(result.getMaxAge()).isEqualTo(300L);
    }
}
