package pro.api4.jsonapi4j.plugin.oas.customizer.util;

import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.RegisteredResource;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.domain.model.OasResourceInfoModel;

import java.util.Optional;

import static org.apache.commons.collections4.MapUtils.emptyIfNull;

/**
 * Reads what a resource declared through {@link pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasResourceInfo}.
 * <p>
 * The identifier accessors exist because an id surfaces in several places at once - a path parameter and the
 * {@code id} member of every schema describing that resource - and all of them have to agree. Declaring it per
 * operation is what let them drift.
 */
public final class OasResourceInfoUtil {

    private static final String DEFAULT_RESOURCE_ID_DESCRIPTION = "Resource unique identifier";
    private static final String DEFAULT_RESOURCE_ID_EXAMPLE = "12345";

    private OasResourceInfoUtil() {

    }

    public static Optional<OasResourceInfoModel> resourceInfo(RegisteredResource<?> registeredResource) {
        if (registeredResource == null) {
            return Optional.empty();
        }
        Object pluginInfo = emptyIfNull(registeredResource.getPluginInfo()).get(JsonApiOasPlugin.NAME);
        return pluginInfo instanceof OasResourceInfoModel oasResourceInfo
                ? Optional.of(oasResourceInfo)
                : Optional.empty();
    }

    public static Optional<OasResourceInfoModel> resourceInfo(DomainRegistry domainRegistry,
                                                              ResourceType resourceType) {
        return domainRegistry == null
                ? Optional.empty()
                : resourceInfo(domainRegistry.getResource(resourceType));
    }

    public static String resourceIdDescription(Optional<OasResourceInfoModel> resourceInfo) {
        return resourceInfo
                .map(OasResourceInfoModel::getResourceIdDescription)
                .filter(StringUtils::isNotBlank)
                .orElse(DEFAULT_RESOURCE_ID_DESCRIPTION);
    }

    public static String resourceIdExample(Optional<OasResourceInfoModel> resourceInfo) {
        return resourceInfo
                .map(OasResourceInfoModel::getResourceIdExample)
                .filter(StringUtils::isNotBlank)
                .orElse(DEFAULT_RESOURCE_ID_EXAMPLE);
    }

}
