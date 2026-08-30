package pro.api4.jsonapi4j.plugin.oas.customizer.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.domain.ResourceType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The singular form reaches clients as generated method names, so a wrong guess is not cosmetic — it becomes part of
 * the published contract.
 */
class OasOperationInfoUtilTests {

    private static String singularizeType(String resourceType) {
        return OasOperationInfoUtil.singularize(new ResourceType(resourceType));
    }

    @Nested
    class Singularize {

        @Test
        void singularize_pluralEndingInIes_replacesItWithY() {
            assertThat(singularizeType("countries")).isEqualTo("country");
            assertThat(singularizeType("currencies")).isEqualTo("currency");
            assertThat(singularizeType("categories")).isEqualTo("category");
        }

        @Test
        void singularize_pluralEndingInSibilantEs_dropsOnlyTheEs() {
            assertThat(singularizeType("addresses")).isEqualTo("address");
            assertThat(singularizeType("boxes")).isEqualTo("box");
            assertThat(singularizeType("batches")).isEqualTo("batch");
            assertThat(singularizeType("dishes")).isEqualTo("dish");
        }

        @Test
        void singularize_regularPlural_dropsTheTrailingS() {
            assertThat(singularizeType("users")).isEqualTo("user");
            assertThat(singularizeType("days")).isEqualTo("day");
        }

        @Test
        void singularize_wordThatOnlyLooksPlural_isLeftAlone() {
            assertThat(singularizeType("status")).isEqualTo("status");
            assertThat(singularizeType("bonus")).isEqualTo("bonus");
        }

        @Test
        void singularize_alreadySingular_isLeftAlone() {
            assertThat(singularizeType("country")).isEqualTo("country");
            assertThat(singularizeType("user")).isEqualTo("user");
        }

    }

}
