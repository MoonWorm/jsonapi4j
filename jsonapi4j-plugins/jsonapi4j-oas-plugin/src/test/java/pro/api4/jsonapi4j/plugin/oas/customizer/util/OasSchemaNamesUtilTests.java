package pro.api4.jsonapi4j.plugin.oas.customizer.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

    @Nested
    class RequestBodyDocSchemaName {

        @Test
        void requestBodyDocSchemaName_resourceWrites_areNamedPerResourceType() {
            assertThat(requestBodyDocSchemaName(USERS, CREATE_RESOURCE)).isEqualTo("UsersCreateRequestDoc");
            assertThat(requestBodyDocSchemaName(USERS, UPDATE_RESOURCE)).isEqualTo("UsersUpdateRequestDoc");
            assertThat(requestBodyDocSchemaName(COUNTRIES, CREATE_RESOURCE)).isEqualTo("CountriesCreateRequestDoc");
        }

        @Test
        void requestBodyDocSchemaName_relationshipWrites_areSharedAcrossResourceTypes() {
            assertThat(requestBodyDocSchemaName(USERS, UPDATE_TO_ONE_RELATIONSHIP))
                    .isEqualTo("ToOneRelationshipRequestDoc")
                    .isEqualTo(requestBodyDocSchemaName(COUNTRIES, UPDATE_TO_ONE_RELATIONSHIP));
            assertThat(requestBodyDocSchemaName(USERS, UPDATE_TO_MANY_RELATIONSHIPS))
                    .isEqualTo("ToManyRelationshipsRequestDoc")
                    .isEqualTo(requestBodyDocSchemaName(COUNTRIES, UPDATE_TO_MANY_RELATIONSHIPS));
        }

        @Test
        void requestBodyDocSchemaName_toManyRelationshipWrites_shareOneSchema() {
            assertThat(List.of(UPDATE_TO_MANY_RELATIONSHIPS, ADD_TO_MANY_RELATIONSHIP, DELETE_TO_MANY_RELATIONSHIP))
                    .allSatisfy(operationType -> assertThat(requestBodyDocSchemaName(USERS, operationType))
                            .isEqualTo("ToManyRelationshipsRequestDoc"));
        }

        @Test
        void requestBodyDocSchemaName_operationsWithoutBody_returnNull() {
            assertThat(List.<OperationType>of(READ_RESOURCE_BY_ID, READ_MULTIPLE_RESOURCES, DELETE_RESOURCE,
                    READ_TO_ONE_RELATIONSHIP, READ_TO_MANY_RELATIONSHIP))
                    .allSatisfy(operationType -> assertThat(requestBodyDocSchemaName(USERS, operationType)).isNull());
        }

    }

}
