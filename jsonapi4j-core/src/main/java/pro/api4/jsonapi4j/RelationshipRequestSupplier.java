package pro.api4.jsonapi4j;

@FunctionalInterface
interface RelationshipRequestSupplier<REQUEST, DATA_SOURCE_DTO> {

    REQUEST create(REQUEST originalRequest,
                   DATA_SOURCE_DTO dataSourceDto);

}
