package pro.api4.jsonapi4j.processor.resolvers.links;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.CursorAwareRequest;
import pro.api4.jsonapi4j.request.LimitOffsetAwareRequest;
import pro.api4.jsonapi4j.response.PaginationContext;
import pro.api4.jsonapi4j.response.PaginationMode;

import static org.assertj.core.api.Assertions.assertThat;

public class LinksGeneratorPaginationLinksTests {

    private static final String BASE_PATH = "/articles";

    // --- generateFirstLink ---

    @Nested
    class GenerateFirstLink {

        @Test
        void nullPaginationContext_returnsNull() {
            // given
            var request = new LimitOffsetRequest(20L, 40L);
            var generator = new LinksGenerator(request);

            // when
            String result = generator.generateFirstLink(BASE_PATH, null, false, false, false, false, false);

            // then
            assertThat(result).isNull();
        }

        @Test
        void cursorMode_returnsUrlWithoutCursor() {
            // given
            var request = new CursorRequest("abc123");
            var generator = new LinksGenerator(request);
            var ctx = PaginationContext.builder().mode(PaginationMode.CURSOR).nextCursor("def456").build();

            // when
            String result = generator.generateFirstLink(BASE_PATH, ctx, false, false, false, false, false);

            // then
            assertThat(result).isEqualTo("/articles");
        }

        @Test
        void limitOffsetMode_returnsUrlWithOffsetZero() {
            // given
            var request = new LimitOffsetRequest(10L, 30L);
            var generator = new LinksGenerator(request);
            var ctx = PaginationContext.builder().mode(PaginationMode.LIMIT_OFFSET).totalItems(100L).build();

            // when
            String result = generator.generateFirstLink(BASE_PATH, ctx, false, false, false, false, false);

            // then
            assertThat(result).isEqualTo("/articles?page%5Blimit%5D=10&page%5Boffset%5D=0");
        }

        @Test
        void limitOffsetMode_defaultLimit_returnsUrlWithDefaultLimit() {
            // given
            var request = new LimitOffsetRequest(null, 40L);
            var generator = new LinksGenerator(request);
            var ctx = PaginationContext.builder().mode(PaginationMode.LIMIT_OFFSET).totalItems(100L).build();

            // when
            String result = generator.generateFirstLink(BASE_PATH, ctx, false, false, false, false, false);

            // then
            assertThat(result).isEqualTo("/articles?page%5Blimit%5D=20&page%5Boffset%5D=0");
        }
    }

    // --- generatePrevLink ---

    @Nested
    class GeneratePrevLink {

        @Test
        void nullPaginationContext_returnsNull() {
            // given
            var request = new LimitOffsetRequest(20L, 40L);
            var generator = new LinksGenerator(request);

            // when
            String result = generator.generatePrevLink(BASE_PATH, null, false, false, false, false, false);

            // then
            assertThat(result).isNull();
        }

        @Test
        void cursorMode_returnsNull() {
            // given
            var request = new CursorRequest("abc123");
            var generator = new LinksGenerator(request);
            var ctx = PaginationContext.builder().mode(PaginationMode.CURSOR).nextCursor("def456").build();

            // when
            String result = generator.generatePrevLink(BASE_PATH, ctx, false, false, false, false, false);

            // then
            assertThat(result).isNull();
        }

        @Test
        void limitOffsetMode_onFirstPage_returnsNull() {
            // given
            var request = new LimitOffsetRequest(10L, 0L);
            var generator = new LinksGenerator(request);
            var ctx = PaginationContext.builder().mode(PaginationMode.LIMIT_OFFSET).totalItems(100L).build();

            // when
            String result = generator.generatePrevLink(BASE_PATH, ctx, false, false, false, false, false);

            // then
            assertThat(result).isNull();
        }

        @Test
        void limitOffsetMode_nullOffset_returnsNull() {
            // given
            var request = new LimitOffsetRequest(10L, null);
            var generator = new LinksGenerator(request);
            var ctx = PaginationContext.builder().mode(PaginationMode.LIMIT_OFFSET).totalItems(100L).build();

            // when
            String result = generator.generatePrevLink(BASE_PATH, ctx, false, false, false, false, false);

            // then
            assertThat(result).isNull();
        }

        @Test
        void limitOffsetMode_onSecondPage_returnsPrevOffset() {
            // given
            var request = new LimitOffsetRequest(10L, 10L);
            var generator = new LinksGenerator(request);
            var ctx = PaginationContext.builder().mode(PaginationMode.LIMIT_OFFSET).totalItems(100L).build();

            // when
            String result = generator.generatePrevLink(BASE_PATH, ctx, false, false, false, false, false);

            // then
            assertThat(result).isEqualTo("/articles?page%5Blimit%5D=10&page%5Boffset%5D=0");
        }

        @Test
        void limitOffsetMode_onThirdPage_returnsPrevOffset() {
            // given
            var request = new LimitOffsetRequest(10L, 20L);
            var generator = new LinksGenerator(request);
            var ctx = PaginationContext.builder().mode(PaginationMode.LIMIT_OFFSET).totalItems(100L).build();

            // when
            String result = generator.generatePrevLink(BASE_PATH, ctx, false, false, false, false, false);

            // then
            assertThat(result).isEqualTo("/articles?page%5Blimit%5D=10&page%5Boffset%5D=10");
        }

        @Test
        void limitOffsetMode_offsetSmallerThanLimit_prevClampsToZero() {
            // given — offset=5, limit=10 → prev should be offset=0
            var request = new LimitOffsetRequest(10L, 5L);
            var generator = new LinksGenerator(request);
            var ctx = PaginationContext.builder().mode(PaginationMode.LIMIT_OFFSET).totalItems(100L).build();

            // when
            String result = generator.generatePrevLink(BASE_PATH, ctx, false, false, false, false, false);

            // then
            assertThat(result).isEqualTo("/articles?page%5Blimit%5D=10&page%5Boffset%5D=0");
        }
    }

    // --- generateLastLink ---

    @Nested
    class GenerateLastLink {

        @Test
        void nullPaginationContext_returnsNull() {
            // given
            var request = new LimitOffsetRequest(20L, 0L);
            var generator = new LinksGenerator(request);

            // when
            String result = generator.generateLastLink(BASE_PATH, null, false, false, false, false, false);

            // then
            assertThat(result).isNull();
        }

        @Test
        void cursorMode_returnsNull() {
            // given
            var request = new CursorRequest("abc123");
            var generator = new LinksGenerator(request);
            var ctx = PaginationContext.builder().mode(PaginationMode.CURSOR).nextCursor("def456").build();

            // when
            String result = generator.generateLastLink(BASE_PATH, ctx, false, false, false, false, false);

            // then
            assertThat(result).isNull();
        }

        @Test
        void limitOffsetMode_nullTotalItems_returnsNull() {
            // given
            var request = new LimitOffsetRequest(10L, 0L);
            var generator = new LinksGenerator(request);
            var ctx = PaginationContext.builder().mode(PaginationMode.LIMIT_OFFSET).totalItems(null).build();

            // when
            String result = generator.generateLastLink(BASE_PATH, ctx, false, false, false, false, false);

            // then
            assertThat(result).isNull();
        }

        @Test
        void limitOffsetMode_totalItemsDivisibleByLimit() {
            // given — 100 items, limit=10 → last offset=90
            var request = new LimitOffsetRequest(10L, 0L);
            var generator = new LinksGenerator(request);
            var ctx = PaginationContext.builder().mode(PaginationMode.LIMIT_OFFSET).totalItems(100L).build();

            // when
            String result = generator.generateLastLink(BASE_PATH, ctx, false, false, false, false, false);

            // then
            assertThat(result).isEqualTo("/articles?page%5Blimit%5D=10&page%5Boffset%5D=90");
        }

        @Test
        void limitOffsetMode_totalItemsNotDivisibleByLimit() {
            // given — 25 items, limit=10 → last offset=20
            var request = new LimitOffsetRequest(10L, 0L);
            var generator = new LinksGenerator(request);
            var ctx = PaginationContext.builder().mode(PaginationMode.LIMIT_OFFSET).totalItems(25L).build();

            // when
            String result = generator.generateLastLink(BASE_PATH, ctx, false, false, false, false, false);

            // then
            assertThat(result).isEqualTo("/articles?page%5Blimit%5D=10&page%5Boffset%5D=20");
        }

        @Test
        void limitOffsetMode_singlePage() {
            // given — 5 items, limit=10 → last offset=0
            var request = new LimitOffsetRequest(10L, 0L);
            var generator = new LinksGenerator(request);
            var ctx = PaginationContext.builder().mode(PaginationMode.LIMIT_OFFSET).totalItems(5L).build();

            // when
            String result = generator.generateLastLink(BASE_PATH, ctx, false, false, false, false, false);

            // then
            assertThat(result).isEqualTo("/articles?page%5Blimit%5D=10&page%5Boffset%5D=0");
        }

        @Test
        void limitOffsetMode_zeroTotalItems() {
            // given — 0 items, limit=10 → last offset=0
            var request = new LimitOffsetRequest(10L, 0L);
            var generator = new LinksGenerator(request);
            var ctx = PaginationContext.builder().mode(PaginationMode.LIMIT_OFFSET).totalItems(0L).build();

            // when
            String result = generator.generateLastLink(BASE_PATH, ctx, false, false, false, false, false);

            // then
            assertThat(result).isEqualTo("/articles?page%5Blimit%5D=10&page%5Boffset%5D=0");
        }

        @Test
        void limitOffsetMode_exactlyOnePage() {
            // given — 10 items, limit=10 → last offset=0
            var request = new LimitOffsetRequest(10L, 0L);
            var generator = new LinksGenerator(request);
            var ctx = PaginationContext.builder().mode(PaginationMode.LIMIT_OFFSET).totalItems(10L).build();

            // when
            String result = generator.generateLastLink(BASE_PATH, ctx, false, false, false, false, false);

            // then
            assertThat(result).isEqualTo("/articles?page%5Blimit%5D=10&page%5Boffset%5D=0");
        }
    }

    // --- Inner request types ---

    @Data
    @AllArgsConstructor
    private static class LimitOffsetRequest implements LimitOffsetAwareRequest {
        private Long limit;
        private Long offset;
    }

    @Data
    @AllArgsConstructor
    private static class CursorRequest implements CursorAwareRequest {
        private String cursor;
    }
}
