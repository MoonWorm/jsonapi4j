package pro.api4.jsonapi4j.sampleapp.config.datasource.model.country;

/**
 * Lightweight relationship reference for a country. A relationship only ever emits a resource
 * identifier ({@code {type, id}}), so it needs nothing more than the country id. The full
 * {@link DownstreamCountry} is materialized separately by {@code CountryResource} (and, on
 * {@code ?include=}, by the Compound-Docs plugin).
 */
public record CountryRef(String id) {
}
