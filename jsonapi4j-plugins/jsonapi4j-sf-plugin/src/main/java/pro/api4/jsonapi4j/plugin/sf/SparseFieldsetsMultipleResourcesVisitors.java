package pro.api4.jsonapi4j.plugin.sf;

import org.apache.commons.collections4.MapUtils;
import pro.api4.jsonapi4j.model.document.data.MultipleResourcesDoc;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.plugin.JsonApiPluginInfo;
import pro.api4.jsonapi4j.plugin.MultipleResourcesVisitors;
import pro.api4.jsonapi4j.processor.multi.resource.MultipleResourcesJsonApiContext;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.response.CursorPageableResponse;

import static pro.api4.jsonapi4j.plugin.sf.SparseFieldsetsUtils.doSparseFieldsets;

class SparseFieldsetsMultipleResourcesVisitors implements MultipleResourcesVisitors {

    @Override
    public <REQUEST, DATA_SOURCE_DTO, DOC extends MultipleResourcesDoc<?>> RelationshipsPostRetrievalPhase<?> onRelationshipsPostRetrieval(
            REQUEST request,
            CursorPageableResponse<DATA_SOURCE_DTO> cursorPageableResponse,
            DOC doc,
            MultipleResourcesJsonApiContext<REQUEST, DATA_SOURCE_DTO, ?> context,
            JsonApiPluginInfo pluginInfo
    ) {
        if (request instanceof JsonApiRequest jsonApiRequest
                && MapUtils.isNotEmpty(jsonApiRequest.getFieldSets())
                && doc != null
                && doc.getData() != null) {
            for (ResourceObject<?, ?> resourceObject : doc.getData()) {
                doSparseFieldsets(jsonApiRequest, resourceObject);
            }
        }
        return RelationshipsPostRetrievalPhase.doNothing();
    }
}
