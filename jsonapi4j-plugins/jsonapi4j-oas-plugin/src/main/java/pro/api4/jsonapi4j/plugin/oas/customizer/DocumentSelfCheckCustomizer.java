package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import pro.api4.jsonapi4j.plugin.oas.diagnostics.OasDiagnostics;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Checks the finished document against itself: every {@code $ref} it contains must resolve to something it also
 * declares.
 * <p>
 * A reference that resolves to nothing is the one defect no reader forgives - a generated client fails to compile,
 * a validator rejects every response, Swagger UI renders an empty box - and it is the one this plugin is most able
 * to produce, since a schema is registered in one customizer and referenced in another. Checking is cheap and the
 * alternative is finding out from whoever consumed the document.
 * <p>
 * Runs last, after every customizer including the application's own, because a reference added at any point is
 * still a reference the document has to honour.
 */
public class DocumentSelfCheckCustomizer implements OasCustomizer {

    private static final String COMPONENTS_PREFIX = "#/components/";

    private final boolean failOnMisconfiguration;

    public DocumentSelfCheckCustomizer(boolean failOnMisconfiguration) {
        this.failOnMisconfiguration = failOnMisconfiguration;
    }

    @Override
    public void customise(OpenAPI openApi) {
        Findings findings = new Findings();
        collectRefs(openApi, new LinkedHashSet<>(), findings, declaredComponents(openApi.getComponents()));

        if (!findings.dangling.isEmpty()) {
            OasDiagnostics.report(
                    failOnMisconfiguration,
                    "The generated document references %s that it does not declare: %s. A reference resolving to "
                            + "nothing breaks client generation and response validation.",
                    findings.dangling.size() == 1 ? "a component" : "components",
                    String.join(", ", findings.dangling)
            );
        }
        if (!findings.unreadable.isEmpty()) {
            OasDiagnostics.report(
                    failOnMisconfiguration,
                    "%d field(s) of the generated document could not be read, so this check did not cover them: %s. "
                            + "A check that silently covers less than it claims is worse than none.",
                    findings.unreadable.size(),
                    String.join(", ", findings.unreadable)
            );
        }
    }

    /**
     * What one pass over the document turned up. Unreadable fields are collected rather than ignored: reflection can
     * be refused - a module that does not open its packages, a security manager - and a check that quietly inspects
     * less than it says it does would let the very defect it exists for through.
     */
    private static final class Findings {
        private final Set<String> dangling = new LinkedHashSet<>();
        private final Set<String> unreadable = new LinkedHashSet<>();
    }

    private Set<String> declaredComponents(Components components) {
        Set<String> declared = new LinkedHashSet<>();
        if (components == null) {
            return declared;
        }
        declare(declared, "schemas", components.getSchemas());
        declare(declared, "responses", components.getResponses());
        declare(declared, "parameters", components.getParameters());
        declare(declared, "examples", components.getExamples());
        declare(declared, "requestBodies", components.getRequestBodies());
        declare(declared, "headers", components.getHeaders());
        declare(declared, "securitySchemes", components.getSecuritySchemes());
        declare(declared, "links", components.getLinks());
        declare(declared, "callbacks", components.getCallbacks());
        return declared;
    }

    private void declare(Set<String> declared,
                         String section,
                         Map<String, ?> entries) {
        if (entries != null) {
            entries.keySet().forEach(name -> declared.add(COMPONENTS_PREFIX + section + "/" + name));
        }
    }

    /**
     * Walks the model rather than its serialized form: serializing to find references would depend on the very
     * mapper configuration the document is about to be written with, and a reference is a field on a model object
     * whichever way it is later printed.
     */
    private void collectRefs(Object node,
                             Set<Object> seen,
                             Findings findings,
                             Set<String> declared) {
        if (node == null || !seen.add(node)) {
            return;
        }
        if (node instanceof Map<?, ?> map) {
            map.values().forEach(value -> collectRefs(value, seen, findings, declared));
            return;
        }
        if (node instanceof Collection<?> collection) {
            collection.forEach(value -> collectRefs(value, seen, findings, declared));
            return;
        }
        // an enum constant carries no reference, and walking one reaches java.lang.Enum's own inaccessible fields
        if (node instanceof Enum<?> || !node.getClass().getName().startsWith("io.swagger.v3.oas.models")) {
            return;
        }
        for (Class<?> type = node.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                readField(node, field, seen, findings, declared);
            }
        }
    }

    private void readField(Object node,
                           Field field,
                           Set<Object> seen,
                           Findings findings,
                           Set<String> declared) {
        if (field.isSynthetic() || java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
            return;
        }
        Object value;
        try {
            field.setAccessible(true);
            value = field.get(node);
        } catch (RuntimeException | ReflectiveOperationException e) {
            findings.unreadable.add(field.getDeclaringClass().getSimpleName() + "." + field.getName());
            return;
        }
        if ("$ref".equals(field.getName()) && value instanceof String ref) {
            if (ref.startsWith(COMPONENTS_PREFIX) && !declared.contains(ref)) {
                findings.dangling.add(ref);
            }
            return;
        }
        collectRefs(value, seen, findings, declared);
    }

}
