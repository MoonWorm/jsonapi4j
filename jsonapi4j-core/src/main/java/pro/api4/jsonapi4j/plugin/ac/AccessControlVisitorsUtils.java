package pro.api4.jsonapi4j.plugin.ac;

import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.plugin.JsonApiPluginInfo;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForCustomClass;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForJsonApiResource;
import pro.api4.jsonapi4j.processor.util.AccessControlUtil;

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

    static <RESOURCE extends ResourceObject<?, ?>> OutboundAccessControlForCustomClass getOutboundAccessControlModel(
            JsonApiPluginInfo pluginInfo,
            RESOURCE resource
    ) {
        OutboundAccessControlForJsonApiResource fromResourceDeclaration = null;
        if (pluginInfo != null && pluginInfo.getResourcePluginInfo() instanceof OutboundAccessControlForJsonApiResource acm) {
            fromResourceDeclaration = acm;
        }
        return AccessControlUtil.getEffectiveOutboundAccessControlSettings(
                resource,
                fromResourceDeclaration
        );
    }

}
