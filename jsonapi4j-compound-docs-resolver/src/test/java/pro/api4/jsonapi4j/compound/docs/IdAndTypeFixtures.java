package pro.api4.jsonapi4j.compound.docs;

import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.processor.IdAndType;

public final class IdAndTypeFixtures {

    private IdAndTypeFixtures() {
    }

    public static IdAndType idAndType(String type, String id) {
        return new IdAndType(id, new ResourceType(type));
    }

}
