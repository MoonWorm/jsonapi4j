package pro.api4.jsonapi4j.meta;

/**
 * Lightweight resource-identifier ({@code type} + {@code id}) used for {@code state}'s relationship linkages.
 */
public record Ref(String type, String id) {
}
