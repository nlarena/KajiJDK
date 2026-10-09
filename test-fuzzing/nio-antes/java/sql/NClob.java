package java.sql;

/**
 * KajiLibrary's java.sql.NClob -- a {@link Clob} in the **national** character set.
 *
 * <p>It adds not a single member, and that is its whole function: it exists so the type says which
 * of the two character sets the column uses. In databases whose normal set is not Unicode, `NCHAR`
 * is the one that is, and putting one where the other goes corrupts the text silently. The
 * distinction lives in the type system precisely because at run time it does not show.
 */
public interface NClob extends Clob {
}
