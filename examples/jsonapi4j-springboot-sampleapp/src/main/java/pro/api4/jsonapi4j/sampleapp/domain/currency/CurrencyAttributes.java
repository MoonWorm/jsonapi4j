package pro.api4.jsonapi4j.sampleapp.domain.currency;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Data
public class CurrencyAttributes {

    @Schema(description = "Currency's full name", example = "South Korean won", requiredMode = REQUIRED)
    private final String name;

    @Schema(description = "Currency's symbol", example = "€", requiredMode = REQUIRED)
    private final String symbol;
}
