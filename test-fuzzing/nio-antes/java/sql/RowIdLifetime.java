package java.sql;

/**
 * KajiLibrary's java.sql.RowIdLifetime -- how long a {@link RowId} is valid in this database.
 *
 * <p>It is what makes `RowId` usable: the interface promises nothing about its duration, and
 * without being able to ask there would be no way to know whether storing one is reasonable. The
 * answer ranges from "I do not support them" to "they are valid forever", and almost every database
 * is in between.
 */
public enum RowIdLifetime {

    /** This database has no row identifiers. */
    ROWID_UNSUPPORTED,

    /** They are valid, but for none of the other durations. */
    ROWID_VALID_OTHER,

    /** Valid for the duration of the session. */
    ROWID_VALID_SESSION,

    /** Valid for the duration of the transaction. */
    ROWID_VALID_TRANSACTION,

    /** Valid forever. */
    ROWID_VALID_FOREVER
}
