package pro.api4.jsonapi4j.processor.relationship;

import lombok.Data;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.single.SingleDataItemSupplier;
import pro.api4.jsonapi4j.processor.single.relationship.ToOneRelationshipProcessor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToOneRelationshipProcessorTests {

    private static final ResourceType COUNTRIES = new ResourceType("countries");
    private static final String COUNTRY_ID = "NO";
    private static final String REQUEST_ID = "1";

    @Mock
    private SingleDataItemSupplier<String, CountryDto> ds;

    // --- Basic composition ---

    @Test
    void toOneRelationship_producesResourceIdentifierObject() {
        // given
        when(ds.get(REQUEST_ID)).thenReturn(new CountryDto(COUNTRY_ID, "Norway"));

        // when
        ToOneRelationshipDoc result = new ToOneRelationshipProcessor()
                .forRequest(REQUEST_ID)
                .dataSupplier(ds)
                .resourceTypeAndIdSupplier(dto -> new IdAndType(dto.getId(), COUNTRIES))
                .toToOneRelationshipDoc();

        // then
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getId()).isEqualTo(COUNTRY_ID);
        assertThat(result.getData().getType()).isEqualTo("countries");
        verify(ds, times(1)).get(REQUEST_ID);
    }

    // --- Null data ---

    @Test
    void dataSupplierReturnsNull_producesDocWithNullData() {
        // given
        when(ds.get(REQUEST_ID)).thenReturn(null);

        // when
        ToOneRelationshipDoc result = new ToOneRelationshipProcessor()
                .forRequest(REQUEST_ID)
                .dataSupplier(ds)
                .resourceTypeAndIdSupplier(dto -> new IdAndType(dto.getId(), COUNTRIES))
                .toToOneRelationshipDoc();

        // then
        assertThat(result.getData()).isNull();
    }

    // --- Top-level links and meta ---

    @Test
    void configureTopLevelLinksResolver_checkResult() {
        // given
        when(ds.get(REQUEST_ID)).thenReturn(new CountryDto(COUNTRY_ID, "Norway"));

        // when
        ToOneRelationshipDoc result = new ToOneRelationshipProcessor()
                .forRequest(REQUEST_ID)
                .dataSupplier(ds)
                .topLevelLinksResolver((req, dto) -> LinksObject.builder()
                        .self("/users/1/relationships/placeOfBirth").build())
                .resourceTypeAndIdSupplier(dto -> new IdAndType(dto.getId(), COUNTRIES))
                .toToOneRelationshipDoc();

        // then
        assertThat(result.getLinks()).isEqualTo(
                LinksObject.builder().self("/users/1/relationships/placeOfBirth").build());
    }

    @Test
    void configureTopLevelMetaResolver_checkResult() {
        // given
        when(ds.get(REQUEST_ID)).thenReturn(new CountryDto(COUNTRY_ID, "Norway"));

        // when
        ToOneRelationshipDoc result = new ToOneRelationshipProcessor()
                .forRequest(REQUEST_ID)
                .dataSupplier(ds)
                .topLevelMetaResolver((req, dto) -> Map.of("source", "cache"))
                .resourceTypeAndIdSupplier(dto -> new IdAndType(dto.getId(), COUNTRIES))
                .toToOneRelationshipDoc();

        // then
        assertThat(result.getMeta()).isEqualTo(Map.of("source", "cache"));
    }

    // --- Resource identifier meta ---

    @Test
    void configureResourceIdentifierMetaResolver_checkResult() {
        // given
        when(ds.get(REQUEST_ID)).thenReturn(new CountryDto(COUNTRY_ID, "Norway"));

        // when
        ToOneRelationshipDoc result = new ToOneRelationshipProcessor()
                .forRequest(REQUEST_ID)
                .dataSupplier(ds)
                .resourceIdentifierMetaResolver((req, dto) -> Map.of("region", "Europe"))
                .resourceTypeAndIdSupplier(dto -> new IdAndType(dto.getId(), COUNTRIES))
                .toToOneRelationshipDoc();

        // then
        assertThat(result.getData().getMeta()).isEqualTo(Map.of("region", "Europe"));
    }

    // --- Null data with links ---

    @Test
    void dataSupplierReturnsNull_linksResolverStillCalled() {
        // given
        when(ds.get(REQUEST_ID)).thenReturn(null);

        // when
        ToOneRelationshipDoc result = new ToOneRelationshipProcessor()
                .forRequest(REQUEST_ID)
                .dataSupplier(ds)
                .topLevelLinksResolver((req, dto) -> LinksObject.builder()
                        .self("/users/1/relationships/placeOfBirth").build())
                .resourceTypeAndIdSupplier(dto -> new IdAndType(dto.getId(), COUNTRIES))
                .toToOneRelationshipDoc();

        // then
        assertThat(result.getData()).isNull();
        assertThat(result.getLinks()).isEqualTo(
                LinksObject.builder().self("/users/1/relationships/placeOfBirth").build());
    }

    // --- Validation ---

    @Test
    void dataSupplierIsNull_checkThrowsNPE() {
        // when/then
        assertThatThrownBy(() -> {
            new ToOneRelationshipProcessor()
                    .forRequest(REQUEST_ID)
                    .dataSupplier((SingleDataItemSupplier<String, CountryDto>) null);
        }).isInstanceOf(NullPointerException.class);
    }

    // --- Inner types ---

    @Data
    private static class CountryDto {
        private final String id;
        private final String name;
    }

}
