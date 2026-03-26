package pro.api4.jsonapi4j.compound.docs;

import java.net.URI;

@FunctionalInterface
public interface DomainUrlResolver {

    URI getDomainUrl(String resourceType);

}
