package pro.api4.jsonapi4j.processor.resolvers;

import pro.api4.jsonapi4j.model.document.LinksObject;

@FunctionalInterface
public interface ResourceLinksResolver<REQUEST, DATA_SOURCE_DTO> {

    LinksObject resolve(REQUEST request,
                        DATA_SOURCE_DTO dataSourceDto);

}
