package pro.api4.jsonapi4j.plugin.ac;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.exception.JsonApi4jException;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.MultipleResourcesDoc;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.operation.OperationMeta;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.plugin.JsonApiPluginInfo;
import pro.api4.jsonapi4j.plugin.MultipleResourcesVisitors;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.util.ReflectionUtils;
import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.multi.resource.MultipleResourcesJsonApiContext;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator.anonymizeObjectIfNeeded;
import static pro.api4.jsonapi4j.plugin.ac.AccessControlVisitorsUtils.getInboundAccessControlModel;
import static pro.api4.jsonapi4j.plugin.ac.AccessControlVisitorsUtils.getOutboundAccessControlModel;

@Slf4j
@Data
public class AccessControlMultipleResourcesVisitors implements MultipleResourcesVisitors {

    private final AccessControlEvaluator accessControlEvaluator;

    /**
     * @see AccessControlSingleResourceVisitors#onDataPreRetrieval
     */
    @Override
    public <REQUEST> DataPreRetrievalPhase<?> onDataPreRetrieval(REQUEST request,
                                                                 OperationMeta operationMeta,
                                                                 MultipleResourcesJsonApiContext<REQUEST, ?, ?> context,
                                                                 JsonApiPluginInfo pluginInfo) {

        AccessControlModel inboundAccessControlSettings = getInboundAccessControlModel(pluginInfo, request);
        if (inboundAccessControlSettings == null) {
            return DataPreRetrievalPhase.doNothing();
        }
        if (accessControlEvaluator.evaluateInboundRequirements(request, inboundAccessControlSettings)) {
            log.debug("Inbound Access is allowed for a request {}. Proceeding...", request);
            return DataPreRetrievalPhase.doNothing();
        } else {
            if (operationMeta.getOperationType().getMethod() == OperationType.Method.GET) {
                log.debug("Inbound Access is not allowed for a request {}, returning empty response", request);
                MultipleResourcesDoc<?> doc = new MultipleResourcesDoc<>(
                        null,
                        context.getTopLevelLinksResolver().resolve(request, null, null),
                        context.getTopLevelMetaResolver().resolve(request, null)
                );
                return DataPreRetrievalPhase.returnDoc(doc);
            } else {
                log.debug("Inbound Access is not allowed for a request {}, restricting access to the operation", request);
                throw new JsonApi4jException(403, DefaultErrorCodes.FORBIDDEN, "Access to the operation is forbidden");
            }
        }
    }

    @Override
    public <REQUEST, DATA_SOURCE_DTO, DOC extends MultipleResourcesDoc<?>> RelationshipsPreRetrievalPhase<?> onRelationshipsPreRetrieval(
            REQUEST request,
            OperationMeta operationMeta,
            PaginationAwareResponse<DATA_SOURCE_DTO> paginationAwareResponse,
            DOC doc,
            MultipleResourcesJsonApiContext<REQUEST, DATA_SOURCE_DTO, ?> context,
            JsonApiPluginInfo pluginInfo
    ) {
        if (doc == null || doc.getData() == null || paginationAwareResponse.getItems() == null) {
            return RelationshipsPreRetrievalPhase.doNothing();
        }

        List<? extends ResourceObject<?, ?>> data = doc.getData();

        Map<IdAndType, DATA_SOURCE_DTO> idAndTypeToDtoMap = paginationAwareResponse.getItems().stream()
                .collect(Collectors.toMap(
                        dto -> context.getResourceTypeAndIdResolver().resolveTypeAndId(dto),
                        dto -> dto
                ));
        List<DATA_SOURCE_DTO> nonAnonymizedDtos = new ArrayList<>();
        List<ResourceObject<?, ?>> anonymizedData = new ArrayList<>();
        for (ResourceObject<?, ?> resourceObject : data) {
            AnonymizationResult<ResourceObject<?, ?>> anonymizationResult = anonymizeObjectIfNeeded(
                    accessControlEvaluator,
                    resourceObject,
                    resourceObject,
                    getOutboundAccessControlModel(pluginInfo, resourceObject)
            );

            if (anonymizationResult.isNothingAnonymized()) {
                DATA_SOURCE_DTO nonAnonymizedDto = idAndTypeToDtoMap.get(
                        new IdAndType(
                                resourceObject.getId(),
                                new ResourceType(resourceObject.getType())
                        )
                );
                nonAnonymizedDtos.add(nonAnonymizedDto);
            }

            anonymizedData.add(anonymizationResult.targetObject());
        }

        if (nonAnonymizedDtos.size() == paginationAwareResponse.getItems().size()) {
            return RelationshipsPreRetrievalPhase.doNothing();
        }

        // data
        ReflectionUtils.setFieldValueThrowing(doc, ToManyRelationshipsDoc.DATA_FIELD, anonymizedData);

        // top-level links
        LinksObject docLinks = context.getTopLevelLinksResolver().resolve(
                request,
                nonAnonymizedDtos,
                paginationAwareResponse.getPaginationContext()
        );
        ReflectionUtils.setFieldValueThrowing(doc, ToManyRelationshipsDoc.LINKS_FIELD, docLinks);

        // top-level meta
        Object docMeta = context.getTopLevelMetaResolver().resolve(request, nonAnonymizedDtos);
        ReflectionUtils.setFieldValueThrowing(doc, ToManyRelationshipsDoc.META_FIELD, docMeta);

        return RelationshipsPreRetrievalPhase.mutatedDoc(doc);
    }

}
