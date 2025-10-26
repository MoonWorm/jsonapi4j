package pro.api4.jsonapi4j.sampleapp.domain.country;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Data
public class CountryAttributes {

    @Schema(description = "Country's official name", example = "Belgium", requiredMode = REQUIRED)
    private final String name;

    @Schema(description = "Country's region", example = "Europe", requiredMode = REQUIRED)
    private final String region;

}
