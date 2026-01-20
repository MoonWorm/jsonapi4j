package pro.api4.jsonapi4j.processor.single;

import pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.processor.util.DataRetrievalUtil;

import static pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator.retrieveDataIfAllowed;

public class SingleDataItemsRetrievalStage {

    private final AccessControlEvaluator accessControlEvaluator;
    private AccessControlModel inboundAccessControlSettings;

    public SingleDataItemsRetrievalStage(AccessControlEvaluator accessControlEvaluator,
                                         AccessControlModel inboundAccessControlSettings) {
        this.accessControlEvaluator = accessControlEvaluator;
        this.inboundAccessControlSettings = inboundAccessControlSettings;
    }

    public <REQUEST, DATA_SOURCE_DTO> DATA_SOURCE_DTO retrieveData(REQUEST request,
                                                                   SingleDataItemSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier) {
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
