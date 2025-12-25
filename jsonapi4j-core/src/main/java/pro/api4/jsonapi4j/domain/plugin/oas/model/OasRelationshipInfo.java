package pro.api4.jsonapi4j.domain.plugin.oas.model;

public @interface OasRelationshipInfo {

    Class<?> resourceLinkageMetaType() default NoLinkageMeta.class;

    String[] relationshipTypes() default {};

    class NoLinkageMeta {

    }

}
