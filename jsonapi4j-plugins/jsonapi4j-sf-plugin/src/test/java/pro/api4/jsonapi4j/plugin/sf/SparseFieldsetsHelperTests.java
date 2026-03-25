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

public class SparseFieldsetsHelperTests {

    private static final String RESOURCE_TYPE = "users";
    private static final String RESOURCE_ID = "123";

    private final DefaultSfProperties sfProperties = new DefaultSfProperties();
    private final SparseFieldsetsHelper helper = new SparseFieldsetsHelper(sfProperties);

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
    public void sparseFieldsets_requestedPathsDontExistAndModeIsSparseAll_attributesIsNull() {
        // given
        sfProperties.setRequestedFieldsDontExistMode(SPARSE_ALL_FIELDS);
        JsonApiRequest request = new JsonApiRequestBuilder()
                .fieldSets(Map.of(RESOURCE_TYPE, List.of("a.b.c.d", "non.existing")))
                .build();
        ResourceObject<Att, ?> resourceObject = new ResourceObject<>(RESOURCE_ID, RESOURCE_TYPE, new Att(), null);

        // when
        helper.sparseFieldsets(request, resourceObject);

        // then
        assertThat(resourceObject).isNotNull();
        assertThat(resourceObject.getAttributes()).isNull();
    }

    @Test
    public void sparseFieldsets_requestedPathsDontExistAndModeIsReturnAll_attributesUntouched() {
        // given
        sfProperties.setRequestedFieldsDontExistMode(RETURN_ALL_FIELDS);
        JsonApiRequest request = new JsonApiRequestBuilder()
                .fieldSets(Map.of(RESOURCE_TYPE, List.of("a.b.c.d", "non.existing")))
                .build();
        ResourceObject<Att, ?> resourceObject = new ResourceObject<>(RESOURCE_ID, RESOURCE_TYPE, new Att(), null);

        // when
        helper.sparseFieldsets(request, resourceObject);

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
    public void sparseFieldsets_requestedThirdLevelPaths_onlyRequestedPathKept() {
        // given
        sfProperties.setRequestedFieldsDontExistMode(RETURN_ALL_FIELDS);
        JsonApiRequest request = new JsonApiRequestBuilder()
                .fieldSets(Map.of(RESOURCE_TYPE, List.of("a.b.c", "non.existing")))
                .build();
        ResourceObject<Att, ?> resourceObject = new ResourceObject<>(RESOURCE_ID, RESOURCE_TYPE, new Att(), null);

        // when
        helper.sparseFieldsets(request, resourceObject);

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
    public void sparseFieldsets_emptyFieldsRequest_attributesIsNull() {
        // given
        sfProperties.setRequestedFieldsDontExistMode(RETURN_ALL_FIELDS);
        JsonApiRequest request = new JsonApiRequestBuilder()
                .fieldSets(Map.of(RESOURCE_TYPE, Collections.emptyList()))
                .build();
        ResourceObject<Att, ?> resourceObject = new ResourceObject<>(RESOURCE_ID, RESOURCE_TYPE, new Att(), null);

        // when
        helper.sparseFieldsets(request, resourceObject);

        // then
        assertThat(resourceObject).isNotNull();
        assertThat(resourceObject.getAttributes()).isNull();
    }

    // --- New tests ---

    @Test
    public void sparseFieldsets_topLevelPath_onlyTopLevelFieldKept() {
        // given
        JsonApiRequest request = new JsonApiRequestBuilder()
                .fieldSets(Map.of(RESOURCE_TYPE, List.of("s")))
                .build();
        ResourceObject<Att, ?> resourceObject = new ResourceObject<>(RESOURCE_ID, RESOURCE_TYPE, new Att(), null);

        // when
        helper.sparseFieldsets(request, resourceObject);

        // then
        assertThat(resourceObject.getAttributes()).isNotNull();
        assertThat(resourceObject.getAttributes().s).isEqualTo("s");
        assertThat(resourceObject.getAttributes().a).isNull();
    }

    @Test
    public void sparseFieldsets_firstLevelNestedPath_onlyFirstLevelBranchKept() {
        // given
        JsonApiRequest request = new JsonApiRequestBuilder()
                .fieldSets(Map.of(RESOURCE_TYPE, List.of("a")))
                .build();
        ResourceObject<Att, ?> resourceObject = new ResourceObject<>(RESOURCE_ID, RESOURCE_TYPE, new Att(), null);

        // when
        helper.sparseFieldsets(request, resourceObject);

        // then
        assertThat(resourceObject.getAttributes()).isNotNull();
        assertThat(resourceObject.getAttributes().s).isNull();
        assertThat(resourceObject.getAttributes().a).isNotNull();
        assertThat(resourceObject.getAttributes().a.b).isNull();
        assertThat(resourceObject.getAttributes().a.i).isNull();
        assertThat(resourceObject.getAttributes().a.iPrimitive).isEqualTo(3);
    }

    @Test
    public void sparseFieldsets_mixedLevelPaths_bothPathsKept() {
        // given
        JsonApiRequest request = new JsonApiRequestBuilder()
                .fieldSets(Map.of(RESOURCE_TYPE, List.of("s", "a.b.c")))
                .build();
        ResourceObject<Att, ?> resourceObject = new ResourceObject<>(RESOURCE_ID, RESOURCE_TYPE, new Att(), null);

        // when
        helper.sparseFieldsets(request, resourceObject);

        // then
        assertThat(resourceObject.getAttributes()).isNotNull();
        assertThat(resourceObject.getAttributes().s).isEqualTo("s");
        assertThat(resourceObject.getAttributes().a).isNotNull();
        assertThat(resourceObject.getAttributes().a.i).isNull();
        assertThat(resourceObject.getAttributes().a.b).isNotNull();
        assertThat(resourceObject.getAttributes().a.b.bool).isNull();
        assertThat(resourceObject.getAttributes().a.b.c).isNotNull();
    }

    @Test
    public void sparseFieldsets_primitiveFieldPreservation_primitivesRetainDefaults() {
        // given
        JsonApiRequest request = new JsonApiRequestBuilder()
                .fieldSets(Map.of(RESOURCE_TYPE, List.of("a.b.c")))
                .build();
        ResourceObject<Att, ?> resourceObject = new ResourceObject<>(RESOURCE_ID, RESOURCE_TYPE, new Att(), null);

        // when
        helper.sparseFieldsets(request, resourceObject);

        // then
        assertThat(resourceObject.getAttributes()).isNotNull();
        assertThat(resourceObject.getAttributes().a.iPrimitive).isEqualTo(3);
        assertThat(resourceObject.getAttributes().a.b.boolPrimitive).isTrue();
        assertThat(resourceObject.getAttributes().a.b.c.lPrimitive).isEqualTo(10L);
    }

    @Test
    public void sparseFieldsets_noFieldSetsForResourceType_noFiltering() {
        // given
        JsonApiRequest request = new JsonApiRequestBuilder()
                .fieldSets(Map.of("orders", List.of("status")))
                .build();
        ResourceObject<Att, ?> resourceObject = new ResourceObject<>(RESOURCE_ID, RESOURCE_TYPE, new Att(), null);

        // when
        helper.sparseFieldsets(request, resourceObject);

        // then
        assertThat(resourceObject.getAttributes()).isNotNull();
        assertThat(resourceObject.getAttributes().s).isEqualTo("s");
        assertThat(resourceObject.getAttributes().a).isNotNull();
        assertThat(resourceObject.getAttributes().a.i).isEqualTo(1);
        assertThat(resourceObject.getAttributes().a.b).isNotNull();
        assertThat(resourceObject.getAttributes().a.b.bool).isTrue();
        assertThat(resourceObject.getAttributes().a.b.c).isNotNull();
        assertThat(resourceObject.getAttributes().a.b.c.l).isEqualTo(2L);
    }

    @Test
    public void denormalizePath_singleSegment_returnsSingleElementList() {
        // given
        String path = "s";

        // when
        List<String> result = helper.denormalizePath(path);

        // then
        assertThat(result).containsExactly("s");
    }

    @Test
    public void denormalizePath_twoSegments_returnsTwoSubpaths() {
        // given
        String path = "a.b";

        // when
        List<String> result = helper.denormalizePath(path);

        // then
        assertThat(result).containsExactly("a", "a.b");
    }

    @Test
    public void denormalizePath_threeSegments_returnsThreeSubpaths() {
        // given
        String path = "a.b.c";

        // when
        List<String> result = helper.denormalizePath(path);

        // then
        assertThat(result).containsExactly("a", "a.b", "a.b.c");
    }

    @Test
    public void denormalizePath_fourSegments_returnsFourSubpaths() {
        // given
        String path = "a.b.c.d";

        // when
        List<String> result = helper.denormalizePath(path);

        // then
        assertThat(result).containsExactly("a", "a.b", "a.b.c", "a.b.c.d");
    }

}
