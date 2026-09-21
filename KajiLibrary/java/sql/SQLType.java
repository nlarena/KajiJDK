package java.sql;

/**
 * KajiLibrary's java.sql.SQLType -- an SQL data type, with a name.
 *
 * <p>It is an interface and not an enum on purpose: the standard types are an enum (`JDBCType`),
 * but a vendor can have its own, and without this interface there would be no way to name them in a
 * signature.
 */
public interface SQLType {

    /** The name of the type, as the vendor calls it. */
    String getName();

    /** The vendor that defines it; for the standard ones, `"java.sql"`. */
    String getVendor();

    /** The number with which that vendor identifies it. */
    Integer getVendorTypeNumber();
}
