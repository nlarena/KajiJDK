package java.sql;

/**
 * KajiLibrary's java.sql.ClientInfoStatus -- why a client property could not be set.
 *
 * <p>It exists because `setClientInfo` can fail on **some** properties and not on others: it is not
 * a success or a failure but a map from property to reason, and this enum is that map's codomain.
 */
public enum ClientInfoStatus {

    /** It is not known why. */
    REASON_UNKNOWN,

    /** The server does not know that property. */
    REASON_UNKNOWN_PROPERTY,

    /** The value is not valid for that property. */
    REASON_VALUE_INVALID,

    /** The value was longer than the property allows. */
    REASON_VALUE_TRUNCATED
}
