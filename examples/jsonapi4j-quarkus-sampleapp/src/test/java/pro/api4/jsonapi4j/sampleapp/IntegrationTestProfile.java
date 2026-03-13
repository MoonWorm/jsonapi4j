package pro.api4.jsonapi4j.sampleapp;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class IntegrationTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.config.locations", "application-integration-test.properties");
    }
}
