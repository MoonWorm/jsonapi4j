package pro.api4.jsonapi4j.principal.entitlement;

/**
 * Ready-made entitlement labels, offered purely so common cases need not invent their own strings.
 *
 * <p>Entitlements are unordered labels: holding one grants nothing beyond that entitlement, and these
 * carry no ranking among themselves. Any string a {@code PrincipalResolver} can produce works just as
 * well, and an application with its own vocabulary should use it.
 *
 * <p>A requirement is expressed by naming the entitlements it needs. There is no label meaning
 * "everyone" or "nobody": omit the requirement to demand nothing, and name an entitlement no principal
 * carries to deny everyone.
 */
public final class DefaultEntitlements {

    public static final String ADMIN = "ADMIN";
    public static final String ROOT_ADMIN = "ROOT_ADMIN";
    public static final String PARTNER = "PARTNER";

    private DefaultEntitlements() {

    }
}
