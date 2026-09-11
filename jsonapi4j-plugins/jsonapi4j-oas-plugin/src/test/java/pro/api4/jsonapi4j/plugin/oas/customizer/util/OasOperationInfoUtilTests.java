package pro.api4.jsonapi4j.plugin.oas.customizer.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.operation.OperationType;

import java.util.List;

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

    /**
     * The list is a contract: a client generator turns it into the failures its callers can handle, and a mock server
     * or a response-validating gateway turns it into the failures it will allow through. A code the server really
     * answers with but the document omits falls through both; a code the server cannot produce makes callers handle a
     * case that never arrives.
     */
    @Nested
    class SupportedHttpErrorCodes {

        @Test
        void resolveSupportedHttpErrorCodes_anyOperation_documentsWhatEveryOperationCanAnswerWith() {
            for (OperationType operationType : OperationType.values()) {
                assertThat(codesOf(operationType))
                        .as(operationType.name())
                        .contains(400, 405, 406, 429, 500);
            }
        }

        @Test
        void resolveSupportedHttpErrorCodes_operationWithIdInThePath_documents404() {
            for (OperationType operationType : OperationType.getExistingResourceAwareOperations()) {
                assertThat(codesOf(operationType)).as(operationType.name()).contains(404);
            }
        }

        @Test
        void resolveSupportedHttpErrorCodes_collectionReadAndCreate_documentNo404() {
            assertThat(codesOf(OperationType.READ_MULTIPLE_RESOURCES)).doesNotContain(404);
            assertThat(codesOf(OperationType.CREATE_RESOURCE)).doesNotContain(404);
        }

        @Test
        void resolveSupportedHttpErrorCodes_operationCarryingARequestBody_documents415() {
            for (OperationType operationType : OperationType.values()) {
                if (OasOperationInfoUtil.hasRequestBody(operationType)) {
                    assertThat(codesOf(operationType)).as(operationType.name()).contains(415);
                }
            }
        }

        @Test
        void resolveSupportedHttpErrorCodes_operationCarryingNoRequestBody_documentsNo415() {
            for (OperationType operationType : OperationType.values()) {
                if (!OasOperationInfoUtil.hasRequestBody(operationType)) {
                    assertThat(codesOf(operationType)).as(operationType.name()).doesNotContain(415);
                }
            }
        }

        @Test
        void resolveSupportedHttpErrorCodes_deleteResource_documentsNo415ButItsRelationshipCounterpartDoes() {
            assertThat(codesOf(OperationType.DELETE_RESOURCE)).doesNotContain(415);
            assertThat(codesOf(OperationType.DELETE_TO_MANY_RELATIONSHIP)).contains(415);
        }

        @Test
        void resolveSupportedHttpErrorCodes_createAndUpdateResource_document409() {
            assertThat(codesOf(OperationType.CREATE_RESOURCE)).contains(409);
            assertThat(codesOf(OperationType.UPDATE_RESOURCE)).contains(409);
        }

        @Test
        void resolveSupportedHttpErrorCodes_anyOtherOperation_documentsNo409() {
            for (OperationType operationType : OperationType.values()) {
                if (operationType != OperationType.CREATE_RESOURCE && operationType != OperationType.UPDATE_RESOURCE) {
                    assertThat(codesOf(operationType)).as(operationType.name()).doesNotContain(409);
                }
            }
        }

        @Test
        void resolveSupportedHttpErrorCodes_createResource_documents403ForAClientGeneratedId() {
            assertThat(codesOf(OperationType.CREATE_RESOURCE)).contains(403);
        }

        @Test
        void resolveSupportedHttpErrorCodes_anyOtherOperation_leaves403ToTheCaller() {
            for (OperationType operationType : OperationType.values()) {
                if (operationType != OperationType.CREATE_RESOURCE) {
                    assertThat(codesOf(operationType)).as(operationType.name()).doesNotContain(403);
                }
            }
        }

        @Test
        void resolveSupportedHttpErrorCodes_anyOperation_leaves401ToTheCaller() {
            for (OperationType operationType : OperationType.values()) {
                assertThat(codesOf(operationType)).as(operationType.name()).doesNotContain(401);
            }
        }

        @Test
        void resolveSupportedHttpErrorCodes_anyOperation_iteratesInAscendingStatusCodeOrder() {
            for (OperationType operationType : OperationType.values()) {
                assertThat(codesOf(operationType)).as(operationType.name()).isSorted();
            }
        }

        private List<Integer> codesOf(OperationType operationType) {
            return OasOperationInfoUtil.resolveSupportedHttpErrorCodes(operationType)
                    .stream()
                    .map(HttpStatusCodes::getCode)
                    .toList();
        }

    }

}
