package pro.api4.jsonapi4j.plugin.sf;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import pro.api4.jsonapi4j.plugin.SingleResourceVisitors;
import pro.api4.jsonapi4j.plugin.context.SingleResourceVisitorContext;
import pro.api4.jsonapi4j.request.JsonApiRequest;

@Slf4j
class SparseFieldsetsSingleResourceVisitors implements SingleResourceVisitors {

    private final SparseFieldsetsHelper helper;

    public SparseFieldsetsSingleResourceVisitors(SparseFieldsetsHelper helper) {
        this.helper = helper;
    }

    @Override
    public <REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> RelationshipsPostRetrievalPhase<?> onRelationshipsPostRetrieval(
            SingleResourceVisitorContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> ctx) {
        if (ctx.getRequest() instanceof JsonApiRequest jsonApiRequest
                && MapUtils.isNotEmpty(jsonApiRequest.getFieldSets())
                && ctx.getDoc() != null
                && ctx.getDoc().getData() != null) {
            log.debug("Applying sparse fieldsets for single resource type '{}', requested fields: {}", ctx.getDoc().getData().getType(), jsonApiRequest.getFieldSets());
            helper.sparseFieldsets(jsonApiRequest, ctx.getDoc().getData());
        }
        return RelationshipsPostRetrievalPhase.doNothing();
    }
}
