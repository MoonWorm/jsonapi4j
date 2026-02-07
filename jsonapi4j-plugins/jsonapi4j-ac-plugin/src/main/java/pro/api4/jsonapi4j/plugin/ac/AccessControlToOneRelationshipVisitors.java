package pro.api4.jsonapi4j.plugin.ac;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.plugin.JsonApiPluginInfo;
import pro.api4.jsonapi4j.plugin.ToOneRelationshipVisitors;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForJsonApiResourceIdentifier;
import pro.api4.jsonapi4j.plugin.utils.ReflectionUtils;
import pro.api4.jsonapi4j.processor.single.relationship.ToOneRelationshipJsonApiContext;

import java.util.Optional;

import static pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator.anonymizeObjectIfNeeded;
import static pro.api4.jsonapi4j.plugin.ac.AccessControlVisitorsUtils.getInboundAccessControlModel;

@Slf4j
@Data
public class AccessControlToOneRelationshipVisitors implements ToOneRelationshipVisitors {

    private final AccessControlEvaluator accessControlEvaluator;

    @Override
    public <REQUEST> DataPreRetrievalPhase<?> onDataPreRetrieval(REQUEST request,
                                                                 ToOneRelationshipJsonApiContext<REQUEST, ?> context,
                                                                 JsonApiPluginInfo pluginInfo) {

        AccessControlModel inboundAccessControlSettings = getInboundAccessControlModel(pluginInfo, request);
        if (inboundAccessControlSettings == null) {
            return DataPreRetrievalPhase.doNothing();
        }
        if (accessControlEvaluator.evaluateInboundRequirements(request, inboundAccessControlSettings)) {
            log.info("Inbound Access is allowed for a request {}. Proceeding...", request);
            return DataPreRetrievalPhase.doNothing();
        } else {
            log.info("Inbound Access is not allowed for a request {}, returning empty response", request);
            ToOneRelationshipDoc doc = new ToOneRelationshipDoc(
                    null,
                    context.getTopLevelLinksResolver().resolve(request, null),
                    context.getTopLevelMetaResolver().resolve(request, null)
            );
            return DataPreRetrievalPhase.returnDoc(doc);
        }
    }

    @Override
    public <REQUEST, DATA_SOURCE_DTO> DataPostRetrievalPhase<?> onDataPostRetrieval(
            REQUEST request,
            DATA_SOURCE_DTO dataSourceDto,
            ToOneRelationshipDoc doc,
            ToOneRelationshipJsonApiContext<REQUEST, DATA_SOURCE_DTO> context,
            JsonApiPluginInfo pluginInfo
    ) {
        if (doc == null) {
            return DataPostRetrievalPhase.doNothing();
        }

        ResourceIdentifierObject data = doc.getData();

        AnonymizationResult<ResourceIdentifierObject> anonymizationResult = anonymizeObjectIfNeeded(
                accessControlEvaluator,
                data,
                data,
                Optional.ofNullable(getOutboundAccessControlModel(pluginInfo))
                        .map(OutboundAccessControlForJsonApiResourceIdentifier::toOutboundRequirementsForCustomClass)
                        .orElse(null)
        );

        if (anonymizationResult.isNothingAnonymized()) {
            return DataPostRetrievalPhase.doNothing();
        }

        if (anonymizationResult.isFullyAnonymized()) {
            // top-level links
            LinksObject docLinks = context.getTopLevelLinksResolver().resolve(request, null);
            ReflectionUtils.setFieldValue(doc, ToOneRelationshipDoc.LINKS_FIELD, docLinks);

            // top-level meta
            Object docMeta = context.getTopLevelMetaResolver().resolve(request, null);
            ReflectionUtils.setFieldValue(doc, ToOneRelationshipDoc.META_FIELD, docMeta);
        }

        ReflectionUtils.setFieldValue(doc, ToOneRelationshipDoc.DATA_FIELD, anonymizationResult.targetObject());

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
