package pro.api4.jsonapi4j.plugin.oas.customizer.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.OperationType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.api4.jsonapi4j.operation.OperationType.ADD_TO_MANY_RELATIONSHIP;
import static pro.api4.jsonapi4j.operation.OperationType.CREATE_RESOURCE;
import static pro.api4.jsonapi4j.operation.OperationType.DELETE_RESOURCE;
import static pro.api4.jsonapi4j.operation.OperationType.DELETE_TO_MANY_RELATIONSHIP;
import static pro.api4.jsonapi4j.operation.OperationType.READ_MULTIPLE_RESOURCES;
import static pro.api4.jsonapi4j.operation.OperationType.READ_RESOURCE_BY_ID;
import static pro.api4.jsonapi4j.operation.OperationType.READ_TO_MANY_RELATIONSHIP;
import static pro.api4.jsonapi4j.operation.OperationType.READ_TO_ONE_RELATIONSHIP;
import static pro.api4.jsonapi4j.operation.OperationType.UPDATE_RESOURCE;
import static pro.api4.jsonapi4j.operation.OperationType.UPDATE_TO_MANY_RELATIONSHIPS;
import static pro.api4.jsonapi4j.operation.OperationType.UPDATE_TO_ONE_RELATIONSHIP;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSchemaNamesUtil.requestBodyDocSchemaName;

class OasSchemaNamesUtilTests {

    private static final ResourceType USERS = new ResourceType("users");
    private static final ResourceType COUNTRIES = new ResourceType("countries");
    private static final RelationshipName RELATIVES = new RelationshipName("relatives");

    private static String sharedLinkageBodyName(ResourceType resourceType,
                                                OperationType operationType) {
        return requestBodyDocSchemaName(resourceType, RELATIVES, operationType, false);
    }

    private static String customLinkageBodyName(ResourceType resourceType,
                                                OperationType operationType) {
        return requestBodyDocSchemaName(resourceType, RELATIVES, operationType, true);
    }

    @Nested
    class RequestBodyDocSchemaName {

        @Test
        void requestBodyDocSchemaName_resourceWrites_areNamedPerResourceType() {
            assertThat(sharedLinkageBodyName(USERS, CREATE_RESOURCE)).isEqualTo("UsersCreateRequestDoc");
            assertThat(sharedLinkageBodyName(USERS, UPDATE_RESOURCE)).isEqualTo("UsersUpdateRequestDoc");
            assertThat(sharedLinkageBodyName(COUNTRIES, CREATE_RESOURCE)).isEqualTo("CountriesCreateRequestDoc");
        }

        @Test
        void requestBodyDocSchemaName_relationshipWritesWithoutLinkageMeta_areSharedAcrossResourceTypes() {
            assertThat(sharedLinkageBodyName(USERS, UPDATE_TO_ONE_RELATIONSHIP))
                    .isEqualTo("ToOneRelationshipRequestDoc")
                    .isEqualTo(sharedLinkageBodyName(COUNTRIES, UPDATE_TO_ONE_RELATIONSHIP));
            assertThat(sharedLinkageBodyName(USERS, UPDATE_TO_MANY_RELATIONSHIPS))
                    .isEqualTo("ToManyRelationshipsRequestDoc")
                    .isEqualTo(sharedLinkageBodyName(COUNTRIES, UPDATE_TO_MANY_RELATIONSHIPS));
        }

        @Test
        void requestBodyDocSchemaName_relationshipWritesWithLinkageMeta_areNamedPerRelationship() {
            assertThat(customLinkageBodyName(USERS, UPDATE_TO_ONE_RELATIONSHIP))
                    .isEqualTo("UsersRelativesToOneRelationshipRequestDoc");
            assertThat(customLinkageBodyName(USERS, UPDATE_TO_MANY_RELATIONSHIPS))
                    .isEqualTo("UsersRelativesToManyRelationshipsRequestDoc");
        }

        @Test
        void requestBodyDocSchemaName_toManyRelationshipWrites_shareOneSchema() {
            assertThat(List.of(UPDATE_TO_MANY_RELATIONSHIPS, ADD_TO_MANY_RELATIONSHIP, DELETE_TO_MANY_RELATIONSHIP))
                    .allSatisfy(operationType -> assertThat(sharedLinkageBodyName(USERS, operationType))
                            .isEqualTo("ToManyRelationshipsRequestDoc"));
        }

        @Test
        void requestBodyDocSchemaName_operationsWithoutBody_returnNull() {
            assertThat(List.<OperationType>of(READ_RESOURCE_BY_ID, READ_MULTIPLE_RESOURCES, DELETE_RESOURCE,
                    READ_TO_ONE_RELATIONSHIP, READ_TO_MANY_RELATIONSHIP))
                    .allSatisfy(operationType -> assertThat(sharedLinkageBodyName(USERS, operationType)).isNull());
        }

    }

}
