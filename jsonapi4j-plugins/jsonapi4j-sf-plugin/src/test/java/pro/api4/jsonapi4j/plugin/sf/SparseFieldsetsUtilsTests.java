package pro.api4.jsonapi4j.plugin.sf;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.plugin.sf.config.DefaultSfProperties;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.request.JsonApiRequestBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.api4.jsonapi4j.plugin.sf.config.SfProperties.RequestedFieldsDontExistMode.RETURN_ALL_FIELDS;
import static pro.api4.jsonapi4j.plugin.sf.config.SfProperties.RequestedFieldsDontExistMode.SPARSE_ALL_FIELDS;

public class SparseFieldsetsUtilsTests {

    private static class Att {

        A a = new A();
        String s = "s";

        private static class A {

            B b = new B();
            Integer i = 1;
            int iPrimitive = 3;

            private static class B {

                C c = new C();
                Boolean bool = true;
                boolean boolPrimitive = true;

                private static class C {
                    Long l = 2L;
                    long lPrimitive = 10L;
                }
            }
        }
    }

    @Test
    public void sparseFieldsets_requestedPathsDontExistAndModeIsSparseAll_checkAttributesIsNull() {
        // given
        DefaultSfProperties sfProperties = new DefaultSfProperties();
        sfProperties.setRequestedFieldsDontExistMode(SPARSE_ALL_FIELDS);
        JsonApiRequest request = new JsonApiRequestBuilder()
                .fieldSets(
                        Map.of(
                                "users", List.of("a.b.c.d", "non.existing")
                        )
                ).build();
        ResourceObject<Att, ?> resourceObject = new ResourceObject<>("123", "users", new Att(), null);

        // when
        SparseFieldsetsUtils.sparseFieldsets(request, resourceObject, sfProperties);

        // then
        assertThat(resourceObject).isNotNull();
        assertThat(resourceObject.getAttributes()).isNull();
    }

    @Test
    public void sparseFieldsets_requestedPathsDontExistAndModeIsReturnAll_checkAttributesIsUntouched() {
        // given
        DefaultSfProperties sfProperties = new DefaultSfProperties();
        sfProperties.setRequestedFieldsDontExistMode(RETURN_ALL_FIELDS);
        JsonApiRequest request = new JsonApiRequestBuilder()
                .fieldSets(
                        Map.of(
                                "users", List.of("a.b.c.d", "non.existing")
                        )
                ).build();
        ResourceObject<Att, ?> resourceObject = new ResourceObject<>("123", "users", new Att(), null);

        // when
        SparseFieldsetsUtils.sparseFieldsets(request, resourceObject, sfProperties);

        // then
        assertThat(resourceObject).isNotNull();
        assertThat(resourceObject.getAttributes()).isNotNull();
        assertThat(resourceObject.getAttributes().s).isNotNull().isEqualTo("s");
        assertThat(resourceObject.getAttributes().a).isNotNull();
        assertThat(resourceObject.getAttributes().a.iPrimitive).isEqualTo(3);
        assertThat(resourceObject.getAttributes().a.i).isNotNull().isEqualTo(1);
        assertThat(resourceObject.getAttributes().a.b).isNotNull();
        assertThat(resourceObject.getAttributes().a.b.bool).isNotNull().isTrue();
        assertThat(resourceObject.getAttributes().a.b.boolPrimitive).isTrue();
        assertThat(resourceObject.getAttributes().a.b.c).isNotNull();
        assertThat(resourceObject.getAttributes().a.b.c.l).isNotNull().isEqualTo(2L);
        assertThat(resourceObject.getAttributes().a.b.c.lPrimitive).isEqualTo(10L);
    }

    @Test
    public void sparseFieldsets_requestedThirdLevelPaths_checkAttributes() {
        // given
        DefaultSfProperties sfProperties = new DefaultSfProperties();
        sfProperties.setRequestedFieldsDontExistMode(RETURN_ALL_FIELDS);
        JsonApiRequest request = new JsonApiRequestBuilder()
                .fieldSets(
                        Map.of(
                                "users", List.of("a.b.c", "non.existing")
                        )
                ).build();
        ResourceObject<Att, ?> resourceObject = new ResourceObject<>("123", "users", new Att(), null);

        // when
        SparseFieldsetsUtils.sparseFieldsets(request, resourceObject, sfProperties);

        // then
        assertThat(resourceObject).isNotNull();
        assertThat(resourceObject.getAttributes()).isNotNull();
        assertThat(resourceObject.getAttributes().s).isNull();
        assertThat(resourceObject.getAttributes().a).isNotNull();
        assertThat(resourceObject.getAttributes().a.iPrimitive).isEqualTo(3);
        assertThat(resourceObject.getAttributes().a.i).isNull();
        assertThat(resourceObject.getAttributes().a.b).isNotNull();
        assertThat(resourceObject.getAttributes().a.b.bool).isNull();
        assertThat(resourceObject.getAttributes().a.b.boolPrimitive).isTrue();
        assertThat(resourceObject.getAttributes().a.b.c).isNotNull();
        assertThat(resourceObject.getAttributes().a.b.c.l).isNull();
        assertThat(resourceObject.getAttributes().a.b.c.lPrimitive).isEqualTo(10L);
    }

    @Test
    public void sparseFieldsets_emptyFieldsRequest_checkAttributesIsNull() {
        // given
        DefaultSfProperties sfProperties = new DefaultSfProperties();
        sfProperties.setRequestedFieldsDontExistMode(RETURN_ALL_FIELDS);
        JsonApiRequest request = new JsonApiRequestBuilder()
                .fieldSets(
                        Map.of(
                                "users", Collections.emptyList()
                        )
                ).build();
        ResourceObject<Att, ?> resourceObject = new ResourceObject<>("123", "users", new Att(), null);

        // when
        SparseFieldsetsUtils.sparseFieldsets(request, resourceObject, sfProperties);

        // then
        assertThat(resourceObject).isNotNull();
        assertThat(resourceObject.getAttributes()).isNull();
    }

}
