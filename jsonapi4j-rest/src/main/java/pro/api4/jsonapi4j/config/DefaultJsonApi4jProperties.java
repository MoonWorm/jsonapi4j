package pro.api4.jsonapi4j.config;

import lombok.Setter;

@Setter
public class DefaultJsonApi4jProperties implements JsonApi4jProperties {

    private String rootPath;
    private CompoundDocsProperties compoundDocs = new DefaultCompoundDocsProperties();

    @Override
    public String rootPath() {
        return rootPath;
    }

    @Override
    public CompoundDocsProperties compoundDocs() {
        return compoundDocs;
    }

}
