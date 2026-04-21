package pro.api4.jsonapi4j.processor.relationship;

import lombok.Data;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.multi.MultipleDataItemsSupplier;
import pro.api4.jsonapi4j.processor.multi.relationship.ToManyRelationshipsProcessor;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToManyRelationshipsProcessorTests {

    private static final ResourceType COUNTRIES = new ResourceType("countries");
    private static final String REQUEST_ID = "1";

    @Mock
    private MultipleDataItemsSupplier<String, CountryDto> ds;

    // --- Basic composition ---

    @Test
    void toManyRelationships_producesResourceIdentifierObjects() {
        // given
        when(ds.get(REQUEST_ID)).thenReturn(PaginationAwareResponse.fromItemsNotPageable(List.of(
                new CountryDto("NO", "Norway"),
                new CountryDto("FI", "Finland")
        )));

        // when
        ToManyRelationshipsDoc result = new ToManyRelationshipsProcessor()
                .forRequest(REQUEST_ID)
                .dataSupplier(ds)
                .resourceTypeAndIdSupplier(dto -> new IdAndType(dto.getId(), COUNTRIES))
                .toToManyRelationshipsDoc();

        // then
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData().get(0).getId()).isEqualTo("NO");
        assertThat(result.getData().get(0).getType()).isEqualTo("countries");
        assertThat(result.getData().get(1).getId()).isEqualTo("FI");
        assertThat(result.getData().get(1).getType()).isEqualTo("countries");
        verify(ds, times(1)).get(REQUEST_ID);
    }

    @Test
    void singleItem_producesOneResourceIdentifier() {
        // given
        when(ds.get(REQUEST_ID)).thenReturn(PaginationAwareResponse.fromItemsNotPageable(List.of(
                new CountryDto("US", "United States")
        )));

        // when
        ToManyRelationshipsDoc result = new ToManyRelationshipsProcessor()
                .forRequest(REQUEST_ID)
                .dataSupplier(ds)
                .resourceTypeAndIdSupplier(dto -> new IdAndType(dto.getId(), COUNTRIES))
                .toToManyRelationshipsDoc();

        // then
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getId()).isEqualTo("US");
    }

    // --- Null/empty data ---

    @Test
    void dataSupplierReturnsNull_producesDocWithEmptyData() {
        // given
        when(ds.get(REQUEST_ID)).thenReturn(null);

        // when
        ToManyRelationshipsDoc result = new ToManyRelationshipsProcessor()
                .forRequest(REQUEST_ID)
                .dataSupplier(ds)
                .resourceTypeAndIdSupplier(dto -> new IdAndType(dto.getId(), COUNTRIES))
                .toToManyRelationshipsDoc();

        // then
        assertThat(result.getData()).isEmpty();
    }

    @Test
    void dataSupplierReturnsEmptyList_producesDocWithEmptyData() {
        // given
        when(ds.get(REQUEST_ID)).thenReturn(PaginationAwareResponse.empty());

        // when
        ToManyRelationshipsDoc result = new ToManyRelationshipsProcessor()
                .forRequest(REQUEST_ID)
                .dataSupplier(ds)
                .resourceTypeAndIdSupplier(dto -> new IdAndType(dto.getId(), COUNTRIES))
                .toToManyRelationshipsDoc();

        // then
        assertThat(result.getData()).isEmpty();
    }

    // --- Top-level links and meta ---

    @Test
    void configureTopLevelLinksResolver_checkResult() {
        // given
        when(ds.get(REQUEST_ID)).thenReturn(PaginationAwareResponse.fromItemsNotPageable(List.of(
                new CountryDto("NO", "Norway")
        )));

        // when
        ToManyRelationshipsDoc result = new ToManyRelationshipsProcessor()
                .forRequest(REQUEST_ID)
                .dataSupplier(ds)
                .topLevelLinksResolver((req, dtos, ctx) -> LinksObject.builder()
                        .self("/users/1/relationships/citizenships").build())
                .resourceTypeAndIdSupplier(dto -> new IdAndType(dto.getId(), COUNTRIES))
                .toToManyRelationshipsDoc();

        // then
        assertThat(result.getLinks()).isEqualTo(
                LinksObject.builder().self("/users/1/relationships/citizenships").build());
    }

    @Test
    void configureTopLevelMetaResolver_checkResult() {
        // given
        when(ds.get(REQUEST_ID)).thenReturn(PaginationAwareResponse.fromItemsNotPageable(List.of(
                new CountryDto("NO", "Norway")
        )));

        // when
        ToManyRelationshipsDoc result = new ToManyRelationshipsProcessor()
                .forRequest(REQUEST_ID)
                .dataSupplier(ds)
                .topLevelMetaResolver((req, dtos) -> Map.of("total", 1))
                .resourceTypeAndIdSupplier(dto -> new IdAndType(dto.getId(), COUNTRIES))
                .toToManyRelationshipsDoc();

        // then
        assertThat(result.getMeta()).isEqualTo(Map.of("total", 1));
    }

    // --- Resource identifier meta ---

    @Test
    void configureResourceIdentifierMetaResolver_checkResult() {
        // given
        when(ds.get(REQUEST_ID)).thenReturn(PaginationAwareResponse.fromItemsNotPageable(List.of(
                new CountryDto("NO", "Norway"),
                new CountryDto("FI", "Finland")
        )));

        // when
        ToManyRelationshipsDoc result = new ToManyRelationshipsProcessor()
                .forRequest(REQUEST_ID)
                .dataSupplier(ds)
                .resourceIdentifierMetaResolver((req, dto) -> Map.of("name", dto.getName()))
                .resourceTypeAndIdSupplier(dto -> new IdAndType(dto.getId(), COUNTRIES))
                .toToManyRelationshipsDoc();

        // then
        assertThat(result.getData().get(0).getMeta()).isEqualTo(Map.of("name", "Norway"));
        assertThat(result.getData().get(1).getMeta()).isEqualTo(Map.of("name", "Finland"));
    }

    // --- Null data with links ---

    @Test
    void dataSupplierReturnsNull_linksResolverStillCalled() {
        // given
        when(ds.get(REQUEST_ID)).thenReturn(null);

        // when
        ToManyRelationshipsDoc result = new ToManyRelationshipsProcessor()
                .forRequest(REQUEST_ID)
                .dataSupplier(ds)
                .topLevelLinksResolver((req, dtos, ctx) -> LinksObject.builder()
                        .self("/users/1/relationships/citizenships").build())
                .resourceTypeAndIdSupplier(dto -> new IdAndType(dto.getId(), COUNTRIES))
                .toToManyRelationshipsDoc();

        // then
        assertThat(result.getData()).isEmpty();
        assertThat(result.getLinks()).isEqualTo(
                LinksObject.builder().self("/users/1/relationships/citizenships").build());
    }

    // --- Validation ---

    @Test
    void dataSupplierIsNull_checkThrowsNPE() {
        // when/then
        assertThatThrownBy(() -> {
            new ToManyRelationshipsProcessor()
                    .forRequest(REQUEST_ID)
                    .dataSupplier((MultipleDataItemsSupplier<String, CountryDto>) null);
        }).isInstanceOf(NullPointerException.class);
    }

    // --- Inner types ---

    @Data
    private static class CountryDto {
        private final String id;
        private final String name;
    }

}
