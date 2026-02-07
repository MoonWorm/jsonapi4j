package pro.api4.jsonapi4j.plugin.oas.domain.model;

import lombok.*;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasRelationshipInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode
public class OasRelationshipInfoModel {

    @Builder.Default
    private Class<?> resourceLinkageMetaType = NoLinkageMeta.class;
    @Builder.Default
    private List<Class<? extends Resource<?>>> relationshipTypes = Collections.emptyList();

    public static OasRelationshipInfoModel fromAnnotation(OasRelationshipInfo oasRelationshipInfo) {
        if (oasRelationshipInfo == null) {
            return null;
        }
        return OasRelationshipInfoModel.builder()
                .resourceLinkageMetaType(oasRelationshipInfo.resourceLinkageMetaType())
                .relationshipTypes(Arrays.asList(oasRelationshipInfo.relationshipTypes()))
                .build();
    }
}
