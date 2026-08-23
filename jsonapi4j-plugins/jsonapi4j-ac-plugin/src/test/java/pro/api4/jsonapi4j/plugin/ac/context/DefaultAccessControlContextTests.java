package pro.api4.jsonapi4j.plugin.ac.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.operation.OperationMeta;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.plugin.context.PluginVisitorContext;
import pro.api4.jsonapi4j.plugin.context.SingleResourceVisitorContext;
import pro.api4.jsonapi4j.principal.AuthenticatedPrincipalContextHolder;
import pro.api4.jsonapi4j.principal.DefaultPrincipal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultAccessControlContextTests {

    private static final ResourceIdentifierObject IDENTIFIER =
            new ResourceIdentifierObject("1", null, "users", null);

    private static final ResourceObject<String, Object> RESOURCE =
            new ResourceObject<>("1", null, "users", "the-attributes", null, null, null);

    @AfterEach
    void clearPrincipal() {
        AuthenticatedPrincipalContextHolder.clear();
    }

    private static PluginVisitorContext<String> visitorContext() {
        return SingleResourceVisitorContext.<String, Object, Object>builder()
                .request("the-request")
                .operationMeta(OperationMeta.builder()
                        .resourceType(new ResourceType("users"))
                        .operationType(OperationType.READ_RESOURCE_BY_ID)
                        .build())
                .build();
    }

    @Nested
    class ResourceNarrowing {

        @Test
        void resource_narrowedToMatchingType_returnsIt() {
            AccessControlContext sut = DefaultAccessControlContext.outbound(visitorContext(), RESOURCE);

            assertThat(sut.resource(ResourceObject.class)).containsSame(RESOURCE);
        }

        @Test
        void resource_narrowedToSupertype_returnsIt() {
            AccessControlContext sut = DefaultAccessControlContext.outbound(visitorContext(), RESOURCE);

            assertThat(sut.resource(ResourceIdentifierObject.class)).containsSame(RESOURCE);
        }

        @Test
        void resource_narrowedToUnrelatedType_returnsEmptyRatherThanThrowing() {
            // a plain identifier is not a full resource object — narrowing must not blow up
            AccessControlContext sut = DefaultAccessControlContext.outbound(visitorContext(), IDENTIFIER);

            assertThat(sut.resource(ResourceObject.class)).isEmpty();
        }

        @Test
        void resource_narrowedWhenThereIsNoResource_returnsEmpty() {
            AccessControlContext sut = DefaultAccessControlContext.inbound(visitorContext());

            assertThat(sut.resource(ResourceObject.class)).isEmpty();
        }

    }

    @Nested
    class Assembly {

        @Test
        void inbound_carriesRequestAndOperationButNoResource() {
            AccessControlContext sut = DefaultAccessControlContext.inbound(visitorContext());

            assertThat(sut.stage()).isEqualTo(Stage.INBOUND);
            assertThat(sut.request()).isEqualTo("the-request");
            assertThat(sut.operation().getOperationType()).isEqualTo(OperationType.READ_RESOURCE_BY_ID);
            assertThat(sut.resource()).isEmpty();
        }

        @Test
        void outbound_carriesTheResource() {
            AccessControlContext sut = DefaultAccessControlContext.outbound(visitorContext(), RESOURCE);

            assertThat(sut.stage()).isEqualTo(Stage.OUTBOUND);
            assertThat(sut.resource()).containsSame(RESOURCE);
        }

        @Test
        void inbound_noVisitorContext_leavesRequestAndOperationNull() {
            AccessControlContext sut = DefaultAccessControlContext.inbound(null);

            assertThat(sut.request()).isNull();
            assertThat(sut.operation()).isNull();
        }

        @Test
        void principal_setOnTheThread_isCarried() {
            AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                    new DefaultPrincipal(List.of("ADMIN"), Set.of(), "user-42", Map.of("tenant", "acme")));

            AccessControlContext sut = DefaultAccessControlContext.inbound(visitorContext());

            assertThat(sut.principal().authenticatedUserId()).isEqualTo("user-42");
            assertThat(sut.principal().attributes()).containsEntry("tenant", "acme");
        }

        @Test
        void principal_noneOnTheThread_isAnonymousRatherThanNull() {
            // a policy reading attributes() must not have to null-check the principal itself
            AccessControlContext sut = DefaultAccessControlContext.inbound(visitorContext());

            assertThat(sut.principal()).isNotNull();
            assertThat(sut.principal().authenticatedUserId()).isNull();
            assertThat(sut.principal().attributes()).isEmpty();
            assertThat(sut.principal().authenticatedClientEntitlements()).isEmpty();
        }

    }

}
