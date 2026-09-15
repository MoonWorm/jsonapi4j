package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import pro.api4.jsonapi4j.http.HttpStatusCodes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.capitalize;

/**
 * Folds responses and parameters that every operation repeats verbatim into {@code components}, leaving a
 * {@code $ref} behind.
 * <p>
 * Nothing about what the document says changes - a {@code $ref} and the object it points at are the same thing to
 * any reader. What changes is that the difference between two operations stops being buried under hundreds of lines
 * of identical error responses.
 * <p>
 * Runs last, after the customizers an application registered, so that a response someone tuned by hand is folded in
 * its final shape rather than being replaced by a {@code $ref} the application never sees.
 */
public class SharedComponentsCustomizer implements OasCustomizer {

    /**
     * The three digits OpenAPI allows as a response key. Anything else it allows - {@code default}, a {@code 4XX}
     * range wildcard - is a key this pass has to name without reading a number out of it.
     */
    private static final Pattern STATUS_CODE = Pattern.compile("\\d{3}");

    private static final String STATUS_NAME_PREFIX = "Status";

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getPaths() == null || openApi.getPaths().isEmpty()) {
            return;
        }
        List<Operation> operations = openApi.getPaths().values().stream()
                .flatMap(pathItem -> pathItem.readOperations().stream())
                .toList();

        shareResponses(openApi, operations);
        shareParameters(openApi, operations);
    }

    private void shareResponses(OpenAPI openApi,
                                List<Operation> operations) {
        Map<String, List<ApiResponse>> byStatus = new LinkedHashMap<>();
        operations.forEach(operation -> operation.getResponses()
                .forEach((status, response) -> byStatus.computeIfAbsent(status, s -> new ArrayList<>()).add(response)));

        Map<String, String> sharedNames = shareable(byStatus, this::responseComponentName, registered(openApi, Components::getResponses));
        if (sharedNames.isEmpty()) {
            return;
        }
        sharedNames.forEach((status, name) -> components(openApi).addResponses(name, byStatus.get(status).get(0)));

        operations.forEach(operation -> sharedNames.forEach((status, name) -> {
            if (operation.getResponses().containsKey(status)) {
                operation.getResponses().put(status, new ApiResponse().$ref(name));
            }
        }));
    }

    private void shareParameters(OpenAPI openApi,
                                 List<Operation> operations) {
        Map<String, List<Parameter>> byName = new LinkedHashMap<>();
        operations.stream()
                .filter(operation -> operation.getParameters() != null)
                .forEach(operation -> operation.getParameters()
                        .forEach(parameter -> byName.computeIfAbsent(parameter.getName(), n -> new ArrayList<>()).add(parameter)));

        Map<String, String> sharedNames = shareable(byName, this::parameterComponentName, registered(openApi, Components::getParameters));
        if (sharedNames.isEmpty()) {
            return;
        }
        sharedNames.forEach((parameterName, name) -> components(openApi).addParameters(name, byName.get(parameterName).get(0)));

        operations.stream()
                .filter(operation -> operation.getParameters() != null)
                .forEach(operation -> operation.setParameters(operation.getParameters().stream()
                        .map(parameter -> sharedNames.containsKey(parameter.getName())
                                ? new Parameter().$ref(sharedNames.get(parameter.getName()))
                                : parameter)
                        .toList()));
    }

    /**
     * Picks the keys worth sharing: those that appear more than once and always look the same. A key whose shape
     * varies between operations is left alone entirely - one {@code $ref} cannot stand for two different objects, and
     * sharing only some of its occurrences would make the document harder to read rather than easier.
     * <p>
     * A key that derives to no name at all is left inline too: a component registered under an empty key is a
     * reference nothing can resolve, which is worse than the repetition this pass exists to remove.
     */
    private <T> Map<String, String> shareable(Map<String, List<T>> occurrences,
                                              Function<String, String> naming,
                                              Map<String, ?> alreadyRegistered) {
        Map<String, String> sharedNames = new LinkedHashMap<>();
        Set<String> takenNames = alreadyRegistered == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(alreadyRegistered.keySet());
        occurrences.forEach((key, values) -> {
            if (values.size() < 2 || new LinkedHashSet<>(values).size() != 1) {
                return;
            }
            String name = naming.apply(key);
            if (!name.isEmpty() && takenNames.add(name)) {
                sharedNames.put(key, name);
            }
        });
        return sharedNames;
    }

    /**
     * Names a shared response after what the status code means rather than after the number, so the components
     * section reads as a list of outcomes: {@code TooManyRequests}, {@code NoContent}, {@code ResourceNotFound}.
     * <p>
     * Not every response key is a number. OpenAPI also allows {@code default} and the {@code 4XX} range wildcards,
     * and an application's own {@link OasCustomizer} - which runs before this pass - is the documented way to add
     * one. Those keep the key in their name ({@code Default}, {@code Status4XX}): naming is the last thing that
     * should be able to bring a document down.
     */
    private String responseComponentName(String status) {
        if (ApiResponses.DEFAULT.equals(status)) {
            return capitalize(ApiResponses.DEFAULT);
        }
        if (!STATUS_CODE.matcher(status).matches()) {
            return STATUS_NAME_PREFIX + status;
        }
        return HttpStatusCodes.fromCode(Integer.parseInt(status))
                .map(code -> pascalCase(code.name().replaceFirst("^SC_\\d+_", "")))
                .orElseGet(() -> STATUS_NAME_PREFIX + status);
    }

    /**
     * JSON:API spells its parameters with brackets, which a component key may not contain, so {@code fields[users]}
     * is registered as {@code FieldsUsers}.
     */
    private String parameterComponentName(String parameterName) {
        return pascalCase(parameterName);
    }

    private String pascalCase(String raw) {
        StringBuilder name = new StringBuilder();
        for (String segment : raw.split("[^A-Za-z0-9]+")) {
            if (!segment.isEmpty()) {
                name.append(capitalize(segment.toLowerCase()));
            }
        }
        return name.toString();
    }

    private Map<String, ?> registered(OpenAPI openApi,
                                      Function<Components, Map<String, ?>> section) {
        return openApi.getComponents() == null ? null : section.apply(openApi.getComponents());
    }

    private Components components(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        return openApi.getComponents();
    }

}
