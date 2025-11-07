package pro.api4.jsonapi4j.processor.resource;

import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.single.resource.SingleResourceProcessor;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlAccessTier;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlOwnership;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlScopes;
import pro.api4.jsonapi4j.plugin.ac.AuthenticatedPrincipalContextHolder;
import pro.api4.jsonapi4j.plugin.ac.tier.TierRootAdmin;
import pro.api4.jsonapi4j.plugin.ac.tier.TierPublic;
import pro.api4.jsonapi4j.plugin.ac.tier.TierPartner;
import pro.api4.jsonapi4j.plugin.ac.tier.TierNoAccess;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlAccessTierModel;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlRequirements;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlRequirementsForObject;
import pro.api4.jsonapi4j.processor.ac.OutboundAccessControlSettingsForResource;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.processor.resolvers.AttributesResolver;
import pro.api4.jsonapi4j.processor.single.SingleDataItemSupplier;
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

import static pro.api4.jsonapi4j.processor.resolvers.relationships.DefaultRelationshipResolvers.all;
import static pro.api4.jsonapi4j.processor.resource.SingleResourceProcessorAccessControlTests.Relationships.RelationshipsRegistry.BARS;
import static pro.api4.jsonapi4j.processor.resource.SingleResourceProcessorAccessControlTests.Relationships.RelationshipsRegistry.FOO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SingleResourceProcessorAccessControlTests {

    private static final String ID = "1";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String ALTERNATE_OWNER_ID = "2";
    private static final Request REQUEST_NO_INCLUDES = new Request(ID);
    private static final Request REQUEST_ALL_INCLUDES = new Request(ID, Set.of("foo", "bars"));
    private static final Dto DTO = new Dto(ID, FIRST_NAME, LAST_NAME);
    private static final Attributes ATTRIBUTES = new Attributes(ID, FIRST_NAME, LAST_NAME, ALTERNATE_OWNER_ID);
    @Mock
    private SingleDataItemSupplier<Request, Dto> ds;
    @Mock
    private AttributesResolver<Dto, Attributes> attributesResolver;

    // TEST: Access Control (User, AccessTier, Scope, ownership) - resource level
    // TEST: Access Control (User, AccessTier, Scope, ownership) - resource level (fields?)
    // TEST: Access Control (User, AccessTier, Scope, ownership) - att level
    // TEST: Access Control (User, AccessTier, Scope, ownership) - att fields
    // TEST: Access Control (User, AccessTier, Scope, ownership) - relationships level
    // TEST: Access Control (User, AccessTier, Scope, ownership) - relationships field level
    // ??? batch for Multi Rel Proc tests ???

    @Test
    public void noAccessAndAllScopesAndTheOwner_checkResult() {
        // given
        AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                new TierNoAccess(),
                Set.of("users.read", "roles.read", "roles.write", "groups.read", "groups.write"),
                ID
        );
        when(ds.get(REQUEST_ALL_INCLUDES)).thenReturn(DTO);
        when(attributesResolver.resolveAttributes(DTO)).thenReturn(ATTRIBUTES);

        // when
        ResourceWithRelationshipsDoc result = new SingleResourceProcessor()
                .forRequest(REQUEST_ALL_INCLUDES)
                .dataSupplier(ds)
                .defaultRelationships(all(Type.SILVER, dto -> String.valueOf(dto.getId()), Relationships.RelationshipsRegistry.values()))
                .toOneRelationshipResolver(FOO, (req, dto) -> new ToOneRelationshipDoc(
                        new ResourceIdentifierObject("31", FOO.getName()),
                        LinksObject.builder().self("/silver/1/relationships/foo").build()
                ))
                .toManyRelationshipResolver(BARS, (req, dto) -> new ToManyRelationshipsDoc(
                        List.of(
                                new ResourceIdentifierObject("51", BARS.getName()),
                                new ResourceIdentifierObject("55", BARS.getName())
                        ),
                        LinksObject.builder().self("/silver/1/relationships/bars").build()
                ))
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), Type.SILVER))
                .toSingleResourceDoc(Relationships::new, JsonApiResourceObjectWithRelationships::new, ResourceWithRelationshipsDoc::new);

        // then
        assertThat(result).hasFieldOrPropertyWithValue("data", null);
        verify(ds, times(1)).get(REQUEST_ALL_INCLUDES);
        verify(attributesResolver, times(1)).resolveAttributes(DTO);
    }

    @Test
    public void publicAccessAndAllScopesAndTheOwner_checkResult() {
        // given
        AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                new TierPublic(),
                Set.of("users.read", "roles.read", "roles.write", "groups.read", "groups.write"),
                ID
        );
        when(ds.get(REQUEST_ALL_INCLUDES)).thenReturn(DTO);
        when(attributesResolver.resolveAttributes(DTO)).thenReturn(ATTRIBUTES);

        // when
        ResourceWithRelationshipsDoc result = new SingleResourceProcessor()
                .forRequest(REQUEST_ALL_INCLUDES)
                .outboundAccessControlSettings(OutboundAccessControlSettingsForResource.builder()
                        .forResource(AccessControlRequirementsForObject.builder()
                                .fieldLevel(
                                        Map.of(
                                                "relationships",
                                                AccessControlRequirements.builder()
                                                        .requiredAccessTier(AccessControlAccessTierModel.builder()
                                                                .requiredAccessTier(TierRootAdmin.ROOT_ADMIN_ACCESS_TIER).build()).build()
                                        )
                                ).build()).build())
                .dataSupplier(ds)
                .defaultRelationships(all(Type.SILVER, dto -> String.valueOf(dto.getId()), Relationships.RelationshipsRegistry.values()))
                .toOneRelationshipResolver(FOO, (req, dto) -> new ToOneRelationshipDoc(
                        new ResourceIdentifierObject("31", FOO.getName()),
                        LinksObject.builder().self("/silver/1/relationships/foo").build()
                ))
                .toManyRelationshipResolver(BARS, (req, dto) -> new ToManyRelationshipsDoc(
                        List.of(
                                new ResourceIdentifierObject("51", BARS.getName()),
                                new ResourceIdentifierObject("55", BARS.getName())),
                        LinksObject.builder().self("/silver/1/relationships/bars").build()
                ))
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), Type.SILVER))
                .toSingleResourceDoc(Relationships::new, JsonApiResourceObjectWithRelationships::new, ResourceWithRelationshipsDoc::new);

        // then
        assertThat(result).hasFieldOrProperty("data")
                .hasFieldOrPropertyWithValue("data.id", "1")
                .hasFieldOrPropertyWithValue("data.type", "silver")
                .hasFieldOrPropertyWithValue("data.attributes", null)
                .hasFieldOrPropertyWithValue("data.relationships.foo", null)
                .hasFieldOrPropertyWithValue("data.relationships.bars", null);
        verify(ds, times(1)).get(REQUEST_ALL_INCLUDES);
        verify(attributesResolver, times(1)).resolveAttributes(DTO);
    }

    @Test
    public void partnerAccessAndAllScopesButWrongOwnerId_checkResult() {
        // given
        AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                new TierPartner(),
                Set.of("users.read", "roles.read", "roles.write", "groups.read", "groups.write"),
                "3123123123"
        );
        when(ds.get(REQUEST_ALL_INCLUDES)).thenReturn(DTO);
        when(attributesResolver.resolveAttributes(DTO)).thenReturn(ATTRIBUTES);

        // when
        ResourceWithRelationshipsDoc result = new SingleResourceProcessor()
                .forRequest(REQUEST_ALL_INCLUDES)
                .dataSupplier(ds)
                .defaultRelationships(all(Type.SILVER, dto -> String.valueOf(dto.getId()), Relationships.RelationshipsRegistry.values()))
                .toOneRelationshipResolver(FOO, (req, dto) -> new ToOneRelationshipDoc(
                        new ResourceIdentifierObject("31", FOO.getName()),
                        LinksObject.builder().self("/silver/1/relationships/foo").build()
                ))
                .toManyRelationshipResolver(BARS, (req, dto) -> new ToManyRelationshipsDoc(
                        List.of(
                                new ResourceIdentifierObject("51", BARS.getName()),
                                new ResourceIdentifierObject("55", BARS.getName())
                        ),
                        LinksObject.builder().self("/silver/1/relationships/bars").build()
                ))
                .attributesResolver(attributesResolver)
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), Type.SILVER))
                .toSingleResourceDoc(Relationships::new, JsonApiResourceObjectWithRelationships::new, ResourceWithRelationshipsDoc::new);

        // then
        assertThat(result).hasFieldOrPropertyWithValue("data", null);
        verify(ds, times(1)).get(REQUEST_ALL_INCLUDES);
        verify(attributesResolver, times(1)).resolveAttributes(DTO);
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
    public static class Request implements IncludeAwareRequest {
        private final String id;
        private Set<String> effectiveIncludes;
    }

    @Data
    public static class Dto {
        private final String id;
        private final String firstName;
        private final String lastName;
    }

    @AccessControl(
            tier = @AccessControlAccessTier(TierPartner.PARTNER_ACCESS_TIER),
            scopes = @AccessControlScopes(requiredScopes = {"groups.write"}),
            ownership = @AccessControlOwnership(ownerIdFieldPath = "id")
    )
    @Data
    public static class Attributes {

        private final String id;

        @AccessControl(
                tier = @AccessControlAccessTier(TierRootAdmin.ROOT_ADMIN_ACCESS_TIER),
                scopes = @AccessControlScopes(requiredScopes = {"groups.read"}),
                ownership = @AccessControlOwnership(ownerIdFieldPath = "id")
        )
        private final String firstName;

        @AccessControl(
                tier = @AccessControlAccessTier(TierRootAdmin.ROOT_ADMIN_ACCESS_TIER)
        )
        private final String lastName;

        private final String customOwnerId;

    }

    public static class Relationships {

        private final ToOneRelationshipDoc foo;

        private final ToManyRelationshipsDoc bars;

        public Relationships(Map<RelationshipName, ToManyRelationshipsDoc> toManyRelationshipsDocMap,
                             Map<RelationshipName, ToOneRelationshipDoc> toOneRelationshipDocMap) {
            this.foo = toOneRelationshipDocMap.get(RelationshipsRegistry.FOO);
            this.bars = toManyRelationshipsDocMap.get(RelationshipsRegistry.BARS);
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

    @AccessControl(
            tier = @AccessControlAccessTier(TierPublic.PUBLIC_TIER),
            scopes = @AccessControlScopes(requiredScopes = {"users.read"}),
            ownership = @AccessControlOwnership(ownerIdFieldPath = "id")
    )
    public static class JsonApiResourceObjectWithRelationships extends ResourceObject<Attributes, Relationships> {

        public JsonApiResourceObjectWithRelationships(String id,
                                                      String type,
                                                      Attributes attributes,
                                                      Relationships relationships,
                                                      LinksObject links,
                                                      Object meta) {
            super(id, type, attributes, relationships, links, meta);
        }
    }

    public static class ResourceWithRelationshipsDoc extends SingleResourceDoc<JsonApiResourceObjectWithRelationships> {

        public ResourceWithRelationshipsDoc(JsonApiResourceObjectWithRelationships data,
                                            LinksObject links,
                                            Object meta) {
            super(data, links, meta);
        }

    }

}
