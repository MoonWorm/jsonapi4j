package pro.api4.jsonapi4j.plugin.sf;

import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.MultipleResourcesVisitors;
import pro.api4.jsonapi4j.plugin.SingleResourceVisitors;
import pro.api4.jsonapi4j.plugin.sf.config.SfProperties;

@Slf4j
public class JsonApiSparseFieldsetsPlugin implements JsonApi4jPlugin {

    public static final String NAME = JsonApiSparseFieldsetsPlugin.class.getSimpleName();

    private final SfProperties sfProperties;

    public JsonApiSparseFieldsetsPlugin(SfProperties sfProperties) {
        this.sfProperties = sfProperties;
    }

    @Override
    public String pluginName() {
        return NAME;
    }

    @Override
    public int precedence() {
        return JsonApi4jPlugin.HIGH_PRECEDENCE;
    }

    @Override
    public SingleResourceVisitors singleResourceVisitors() {
        return new SparseFieldsetsSingleResourceVisitors(sfProperties);
    }

    @Override
    public MultipleResourcesVisitors multipleResourcesVisitors() {
        return new SparseFieldsetsMultipleResourcesVisitors(sfProperties);
    }

}