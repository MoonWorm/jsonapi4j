package pro.api4.jsonapi4j.sampleapp.domain.currency;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CurrencyAttributes {

    @Schema(description = "Currency's full name", example = "South Korean won")
    private final String name;

    @Schema(description = "Currency's symbol", example = "€")
    private final String symbol;
}
