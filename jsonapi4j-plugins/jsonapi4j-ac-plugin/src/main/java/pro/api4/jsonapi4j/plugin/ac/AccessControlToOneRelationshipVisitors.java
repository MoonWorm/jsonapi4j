package pro.api4.jsonapi4j.plugin.ac;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.exception.JsonApi4jException;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.model.document.error.AuthErrorCodes;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.plugin.JsonApiPluginInfo;
import pro.api4.jsonapi4j.plugin.ToOneRelationshipVisitors;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForJsonApiResourceIdentifier;
import pro.api4.jsonapi4j.plugin.context.ToOneRelationshipVisitorContext;
import pro.api4.jsonapi4j.util.ReflectionUtils;

import java.util.Optional;

import static pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator.anonymizeObjectIfNeeded;
import static pro.api4.jsonapi4j.plugin.ac.AccessControlVisitorsUtils.getInboundAccessControlModel;

@Slf4j
@Data
public class AccessControlToOneRelationshipVisitors implements ToOneRelationshipVisitors {

    private final AccessControlEvaluator accessControlEvaluator;

    /**
     * @see AccessControlSingleResourceVisitors#onDataPreRetrieval
     */
    @Override
    public <REQUEST, DATA_SOURCE_DTO> DataPreRetrievalPhase<?> onDataPreRetrieval(
            ToOneRelationshipVisitorContext<REQUEST, DATA_SOURCE_DTO> ctx) {

        AccessControlModel inboundAccessControlSettings = getInboundAccessControlModel(ctx.getPluginInfo(), ctx.getRequest());
        if (inboundAccessControlSettings == null) {
            return DataPreRetrievalPhase.doNothing();
        }
        if (accessControlEvaluator.evaluateInboundRequirements(ctx.getRequest(), inboundAccessControlSettings)) {
            log.debug("Inbound Access is allowed for a request {}. Proceeding...", ctx.getRequest());
            return DataPreRetrievalPhase.doNothing();
        } else {
            if (ctx.getOperationMeta().getOperationType().getMethod() == OperationType.Method.GET) {
                log.debug("Inbound Access is not allowed for a request {}, returning empty response", ctx.getRequest());
                ToOneRelationshipDoc doc = new ToOneRelationshipDoc(
                        null,
                        ctx.getJsonApiContext().getTopLevelLinksResolver().resolve(ctx.getRequest(), null),
                        ctx.getJsonApiContext().getTopLevelMetaResolver().resolve(ctx.getRequest(), null)
                );
                return DataPreRetrievalPhase.returnDoc(doc);
            } else {
                log.debug("Inbound Access is not allowed for a request {}, restricting access to the operation", ctx.getRequest());
                throw new JsonApi4jException(403, AuthErrorCodes.FORBIDDEN, "Access to the operation is forbidden");
            }

        }
    }

    @Override
    public <REQUEST, DATA_SOURCE_DTO> DataPostRetrievalPhase<?> onDataPostRetrieval(
            ToOneRelationshipVisitorContext<REQUEST, DATA_SOURCE_DTO> ctx) {
        if (ctx.getDoc() == null) {
            return DataPostRetrievalPhase.doNothing();
        }

        ToOneRelationshipDoc doc = ctx.getDoc();
        ResourceIdentifierObject data = doc.getData();

        AnonymizationResult<ResourceIdentifierObject> anonymizationResult = anonymizeObjectIfNeeded(
                accessControlEvaluator,
                data,
                data,
                Optional.ofNullable(getOutboundAccessControlModel(ctx.getPluginInfo()))
                        .map(OutboundAccessControlForJsonApiResourceIdentifier::toOutboundRequirementsForCustomClass)
                        .orElse(null)
        );

        if (anonymizationResult.isNothingAnonymized()) {
            return DataPostRetrievalPhase.doNothing();
        }

        if (anonymizationResult.isFullyAnonymized()) {
            // top-level links
            LinksObject docLinks = ctx.getJsonApiContext().getTopLevelLinksResolver().resolve(ctx.getRequest(), null);
            ReflectionUtils.setFieldValueThrowing(doc, ToOneRelationshipDoc.LINKS_FIELD, docLinks);

            // top-level meta
            Object docMeta = ctx.getJsonApiContext().getTopLevelMetaResolver().resolve(ctx.getRequest(), null);
            ReflectionUtils.setFieldValueThrowing(doc, ToOneRelationshipDoc.META_FIELD, docMeta);
        }

        ReflectionUtils.setFieldValueThrowing(doc, ToOneRelationshipDoc.DATA_FIELD, anonymizationResult.targetObject());

        return DataPostRetrievalPhase.mutatedDoc(doc);
    }

    private static OutboundAccessControlForJsonApiResourceIdentifier getOutboundAccessControlModel(
            JsonApiPluginInfo pluginInfo
    ) {
        if (pluginInfo != null
                && pluginInfo.getRelationshipPluginInfo() instanceof OutboundAccessControlForJsonApiResourceIdentifier acm) {
            return acm;
        }
        return null;
    }

}
