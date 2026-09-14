package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSchemaNamesUtil;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import pro.api4.jsonapi4j.plugin.PluginRegistry;

/**
 * Documents the {@code 403} this plugin makes reachable.
 * <p>
 * The OpenAPI plugin derives an operation's responses from what the framework core does, and knows nothing about
 * access control - it should not have to. A plugin that adds an outcome describes that outcome itself, which is what
 * this customizer is: register it alongside the access control plugin and the generated document gains a {@code 403}
 * on every write.
 * <p>
 * Writes only. A denied read is answered with an empty document and a {@code 200}, deliberately, so that the
 * compound-documents resolver can keep going - see {@code AccessControlSingleResourceVisitors}.
 */
public class AccessControlOasCustomizer implements OasCustomizer {

    /**
     * Matched by name: this module describes what the access control plugin contributes without depending on it, and
     * a plugin that is absent or disabled contributes nothing to describe.
     */
    private static final String ACCESS_CONTROL_PLUGIN_NAME = "JsonApiAccessControlPlugin";

    private static final String FORBIDDEN = String.valueOf(HttpStatusCodes.SC_403_FORBIDDEN.getCode());

    private static final Set<PathItem.HttpMethod> READ_METHODS = Set.of(PathItem.HttpMethod.GET);

    private final Supplier<PluginRegistry> pluginRegistry;

    /**
     * The registry is supplied rather than injected: it is what holds this plugin, so taking it directly would close
     * a cycle. It is resolved when the document is generated, by which time it exists.
     */
    public AccessControlOasCustomizer(Supplier<PluginRegistry> pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
    }

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getPaths() == null || !pluginRegistry.get().isActivePlugin(ACCESS_CONTROL_PLUGIN_NAME)) {
            return;
        }
        openApi.getPaths().values().forEach(pathItem -> pathItem.readOperationsMap()
                .forEach((method, operation) -> {
                    if (!READ_METHODS.contains(method)) {
                        addForbidden(operation);
                    }
                }));
    }

    private void addForbidden(Operation operation) {
        if (operation.getResponses() == null || operation.getResponses().containsKey(FORBIDDEN)) {
            return;
        }
        String exampleName = ErrorExamplesCustomizer.CODES_TO_EXAMPLE_NAME.get(HttpStatusCodes.SC_403_FORBIDDEN);
        operation.getResponses().addApiResponse(FORBIDDEN, new ApiResponse()
                .description(HttpStatusCodes.SC_403_FORBIDDEN.getDescription())
                .content(new Content().addMediaType(
                        JsonApiMediaType.MEDIA_TYPE,
                        new MediaType()
                                .schema(new Schema<>().$ref(OasSchemaNamesUtil.errorsDocSchemaName()))
                                .examples(exampleName == null
                                        ? null
                                        : Map.of(exampleName, new Example().$ref("#/components/examples/" + exampleName)))
                )));
    }

}
