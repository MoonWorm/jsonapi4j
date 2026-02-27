package pro.api4.jsonapi4j;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.ReadResourceByIdOperation;
import pro.api4.jsonapi4j.operation.ReadToManyRelationshipOperation;
import pro.api4.jsonapi4j.operation.annotation.JsonApiRelationshipOperation;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.request.DefaultJsonApiRequest;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.response.CursorPageableResponse;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonApi4jRelationshipSelfLinksCompatibilityTests {

    @Test
    public void strictMode_relationshipSelfLinkWithoutReadOperation_isNotEmitted() {
        JsonApi4j jsonApi4j = sut(JsonApi4jCompatibilityMode.STRICT, false);

        ToManyRelationshipsDoc bars = barsRelationshipDoc(jsonApi4j.execute(readFooByIdRequest("1")));

        assertThat(bars).isNotNull();
        assertThat(bars.getLinks()).isNull();
    }

    @Test
    public void legacyMode_relationshipSelfLinkWithoutReadOperation_isEmitted() {
        JsonApi4j jsonApi4j = sut(JsonApi4jCompatibilityMode.LEGACY, false);

        ToManyRelationshipsDoc bars = barsRelationshipDoc(jsonApi4j.execute(readFooByIdRequest("1")));

        assertThat(bars).isNotNull();
        assertThat(bars.getLinks()).isNotNull();
        assertThat(bars.getLinks().getSelf()).isEqualTo("/foos/1/relationships/bars");
    }

    @Test
    public void strictMode_relationshipSelfLinkWithReadOperation_isEmitted() {
        JsonApi4j jsonApi4j = sut(JsonApi4jCompatibilityMode.STRICT, true);

        ToManyRelationshipsDoc bars = barsRelationshipDoc(jsonApi4j.execute(readFooByIdRequest("1")));

        assertThat(bars).isNotNull();
        assertThat(bars.getLinks()).isNotNull();
        assertThat(bars.getLinks().getSelf()).isEqualTo("/foos/1/relationships/bars");
    }

    private static JsonApi4j sut(JsonApi4jCompatibilityMode compatibilityMode,
                                 boolean withReadRelationshipOperation) {
        DomainRegistry domainRegistry = DomainRegistry.builder(Collections.emptyList())
                .resource(new FooResource())
                .relationship(new FooBarsRelationship())
                .build();

        OperationsRegistry.OperationsRegistryBuilder operationsRegistryBuilder = OperationsRegistry.builder(Collections.emptyList())
                .operation(new ReadFooByIdOperation());
        if (withReadRelationshipOperation) {
            operationsRegistryBuilder.operation(new ReadFooBarsRelationshipOperation());
        }
        OperationsRegistry operationsRegistry = operationsRegistryBuilder.build();

        return JsonApi4j.builder()
                .domainRegistry(domainRegistry)
                .operationsRegistry(operationsRegistry)
                .compatibilityMode(compatibilityMode)
                .build();
    }

    private static JsonApiRequest readFooByIdRequest(String fooId) {
        return DefaultJsonApiRequest.composeResourceRequest(
                fooId,
                new ResourceType("foos"),
                OperationType.READ_RESOURCE_BY_ID
        );
    }

    @SuppressWarnings("unchecked")
    private static ToManyRelationshipsDoc barsRelationshipDoc(Object response) {
        SingleResourceDoc<?> singleResourceDoc = (SingleResourceDoc<?>) response;
        Map<String, Object> relationships = (Map<String, Object>) singleResourceDoc.getData().getRelationships();
        return (ToManyRelationshipsDoc) relationships.get("bars");
    }

    @JsonApiResource(resourceType = "foos")
    private static class FooResource implements Resource<FooDto> {

        @Override
        public String resolveResourceId(FooDto dataSourceDto) {
            return dataSourceDto.id();
        }
    }

    @JsonApiRelationship(parentResource = FooResource.class, relationshipName = "bars")
    private static class FooBarsRelationship implements ToManyRelationship<BarDto> {

        @Override
        public String resolveResourceIdentifierType(BarDto relationshipDto) {
            return "bars";
        }

        @Override
        public String resolveResourceIdentifierId(BarDto relationshipDto) {
            return relationshipDto == null ? "unknown" : relationshipDto.id();
        }
    }

    @JsonApiResourceOperation(resource = FooResource.class)
    private static class ReadFooByIdOperation implements ReadResourceByIdOperation<FooDto> {

        @Override
        public FooDto readById(JsonApiRequest request) {
            return new FooDto(request.getResourceId());
        }
    }

    @JsonApiRelationshipOperation(relationship = FooBarsRelationship.class)
    private static class ReadFooBarsRelationshipOperation implements ReadToManyRelationshipOperation<FooDto, BarDto> {

        @Override
        public CursorPageableResponse<BarDto> readMany(JsonApiRequest relationshipRequest) {
            return CursorPageableResponse.empty();
        }
    }

    private record FooDto(String id) {
    }

    private record BarDto(String id) {
    }
}
