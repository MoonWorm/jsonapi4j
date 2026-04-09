package pro.api4.jsonapi4j.plugin.ac;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.exception.JsonApi4jException;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.operation.OperationMeta;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.plugin.JsonApiPluginInfo;
import pro.api4.jsonapi4j.plugin.SingleResourceVisitors;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.util.ReflectionUtils;
import pro.api4.jsonapi4j.processor.single.resource.SingleResourceJsonApiContext;

import static pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator.anonymizeObjectIfNeeded;
import static pro.api4.jsonapi4j.plugin.ac.AccessControlVisitorsUtils.getInboundAccessControlModel;
import static pro.api4.jsonapi4j.plugin.ac.AccessControlVisitorsUtils.getOutboundAccessControlModel;

@Slf4j
@Data
public class AccessControlSingleResourceVisitors implements SingleResourceVisitors {

    private final AccessControlEvaluator accessControlEvaluator;

    @Override
    public <REQUEST> DataPreRetrievalPhase<?> onDataPreRetrieval(REQUEST request,
                                                                 OperationMeta operationMeta,
                                                                 SingleResourceJsonApiContext<REQUEST, ?, ?> context,
                                                                 JsonApiPluginInfo pluginInfo) {

        AccessControlModel inboundAccessControlSettings = getInboundAccessControlModel(pluginInfo, request);
        if (inboundAccessControlSettings == null) {
            return DataPreRetrievalPhase.doNothing();
        }
        if (accessControlEvaluator.evaluateInboundRequirements(request, inboundAccessControlSettings)) {
            log.info("Inbound Access is allowed for a request {}. Proceeding...", request);
            return DataPreRetrievalPhase.doNothing();
        } else {
            if (operationMeta.getOperationType().getMethod() == OperationType.Method.GET) {
                log.info("Inbound Access is not allowed for a request {}, returning empty response", request);
                SingleResourceDoc<?> doc = new SingleResourceDoc<>(
                        null,
                        context.getTopLevelLinksResolver().resolve(request, null),
                        context.getTopLevelMetaResolver().resolve(request, null)
                );
                return DataPreRetrievalPhase.returnDoc(doc);
            } else {
                log.info("Inbound Access is not allowed for a request {}, restricting access to the operation", request);
                throw new JsonApi4jException(403, DefaultErrorCodes.FORBIDDEN, "Access to the operation is forbidden");
            }
        }
    }

    @Override
    public <REQUEST, DATA_SOURCE_DTO, DOC extends SingleResourceDoc<?>> RelationshipsPreRetrievalPhase<?> onRelationshipsPreRetrieval(
            REQUEST request,
            OperationMeta operationMeta,
            DATA_SOURCE_DTO dataSourceDto,
            DOC doc,
            SingleResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ?> context,
            JsonApiPluginInfo pluginInfo
    ) {
        if (doc == null) {
            return RelationshipsPreRetrievalPhase.doNothing();
        }
        ResourceObject<?, ?> resource = doc.getData();
        AnonymizationResult<ResourceObject<?, ?>> anonymizationResult = anonymizeObjectIfNeeded(
                accessControlEvaluator,
                resource,
                resource,
                getOutboundAccessControlModel(pluginInfo, resource)
        );

        if (anonymizationResult.isNothingAnonymized()) {
            return RelationshipsPreRetrievalPhase.doNothing();
        }

        if (anonymizationResult.isFullyAnonymized()) {
            ReflectionUtils.setFieldValueThrowing(doc, SingleResourceDoc.DATA_FIELD, null);
            return RelationshipsPreRetrievalPhase.returnDoc(doc);
        }

        ReflectionUtils.setFieldValueThrowing(doc, SingleResourceDoc.DATA_FIELD, anonymizationResult.targetObject());
        return RelationshipsPreRetrievalPhase.mutatedDoc(doc);
    }

}
