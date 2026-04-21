package pro.api4.jsonapi4j.processor.resource;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.*;
import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.multi.MultipleDataItemsSupplier;
import pro.api4.jsonapi4j.processor.multi.resource.MultipleResourcesProcessor;
import pro.api4.jsonapi4j.processor.resolvers.AttributesResolver;
import pro.api4.jsonapi4j.processor.resolvers.ToManyRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.ToOneRelationshipResolver;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static pro.api4.jsonapi4j.processor.resolvers.relationships.DefaultRelationshipResolvers.all;

@ExtendWith(MockitoExtension.class)
public class MultipleResourcesProcessorTests {

    private static final ResourceType SILVER = new ResourceType("silver");
    private static final RelationshipName FOO = new RelationshipName("foo");
    private static final RelationshipName BARS = new RelationshipName("bars");

    private static final String ID_1 = "1";
    private static final String ID_2 = "2";
    private static final String NAME_1 = "first";
    private static final String NAME_2 = "second";
    private static final Request REQUEST_NO_INCLUDES = new Request();
    private static final Request REQUEST_ALL_INCLUDES = new Request(List.of("foo", "bars"));
    private static final Dto DTO_1 = new Dto(ID_1, NAME_1);
    private static final Dto DTO_2 = new Dto(ID_2, NAME_2);
    private static final Attributes ATTRIBUTES_1 = new Attributes(ID_1, NAME_1);
    private static final Attributes ATTRIBUTES_2 = new Attributes(ID_2, NAME_2);

    @Mock
    private MultipleDataItemsSupplier<Request, Dto> ds;

    @Mock
    private AttributesResolver<Dto, Attributes> attributesResolver;

    @Mock
    private ToOneRelationshipResolver<Request, Dto> fooRelSupplier;

    @Mock
    private ToManyRelationshipResolver<Request, Dto> barsRelSupplier;

    // --- Basic resource composition ---

    @Test
    public void multipleResources_noRelationships_checkResult() {
        // given
        when(ds.get(REQUEST_NO_INCLUDES)).thenReturn(PaginationAwareResponse.fromItemsNotPageable(List.of(DTO_1, DTO_2)));
        when(attributesResolver.resolveAttributes(DTO_1)).thenReturn(ATTRIBUTES_1);
        when(attributesResolver.resolveAttributes(DTO_2)).thenReturn(ATTRIBUTES_2);

        // when
        MultipleResourcesDoc<?> result = new MultipleResourcesProcessor()
                .forRequest(REQUEST_NO_INCLUDES)
                .dataSupplier(ds)
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), SILVER))
                .toMultipleResourcesDoc();

        // then
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData().get(0))
                .hasFieldOrPropertyWithValue("id", ID_1)
                .hasFieldOrPropertyWithValue("type", SILVER.getType());
        assertThat(result.getData().get(0).getAttributes())
                .hasFieldOrPropertyWithValue("name", NAME_1);
        assertThat(result.getData().get(1))
                .hasFieldOrPropertyWithValue("id", ID_2)
                .hasFieldOrPropertyWithValue("type", SILVER.getType());
        assertThat(result.getData().get(1).getAttributes())
                .hasFieldOrPropertyWithValue("name", NAME_2);
        verify(ds, times(1)).get(REQUEST_NO_INCLUDES);
        verify(attributesResolver, times(1)).resolveAttributes(DTO_1);
        verify(attributesResolver, times(1)).resolveAttributes(DTO_2);
    }

    // --- Null/empty data source ---

    @Test
    public void dataSupplierReturnsNull_producesEmptyDoc() {
        // given
        when(ds.get(REQUEST_NO_INCLUDES)).thenReturn(null);

        // when
        MultipleResourcesDoc<?> result = new MultipleResourcesProcessor()
                .forRequest(REQUEST_NO_INCLUDES)
                .dataSupplier(ds)
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), SILVER))
                .toMultipleResourcesDoc();

        // then
        assertThat(result.getData()).isEmpty();
        verify(attributesResolver, never()).resolveAttributes(any());
    }

    @Test
    public void dataSupplierReturnsEmptyList_producesEmptyDataArray() {
        // given
        when(ds.get(REQUEST_NO_INCLUDES)).thenReturn(PaginationAwareResponse.empty());

        // when
        MultipleResourcesDoc<?> result = new MultipleResourcesProcessor()
                .forRequest(REQUEST_NO_INCLUDES)
                .dataSupplier(ds)
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), SILVER))
                .toMultipleResourcesDoc();

        // then
        assertThat(result.getData()).isEmpty();
        verify(attributesResolver, never()).resolveAttributes(any());
    }

    // --- Top-level links and meta ---

    @Test
    public void configureTopLevelLinksResolver_checkResult() {
        // given
        when(ds.get(REQUEST_NO_INCLUDES)).thenReturn(PaginationAwareResponse.fromItemsNotPageable(List.of(DTO_1)));
        when(attributesResolver.resolveAttributes(DTO_1)).thenReturn(ATTRIBUTES_1);

        // when
        MultipleResourcesDoc<?> result = new MultipleResourcesProcessor()
                .forRequest(REQUEST_NO_INCLUDES)
                .dataSupplier(ds)
                .topLevelLinksResolver((req, dtos, ctx) -> LinksObject.builder().self("/silver").build())
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), SILVER))
                .toMultipleResourcesDoc();

        // then
        assertThat(result.getLinks()).isEqualTo(LinksObject.builder().self("/silver").build());
    }

    @Test
    public void configureTopLevelMetaResolver_checkResult() {
        // given
        when(ds.get(REQUEST_NO_INCLUDES)).thenReturn(PaginationAwareResponse.fromItemsNotPageable(List.of(DTO_1)));
        when(attributesResolver.resolveAttributes(DTO_1)).thenReturn(ATTRIBUTES_1);

        // when
        MultipleResourcesDoc<?> result = new MultipleResourcesProcessor()
                .forRequest(REQUEST_NO_INCLUDES)
                .dataSupplier(ds)
                .topLevelMetaResolver((req, dtos) -> Map.of("total", 1))
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), SILVER))
                .toMultipleResourcesDoc();

        // then
        assertThat(result.getMeta()).isEqualTo(Map.of("total", 1));
    }

    // --- Resource-level links and meta ---

    @Test
    public void configureResourceLinksResolver_checkResult() {
        // given
        when(ds.get(REQUEST_NO_INCLUDES)).thenReturn(PaginationAwareResponse.fromItemsNotPageable(List.of(DTO_1)));
        when(attributesResolver.resolveAttributes(DTO_1)).thenReturn(ATTRIBUTES_1);

        // when
        MultipleResourcesDoc<?> result = new MultipleResourcesProcessor()
                .forRequest(REQUEST_NO_INCLUDES)
                .dataSupplier(ds)
                .resourceLinksResolver((req, dto) -> LinksObject.builder().self("/silver/" + dto.getId()).build())
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), SILVER))
                .toMultipleResourcesDoc();

        // then
        assertThat(result.getData().get(0).getLinks())
                .isEqualTo(LinksObject.builder().self("/silver/1").build());
    }

    @Test
    public void configureResourceMetaResolver_checkResult() {
        // given
        when(ds.get(REQUEST_NO_INCLUDES)).thenReturn(PaginationAwareResponse.fromItemsNotPageable(List.of(DTO_1)));
        when(attributesResolver.resolveAttributes(DTO_1)).thenReturn(ATTRIBUTES_1);

        // when
        MultipleResourcesDoc<?> result = new MultipleResourcesProcessor()
                .forRequest(REQUEST_NO_INCLUDES)
                .dataSupplier(ds)
                .resourceMetaResolver((req, dto) -> Map.of("version", "v1"))
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), SILVER))
                .toMultipleResourcesDoc();

        // then
        assertThat(result.getData().get(0).getMeta()).isEqualTo(Map.of("version", "v1"));
    }

    // --- Relationships ---

    @Test
    public void multipleResourcesWithRelationships_includesRequested_checkResult() {
        // given
        when(ds.get(REQUEST_ALL_INCLUDES)).thenReturn(PaginationAwareResponse.fromItemsNotPageable(List.of(DTO_1)));
        when(attributesResolver.resolveAttributes(DTO_1)).thenReturn(ATTRIBUTES_1);
        when(fooRelSupplier.resolveRequestedData(REQUEST_ALL_INCLUDES, DTO_1)).thenReturn(
                new ToOneRelationshipObject(
                        new ResourceIdentifierObject("31", null, FOO.getName(), null),
                        LinksObject.builder().self("/silver/1/relationships/foo").build()
                )
        );
        when(barsRelSupplier.resolveRequestedData(REQUEST_ALL_INCLUDES, DTO_1)).thenReturn(
                new ToManyRelationshipObject(
                        List.of(new ResourceIdentifierObject("51", null, BARS.getName(), null)),
                        LinksObject.builder().self("/silver/1/relationships/bars").build()
                )
        );

        // when
        MultipleResourcesDoc<?> result = new MultipleResourcesProcessor()
                .forRequest(REQUEST_ALL_INCLUDES)
                .dataSupplier(ds)
                .defaultRelationships(all(SILVER, dto -> String.valueOf(dto.getId()), new RelationshipName[]{FOO, BARS}))
                .toOneRelationshipResolver(FOO, fooRelSupplier)
                .toManyRelationshipResolver(BARS, barsRelSupplier)
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), SILVER))
                .toMultipleResourcesDoc();

        // then
        assertThat(result.getData()).hasSize(1);
        Object relationships = result.getData().get(0).getRelationships();
        assertThat(relationships).isNotNull();
        verify(fooRelSupplier, times(1)).resolveRequestedData(REQUEST_ALL_INCLUDES, DTO_1);
        verify(barsRelSupplier, times(1)).resolveRequestedData(REQUEST_ALL_INCLUDES, DTO_1);
    }

    @Test
    public void multipleResourcesWithRelationships_includesNotRequested_defaultLinksOnly() {
        // given
        when(ds.get(REQUEST_NO_INCLUDES)).thenReturn(PaginationAwareResponse.fromItemsNotPageable(List.of(DTO_1)));
        when(attributesResolver.resolveAttributes(DTO_1)).thenReturn(ATTRIBUTES_1);

        // when
        MultipleResourcesDoc<?> result = new MultipleResourcesProcessor()
                .forRequest(REQUEST_NO_INCLUDES)
                .dataSupplier(ds)
                .defaultRelationships(all(SILVER, dto -> String.valueOf(dto.getId()), new RelationshipName[]{FOO, BARS}))
                .toOneRelationshipResolver(FOO, fooRelSupplier)
                .toManyRelationshipResolver(BARS, barsRelSupplier)
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), SILVER))
                .toMultipleResourcesDoc();

        // then
        assertThat(result.getData()).hasSize(1);
        Object relationships = result.getData().get(0).getRelationships();
        assertThat(relationships).isNotNull();
        // relationship resolvers should NOT be called when includes are not requested
        verify(fooRelSupplier, never()).resolveRequestedData(any(), any());
        verify(barsRelSupplier, never()).resolveRequestedData(any(), any());
    }

    // --- Validation ---

    @Test
    public void dataSupplierIsNull_checkThrowsNPE() {
        // when/then
        assertThatThrownBy(() -> {
            new MultipleResourcesProcessor()
                    .forRequest(REQUEST_NO_INCLUDES)
                    .dataSupplier((MultipleDataItemsSupplier<Request, Dto>) null);
        }).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void resourceTypeAndIdResolverIsNull_checkThrowsNPE() {
        // when/then
        assertThatThrownBy(() -> {
            new MultipleResourcesProcessor()
                    .forRequest(REQUEST_NO_INCLUDES)
                    .dataSupplier(ds)
                    .attributesResolver(attributesResolver)
                    .resourceTypeAndIdResolver(null);
        }).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void missingRelationshipResolver_checkThrowsIllegalStateException() {
        // when/then — missing to-one resolver for FOO
        assertThatThrownBy(() -> {
            new MultipleResourcesProcessor()
                    .forRequest(REQUEST_NO_INCLUDES)
                    .dataSupplier(ds)
                    .defaultRelationships(all(SILVER, dto -> String.valueOf(dto.getId()), new RelationshipName[]{FOO, BARS}))
                    .toManyRelationshipResolver(BARS, barsRelSupplier)
                    .attributesResolver(attributesResolver)
                    .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), SILVER))
                    .toMultipleResourcesDoc();
        }).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("foo");
    }

    // --- Inner types ---

    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor
    private static class Request implements IncludeAwareRequest {
        private List<String> effectiveIncludes;
    }

    @Data
    private static class Dto {
        private final String id;
        private final String name;
    }

    @Data
    private static class Attributes {
        private final String id;
        private final String name;
    }

}
