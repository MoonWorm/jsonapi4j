package pro.api4.jsonapi4j.plugin.sf;

import org.apache.commons.collections4.MapUtils;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.plugin.JsonApiPluginInfo;
import pro.api4.jsonapi4j.plugin.SingleResourceVisitors;
import pro.api4.jsonapi4j.plugin.sf.config.SfProperties;
import pro.api4.jsonapi4j.processor.single.resource.SingleResourceJsonApiContext;
import pro.api4.jsonapi4j.request.JsonApiRequest;

import static pro.api4.jsonapi4j.plugin.sf.SparseFieldsetsUtils.sparseFieldsets;

class SparseFieldsetsSingleResourceVisitors implements SingleResourceVisitors {

    private final SfProperties sfProperties;

    public SparseFieldsetsSingleResourceVisitors(SfProperties sfProperties) {
        this.sfProperties = sfProperties;
    }

    @Override
    public <REQUEST, DATA_SOURCE_DTO, DOC extends SingleResourceDoc<?>> RelationshipsPostRetrievalPhase<?> onRelationshipsPostRetrieval(
            REQUEST request,
            DATA_SOURCE_DTO dataSourceDto,
            DOC doc,
            SingleResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ?> context,
            JsonApiPluginInfo pluginInfo
    ) {
        if (request instanceof JsonApiRequest jsonApiRequest
                && MapUtils.isNotEmpty(jsonApiRequest.getFieldSets())
                && doc != null
                && doc.getData() != null) {
            sparseFieldsets(jsonApiRequest, doc.getData(), sfProperties);
        }
        return RelationshipsPostRetrievalPhase.doNothing();
    }
}
