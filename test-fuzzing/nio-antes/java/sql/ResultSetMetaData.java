package java.sql;

/**
 * KajiLibrary's java.sql.ResultSetMetaData -- which columns a query brought.
 *
 * <p>It exists because a query can be written without knowing its shape: a `select *`, or one that
 * arrives as text. Without this there would be no way to walk a generic result -- neither to know
 * how many columns there are nor how to read each one.
 *
 * <p>Columns are numbered **from one**, not from zero, throughout the JDBC API. It is SQL's
 * convention and not an oversight.
 */
public interface ResultSetMetaData extends Wrapper {

    /** The column does not allow nulls. */
    int columnNoNulls = 0;

    /** The column allows nulls. */
    int columnNullable = 1;

    /** It is not known whether it allows them. */
    int columnNullableUnknown = 2;

    /** How many columns there are. */
    int getColumnCount() throws SQLException;

    /** Whether the column numbers itself. */
    boolean isAutoIncrement(int column) throws SQLException;

    /** Whether it is case sensitive. */
    boolean isCaseSensitive(int column) throws SQLException;

    /** Whether it can be used in a `where`. */
    boolean isSearchable(int column) throws SQLException;

    /** Whether it is a monetary value. */
    boolean isCurrency(int column) throws SQLException;

    /** Whether it allows nulls: one of the three `column*` nullability constants. */
    int isNullable(int column) throws SQLException;

    /** Whether it is a signed number. */
    boolean isSigned(int column) throws SQLException;

    /** The normal width in characters, for displaying it. */
    int getColumnDisplaySize(int column) throws SQLException;

    /** The suggested title -- the query's `as`, if there was one. */
    String getColumnLabel(int column) throws SQLException;

    /** The real name of the column. */
    String getColumnName(int column) throws SQLException;

    String getSchemaName(int column) throws SQLException;

    /** The total digits of a number, or the characters of a text. */
    int getPrecision(int column) throws SQLException;

    /** The digits to the right of the point. */
    int getScale(int column) throws SQLException;

    String getTableName(int column) throws SQLException;

    String getCatalogName(int column) throws SQLException;

    /** The SQL type, as a code. */
    int getColumnType(int column) throws SQLException;

    /** The SQL type, as the vendor calls it. */
    String getColumnTypeName(int column) throws SQLException;

    boolean isReadOnly(int column) throws SQLException;

    /** Whether it **could** be written. */
    boolean isWritable(int column) throws SQLException;

    /** Whether writing it **will** work; stronger than {@link #isWritable}. */
    boolean isDefinitelyWritable(int column) throws SQLException;

    /** The Java class `ResultSet.getObject` returns for this column. */
    String getColumnClassName(int column) throws SQLException;
}
