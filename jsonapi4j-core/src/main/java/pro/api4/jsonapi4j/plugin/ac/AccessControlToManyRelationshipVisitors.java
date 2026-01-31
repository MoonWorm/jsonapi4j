package pro.api4.jsonapi4j.plugin.ac;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.plugin.JsonApiPluginInfo;
import pro.api4.jsonapi4j.plugin.ToManyRelationshipVisitors;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForJsonApiResourceIdentifier;
import pro.api4.jsonapi4j.plugin.utils.ReflectionUtils;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.multi.relationship.ToManyRelationshipsJsonApiContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator.anonymizeObjectIfNeeded;
import static pro.api4.jsonapi4j.plugin.ac.AccessControlVisitorsUtils.getInboundAccessControlModel;

@Slf4j
@Data
public class AccessControlToManyRelationshipVisitors implements ToManyRelationshipVisitors {

    private final AccessControlEvaluator accessControlEvaluator;

    @Override
    public <REQUEST> DataPreRetrievalPhase<?> onDataPreRetrieval(REQUEST request,
                                                                 ToManyRelationshipsJsonApiContext<REQUEST, ?> context,
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
            ToManyRelationshipsDoc doc = new ToManyRelationshipsDoc(
                    null,
                    context.getTopLevelLinksResolver().resolve(request, null, null),
                    context.getTopLevelMetaResolver().resolve(request, null)
            );
            return DataPreRetrievalPhase.returnDoc(doc);
        }
    }

    @Override
    public <REQUEST, DATA_SOURCE_DTO> DataPostRetrievalPhase<?> onDataPostRetrieval(
            REQUEST request,
            CursorPageableResponse<DATA_SOURCE_DTO> cursorPageableResponse,
            ToManyRelationshipsDoc doc,
            ToManyRelationshipsJsonApiContext<REQUEST, DATA_SOURCE_DTO> context,
            JsonApiPluginInfo pluginInfo
    ) {
        if (doc == null || doc.getData() == null || cursorPageableResponse.getItems() == null) {
            return DataPostRetrievalPhase.doNothing();
        }

        List<ResourceIdentifierObject> data = doc.getData();

        Map<IdAndType, DATA_SOURCE_DTO> idAndTypeToDtoMap = cursorPageableResponse.getItems().stream()
                .collect(Collectors.toMap(
                        dto -> context.getResourceTypeAndIdResolver().resolveTypeAndId(dto),
                        dto -> dto
                ));
        List<DATA_SOURCE_DTO> nonAnonymizedDtos = new ArrayList<>();
        List<ResourceIdentifierObject> anonymizedData = new ArrayList<>();
        for (ResourceIdentifierObject resourceIdentifierObject : data) {
            AnonymizationResult<ResourceIdentifierObject> anonymizationResult = anonymizeObjectIfNeeded(
                    accessControlEvaluator,
                    resourceIdentifierObject,
                    resourceIdentifierObject,
                    Optional.ofNullable(getOutboundAccessControlModel(pluginInfo))
                            .map(OutboundAccessControlForJsonApiResourceIdentifier::toOutboundRequirementsForCustomClass)
                            .orElse(null)
            );

            if (anonymizationResult.isNothingAnonymized()) {
                DATA_SOURCE_DTO nonAnonymizedDto = idAndTypeToDtoMap.get(
                        new IdAndType(
                                resourceIdentifierObject.getId(),
                                new ResourceType(resourceIdentifierObject.getType())
                        )
                );
                nonAnonymizedDtos.add(nonAnonymizedDto);
            }

            anonymizedData.add(anonymizationResult.targetObject());
        }

        if (nonAnonymizedDtos.size() == cursorPageableResponse.getItems().size()) {
            return DataPostRetrievalPhase.doNothing();
        }

        // data
        ReflectionUtils.setFieldValue(doc, ToManyRelationshipsDoc.DATA_FIELD, anonymizedData);

        // top-level links
        LinksObject docLinks = context.getTopLevelLinksResolver().resolve(
                request,
                nonAnonymizedDtos,
                cursorPageableResponse.getNextCursor()
        );
        ReflectionUtils.setFieldValue(doc, ToManyRelationshipsDoc.LINKS_FIELD, docLinks);

        // top-level meta
        Object docMeta = context.getTopLevelMetaResolver().resolve(request, nonAnonymizedDtos);
        ReflectionUtils.setFieldValue(doc, ToManyRelationshipsDoc.META_FIELD, docMeta);

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
