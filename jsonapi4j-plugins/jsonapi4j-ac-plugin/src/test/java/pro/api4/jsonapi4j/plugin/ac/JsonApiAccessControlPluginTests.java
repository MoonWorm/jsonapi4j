package pro.api4.jsonapi4j.plugin.ac;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.ToOneRelationship;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.operation.ReadResourceByIdOperation;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlAccessTier;
import pro.api4.jsonapi4j.plugin.ac.annotation.Authenticated;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForJsonApiResource;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForJsonApiResourceIdentifier;
import pro.api4.jsonapi4j.principal.tier.TierAdmin;
import pro.api4.jsonapi4j.request.JsonApiRequest;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class JsonApiAccessControlPluginTests {

    @Mock
    private AccessControlEvaluator accessControlEvaluatorMock;

    @InjectMocks
    private JsonApiAccessControlPlugin sut;

    @Test
    public void extractPluginInfoFromResource_readAllScenarios_checkResult() {
        MyResource myResource = new MyResource();

        OutboundAccessControlForJsonApiResource result =
                (OutboundAccessControlForJsonApiResource) sut.extractPluginInfoFromResource(myResource);

        assertThat(result).isNotNull();
        assertThat(result.getResourceClassLevel()).isNotNull();
        assertThat(result.getResourceClassLevel().getAuthenticated()).isNotNull();
        assertThat(result.getResourceClassLevel().getAuthenticated().getAuthenticated()).isNotNull().isEqualTo(Authenticated.AUTHENTICATED);

        assertThat(result.getResourceAttributesFieldLevel()).isNotNull();
        assertThat(result.getResourceAttributesFieldLevel().getAuthenticated()).isNotNull();
        assertThat(result.getResourceAttributesFieldLevel().getAuthenticated().getAuthenticated()).isNotNull().isEqualTo(Authenticated.AUTHENTICATED);

        assertThat(result.getResourceLinksFieldLevel()).isNotNull();
        assertThat(result.getResourceLinksFieldLevel().getAuthenticated()).isNotNull();
        assertThat(result.getResourceLinksFieldLevel().getAuthenticated().getAuthenticated()).isNotNull().isEqualTo(Authenticated.AUTHENTICATED);

        assertThat(result.getResourceMetaFieldLevel()).isNotNull();
        assertThat(result.getResourceMetaFieldLevel().getAuthenticated()).isNotNull();
        assertThat(result.getResourceMetaFieldLevel().getAuthenticated().getAuthenticated()).isNotNull().isEqualTo(Authenticated.AUTHENTICATED);

        assertThat(result.getAttributesNested()).isNull();
    }

    @Test
    public void extractPluginInfoFromRelationship_readAllScenarios_checkResult() {
        MyRelationship myRelationship = new MyRelationship();

        OutboundAccessControlForJsonApiResourceIdentifier result =
                (OutboundAccessControlForJsonApiResourceIdentifier) sut.extractPluginInfoFromRelationship(myRelationship);

        assertThat(result).isNotNull();
        assertThat(result.getResourceIdentifierClassLevel()).isNotNull();
        assertThat(result.getResourceIdentifierClassLevel().getAuthenticated()).isNotNull();
        assertThat(result.getResourceIdentifierClassLevel().getAuthenticated().getAuthenticated()).isNotNull().isEqualTo(Authenticated.AUTHENTICATED);

        assertThat(result).isNotNull();
        assertThat(result.getResourceIdentifierMetaFieldLevel()).isNotNull();
        assertThat(result.getResourceIdentifierMetaFieldLevel().getAuthenticated()).isNotNull();
        assertThat(result.getResourceIdentifierMetaFieldLevel().getAuthenticated().getAuthenticated()).isNotNull().isEqualTo(Authenticated.AUTHENTICATED);
    }

    @Test
    public void extractPluginInfoFromOperation_readAllScenarios_checkResult() {
        MyOperation myOperation = new MyOperation();

        AccessControlModel result =
                (AccessControlModel) sut.extractPluginInfoFromOperation(myOperation, ReadResourceByIdOperation.class);

        assertThat(result).isNotNull();
        assertThat(result.getAuthenticated()).isNotNull();
        assertThat(result.getAuthenticated().getAuthenticated()).isNotNull().isEqualTo(Authenticated.AUTHENTICATED);

        assertThat(result.getRequiredAccessTier()).isNotNull();
        assertThat(result.getRequiredAccessTier().getRequiredAccessTier()).isNotNull().isEqualTo(TierAdmin.ADMIN_ACCESS_TIER);

        assertThat(result.getRequiredOwnership()).isNull();
        assertThat(result.getRequiredScopes()).isNull();
    }

    @AccessControl(authenticated = Authenticated.AUTHENTICATED)
    private static class MyResource implements Resource<String> {

        @Override
        public String resolveResourceId(String dataSourceDto) {
            return "1";
        }

        @AccessControl(authenticated = Authenticated.AUTHENTICATED)
        @Override
        public Object resolveAttributes(String dataSourceDto) {
            return Resource.super.resolveAttributes(dataSourceDto);
        }

        @AccessControl(authenticated = Authenticated.AUTHENTICATED)
        @Override
        public LinksObject resolveResourceLinks(JsonApiRequest request, String dataSourceDto) {
            return Resource.super.resolveResourceLinks(request, dataSourceDto);
        }

        @AccessControl(authenticated = Authenticated.AUTHENTICATED)
        @Override
        public Object resolveResourceMeta(JsonApiRequest request, String dataSourceDto) {
            return Resource.super.resolveResourceMeta(request, dataSourceDto);
        }
    }

    @AccessControl(authenticated = Authenticated.AUTHENTICATED)
    private static class MyRelationship implements ToOneRelationship<String> {

        @Override
        public String resolveResourceIdentifierType(String s) {
            return "foo";
        }

        @Override
        public String resolveResourceIdentifierId(String s) {
            return "1";
        }

        @AccessControl(authenticated = Authenticated.AUTHENTICATED)
        @Override
        public Object resolveResourceIdentifierMeta(JsonApiRequest relationshipRequest, String s) {
            return ToOneRelationship.super.resolveResourceIdentifierMeta(relationshipRequest, s);
        }
    }

    @AccessControl(tier = @AccessControlAccessTier(TierAdmin.ADMIN_ACCESS_TIER))
    private static class MyOperation implements ReadResourceByIdOperation<String> {

        @AccessControl(authenticated = Authenticated.AUTHENTICATED)
        @Override
        public String readById(JsonApiRequest request) {
            return "123";
        }

    }

}
