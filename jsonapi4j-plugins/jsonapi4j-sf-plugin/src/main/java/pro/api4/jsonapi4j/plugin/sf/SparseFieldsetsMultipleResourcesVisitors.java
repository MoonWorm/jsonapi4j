package pro.api4.jsonapi4j.plugin.sf;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import pro.api4.jsonapi4j.model.document.data.MultipleResourcesDoc;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.operation.OperationMeta;
import pro.api4.jsonapi4j.plugin.JsonApiPluginInfo;
import pro.api4.jsonapi4j.plugin.MultipleResourcesVisitors;
import pro.api4.jsonapi4j.processor.multi.resource.MultipleResourcesJsonApiContext;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;

@Slf4j
class SparseFieldsetsMultipleResourcesVisitors implements MultipleResourcesVisitors {

    private final SparseFieldsetsHelper helper;

    public SparseFieldsetsMultipleResourcesVisitors(SparseFieldsetsHelper helper) {
        this.helper = helper;
    }

    @Override
    public <REQUEST, DATA_SOURCE_DTO, DOC extends MultipleResourcesDoc<?>> RelationshipsPostRetrievalPhase<?> onRelationshipsPostRetrieval(
            REQUEST request,
            OperationMeta meta,
            PaginationAwareResponse<DATA_SOURCE_DTO> paginationAwareResponse,
            DOC doc,
            MultipleResourcesJsonApiContext<REQUEST, DATA_SOURCE_DTO, ?> context,
            JsonApiPluginInfo pluginInfo
    ) {
        if (request instanceof JsonApiRequest jsonApiRequest
                && MapUtils.isNotEmpty(jsonApiRequest.getFieldSets())
                && doc != null
                && doc.getData() != null) {
            log.debug("Applying sparse fieldsets for {} resources, requested fields: {}", doc.getData().size(), jsonApiRequest.getFieldSets());
            for (ResourceObject<?, ?> resourceObject : doc.getData()) {
                helper.sparseFieldsets(jsonApiRequest, resourceObject);
            }
        }
        return RelationshipsPostRetrievalPhase.doNothing();
    }
}
