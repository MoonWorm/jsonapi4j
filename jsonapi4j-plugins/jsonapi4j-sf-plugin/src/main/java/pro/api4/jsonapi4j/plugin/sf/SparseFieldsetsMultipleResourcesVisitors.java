package pro.api4.jsonapi4j.plugin.sf;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.plugin.MultipleResourcesVisitors;
import pro.api4.jsonapi4j.plugin.context.MultipleResourcesVisitorContext;
import pro.api4.jsonapi4j.request.JsonApiRequest;

@Slf4j
class SparseFieldsetsMultipleResourcesVisitors implements MultipleResourcesVisitors {

    private final SparseFieldsetsHelper helper;

    public SparseFieldsetsMultipleResourcesVisitors(SparseFieldsetsHelper helper) {
        this.helper = helper;
    }

    @Override
    public <REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> RelationshipsPostRetrievalPhase<?> onRelationshipsPostRetrieval(
            MultipleResourcesVisitorContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> ctx) {
        if (ctx.getRequest() instanceof JsonApiRequest jsonApiRequest
                && MapUtils.isNotEmpty(jsonApiRequest.getFieldSets())
                && ctx.getDoc() != null
                && ctx.getDoc().getData() != null) {
            log.debug("Applying sparse fieldsets for {} resources, requested fields: {}", ctx.getDoc().getData().size(), jsonApiRequest.getFieldSets());
            for (ResourceObject<?, ?> resourceObject : ctx.getDoc().getData()) {
                helper.sparseFieldsets(jsonApiRequest, resourceObject);
            }
        }
        return RelationshipsPostRetrievalPhase.doNothing();
    }
}
