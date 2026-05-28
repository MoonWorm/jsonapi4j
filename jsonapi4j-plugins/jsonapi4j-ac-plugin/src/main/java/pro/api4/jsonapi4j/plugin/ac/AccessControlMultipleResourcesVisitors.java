package pro.api4.jsonapi4j.plugin.ac;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.exception.JsonApi4jException;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.MultipleResourcesDoc;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.error.AuthErrorCodes;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.plugin.MultipleResourcesVisitors;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.context.MultipleResourcesVisitorContext;
import pro.api4.jsonapi4j.util.ReflectionUtils;
import pro.api4.jsonapi4j.processor.IdAndType;

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
    public <REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> DataPreRetrievalPhase<?> onDataPreRetrieval(
            MultipleResourcesVisitorContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> ctx) {

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
                MultipleResourcesDoc<?> doc = new MultipleResourcesDoc<>(
                        null,
                        ctx.getJsonApiContext().getTopLevelLinksResolver().resolve(ctx.getRequest(), null, null),
                        ctx.getJsonApiContext().getTopLevelMetaResolver().resolve(ctx.getRequest(), null, null)
                );
                return DataPreRetrievalPhase.returnDoc(doc);
            } else {
                log.debug("Inbound Access is not allowed for a request {}, restricting access to the operation", ctx.getRequest());
                throw new JsonApi4jException(403, AuthErrorCodes.FORBIDDEN, "Access to the operation is forbidden");
            }
        }
    }

    @Override
    public <REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> RelationshipsPreRetrievalPhase<?> onRelationshipsPreRetrieval(
            MultipleResourcesVisitorContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> ctx) {
        MultipleResourcesDoc<?> doc = ctx.getDoc();
        if (doc == null || doc.getData() == null || ctx.getPaginationAwareResponse().getItems() == null) {
            return RelationshipsPreRetrievalPhase.doNothing();
        }

        List<? extends ResourceObject<?, ?>> data = doc.getData();

        Map<IdAndType, DATA_SOURCE_DTO> idAndTypeToDtoMap = ctx.getPaginationAwareResponse().getItems().stream()
                .collect(Collectors.toMap(
                        dto -> ctx.getJsonApiContext().getResourceTypeAndIdResolver().resolveTypeAndId(dto),
                        dto -> dto
                ));
        List<DATA_SOURCE_DTO> nonAnonymizedDtos = new ArrayList<>();
        List<ResourceObject<?, ?>> anonymizedData = new ArrayList<>();
        for (ResourceObject<?, ?> resourceObject : data) {
            AnonymizationResult<ResourceObject<?, ?>> anonymizationResult = anonymizeObjectIfNeeded(
                    accessControlEvaluator,
                    resourceObject,
                    resourceObject,
                    getOutboundAccessControlModel(ctx.getPluginInfo(), resourceObject)
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

        if (nonAnonymizedDtos.size() == ctx.getPaginationAwareResponse().getItems().size()) {
            return RelationshipsPreRetrievalPhase.doNothing();
        }

        // data
        ReflectionUtils.setFieldValueThrowing(doc, ToManyRelationshipsDoc.DATA_FIELD, anonymizedData);

        // top-level links
        LinksObject docLinks = ctx.getJsonApiContext().getTopLevelLinksResolver().resolve(
                ctx.getRequest(),
                nonAnonymizedDtos,
                ctx.getPaginationAwareResponse().getPaginationContext()
        );
        ReflectionUtils.setFieldValueThrowing(doc, ToManyRelationshipsDoc.LINKS_FIELD, docLinks);

        // top-level meta
        Object docMeta = ctx.getJsonApiContext().getTopLevelMetaResolver().resolve(
                ctx.getRequest(),
                nonAnonymizedDtos,
                ctx.getPaginationAwareResponse().getPaginationContext()
        );
        ReflectionUtils.setFieldValueThrowing(doc, ToManyRelationshipsDoc.META_FIELD, docMeta);

        return RelationshipsPreRetrievalPhase.mutatedDoc(doc);
    }

}
