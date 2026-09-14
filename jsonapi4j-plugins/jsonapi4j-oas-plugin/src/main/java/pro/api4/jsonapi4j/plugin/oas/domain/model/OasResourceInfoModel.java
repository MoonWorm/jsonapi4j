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
    private Class<?> attributes = NoAttributes.class;
    @Builder.Default
    private String resourceIdDescription = "";
    @Builder.Default
    private String resourceIdExample = "";
    @Builder.Default
    private boolean deprecated = false;

    public static OasResourceInfoModel fromAnnotation(OasResourceInfo oasResourceInfo) {
        if (oasResourceInfo == null) {
            return null;
        }
        return OasResourceInfoModel.builder()
                .resourceNameSingle(oasResourceInfo.resourceNameSingle())
                .attributes(oasResourceInfo.attributes())
                .resourceIdDescription(oasResourceInfo.resourceIdDescription())
                .resourceIdExample(oasResourceInfo.resourceIdExample())
                .deprecated(oasResourceInfo.deprecated())
                .build();
    }

}
