package pro.api4.jsonapi4j.plugin.sf;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.MultipleResourcesVisitors;
import pro.api4.jsonapi4j.plugin.SingleResourceVisitors;
import pro.api4.jsonapi4j.plugin.ToManyRelationshipVisitors;
import pro.api4.jsonapi4j.plugin.ToOneRelationshipVisitors;
import pro.api4.jsonapi4j.plugin.sf.config.DefaultSfProperties;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonApiSparseFieldsetsPluginTests {

    @Test
    public void pluginName_returnsClassName() {
        // given
        JsonApiSparseFieldsetsPlugin plugin = new JsonApiSparseFieldsetsPlugin(new DefaultSfProperties());

        // when
        String name = plugin.pluginName();

        // then
        assertThat(name).isEqualTo("JsonApiSparseFieldsetsPlugin");
    }

    @Test
    public void enabled_delegatesToSfProperties() {
        // given
        DefaultSfProperties sfProperties = new DefaultSfProperties();
        sfProperties.setEnabled(true);
        JsonApiSparseFieldsetsPlugin plugin = new JsonApiSparseFieldsetsPlugin(sfProperties);

        // when/then
        assertThat(plugin.enabled()).isTrue();
    }

    @Test
    public void enabled_disabledProperties_returnsFalse() {
        // given
        DefaultSfProperties sfProperties = new DefaultSfProperties();
        sfProperties.setEnabled(false);
        JsonApiSparseFieldsetsPlugin plugin = new JsonApiSparseFieldsetsPlugin(sfProperties);

        // when/then
        assertThat(plugin.enabled()).isFalse();
    }

    @Test
    public void precedence_returnsHighPrecedence() {
        // given
        JsonApiSparseFieldsetsPlugin plugin = new JsonApiSparseFieldsetsPlugin(new DefaultSfProperties());

        // when
        int precedence = plugin.precedence();

        // then
        assertThat(precedence).isEqualTo(JsonApi4jPlugin.HIGH_PRECEDENCE);
    }

    @Test
    public void singleResourceVisitors_returnsNonNull() {
        // given
        JsonApiSparseFieldsetsPlugin plugin = new JsonApiSparseFieldsetsPlugin(new DefaultSfProperties());

        // when
        SingleResourceVisitors visitors = plugin.singleResourceVisitors();

        // then
        assertThat(visitors).isNotNull().isInstanceOf(SparseFieldsetsSingleResourceVisitors.class);
    }

    @Test
    public void multipleResourcesVisitors_returnsNonNull() {
        // given
        JsonApiSparseFieldsetsPlugin plugin = new JsonApiSparseFieldsetsPlugin(new DefaultSfProperties());

        // when
        MultipleResourcesVisitors visitors = plugin.multipleResourcesVisitors();

        // then
        assertThat(visitors).isNotNull().isInstanceOf(SparseFieldsetsMultipleResourcesVisitors.class);
    }

    @Test
    public void toOneRelationshipVisitors_returnsNull() {
        // given
        JsonApiSparseFieldsetsPlugin plugin = new JsonApiSparseFieldsetsPlugin(new DefaultSfProperties());

        // when
        ToOneRelationshipVisitors visitors = plugin.toOneRelationshipVisitors();

        // then
        assertThat(visitors).isNotNull().isInstanceOf(ToOneRelationshipVisitors.class);
    }

    @Test
    public void toManyRelationshipVisitors_returnsNull() {
        // given
        JsonApiSparseFieldsetsPlugin plugin = new JsonApiSparseFieldsetsPlugin(new DefaultSfProperties());

        // when
        ToManyRelationshipVisitors visitors = plugin.toManyRelationshipVisitors();

        // then
        assertThat(visitors).isNotNull().isInstanceOf(ToManyRelationshipVisitors.class);
    }

}
