package pro.api4.jsonapi4j.sampleapp.domain;

import pro.api4.jsonapi4j.domain.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SampleAppDomainResourceTypes implements ResourceType {

    COUNTRIES("countries"),
    CURRENCIES("currencies"),
    USERS("users");

    private final String type;

    public static SampleAppDomainResourceTypes fromName(String name) {
        return Arrays.stream(SampleAppDomainResourceTypes.values()).filter(t -> t.getType().equals(name)).findFirst().orElse(null);
    }
}
