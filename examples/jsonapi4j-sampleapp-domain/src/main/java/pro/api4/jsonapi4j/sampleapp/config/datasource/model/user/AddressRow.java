package pro.api4.jsonapi4j.sampleapp.config.datasource.model.user;

/**
 * An address as the data source holds it.
 *
 * <p>{@code doorCode} is set only for a residential address; the resource maps a row carrying one to a
 * {@code HomeAddress} and the rest to a plain {@code Address}, so the concrete attributes type follows the
 * data rather than being fixed by the field's declaration.
 */
public record AddressRow(String city, String zip, String doorCode) {

    public static AddressRow home(String city, String zip, String doorCode) {
        return new AddressRow(city, zip, doorCode);
    }

    public static AddressRow of(String city, String zip) {
        return new AddressRow(city, zip, null);
    }

}
