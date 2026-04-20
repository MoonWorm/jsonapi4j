---
title: "Writing a Custom Plugin"
permalink: /writing-a-custom-plugin/
---

This guide walks through building a custom JsonApi4j plugin from scratch. By the end, you'll have a working **Field Masking Plugin** that redacts sensitive attributes (emails, phone numbers) in API responses based on a custom annotation.

The finished plugin will:
1. Define a `@Masked` annotation to mark sensitive fields
2. Extract annotation metadata at registration time
3. Hook into the response pipeline to mask values before they reach the client

## Step 1: Define the Annotation

Create a custom annotation that marks which attribute fields should be masked:

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Masked {

    /**
     * Number of characters to keep visible at the start.
     * For example, 3 would turn "john@example.com" into "joh***".
     */
    int keepFirst() default 0;

    /**
     * Number of characters to keep visible at the end.
     * For example, 4 would turn "john@example.com" into "***e.com".
     * Combine with keepFirst: "joh***com".
     */
    int keepLast() default 0;
}
```

Apply it to your attributes class:

```java
@Data
public class UserAttributes {

    private String firstName;
    private String lastName;

    @Masked(keepFirst = 3, keepLast = 0)
    private String email;

    @Masked(keepFirst = 0, keepLast = 4)
    private String phone;
}
```

With this configuration, `john@example.com` becomes `joh***` and `+1-555-123-4567` becomes `***4567`.

## Step 2: Create the Plugin Info Model

The plugin needs to store which fields are masked and how. Create a simple model that holds this metadata per resource:

```java
@Data
public class MaskedFieldInfo {

    private final String fieldName;
    private final int keepFirst;
    private final int keepLast;
}
```

## Step 3: Implement `JsonApi4jPlugin`

The plugin class is the entry point. It tells the framework:
- What metadata to extract at registration time (`extractPluginInfoFromResource`)
- Which pipeline stages to hook into (visitor methods)

```java
public class FieldMaskingPlugin implements JsonApi4jPlugin {

    @Override
    public String pluginName() {
        return "field-masking";
    }

    @Override
    public int precedence() {
        // Run after most plugins but before response serialization
        return LOW_PRECEDENCE;
    }

    @Override
    public Object extractPluginInfoFromResource(Resource<?> resource) {
        // Find the attributes class by inspecting the resolveAttributes method
        Method method = ReflectionUtils.findMethod(
            resource.getClass(),
            Resource.RESOLVE_ATTRIBUTES_METHOD_NAME
        );
        if (method == null) {
            return null;
        }
        Class<?> attributesClass = method.getReturnType();
        List<MaskedFieldInfo> maskedFields = new ArrayList<>();
        for (Field field : attributesClass.getDeclaredFields()) {
            Masked masked = field.getAnnotation(Masked.class);
            if (masked != null) {
                maskedFields.add(new MaskedFieldInfo(
                    field.getName(), masked.keepFirst(), masked.keepLast()
                ));
            }
        }
        return maskedFields.isEmpty() ? null : maskedFields;
    }

    @Override
    public SingleResourceVisitors singleResourceVisitors() {
        return new FieldMaskingSingleResourceVisitors();
    }

    @Override
    public MultipleResourcesVisitors multipleResourcesVisitors() {
        return new FieldMaskingMultipleResourcesVisitors();
    }
}
```

**Key points:**
- `extractPluginInfoFromResource()` runs once at startup for each registered resource. It scans the attributes class for `@Masked` fields and returns a list of `MaskedFieldInfo`. This metadata is later available in visitors via `pluginInfo.getResourcePluginInfo()`.
- `singleResourceVisitors()` and `multipleResourcesVisitors()` return visitor implementations that handle the actual masking.

## Step 4: Implement the Visitors

Visitors hook into the [request processing pipeline](/request-processing-pipeline/). For masking, we use `RelationshipsPostRetrievalPhase` — the last stage before the response is serialized. At this point, the full JSON:API document is assembled and we can modify attribute values.

### Single Resource Visitor

Handles `GET /users/{id}` responses:

```java
class FieldMaskingSingleResourceVisitors implements SingleResourceVisitors {

    @Override
    public <REQUEST, DATA_SOURCE_DTO, DOC extends SingleResourceDoc<?>>
    RelationshipsPostRetrievalPhase<?> onRelationshipsPostRetrieval(
            REQUEST request,
            OperationMeta operationMeta,
            DATA_SOURCE_DTO dataSourceDto,
            DOC doc,
            SingleResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ?> context,
            JsonApiPluginInfo pluginInfo) {

        if (doc != null && doc.getData() != null) {
            maskAttributes(doc.getData().getAttributes(), pluginInfo);
        }
        return RelationshipsPostRetrievalPhase.doNothing();
    }

    @SuppressWarnings("unchecked")
    static void maskAttributes(Object attributes, JsonApiPluginInfo pluginInfo) {
        List<MaskedFieldInfo> maskedFields =
                (List<MaskedFieldInfo>) pluginInfo.getResourcePluginInfo();
        if (maskedFields == null || attributes == null) {
            return;
        }
        for (MaskedFieldInfo info : maskedFields) {
            if (!ReflectionUtils.fieldPathExists(attributes, info.getFieldName())) {
                continue;
            }
            Object value = ReflectionUtils.getFieldValueThrowing(attributes, info.getFieldName());
            if (value instanceof String str) {
                ReflectionUtils.setFieldPathValueSilent(attributes, info.getFieldName(),
                    mask(str, info.getKeepFirst(), info.getKeepLast()));
            }
        }
    }

    private static String mask(String value, int keepFirst, int keepLast) {
        if (value.length() <= keepFirst + keepLast) {
            return value;
        }
        String prefix = value.substring(0, keepFirst);
        String suffix = keepLast > 0 ? value.substring(value.length() - keepLast) : "";
        return prefix + "***" + suffix;
    }
}
```

### Multiple Resources Visitor

Handles `GET /users` responses — applies the same masking to every resource in the list:

```java
class FieldMaskingMultipleResourcesVisitors implements MultipleResourcesVisitors {

    @Override
    public <REQUEST, DATA_SOURCE_DTO, DOC extends MultipleResourcesDoc<?>>
    RelationshipsPostRetrievalPhase<?> onRelationshipsPostRetrieval(
            REQUEST request,
            OperationMeta operationMeta,
            PaginationAwareResponse<DATA_SOURCE_DTO> paginationAwareResponse,
            DOC doc,
            MultipleResourcesJsonApiContext<REQUEST, DATA_SOURCE_DTO, ?> context,
            JsonApiPluginInfo pluginInfo) {

        if (doc != null && doc.getData() != null) {
            doc.getData().forEach(resourceObject ->
                FieldMaskingSingleResourceVisitors.maskAttributes(
                    resourceObject.getAttributes(), pluginInfo
                )
            );
        }
        return RelationshipsPostRetrievalPhase.doNothing();
    }
}
```

## Step 5: Register the Plugin

<div class="tabs" markdown="0">
  <div class="tab-buttons">
    <button class="tab-btn active" data-tab="plug-springboot">Spring Boot</button>
    <button class="tab-btn" data-tab="plug-quarkus">Quarkus</button>
    <button class="tab-btn" data-tab="plug-servlet">Servlet API</button>
  </div>
  <div id="plug-springboot" class="tab-panel active">
    <p>Register the plugin as a Spring bean. The framework auto-discovers all <code>JsonApi4jPlugin</code> beans.</p>
    <div class="language-java highlighter-rouge"><div class="highlight"><pre class="highlight"><code><span class="nd">@Configuration</span>
<span class="kd">public</span> <span class="kd">class</span> <span class="nc">PluginConfig</span> <span class="o">{</span>

    <span class="nd">@Bean</span>
    <span class="kd">public</span> <span class="nc">JsonApi4jPlugin</span> <span class="nf">fieldMaskingPlugin</span><span class="o">()</span> <span class="o">{</span>
        <span class="k">return</span> <span class="k">new</span> <span class="nc">FieldMaskingPlugin</span><span class="o">();</span>
    <span class="o">}</span>
<span class="o">}</span></code></pre></div></div>
  </div>
  <div id="plug-quarkus" class="tab-panel">
    <p>Provide the plugin as a CDI bean.</p>
    <div class="language-java highlighter-rouge"><div class="highlight"><pre class="highlight"><code><span class="kd">public</span> <span class="kd">class</span> <span class="nc">PluginConfig</span> <span class="o">{</span>

    <span class="nd">@Produces</span>
    <span class="nd">@Singleton</span>
    <span class="kd">public</span> <span class="nc">JsonApi4jPlugin</span> <span class="nf">fieldMaskingPlugin</span><span class="o">()</span> <span class="o">{</span>
        <span class="k">return</span> <span class="k">new</span> <span class="nc">FieldMaskingPlugin</span><span class="o">();</span>
    <span class="o">}</span>
<span class="o">}</span></code></pre></div></div>
  </div>
  <div id="plug-servlet" class="tab-panel">
    <p>Pass the plugin to the <code>JsonApi4j</code> builder.</p>
    <div class="language-java highlighter-rouge"><div class="highlight"><pre class="highlight"><code><span class="nc">JsonApi4j</span> <span class="n">jsonApi4j</span> <span class="o">=</span> <span class="nc">JsonApi4j</span><span class="o">.</span><span class="na">builder</span><span class="o">()</span>
    <span class="o">.</span><span class="na">domainRegistry</span><span class="o">(</span><span class="n">domainRegistry</span><span class="o">)</span>
    <span class="o">.</span><span class="na">operationsRegistry</span><span class="o">(</span><span class="n">operationsRegistry</span><span class="o">)</span>
    <span class="o">.</span><span class="na">plugins</span><span class="o">(</span><span class="nc">List</span><span class="o">.</span><span class="na">of</span><span class="o">(</span><span class="k">new</span> <span class="nc">FieldMaskingPlugin</span><span class="o">()))</span>
    <span class="o">.</span><span class="na">build</span><span class="o">();</span></code></pre></div></div>
  </div>
</div>

## Result

With the plugin registered, API responses automatically mask annotated fields:

```json
{
  "data": {
    "type": "users",
    "id": "1",
    "attributes": {
      "firstName": "John",
      "lastName": "Doe",
      "email": "joh***",
      "phone": "***4567"
    }
  }
}
```

No changes to your operations, resources, or domain model — just the annotation and the plugin.

## Recap

| Step | What You Implement | Purpose |
|------|-------------------|---------|
| Annotation | `@Masked` | Declarative configuration on attribute fields |
| Plugin info model | `MaskedFieldInfo` | Carries annotation metadata through the pipeline |
| Plugin class | `FieldMaskingPlugin` | Entry point — extracts metadata and provides visitors |
| Visitors | `SingleResourceVisitors`, `MultipleResourcesVisitors` | Hooks into the pipeline to transform the response |
| Registration | Spring `@Bean` / Quarkus `@Produces` / Builder | Makes the framework aware of your plugin |

For more on how the pipeline stages work, see [Plugin System](/plugins/) and [Request Processing Pipeline](/request-processing-pipeline/).
