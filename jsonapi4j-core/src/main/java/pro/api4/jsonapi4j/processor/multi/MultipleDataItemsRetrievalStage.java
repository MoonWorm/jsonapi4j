package pro.api4.jsonapi4j.processor.multi;

import pro.api4.jsonapi4j.plugin.ac.impl.AccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.impl.model.AccessControlModel;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.processor.util.DataRetrievalUtil;

import static pro.api4.jsonapi4j.plugin.ac.impl.AccessControlEvaluator.retrieveDataIfAllowed;

public class MultipleDataItemsRetrievalStage {

    private final AccessControlEvaluator accessControlEvaluator;
    private AccessControlModel inboundAccessControlSettings;

    public MultipleDataItemsRetrievalStage(AccessControlEvaluator accessControlEvaluator,
                                           AccessControlModel inboundAccessControlSettings) {
        this.accessControlEvaluator = accessControlEvaluator;
        this.inboundAccessControlSettings = inboundAccessControlSettings;
    }

    public <REQUEST, DATA_SOURCE_DTO> CursorPageableResponse<DATA_SOURCE_DTO> retrieveData(REQUEST request,
                                                                                           MultipleDataItemsSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier) {
        if (request != null && accessControlEvaluator != null) {
            // apply settings from annotations if any
            inboundAccessControlSettings = AccessControlModel.merge(
                    AccessControlModel.fromClassAnnotation(request.getClass()),
                    inboundAccessControlSettings
            );
        }
        // retrieve data
        return retrieveDataIfAllowed(
                accessControlEvaluator,
                request,
                () -> DataRetrievalUtil.retrieveDataNullable(() -> dataSupplier.get(request)),
                inboundAccessControlSettings
        );
    }

}
