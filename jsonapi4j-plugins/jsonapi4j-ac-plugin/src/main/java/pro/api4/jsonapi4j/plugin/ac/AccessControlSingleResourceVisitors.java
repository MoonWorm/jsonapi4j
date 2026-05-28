package pro.api4.jsonapi4j.plugin.ac;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.exception.JsonApi4jException;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.error.AuthErrorCodes;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.plugin.SingleResourceVisitors;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.context.SingleResourceVisitorContext;
import pro.api4.jsonapi4j.util.ReflectionUtils;

import static pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator.anonymizeObjectIfNeeded;
import static pro.api4.jsonapi4j.plugin.ac.AccessControlVisitorsUtils.getInboundAccessControlModel;
import static pro.api4.jsonapi4j.plugin.ac.AccessControlVisitorsUtils.getOutboundAccessControlModel;

@Slf4j
@Data
public class AccessControlSingleResourceVisitors implements SingleResourceVisitors {

    private final AccessControlEvaluator accessControlEvaluator;

    /**
     * Evaluates inbound access control requirements before data retrieval.
     * <p>
     * When access is denied, the behavior differs based on the HTTP method:
     * <ul>
     *     <li><b>GET</b> — returns an empty document ({@code data: null}) with a 200 status code.
     *         This is intentional: the Compound Documents resolver makes internal GET requests
     *         to fetch included resources. Returning 403 would either break the entire compound
     *         document resolution (with {@code errorStrategy: FAIL}) or silently drop the included
     *         resource (with {@code errorStrategy: IGNORE}). By returning an empty document instead,
     *         the resolver receives a valid JSON:API response and can continue processing — the
     *         resource simply has no data, which is a legitimate "nothing to show" signal.</li>
     *     <li><b>Non-GET (PATCH, DELETE, POST)</b> — throws a 403 Forbidden exception.
     *         Write operations should fail explicitly since there is no Compound Documents
     *         resolution involved.</li>
     * </ul>
     */
    @Override
    public <REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> DataPreRetrievalPhase<?> onDataPreRetrieval(
            SingleResourceVisitorContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> ctx) {

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
                SingleResourceDoc<?> doc = new SingleResourceDoc<>(
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
    public <REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> RelationshipsPreRetrievalPhase<?> onRelationshipsPreRetrieval(
            SingleResourceVisitorContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> ctx) {
        if (ctx.getDoc() == null) {
            return RelationshipsPreRetrievalPhase.doNothing();
        }
        SingleResourceDoc<?> doc = ctx.getDoc();
        ResourceObject<?, ?> resource = doc.getData();
        AnonymizationResult<ResourceObject<?, ?>> anonymizationResult = anonymizeObjectIfNeeded(
                accessControlEvaluator,
                resource,
                resource,
                getOutboundAccessControlModel(ctx.getPluginInfo(), resource)
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
