package pro.api4.jsonapi4j.plugin.sf;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.operation.OperationMeta;
import pro.api4.jsonapi4j.plugin.JsonApiPluginInfo;
import pro.api4.jsonapi4j.plugin.SingleResourceVisitors;
import pro.api4.jsonapi4j.processor.single.resource.SingleResourceJsonApiContext;
import pro.api4.jsonapi4j.request.JsonApiRequest;

@Slf4j
class SparseFieldsetsSingleResourceVisitors implements SingleResourceVisitors {

    private final SparseFieldsetsHelper helper;

    public SparseFieldsetsSingleResourceVisitors(SparseFieldsetsHelper helper) {
        this.helper = helper;
    }

    @Override
    public <REQUEST, DATA_SOURCE_DTO, DOC extends SingleResourceDoc<?>> RelationshipsPostRetrievalPhase<?> onRelationshipsPostRetrieval(
            REQUEST request,
            OperationMeta meta,
            DATA_SOURCE_DTO dataSourceDto,
            DOC doc,
            SingleResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ?> context,
            JsonApiPluginInfo pluginInfo
    ) {
        if (request instanceof JsonApiRequest jsonApiRequest
                && MapUtils.isNotEmpty(jsonApiRequest.getFieldSets())
                && doc != null
                && doc.getData() != null) {
            log.debug("Applying sparse fieldsets for single resource type '{}', requested fields: {}", doc.getData().getType(), jsonApiRequest.getFieldSets());
            helper.sparseFieldsets(jsonApiRequest, doc.getData());
        }
        return RelationshipsPostRetrievalPhase.doNothing();
    }
}
