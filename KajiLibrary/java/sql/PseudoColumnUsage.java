package java.sql;

/**
 * KajiLibrary's java.sql.PseudoColumnUsage -- where a pseudocolumn can be used.
 *
 * <p>A pseudocolumn is one the database offers without it being in the table --Oracle's `ROWID` is
 * the example-- and not all can be used everywhere: some only come out in the `select`, others only
 * serve to filter. Without this, a tool that builds queries would have to try.
 */
public enum PseudoColumnUsage {

    /** Only in the `select` list. */
    SELECT_LIST_ONLY,

    /** Only in the `where`. */
    WHERE_CLAUSE_ONLY,

    /** Anywhere. */
    NO_USAGE_RESTRICTIONS,

    /** It is not known. */
    USAGE_UNKNOWN
}
