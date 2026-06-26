package pro.api4.jsonapi4j.sampleapp.config.datasource.model.country;

/**
 * Lightweight relationship reference for a currency. Carries only the currency code (its id) — the
 * linkage emitted by a relationship needs nothing else. The full currency resource is materialized
 * separately by {@code CurrencyResource} on {@code ?include=currencies}.
 */
public record CurrencyRef(String code) {
}
