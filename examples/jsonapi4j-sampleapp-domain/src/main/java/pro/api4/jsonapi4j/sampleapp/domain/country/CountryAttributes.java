package pro.api4.jsonapi4j.sampleapp.domain.country;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CountryAttributes {

    @Schema(description = "Country's official name", example = "Belgium")
    private final String name;

    @Schema(description = "Country's region", example = "Europe")
    private final String region;

}
