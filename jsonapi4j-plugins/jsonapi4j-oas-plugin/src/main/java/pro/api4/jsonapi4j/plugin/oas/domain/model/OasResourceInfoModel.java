package pro.api4.jsonapi4j.plugin.oas.domain.model;

import lombok.*;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasResourceInfo;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode
public class OasResourceInfoModel {

    @Builder.Default
    private String resourceNameSingle = "";
    @Builder.Default
    private String resourceNamePlural = "";
    @Builder.Default
    private Class<?> attributes = NoAttributes.class;

    public static OasResourceInfoModel fromAnnotation(OasResourceInfo oasResourceInfo) {
        if (oasResourceInfo == null) {
            return null;
        }
        return OasResourceInfoModel.builder()
                .resourceNameSingle(oasResourceInfo.resourceNameSingle())
                .resourceNamePlural(oasResourceInfo.resourceNamePlural())
                .attributes(oasResourceInfo.attributes())
                .build();
    }

}
