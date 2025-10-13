package io.jsonapi4j.processor.resource;

import io.jsonapi4j.processor.IdAndType;
import io.jsonapi4j.processor.single.resource.SingleResourceProcessor;
import io.jsonapi4j.domain.RelationshipName;
import io.jsonapi4j.domain.ResourceType;
import io.jsonapi4j.request.IncludeAwareRequest;
import io.jsonapi4j.model.document.LinksObject;
import io.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import io.jsonapi4j.model.document.data.ResourceObject;
import io.jsonapi4j.model.document.data.ResourceIdentifierObject;
import io.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import io.jsonapi4j.model.document.data.SingleResourceDoc;
import io.jsonapi4j.processor.resolvers.AttributesResolver;
import io.jsonapi4j.processor.resolvers.ToManyRelationshipResolver;
import io.jsonapi4j.processor.resolvers.ToOneRelationshipResolver;
import io.jsonapi4j.processor.single.SingleDataItemSupplier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.jsonapi4j.processor.resolvers.relationships.DefaultRelationshipResolvers.all;
import static io.jsonapi4j.processor.resource.SingleResourceProcessorTests.Relationships.RelationshipsRegistry.BARS;
import static io.jsonapi4j.processor.resource.SingleResourceProcessorTests.Relationships.RelationshipsRegistry.FOO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SingleResourceProcessorTests {

    private static final String ID = "1";
    private static final String NAME = "foo bar";
    private static final Request REQUEST_NO_INCLUDES = new Request(ID);
    private static final Request REQUEST_ALL_INCLUDES = new Request(ID, Set.of("foo", "bars"));
    private static final Dto DTO = new Dto(ID, NAME);
    private static final Attributes ATTRIBUTES = new Attributes(ID, NAME);

    @Mock
    private SingleDataItemSupplier<Request, Dto> ds;

    @Mock
    private AttributesResolver<Dto, Attributes> attributesResolver;

    @Mock
    private ToOneRelationshipResolver<Request, Dto> fooRelSupplier;

    @Mock
    private ToManyRelationshipResolver<Request, Dto> barsRelSupplier;

    @Test
    public void noRelationshipsResource_checkResult() {
        // given
        when(ds.get(REQUEST_NO_INCLUDES)).thenReturn(DTO);
        when(attributesResolver.resolveAttributes(DTO)).thenReturn(ATTRIBUTES);

        // when
        ResourceWithNoRelationshipsDoc result = new SingleResourceProcessor()
                .forRequest(REQUEST_NO_INCLUDES)
                .dataSupplier(ds)
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), Type.SILVER))
                .toSingleResourceDoc(JsonApiResourceObjectWithNoRelationships::new, ResourceWithNoRelationshipsDoc::new);

        // then
        assertThat(result)
                .extracting("data")
                .hasFieldOrPropertyWithValue("id", ID)
                .hasFieldOrPropertyWithValue("type", Type.SILVER.getType())
                .hasFieldOrPropertyWithValue("relationships", null)
                .extracting("attributes")
                .hasFieldOrPropertyWithValue("name", NAME);
        verify(ds, times(1)).get(REQUEST_NO_INCLUDES);
        verify(attributesResolver, times(1)).resolveAttributes(DTO);
    }

    @Test
    public void resourceWithRelationshipsAndTheRelationshipsWereRequested_checkResult() {
        // given
        when(ds.get(REQUEST_ALL_INCLUDES)).thenReturn(DTO);
        when(attributesResolver.resolveAttributes(DTO)).thenReturn(ATTRIBUTES);
        when(fooRelSupplier.resolveRequestedData(REQUEST_ALL_INCLUDES, DTO)).thenReturn(
                new ToOneRelationshipDoc(
                        new ResourceIdentifierObject("31", FOO.getName()),
                        LinksObject.builder().self("/silver/1/relationships/foo").build()
                )
        );
        when(barsRelSupplier.resolveRequestedData(REQUEST_ALL_INCLUDES, DTO)).thenReturn(
                new ToManyRelationshipsDoc(
                        List.of(
                                new ResourceIdentifierObject("51", BARS.getName()),
                                new ResourceIdentifierObject("55", BARS.getName())
                        ),
                        LinksObject.builder().self("/silver/1/relationships/bars").build()
                )
        );

        // when
        ResourceWithRelationshipsDoc result = new SingleResourceProcessor()
                .forRequest(REQUEST_ALL_INCLUDES)
                .dataSupplier(ds)
                .defaultRelationships(all(Type.SILVER, dto -> String.valueOf(dto.getId()), Relationships.RelationshipsRegistry.values()))
                .toOneRelationshipResolver(FOO, fooRelSupplier)
                .toManyRelationshipResolver(BARS, barsRelSupplier)
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), Type.SILVER))
                .toSingleResourceDoc(Relationships::new, JsonApiResourceObjectWithRelationships::new, ResourceWithRelationshipsDoc::new);

        // then
        assertThat(result)
                .extracting("data")
                .hasFieldOrPropertyWithValue("id", ID)
                .hasFieldOrPropertyWithValue("type", Type.SILVER.getType())
                .hasFieldOrPropertyWithValue(
                        "relationships",
                        new Relationships(
                            new ToOneRelationshipDoc(
                                    new ResourceIdentifierObject("31", FOO.getName()),
                                    LinksObject.builder().self("/silver/1/relationships/foo").build()
                            ),
                            new ToManyRelationshipsDoc(
                                    List.of(
                                            new ResourceIdentifierObject("51", BARS.getName()),
                                            new ResourceIdentifierObject("55", BARS.getName())
                                    ),
                                    LinksObject.builder().self("/silver/1/relationships/bars").build()
                            )
                        )
                )
                .extracting("attributes")
                .hasFieldOrPropertyWithValue("name", NAME);
        verify(ds, times(1)).get(REQUEST_ALL_INCLUDES);
        verify(attributesResolver, times(1)).resolveAttributes(DTO);
        verify(fooRelSupplier, times(1)).resolveRequestedData(REQUEST_ALL_INCLUDES, DTO);
        verify(barsRelSupplier, times(1)).resolveRequestedData(REQUEST_ALL_INCLUDES, DTO);
    }

    @Test
    public void resourceWithRelationshipsAndTheRelationshipsWereNotRequested_checkResult() {
        // given
        when(ds.get(REQUEST_NO_INCLUDES)).thenReturn(DTO);
        when(attributesResolver.resolveAttributes(DTO)).thenReturn(ATTRIBUTES);

        // when
        ResourceWithRelationshipsDoc result = new SingleResourceProcessor()
                .forRequest(REQUEST_NO_INCLUDES)
                .dataSupplier(ds)
                .defaultRelationships(all(Type.SILVER, dto -> String.valueOf(dto.getId()), Relationships.RelationshipsRegistry.values()))
                .toOneRelationshipResolver(FOO, fooRelSupplier)
                .toManyRelationshipResolver(BARS, barsRelSupplier)
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), Type.SILVER))
                .toSingleResourceDoc(Relationships::new, JsonApiResourceObjectWithRelationships::new, ResourceWithRelationshipsDoc::new);

        // then
        assertThat(result)
                .extracting("data")
                .hasFieldOrPropertyWithValue("id", ID)
                .hasFieldOrPropertyWithValue("type", Type.SILVER.getType())
                .hasFieldOrPropertyWithValue("relationships", new Relationships(
                        new ToOneRelationshipDoc(LinksObject.builder().self("/silver/1/relationships/foo").build()),
                        new ToManyRelationshipsDoc(LinksObject.builder().self("/silver/1/relationships/bars").build()))
                )
                .extracting("attributes")
                .hasFieldOrPropertyWithValue("name", NAME);
        verify(ds, times(1)).get(REQUEST_NO_INCLUDES);
        verify(attributesResolver, times(1)).resolveAttributes(DTO);
        verify(fooRelSupplier, times(0)).resolveRequestedData(REQUEST_ALL_INCLUDES, DTO);
        verify(barsRelSupplier, times(0)).resolveRequestedData(REQUEST_ALL_INCLUDES, DTO);
    }

    @Test
    public void configureResourceLevelLinksResolver_checkResult() {
        // given
        when(ds.get(REQUEST_NO_INCLUDES)).thenReturn(DTO);
        when(attributesResolver.resolveAttributes(DTO)).thenReturn(ATTRIBUTES);

        // when
        ResourceWithNoRelationshipsDoc result = new SingleResourceProcessor()
                .forRequest(REQUEST_NO_INCLUDES)
                .dataSupplier(ds)
                .resourceLinksResolver((req, dto) -> LinksObject.builder().self("/silver/1").build())
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), Type.SILVER))
                .toSingleResourceDoc(JsonApiResourceObjectWithNoRelationships::new, ResourceWithNoRelationshipsDoc::new);

        // then
        assertThat(result)
                .extracting("data")
                .hasFieldOrPropertyWithValue("id", ID)
                .hasFieldOrPropertyWithValue("type", Type.SILVER.getType())
                .hasFieldOrPropertyWithValue("links", LinksObject.builder().self("/silver/1").build())
                .extracting("attributes")
                .hasFieldOrPropertyWithValue("name", NAME);
        verify(ds, times(1)).get(REQUEST_NO_INCLUDES);
        verify(attributesResolver, times(1)).resolveAttributes(DTO);
    }

    @Test
    public void configureResourceLevelMetaResolver_checkResult() {
        // given
        when(ds.get(REQUEST_NO_INCLUDES)).thenReturn(DTO);
        when(attributesResolver.resolveAttributes(DTO)).thenReturn(ATTRIBUTES);

        // when
        ResourceWithNoRelationshipsDoc result = new SingleResourceProcessor()
                .forRequest(REQUEST_NO_INCLUDES)
                .dataSupplier(ds)
                .resourceMetaResolver((req, dto) -> Map.of("test_prop", "some_value"))
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), Type.SILVER))
                .toSingleResourceDoc(JsonApiResourceObjectWithNoRelationships::new, ResourceWithNoRelationshipsDoc::new);

        // then
        assertThat(result)
                .extracting("data")
                .hasFieldOrPropertyWithValue("id", ID)
                .hasFieldOrPropertyWithValue("type", Type.SILVER.getType())
                .hasFieldOrPropertyWithValue("meta", Map.of("test_prop", "some_value"))
                .extracting("attributes")
                .hasFieldOrPropertyWithValue("name", NAME);
        verify(ds, times(1)).get(REQUEST_NO_INCLUDES);
        verify(attributesResolver, times(1)).resolveAttributes(DTO);
    }

    @Test
    public void configureDocLevelLinksResolver_checkResult() {
        // given
        when(ds.get(REQUEST_NO_INCLUDES)).thenReturn(DTO);
        when(attributesResolver.resolveAttributes(DTO)).thenReturn(ATTRIBUTES);

        // when
        ResourceWithNoRelationshipsDoc result = new SingleResourceProcessor()
                .forRequest(REQUEST_NO_INCLUDES)
                .dataSupplier(ds)
                .topLevelLinksResolver((req, dto) -> LinksObject.builder().self("/silver").build())
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), Type.SILVER))
                .toSingleResourceDoc(JsonApiResourceObjectWithNoRelationships::new, ResourceWithNoRelationshipsDoc::new);

        // then
        assertThat(result)
                .hasFieldOrPropertyWithValue("links", LinksObject.builder().self("/silver").build())
                .extracting("data")
                .hasFieldOrPropertyWithValue("id", ID)
                .hasFieldOrPropertyWithValue("type", Type.SILVER.getType())
                .extracting("attributes")
                .hasFieldOrPropertyWithValue("name", NAME);
        verify(ds, times(1)).get(REQUEST_NO_INCLUDES);
        verify(attributesResolver, times(1)).resolveAttributes(DTO);
    }

    @Test
    public void configureDocLevelMetaResolver_checkResult() {
        // given
        when(ds.get(REQUEST_NO_INCLUDES)).thenReturn(DTO);
        when(attributesResolver.resolveAttributes(DTO)).thenReturn(ATTRIBUTES);

        // when
        ResourceWithNoRelationshipsDoc result = new SingleResourceProcessor()
                .forRequest(REQUEST_NO_INCLUDES)
                .dataSupplier(ds)
                .topLevelMetaResolver((req, dto) -> Map.of("test_prop", "some_value"))
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), Type.SILVER))
                .toSingleResourceDoc(JsonApiResourceObjectWithNoRelationships::new, ResourceWithNoRelationshipsDoc::new);

        // then
        assertThat(result)
                .hasFieldOrPropertyWithValue("meta", Map.of("test_prop", "some_value"))
                .extracting("data")
                .hasFieldOrPropertyWithValue("id", ID)
                .hasFieldOrPropertyWithValue("type", Type.SILVER.getType())
                .extracting("attributes")
                .hasFieldOrPropertyWithValue("name", NAME);
        verify(ds, times(1)).get(REQUEST_NO_INCLUDES);
        verify(attributesResolver, times(1)).resolveAttributes(DTO);
    }

    @Test
    public void resourceWithRelationships_fullRelationshipResolversAreNotConfigured_checkThrowsIllegalStateException() {
        // when
        // to one rel is missing
        assertThatThrownBy(() -> {
            new SingleResourceProcessor()
                    .forRequest(REQUEST_NO_INCLUDES)
                    .dataSupplier(ds)
                    .defaultRelationships(all(Type.SILVER, dto -> String.valueOf(dto.getId()), Relationships.RelationshipsRegistry.values()))
                    .toManyRelationshipResolver(BARS, (req, dto) -> new ToManyRelationshipsDoc())
                    .attributesResolver(attributesResolver)
                    .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), Type.SILVER))
                    .toSingleResourceDoc(Relationships::new, JsonApiResourceObjectWithRelationships::new, ResourceWithRelationshipsDoc::new);
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("Every declared 'default' relationship must also has either 'ToOneRelationshipResolver' or 'ToManyRelationshipsResolver' being configured (or their batch alternatives). Missing for 'foo' relationship.");

        // multi rel is missing
        assertThatThrownBy(() -> {
            new SingleResourceProcessor()
                    .forRequest(REQUEST_NO_INCLUDES)
                    .dataSupplier(ds)
                    .defaultRelationships(all(Type.SILVER, dto -> String.valueOf(dto.getId()), Relationships.RelationshipsRegistry.values()))
                    .toOneRelationshipResolver(FOO, (req, dto) -> new ToOneRelationshipDoc())
                    .attributesResolver(attributesResolver)
                    .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), Type.SILVER))
                    .toSingleResourceDoc(Relationships::new, JsonApiResourceObjectWithRelationships::new, ResourceWithRelationshipsDoc::new);
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("Every declared 'default' relationship must also has either 'ToOneRelationshipResolver' or 'ToManyRelationshipsResolver' being configured (or their batch alternatives). Missing for 'bars' relationship.");

        // then
        verify(ds, times(0)).get(REQUEST_ALL_INCLUDES);
        verify(attributesResolver, times(0)).resolveAttributes(DTO);
    }

    @Test
    public void dataSourceProviderIsNull_checkThrowsNPE() {
        // when
        assertThatThrownBy(() -> {
            new SingleResourceProcessor()
                    .forRequest(REQUEST_NO_INCLUDES)
                    .dataSupplier((SingleDataItemSupplier<Request, Dto>) null);
        }).isInstanceOf(NullPointerException.class);

        // then
        verify(ds, times(0)).get(REQUEST_ALL_INCLUDES);
        verify(attributesResolver, times(0)).resolveAttributes(DTO);
    }

    @Test
    public void resourceTypeAndIdResolverIsNull_checkThrowsNPE() {
        // when
        assertThatThrownBy(() -> {
            new SingleResourceProcessor()
                    .forRequest(REQUEST_NO_INCLUDES)
                    .dataSupplier(ds)
                    .resourceTypeAndIdResolver(null);
        }).isInstanceOf(NullPointerException.class);

        // then
        verify(ds, times(0)).get(REQUEST_ALL_INCLUDES);
        verify(attributesResolver, times(0)).resolveAttributes(DTO);
    }

    @Test
    public void requestCanBeNull_checkNoExceptionThrown() {
        // given
        when(ds.get(null)).thenReturn(DTO);
        when(attributesResolver.resolveAttributes(DTO)).thenReturn(ATTRIBUTES);

        // when
        // with rels
        new SingleResourceProcessor()
                .forRequest((Request) null)
                .dataSupplier(ds)
                .defaultRelationships(all(Type.SILVER, dto -> String.valueOf(dto.getId()), Relationships.RelationshipsRegistry.values()))
                .toOneRelationshipResolver(FOO, (req, dto) -> new ToOneRelationshipDoc())
                .toManyRelationshipResolver(BARS, (req, dto) -> new ToManyRelationshipsDoc())
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), Type.SILVER))
                .toSingleResourceDoc(Relationships::new, JsonApiResourceObjectWithRelationships::new, ResourceWithRelationshipsDoc::new);

        // no rels
        new SingleResourceProcessor()
                .forRequest((Request) null)
                .dataSupplier(ds)
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), Type.SILVER))
                .toSingleResourceDoc(JsonApiResourceObjectWithNoRelationships::new, ResourceWithNoRelationshipsDoc::new);

        // then
        verify(ds, times(2)).get(null);
        verify(attributesResolver, times(2)).resolveAttributes(DTO);
    }

    @Test
    public void relationshipSupplierIsNull_fallbackToNoRelToResponseFlow() {
        // given
        when(ds.get(REQUEST_NO_INCLUDES)).thenReturn(DTO);
        when(attributesResolver.resolveAttributes(DTO)).thenReturn(ATTRIBUTES);

        // when
        new SingleResourceProcessor()
                .forRequest(REQUEST_NO_INCLUDES)
                .dataSupplier(ds)
                .defaultRelationships(all(Type.SILVER, dto -> String.valueOf(dto.getId()), Relationships.RelationshipsRegistry.values()))
                .toOneRelationshipResolver(FOO, (req, dto) -> new ToOneRelationshipDoc())
                .toManyRelationshipResolver(BARS, (req, dto) -> new ToManyRelationshipsDoc())
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), Type.SILVER))
                .toSingleResourceDoc(null, JsonApiResourceObjectWithRelationships::new, ResourceWithRelationshipsDoc::new);

        // then
        verify(ds, times(1)).get(REQUEST_NO_INCLUDES);
        verify(attributesResolver, times(1)).resolveAttributes(DTO);
    }

    @Test
    public void resourceSupplierIsNull_shouldThrowNpe() {
        // when
        // with rels
        assertThatThrownBy(() -> {
            new SingleResourceProcessor()
                    .forRequest(REQUEST_NO_INCLUDES)
                    .dataSupplier(ds)
                    .defaultRelationships(all(Type.SILVER, dto -> String.valueOf(dto.getId()), Relationships.RelationshipsRegistry.values()))
                    .toOneRelationshipResolver(FOO, (req, dto) -> new ToOneRelationshipDoc())
                    .toManyRelationshipResolver(BARS, (req, dto) -> new ToManyRelationshipsDoc())
                    .attributesResolver(attributesResolver)
                    .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), Type.SILVER))
                    .toSingleResourceDoc(Relationships::new, null, ResourceWithRelationshipsDoc::new);
        }).isInstanceOf(NullPointerException.class);

        // no rels
        assertThatThrownBy(() -> {
            new SingleResourceProcessor()
                    .forRequest(REQUEST_NO_INCLUDES)
                    .dataSupplier(ds)
                    .attributesResolver(attributesResolver)
                    .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), Type.SILVER))
                    .toSingleResourceDoc(null, ResourceWithNoRelationshipsDoc::new);
        }).isInstanceOf(NullPointerException.class);

        // then
        verify(ds, times(0)).get(REQUEST_NO_INCLUDES);
        verify(attributesResolver, times(0)).resolveAttributes(DTO);
    }

    @Test
    public void responseSupplierIsNull_shouldThrowNpe() {
        // when
        // with rels
        assertThatThrownBy(() -> {
            new SingleResourceProcessor()
                    .forRequest(REQUEST_NO_INCLUDES)
                    .dataSupplier(ds)
                    .defaultRelationships(all(Type.SILVER, dto -> String.valueOf(dto.getId()), Relationships.RelationshipsRegistry.values()))
                    .toOneRelationshipResolver(FOO, (req, dto) -> new ToOneRelationshipDoc())
                    .toManyRelationshipResolver(BARS, (req, dto) -> new ToManyRelationshipsDoc())
                    .attributesResolver(attributesResolver)
                    .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), Type.SILVER))
                    .toSingleResourceDoc(Relationships::new, JsonApiResourceObjectWithRelationships::new, null);
        }).isInstanceOf(NullPointerException.class);

        // no rels
        assertThatThrownBy(() -> {
            new SingleResourceProcessor()
                    .forRequest(REQUEST_ALL_INCLUDES)
                    .dataSupplier(ds)
                    .attributesResolver(attributesResolver)
                    .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), Type.SILVER))
                    .toSingleResourceDoc(JsonApiResourceObjectWithNoRelationships::new, null);
        }).isInstanceOf(NullPointerException.class);

        // then
        verify(ds, times(0)).get(REQUEST_NO_INCLUDES);
        verify(attributesResolver, times(0)).resolveAttributes(DTO);
    }

    @Getter
    private enum Type implements ResourceType {
        SILVER("silver");

        private final String type;

        Type(String type) {
            this.type = type;
        }
    }

    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor
    private static class Request implements IncludeAwareRequest {
        private final String id;
        private Set<String> effectiveIncludes;
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

    private static class JsonApiResourceObjectWithNoRelationships extends ResourceObject<Attributes, Object> {

        public JsonApiResourceObjectWithNoRelationships(String id,
                                                        String type,
                                                        Attributes attributes,
                                                        Object relationships,
                                                        LinksObject links,
                                                        Object meta) {
            super(id, type, attributes, null, links, meta);
        }
    }

    @Data
    public static class Relationships {

        private final ToOneRelationshipDoc foo;
        private final ToManyRelationshipsDoc bars;

        public Relationships(Map<RelationshipName, ToManyRelationshipsDoc> toManyRelationshipsDocMap,
                             Map<RelationshipName, ToOneRelationshipDoc> toOneRelationshipDocMap) {
            this.foo = toOneRelationshipDocMap.get(FOO);
            this.bars = toManyRelationshipsDocMap.get(BARS);
        }

        public Relationships(ToOneRelationshipDoc foo, ToManyRelationshipsDoc bars) {
            this.foo = foo;
            this.bars = bars;
        }

        @Getter
        public enum RelationshipsRegistry implements RelationshipName {
            FOO("foo"), BARS("bars");

            private final String name;

            RelationshipsRegistry(String name) {
                this.name = name;
            }
        }

    }

    private static class JsonApiResourceObjectWithRelationships extends ResourceObject<Attributes, Relationships> {

        public JsonApiResourceObjectWithRelationships(String id,
                                                      String type,
                                                      Attributes attributes,
                                                      Relationships relationships,
                                                      LinksObject links,
                                                      Object meta) {
            super(id, type, attributes, relationships, links, meta);
        }
    }

    private static class ResourceWithNoRelationshipsDoc extends SingleResourceDoc<JsonApiResourceObjectWithNoRelationships> {

        public ResourceWithNoRelationshipsDoc(JsonApiResourceObjectWithNoRelationships data,
                                              LinksObject links,
                                              Object meta) {
            super(data, links, meta);
        }

    }

    private static class ResourceWithRelationshipsDoc extends SingleResourceDoc<JsonApiResourceObjectWithRelationships> {

        public ResourceWithRelationshipsDoc(JsonApiResourceObjectWithRelationships data,
                                            LinksObject links,
                                            Object meta) {
            super(data, links, meta);
        }

    }

}
