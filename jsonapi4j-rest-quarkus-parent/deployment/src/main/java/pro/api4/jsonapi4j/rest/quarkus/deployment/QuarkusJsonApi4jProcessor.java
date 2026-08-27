package pro.api4.jsonapi4j.rest.quarkus.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.undertow.deployment.FilterBuildItem;
import io.quarkus.undertow.deployment.IgnoredServletContainerInitializerBuildItem;
import io.quarkus.undertow.deployment.ListenerBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import jakarta.servlet.DispatcherType;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.filter.principal.PrincipalResolvingFilter;
import pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer;
import pro.api4.jsonapi4j.rest.quarkus.runtime.*;
import pro.api4.jsonapi4j.rest.quarkus.runtime.ac.QuarkusJsonApi4jAcPluginBeans;
import pro.api4.jsonapi4j.rest.quarkus.runtime.ac.QuarkusJsonApi4jAcProperties;
import pro.api4.jsonapi4j.rest.quarkus.runtime.cd.QuarkusJsonApi4jCompoundDocsPluginBeans;
import pro.api4.jsonapi4j.rest.quarkus.runtime.cd.QuarkusJsonApi4jCompoundDocsProperties;
import pro.api4.jsonapi4j.rest.quarkus.runtime.cd.QuarkusJsonApi4jCompoundDocsServletContextListener;
import pro.api4.jsonapi4j.rest.quarkus.runtime.oas.QuarkusJsonApi4jOasPluginBeans;
import pro.api4.jsonapi4j.rest.quarkus.runtime.oas.QuarkusJsonApi4jOasProperties;
import pro.api4.jsonapi4j.rest.quarkus.runtime.oas.QuarkusJsonApi4jOasServletContextListener;
import pro.api4.jsonapi4j.rest.quarkus.runtime.sf.QuarkusJsonApi4jSfPluginBeans;
import pro.api4.jsonapi4j.rest.quarkus.runtime.sf.QuarkusJsonApi4jSfProperties;
import pro.api4.jsonapi4j.servlet.JsonApi4jDispatcherServlet;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;

import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.*;

class QuarkusJsonApi4jProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(QuarkusJsonApi4jProcessor.class);

    private static final String FEATURE = "jsonapi4j-rest-quarkus";

    private static final String AC_PLUGIN_CLASSNAME = "pro.api4.jsonapi4j.plugin.ac.JsonApiAccessControlPlugin";

    private static final DotName ACCESS_CONTROL_ANNOTATION
            = DotName.createSimple("pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl");

    private static final DotName RESOURCE_INTERFACE = DotName.createSimple(Resource.class.getName());
    private static final DotName OBJECT_CLASS = DotName.createSimple(Object.class.getName());

    private static final String OAS_PLUGIN_CLASSNAME = "pro.api4.jsonapi4j.plugin.oas.init.JsonApiOasServletContainerInitializer";

    private static final String OAS_MODEL_GROUP_ID = "io.swagger.core.v3";
    private static final String OAS_MODEL_ARTIFACT_ID = "swagger-models-jakarta";
    private static final String OAS_MODEL_PACKAGE_PREFIX = "io.swagger.v3.oas.models.";
    private static final String OAS_ERROR_EXAMPLES_RESOURCE_PATTERN = "oas/errorExamples/.*\\.json";
    private static final String JACKSON_BEAN_DESCRIPTION_CLASSNAME = "com.fasterxml.jackson.databind.BeanDescription";

    private static final String SF_PLUGIN_CLASSNAME = "pro.api4.jsonapi4j.plugin.sf.JsonApiSparseFieldsetsPlugin";
    private static final String CD_SERVLET_INITIALIZER_CLASSNAME = "pro.api4.jsonapi4j.plugin.cd.init.JsonApi4jCompoundDocsServletContainerInitializer";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ServletBuildItem registersJsonApi4jDispatcherServlet(QuarkusJsonApi4jProperties props) {
        String mapping = toServletMapping(props.rootPath());
        LOG.info("Registering JsonApi4jDispatcherServlet on '{}'", mapping);
        return ServletBuildItem.builder(JSONAPI4J_DISPATCHER_SERVLET_NAME, JsonApi4jDispatcherServlet.class.getName())
                .addMapping(mapping)
                .setLoadOnStartup(1)
                .build();
    }

    @BuildStep
    void registerOasServlet(QuarkusJsonApi4jOasProperties oasProperties,
                            BuildProducer<ServletBuildItem> servlets) {

        if (isOasPluginEnabled(oasProperties)) {
            String mapping = toServletMapping(oasProperties.oasRootPath());
            LOG.info("{} plugin is enabled in properties ('jsonapi4j.oas.enabled') and related classes are present in classpath, registering OAS Servlet", OAS_PLUGIN_CLASSNAME);
            servlets.produce(
                    ServletBuildItem.builder("jsonApi4jOasServlet", "pro.api4.jsonapi4j.plugin.oas.OasServlet")
                            .addMapping(mapping)
                            .setLoadOnStartup(2)
                            .build()
            );
        } else {
            LOG.info("{} plugin is either disabled in properties ('jsonapi4j.oas.enabled') or related classes are not available in classpath, skipping OAS Servlet registration", OAS_PLUGIN_CLASSNAME);
        }
    }

    @BuildStep
    void registerCompoundDocsFilter(QuarkusJsonApi4jProperties jsonApi4jProperties,
                                    QuarkusJsonApi4jCompoundDocsProperties cdProperties,
                                    BuildProducer<FilterBuildItem> filters) {
        if (!cdProperties.enabled()) {
            LOG.info("Compound docs disabled, skipping CompoundDocsFilter registration");
            return;
        }

        String mapping = toServletMapping(jsonApi4jProperties.rootPath());
        LOG.info("Registering CompoundDocsFilter on '{}'", mapping);
        filters.produce(
                FilterBuildItem.builder("jsonapi4jCompoundDocsFilter", "pro.api4.jsonapi4j.plugin.cd.CompoundDocsFilter")
                        .addFilterUrlMapping(mapping, DispatcherType.REQUEST)
                        .setLoadOnStartup(2)
                        .build()
        );
    }

    @BuildStep
    FilterBuildItem registerPrincipalResolvingFilter(QuarkusJsonApi4jProperties props) {
        String mapping = toServletMapping(props.rootPath());
        LOG.info("Registering PrincipalResolvingFilter on '{}'", mapping);
        return FilterBuildItem.builder(JSONAPI4J_PRINCIPAL_RESOLVING_FILTER_NAME, PrincipalResolvingFilter.class.getName())
                .addFilterUrlMapping(mapping, DispatcherType.REQUEST)
                .setLoadOnStartup(1)
                .build();
    }

    @BuildStep
    FilterBuildItem registerRequestBodyCachingFilter(QuarkusJsonApi4jProperties props) {
        String mapping = toServletMapping(props.rootPath());
        LOG.info("Registering RequestBodyCachingFilter on '{}'", mapping);
        return FilterBuildItem.builder(
                        JSONAPI4J_REQUEST_BODY_CACHING_FILTER_NAME,
                        "pro.api4.jsonapi4j.servlet.request.body.RequestBodyCachingFilter"
                ).addFilterUrlMapping(mapping, DispatcherType.REQUEST)
                .setLoadOnStartup(1)
                .build();
    }

    /**
     * Registers the JSON:API document model itself.
     *
     * <p>These are the framework's own classes, not the application's, and two things need them at
     * runtime: access control walks the composed document reflectively to find what to hide, and Jackson
     * serializes it on the way out. Neither is visible to the native-image analysis — the document is
     * written through the framework's own mapper rather than a declared endpoint return type — so without
     * this a request returns {@code 500} the moment anonymization touches it, and the error document
     * describing that failure serializes to an empty body.
     */
    @BuildStep
    void registerJsonApiDocumentModel(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                        "pro.api4.jsonapi4j.model.document.BaseDoc",
                        "pro.api4.jsonapi4j.model.document.LinkObject",
                        "pro.api4.jsonapi4j.model.document.LinksObject",
                        "pro.api4.jsonapi4j.model.document.data.JsonApiObject",
                        "pro.api4.jsonapi4j.model.document.data.MultipleResourcesDoc",
                        "pro.api4.jsonapi4j.model.document.data.RelationshipObject",
                        "pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject",
                        "pro.api4.jsonapi4j.model.document.data.ResourceObject",
                        "pro.api4.jsonapi4j.model.document.data.SingleResourceDoc",
                        "pro.api4.jsonapi4j.model.document.data.ToManyRelationshipObject",
                        "pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc",
                        "pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc",
                        "pro.api4.jsonapi4j.model.document.data.ToOneRelationshipObject",
                        "pro.api4.jsonapi4j.model.document.error.ErrorObject",
                        "pro.api4.jsonapi4j.model.document.error.ErrorSourceObject",
                        "pro.api4.jsonapi4j.model.document.error.ErrorsDoc",
                        "pro.api4.jsonapi4j.model.document.meta.MetaDoc")
                .constructors(true)
                .methods(true)
                .fields(true)
                .serialization(true)
                .build());
    }

    /**
     * Puts the OpenAPI model jar into the index, so the registration below has something to find.
     *
     * <p>This is the case {@code quarkus.index-dependency} exists for: a third-party jar that ships no Jandex
     * index and cannot be changed to ship one. The framework's own jars carry an index instead.
     *
     * <p>Produced only when the OAS plugin is both enabled and on the classpath, so an application that does not
     * serve an OpenAPI document pays neither the indexing nor the image size.
     */
    @BuildStep
    void indexOasModel(QuarkusJsonApi4jOasProperties oasProperties,
                       BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        if (!isOasPluginEnabled(oasProperties)) {
            return;
        }
        indexDependencies.produce(new IndexDependencyBuildItem(OAS_MODEL_GROUP_ID, OAS_MODEL_ARTIFACT_ID));
    }

    /**
     * Registers the OpenAPI model, so that the served document can be built in a native image.
     *
     * <p>The plugin assembles the document by binding into Swagger's model rather than by constructing it field
     * by field, which needs reflective access to those classes. They are third-party and ship no native-image
     * metadata of their own, and {@link #addReferencedClasses} deliberately walks only indexed types — so
     * without this step the image builds and then answers every request for the OpenAPI document with
     * {@code 500 Cannot construct instance of io.swagger.v3.oas.models.media.ObjectSchema}.
     *
     * <p>Registered by package rather than by naming classes: the model is large, entirely reachable from the
     * document root, and grows between Swagger versions.
     */
    @BuildStep
    void registerOasModelForSerialization(QuarkusJsonApi4jOasProperties oasProperties,
                                          CombinedIndexBuildItem combinedIndex,
                                          BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        if (!isOasPluginEnabled(oasProperties)) {
            return;
        }
        String[] classNames = combinedIndex.getIndex().getKnownClasses().stream()
                .map(classInfo -> classInfo.name().toString())
                .filter(className -> className.startsWith(OAS_MODEL_PACKAGE_PREFIX))
                .toArray(String[]::new);
        if (classNames.length == 0) {
            LOG.warn("{} plugin is enabled but no OpenAPI model classes were found in the Jandex index, so "
                            + "nothing was registered for native-image serialization. The OpenAPI document will "
                            + "fail to build in a native image. Expected to find '{}' classes from '{}:{}'.",
                    OAS_PLUGIN_CLASSNAME, OAS_MODEL_PACKAGE_PREFIX, OAS_MODEL_GROUP_ID, OAS_MODEL_ARTIFACT_ID);
            return;
        }
        LOG.info("Registering {} OpenAPI model classes for native-image serialization", classNames.length);
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(classNames)
                .constructors(true)
                .methods(true)
                .fields(true)
                .serialization(true)
                .build());

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(JACKSON_BEAN_DESCRIPTION_CLASSNAME)
                .methods(true)
                .build());
    }

    /**
     * Includes the OAS plugin's canned error examples in the native image.
     *
     * <p>A native image carries no classpath resource unless it was asked to. The plugin reads these with
     * {@code getResourceAsStream} and treats a {@code null} stream as a bad path, so leaving them out surfaces
     * as {@code IllegalArgumentException: wrong path} while serving the document rather than as a missing file.
     *
     * <p>Matched by pattern so that examples added to the plugin later are picked up without touching this.
     */
    @BuildStep
    void registerOasErrorExampleResources(QuarkusJsonApi4jOasProperties oasProperties,
                                          BuildProducer<NativeImageResourcePatternsBuildItem> resourcePatterns) {
        if (!isOasPluginEnabled(oasProperties)) {
            return;
        }
        resourcePatterns.produce(NativeImageResourcePatternsBuildItem.builder()
                .includePattern(OAS_ERROR_EXAMPLES_RESOURCE_PATTERN)
                .build());
    }

    /**
     * Registers the attributes objects resources expose, so that Jackson can serialize them in a native image.
     *
     * <p>An attributes object never appears as a declared endpoint return type — the framework hands it to its
     * own mapper as an {@code Object} — so the native-image analysis has no reason to keep its members. Without
     * registration the class builds fine and then serializes to {@code No serializer found for class ...} on the
     * first request that returns it.
     *
     * <p>{@code Resource<RESOURCE_DTO>} is parameterized by the downstream DTO, not by the attributes type, so
     * the type argument is the wrong thing to read. The attributes type is whatever
     * {@link pro.api4.jsonapi4j.domain.Resource#resolveAttributes} is narrowed to by the override — an
     * implementor that leaves the declared {@code Object} return type in place is telling us nothing, and is
     * skipped.
     */
    @BuildStep
    void registerResourceAttributesForSerialization(CombinedIndexBuildItem combinedIndex,
                                                    BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        IndexView index = combinedIndex.getIndex();
        Set<DotName> serializable = new LinkedHashSet<>();
        for (ClassInfo resource : index.getAllKnownImplementors(RESOURCE_INTERFACE)) {
            for (MethodInfo method : resource.methods()) {
                if (method.isSynthetic()
                        || !Resource.RESOLVE_ATTRIBUTES_METHOD_NAME.equals(method.name())
                        || method.parametersCount() != 1) {
                    continue;
                }
                Type returnType = method.returnType();
                if (returnType.kind() != Type.Kind.VOID && !OBJECT_CLASS.equals(returnType.name())) {
                    collectClassNames(returnType, serializable);
                }
            }
        }
        serializable.removeIf(name -> index.getClassByName(name) == null);
        if (serializable.isEmpty()) {
            return;
        }
        addReferencedClasses(index, serializable);

        String[] classNames = serializable.stream().map(DotName::toString).toArray(String[]::new);
        LOG.info("Registering {} resource attributes classes for native-image serialization", classNames.length);
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(classNames)
                .constructors(true)
                .methods(true)
                .fields(true)
                .serialization(true)
                .build());
    }

    /**
     * Grows the set to include everything reachable from an attributes object, repeatedly, until it stops
     * changing.
     *
     * <p>Jackson walks the whole graph, so a nested type needs registering just as much as the attributes class
     * that holds it. Subtypes are pulled in too: a field declared as {@code Address} can hold a
     * {@code HomeAddress} at runtime, and only the declared type is visible from the field alone.
     *
     * <p>Types absent from the index are dropped rather than registered — that keeps the JDK and third-party
     * libraries out, which bring their own native-image metadata and must not be walked here.
     */
    private static void addReferencedClasses(IndexView index, Set<DotName> serializable) {
        Deque<DotName> pending = new ArrayDeque<>(serializable);
        while (!pending.isEmpty()) {
            ClassInfo current = index.getClassByName(pending.poll());
            if (current == null) {
                continue;
            }
            Set<DotName> referenced = new LinkedHashSet<>();
            current.fields().forEach(field -> collectClassNames(field.type(), referenced));
            index.getAllKnownSubclasses(current.name()).forEach(subclass -> referenced.add(subclass.name()));
            for (DotName candidate : referenced) {
                if (index.getClassByName(candidate) != null && serializable.add(candidate)) {
                    pending.add(candidate);
                }
            }
        }
    }

    /**
     * Collects every class named by a type, looking through the generics and arrays that wrap it.
     *
     * <p>A {@code List<Address>} names {@code java.util.List} at the top level; the element type only shows up
     * in the type arguments, and it is the one that has to be serializable.
     */
    private static void collectClassNames(Type type, Set<DotName> out) {
        switch (type.kind()) {
            case CLASS -> out.add(type.name());
            case PARAMETERIZED_TYPE -> {
                out.add(type.name());
                type.asParameterizedType().arguments().forEach(argument -> collectClassNames(argument, out));
            }
            case ARRAY -> collectClassNames(type.asArrayType().component(), out);
            default -> { }
        }
    }

    /**
     * Registers the classes access control may have to redact, so that redaction works in a native image.
     *
     * <p>Hiding a field produces a copy of the object with that field blanked, and building that copy
     * instantiates the class without running its constructor. Native image permits that only for classes it
     * was told about ahead of time; without the registration the image still builds, and then fails at
     * runtime the first time a caller is denied — which is exactly the request you least want to fail.
     *
     * <p>Registration covers more than the annotated classes themselves. Redacting a field deep in a graph
     * copies every object on the path to it, so a class that merely holds an annotated one is copied too and
     * has to be registered as well. The set is therefore closed over "declares a field of a registered
     * type" until it stops growing.
     */
    @BuildStep
    void registerAccessControlledClassesForRedaction(QuarkusJsonApi4jAcProperties acProperties,
                                                     CombinedIndexBuildItem combinedIndex,
                                                     BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        if (!isAcPluginEnabled(acProperties)) {
            return;
        }
        IndexView index = combinedIndex.getIndex();
        Set<DotName> redactable = new LinkedHashSet<>();
        for (AnnotationInstance annotation : index.getAnnotations(ACCESS_CONTROL_ANNOTATION)) {
            AnnotationTarget target = annotation.target();
            if (target.kind() == AnnotationTarget.Kind.CLASS) {
                redactable.add(target.asClass().name());
            } else if (target.kind() == AnnotationTarget.Kind.FIELD) {
                redactable.add(target.asField().declaringClass().name());
            }
        }
        if (redactable.isEmpty()) {
            LOG.warn("Access control is enabled but no @AccessControl was found in the Jandex index, so "
                    + "nothing was registered for native-image redaction. Quarkus indexes the application "
                    + "module automatically but not its dependencies: if your attributes classes live in "
                    + "another jar, build a Jandex index into it (jandex-maven-plugin), or index it from "
                    + "the application with 'quarkus.index-dependency.<name>.group-id' and '.artifact-id'.");
            return;
        }
        addClassesHolding(index, redactable);

        String[] classNames = redactable.stream().map(DotName::toString).toArray(String[]::new);
        LOG.info("Registering {} access-controlled classes for native-image redaction", classNames.length);
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(classNames)
                .constructors(true)
                .methods(true)
                .fields(true)
                .serialization(true)
                .unsafeAllocated(true)
                .build());
    }

    /**
     * Whether a field's type is one of the given types, or holds one.
     *
     * <p>A {@code List<Address>} names {@code java.util.List}, so the element type has to be read from the
     * type arguments — otherwise a class holding a list of an annotated type is never registered, and
     * rebuilding that list at runtime fails in a native image.
     */
    private static boolean mentions(Type type, Set<DotName> redactable) {
        if (redactable.contains(type.name())) {
            return true;
        }
        return switch (type.kind()) {
            case PARAMETERIZED_TYPE -> type.asParameterizedType().arguments().stream()
                    .anyMatch(argument -> mentions(argument, redactable));
            case ARRAY -> mentions(type.asArrayType().component(), redactable);
            default -> false;
        };
    }

    /**
     * Grows the set to include every class holding a field of an already-included type, repeatedly, until it
     * stops changing.
     */
    private static void addClassesHolding(IndexView index, Set<DotName> redactable) {
        boolean grown = true;
        while (grown) {
            grown = false;
            for (ClassInfo candidate : index.getKnownClasses()) {
                if (redactable.contains(candidate.name())) {
                    continue;
                }
                boolean holdsRedactable = candidate.fields().stream()
                        .anyMatch(field -> mentions(field.type(), redactable));
                if (holdsRedactable) {
                    redactable.add(candidate.name());
                    grown = true;
                }
            }
        }
    }

    @BuildStep
    AdditionalBeanBuildItem jsonapi4jCdiBeans(QuarkusJsonApi4jOasProperties oasProperties,
                                              QuarkusJsonApi4jAcProperties acProperties,
                                              QuarkusJsonApi4jSfProperties sfProperties,
                                              QuarkusJsonApi4jCompoundDocsProperties cdProperties) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClass(QuarkusJsonApi4jDispatcherServletContextListener.class)
                .addBeanClass(QuarkusJsonApi4jDefaultBeans.class)
                .addBeanClass(QuarkusJsonApi4jProperties.class);

        if (isAcPluginEnabled(acProperties)) {
            LOG.info("{} plugin is enabled, registering AC-related CDI beans", AC_PLUGIN_CLASSNAME);
            builder.addBeanClass(QuarkusJsonApi4jAcProperties.class.getName());
            builder.addBeanClass(QuarkusJsonApi4jAcPluginBeans.class.getName());
        } else {
            LOG.info("{} plugin is disabled, skipping AC-related CDI beans registration", AC_PLUGIN_CLASSNAME);
        }

        if (isOasPluginEnabled(oasProperties)) {
            LOG.info("{} plugin is enabled, registering OAS-related CDI beans", OAS_PLUGIN_CLASSNAME);
            builder.addBeanClass(QuarkusJsonApi4jOasServletContextListener.class.getName());
            builder.addBeanClass(QuarkusJsonApi4jOasProperties.class.getName());
            builder.addBeanClass(QuarkusJsonApi4jOasPluginBeans.class.getName());
        } else {
            LOG.info("{} plugin is disabled, skipping OAS-related CDI beans registration", OAS_PLUGIN_CLASSNAME);
        }

        if (isSfPluginEnabled(sfProperties)) {
            LOG.info("{} plugin is enabled, registering SF-related CDI beans", SF_PLUGIN_CLASSNAME);
            builder.addBeanClass(QuarkusJsonApi4jSfProperties.class.getName());
            builder.addBeanClass(QuarkusJsonApi4jSfPluginBeans.class.getName());
        } else {
            LOG.info("{} plugin is disabled, skipping SF-related CDI beans registration", SF_PLUGIN_CLASSNAME);
        }

        if (isCdPluginEnabled(cdProperties)) {
            LOG.info("Compound Docs plugin is enabled, registering CD-related CDI beans");
            builder.addBeanClass(QuarkusJsonApi4jCompoundDocsProperties.class.getName());
            builder.addBeanClass(QuarkusJsonApi4jCompoundDocsServletContextListener.class.getName());
            builder.addBeanClass(QuarkusJsonApi4jCompoundDocsPluginBeans.class.getName());
        } else {
            LOG.info("Compound Docs plugin is disabled, skipping CD-related CDI beans registration");
        }

        return builder.build();
    }

    @BuildStep
    ListenerBuildItem jsonApi4jDispatcherServletContextListener() {
        return new ListenerBuildItem(QuarkusJsonApi4jDispatcherServletContextListener.class.getName());
    }

    @BuildStep
    void oasServletContextListener(BuildProducer<ListenerBuildItem> listeners,
                                   QuarkusJsonApi4jOasProperties oasProperties) {
        if (isOasPluginEnabled(oasProperties)) {
            listeners.produce(new ListenerBuildItem(QuarkusJsonApi4jOasServletContextListener.class.getName()));
        }
    }

    @BuildStep
    void cdServletContextListener(BuildProducer<ListenerBuildItem> listeners,
                                   QuarkusJsonApi4jCompoundDocsProperties cdProperties) {
        if (isCdPluginEnabled(cdProperties)) {
            listeners.produce(new ListenerBuildItem(QuarkusJsonApi4jCompoundDocsServletContextListener.class.getName()));
        }
    }

    @BuildStep
    IgnoredServletContainerInitializerBuildItem ignoreJsonApi4jDispatcherServletContextInitializer() {
        return new IgnoredServletContainerInitializerBuildItem(
                JsonApi4jServletContainerInitializer.class.getName()
        );
    }

    @BuildStep
    void ignoreJsonApi4OasServletContextInitializer(
            BuildProducer<IgnoredServletContainerInitializerBuildItem> producer
    ) {
        if (isClassPresent(OAS_PLUGIN_CLASSNAME)) {
            producer.produce(
                    new IgnoredServletContainerInitializerBuildItem("pro.api4.jsonapi4j.plugin.oas.init.JsonApiOasServletContainerInitializer")
            );
        }
    }

    @BuildStep
    void ignoreJsonApi4CdServletContextInitializer(
            BuildProducer<IgnoredServletContainerInitializerBuildItem> producer
    ) {
        if (isClassPresent(CD_SERVLET_INITIALIZER_CLASSNAME)) {
            producer.produce(
                    new IgnoredServletContainerInitializerBuildItem(CD_SERVLET_INITIALIZER_CLASSNAME)
            );
        }
    }

    private static String toServletMapping(String rootPath) {
        if (rootPath == null) {
            return "/*";
        }
        String normalized = rootPath.trim();
        if (normalized.isEmpty() || "/".equals(normalized)) {
            return "/*";
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized.endsWith("/*") ? normalized : normalized + "/*";
    }

    private static boolean isClassPresent(String className) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean isAcPluginEnabled(QuarkusJsonApi4jAcProperties acProperties) {
        boolean acPluginClassesPresentInClasspath = isClassPresent(AC_PLUGIN_CLASSNAME);
        boolean enabled = acProperties.enabled();
        return enabled && acPluginClassesPresentInClasspath;
    }

    private static boolean isOasPluginEnabled(QuarkusJsonApi4jOasProperties oasProperties) {
        boolean oasPluginClassesPresentInClasspath = isClassPresent(OAS_PLUGIN_CLASSNAME);
        boolean enabled = oasProperties.enabled();
        return enabled && oasPluginClassesPresentInClasspath;
    }

    private static boolean isSfPluginEnabled(QuarkusJsonApi4jSfProperties sfProperties) {
        boolean sfPluginClassesPresentInClasspath = isClassPresent(SF_PLUGIN_CLASSNAME);
        boolean enabled = sfProperties.enabled();
        return enabled && sfPluginClassesPresentInClasspath;
    }

    private static boolean isCdPluginEnabled(QuarkusJsonApi4jCompoundDocsProperties cdProperties) {
        boolean cdPluginClassesPresentInClasspath = isClassPresent(CD_SERVLET_INITIALIZER_CLASSNAME);
        boolean enabled = cdProperties.enabled();
        return enabled && cdPluginClassesPresentInClasspath;
    }

}
