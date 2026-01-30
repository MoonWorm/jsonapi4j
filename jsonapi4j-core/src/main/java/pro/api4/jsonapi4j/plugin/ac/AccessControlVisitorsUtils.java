package pro.api4.jsonapi4j.plugin.ac;

import pro.api4.jsonapi4j.plugin.JsonApiPluginInfo;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;

public final class AccessControlVisitorsUtils {

    private AccessControlVisitorsUtils() {

    }

    static <REQUEST> AccessControlModel getInboundAccessControlModel(
            JsonApiPluginInfo pluginInfo,
            REQUEST request
    ) {
        AccessControlModel fromOperationDeclaration = null;
        if (pluginInfo != null && pluginInfo.getOperationPluginInfo() instanceof AccessControlModel acm) {
            fromOperationDeclaration = acm;
        }
        if (request != null) {
            AccessControlModel fromRequest = AccessControlModel.fromClassAnnotation(request.getClass());
            return AccessControlModel.merge(fromRequest, fromOperationDeclaration);
        }
        return fromOperationDeclaration;
    }

}
