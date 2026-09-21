package javax.sql.rowset;

import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.RowSetMetaData;

/**
 * The column metadata of a {@code RowSet}, in its writable version.
 *
 * <h2>Why a writable version is needed</h2>
 *
 * <p>The metadata of a {@code ResultSet} is read-only because the driver produces it: whoever
 * queries does not invent it, they receive it. A disconnected {@code RowSet}, on the other hand,
 * can be filled by hand —without a database in between— and there <strong>somebody has to
 * declare</strong> how many columns there are, what they are called and of which type they are.
 *
 * <p>This class is that somebody. The columns are set on it and then it is passed to
 * {@link CachedRowSet#setMetaData}.
 *
 * <h2>The mandatory order</h2>
 *
 * <p>{@link #setColumnCount} has to go first. Before knowing how many columns there are there is
 * nowhere to keep anything, and any other {@code set} fails with an index out of range. Calling it
 * again discards what had been set.
 *
 * <h2>Columns are counted from 1</h2>
 *
 * <p>It is the convention of all of JDBC and it comes from SQL, not from Java. Inside, it is kept
 * in arrays that start at zero, and that subtraction is the only place where the offset appears.
 *
 * <h2>The three methods that cannot be set</h2>
 *
 * <p>{@link #isReadOnly}, {@link #isWritable} and {@link #isDefinitelyWritable} have no
 * corresponding {@code set} in {@link RowSetMetaData}, so they answer the only reasonable thing for
 * a set that was filled by hand: writable. It is what the JDK does and it is coherent — whoever set
 * up the columns can write them.
 *
 * @since 1.5
 */
public class RowSetMetaDataImpl implements RowSetMetaData, Serializable {

    private static final long serialVersionUID = 6893806403181801867L;

    private int colCount;
    private ColInfo[] colInfo;

    /** What is kept for each column. */
    private static class ColInfo implements Serializable {
        private static final long serialVersionUID = 5490834817919311283L;
        boolean autoIncrement;
        boolean caseSensitive;
        boolean currency;
        boolean searchable;
        boolean signed;
        int nullable = columnNullableUnknown;
        int columnDisplaySize;
        String columnLabel;
        String columnName;
        String schemaName = "";
        int colPrecision;
        int colScale;
        String tableName = "";
        String catName = "";
        int colType;
        String colTypeName = "";
    }

    /** Without columns; {@link #setColumnCount} has to be called before anything else. */
    public RowSetMetaDataImpl() {
    }

    /**
     * Validates the index and returns the position in the internal array.
     *
     * <p>The validation is centralized on purpose: there are thirty-odd methods that receive an
     * index, and repeating the check in each one guarantees that some will be left without it.
     */
    private ColInfo col(final int columnIndex) throws SQLException {
        if (colInfo == null) {
            throw new SQLException("setColumnCount has to be called before using the metadata");
        }
        if (columnIndex < 1 || columnIndex > colCount) {
            throw new SQLException("column index out of range: " + columnIndex
                    + " (there are " + colCount + ")");
        }
        return colInfo[columnIndex - 1];
    }

    /**
     * How many columns it is going to have.
     *
     * <p>It discards whatever had been set before.
     *
     * @param columnCount the count
     * @throws SQLException if it is negative
     */
    public void setColumnCount(final int columnCount) throws SQLException {
        if (columnCount < 0) {
            throw new SQLException("the number of columns cannot be negative");
        }
        this.colCount = columnCount;
        // One too many, like the JDK. The last position is not used: `col` validates the range and
        // then subtracts one, so the highest index reached is columnCount - 1.
        this.colInfo = new ColInfo[columnCount + 1];
        for (int i = 0; i < colInfo.length; i++) {
            colInfo[i] = new ColInfo();
        }
    }

    /**
     * Whether the column numbers itself.
     *
     * @param columnIndex the column, from 1
     * @param property whether it is auto-increment
     * @throws SQLException if the index is not valid
     */
    public void setAutoIncrement(final int columnIndex, final boolean property)
            throws SQLException {
        col(columnIndex).autoIncrement = property;
    }

    /**
     * Whether upper and lower case are told apart when comparing.
     *
     * @param columnIndex the column, from 1
     * @param property whether it tells them apart
     * @throws SQLException if the index is not valid
     */
    public void setCaseSensitive(final int columnIndex, final boolean property)
            throws SQLException {
        col(columnIndex).caseSensitive = property;
    }

    /**
     * Whether the column can appear in a {@code WHERE}.
     *
     * @param columnIndex the column, from 1
     * @param property whether it is searchable
     * @throws SQLException if the index is not valid
     */
    public void setSearchable(final int columnIndex, final boolean property) throws SQLException {
        col(columnIndex).searchable = property;
    }

    /**
     * Whether the column is a monetary value.
     *
     * @param columnIndex the column, from 1
     * @param property whether it is currency
     * @throws SQLException if the index is not valid
     */
    public void setCurrency(final int columnIndex, final boolean property) throws SQLException {
        col(columnIndex).currency = property;
    }

    /**
     * Whether the column admits nulls.
     *
     * @param columnIndex the column, from 1
     * @param property one of the {@code columnNo*} constants of {@code ResultSetMetaData}
     * @throws SQLException if the index is not valid or the constant is not
     */
    public void setNullable(final int columnIndex, final int property) throws SQLException {
        if (property < columnNoNulls || property > columnNullableUnknown) {
            throw new SQLException("invalid nullability value: " + property);
        }
        col(columnIndex).nullable = property;
    }

    /**
     * Whether the value is signed.
     *
     * @param columnIndex the column, from 1
     * @param property whether it is signed
     * @throws SQLException if the index is not valid
     */
    public void setSigned(final int columnIndex, final boolean property) throws SQLException {
        col(columnIndex).signed = property;
    }

    /**
     * The normal width of the column, in characters.
     *
     * @param columnIndex the column, from 1
     * @param size the width
     * @throws SQLException if the index is not valid or the width is negative
     */
    public void setColumnDisplaySize(final int columnIndex, final int size) throws SQLException {
        if (size < 0) {
            throw new SQLException("the width cannot be negative");
        }
        col(columnIndex).columnDisplaySize = size;
    }

    /**
     * The title to show the column with.
     *
     * @param columnIndex the column, from 1
     * @param label the title
     * @throws SQLException if the index is not valid
     */
    public void setColumnLabel(final int columnIndex, final String label) throws SQLException {
        col(columnIndex).columnLabel = label == null ? "" : label;
    }

    /**
     * The name of the column.
     *
     * @param columnIndex the column, from 1
     * @param columnName the name
     * @throws SQLException if the index is not valid
     */
    public void setColumnName(final int columnIndex, final String columnName) throws SQLException {
        col(columnIndex).columnName = columnName == null ? "" : columnName;
    }

    /**
     * The schema of the column's table.
     *
     * @param columnIndex the column, from 1
     * @param schemaName the schema
     * @throws SQLException if the index is not valid
     */
    public void setSchemaName(final int columnIndex, final String schemaName) throws SQLException {
        col(columnIndex).schemaName = schemaName == null ? "" : schemaName;
    }

    /**
     * How many significant digits it has.
     *
     * @param columnIndex the column, from 1
     * @param precision the precision
     * @throws SQLException if the index is not valid or the precision is negative
     */
    public void setPrecision(final int columnIndex, final int precision) throws SQLException {
        if (precision < 0) {
            throw new SQLException("the precision cannot be negative");
        }
        col(columnIndex).colPrecision = precision;
    }

    /**
     * How many digits there are to the right of the point.
     *
     * @param columnIndex the column, from 1
     * @param scale the scale
     * @throws SQLException if the index is not valid or the scale is negative
     */
    public void setScale(final int columnIndex, final int scale) throws SQLException {
        if (scale < 0) {
            throw new SQLException("the scale cannot be negative");
        }
        col(columnIndex).colScale = scale;
    }

    /**
     * The table the column comes from.
     *
     * @param columnIndex the column, from 1
     * @param tableName the table
     * @throws SQLException if the index is not valid
     */
    public void setTableName(final int columnIndex, final String tableName) throws SQLException {
        col(columnIndex).tableName = tableName == null ? "" : tableName;
    }

    /**
     * The catalog of the table.
     *
     * @param columnIndex the column, from 1
     * @param catalogName the catalog
     * @throws SQLException if the index is not valid
     */
    public void setCatalogName(final int columnIndex, final String catalogName)
            throws SQLException {
        col(columnIndex).catName = catalogName == null ? "" : catalogName;
    }

    /**
     * The SQL type of the column.
     *
     * @param columnIndex the column, from 1
     * @param SQLType a constant of {@link Types}
     * @throws SQLException if the index is not valid
     */
    public void setColumnType(final int columnIndex, final int SQLType) throws SQLException {
        col(columnIndex).colType = SQLType;
    }

    /**
     * The name of the type, as the database calls it.
     *
     * @param columnIndex the column, from 1
     * @param typeName the name of the type
     * @throws SQLException if the index is not valid
     */
    public void setColumnTypeName(final int columnIndex, final String typeName)
            throws SQLException {
        col(columnIndex).colTypeName = typeName == null ? "" : typeName;
    }

    /**
     * How many columns there are.
     *
     * @return the count
     * @throws SQLException never
     */
    public int getColumnCount() throws SQLException {
        return colCount;
    }

    /**
     * Whether the column numbers itself.
     *
     * @param columnIndex the column, from 1
     * @return whether it is auto-increment
     * @throws SQLException if the index is not valid
     */
    public boolean isAutoIncrement(final int columnIndex) throws SQLException {
        return col(columnIndex).autoIncrement;
    }

    /**
     * Whether it tells upper and lower case apart.
     *
     * @param columnIndex the column, from 1
     * @return whether it tells them apart
     * @throws SQLException if the index is not valid
     */
    public boolean isCaseSensitive(final int columnIndex) throws SQLException {
        return col(columnIndex).caseSensitive;
    }

    /**
     * Whether it can appear in a {@code WHERE}.
     *
     * @param columnIndex the column, from 1
     * @return whether it is searchable
     * @throws SQLException if the index is not valid
     */
    public boolean isSearchable(final int columnIndex) throws SQLException {
        return col(columnIndex).searchable;
    }

    /**
     * Whether it is a monetary value.
     *
     * @param columnIndex the column, from 1
     * @return whether it is currency
     * @throws SQLException if the index is not valid
     */
    public boolean isCurrency(final int columnIndex) throws SQLException {
        return col(columnIndex).currency;
    }

    /**
     * Whether it admits nulls.
     *
     * @param columnIndex the column, from 1
     * @return one of the {@code columnNo*} constants
     * @throws SQLException if the index is not valid
     */
    public int isNullable(final int columnIndex) throws SQLException {
        return col(columnIndex).nullable;
    }

    /**
     * Whether the value is signed.
     *
     * @param columnIndex the column, from 1
     * @return whether it is signed
     * @throws SQLException if the index is not valid
     */
    public boolean isSigned(final int columnIndex) throws SQLException {
        return col(columnIndex).signed;
    }

    /**
     * The normal width of the column.
     *
     * @param columnIndex the column, from 1
     * @return the width in characters
     * @throws SQLException if the index is not valid
     */
    public int getColumnDisplaySize(final int columnIndex) throws SQLException {
        return col(columnIndex).columnDisplaySize;
    }

    /**
     * The title to show it with.
     *
     * <p>If none was set it returns the name: it is what a viewer is going to want to show, and not
     * having a title is no reason to show a column without a header. JDK 25 returns {@code null}
     * instead.
     *
     * @param columnIndex the column, from 1
     * @return the title
     * @throws SQLException if the index is not valid
     */
    public String getColumnLabel(final int columnIndex) throws SQLException {
        final ColInfo c = col(columnIndex);
        return c.columnLabel != null && c.columnLabel.length() > 0
                ? c.columnLabel : getColumnName(columnIndex);
    }

    /**
     * The name of the column.
     *
     * @param columnIndex the column, from 1
     * @return the name, or an empty string if it was not set; JDK 25 returns {@code null}
     * @throws SQLException if the index is not valid
     */
    public String getColumnName(final int columnIndex) throws SQLException {
        final String n = col(columnIndex).columnName;
        return n == null ? "" : n;
    }

    /**
     * The schema of the table.
     *
     * @param columnIndex the column, from 1
     * @return the schema
     * @throws SQLException if the index is not valid
     */
    public String getSchemaName(final int columnIndex) throws SQLException {
        return col(columnIndex).schemaName;
    }

    /**
     * The significant digits.
     *
     * @param columnIndex the column, from 1
     * @return the precision
     * @throws SQLException if the index is not valid
     */
    public int getPrecision(final int columnIndex) throws SQLException {
        return col(columnIndex).colPrecision;
    }

    /**
     * The digits to the right of the point.
     *
     * @param columnIndex the column, from 1
     * @return the scale
     * @throws SQLException if the index is not valid
     */
    public int getScale(final int columnIndex) throws SQLException {
        return col(columnIndex).colScale;
    }

    /**
     * The table it comes from.
     *
     * @param columnIndex the column, from 1
     * @return the table
     * @throws SQLException if the index is not valid
     */
    public String getTableName(final int columnIndex) throws SQLException {
        return col(columnIndex).tableName;
    }

    /**
     * The catalog of the table.
     *
     * @param columnIndex the column, from 1
     * @return the catalog
     * @throws SQLException if the index is not valid
     */
    public String getCatalogName(final int columnIndex) throws SQLException {
        return col(columnIndex).catName;
    }

    /**
     * The SQL type.
     *
     * @param columnIndex the column, from 1
     * @return a constant of {@link Types}
     * @throws SQLException if the index is not valid
     */
    public int getColumnType(final int columnIndex) throws SQLException {
        return col(columnIndex).colType;
    }

    /**
     * The name of the type.
     *
     * @param columnIndex the column, from 1
     * @return the name
     * @throws SQLException if the index is not valid
     */
    public String getColumnTypeName(final int columnIndex) throws SQLException {
        return col(columnIndex).colTypeName;
    }

    /**
     * Whether the column is read-only.
     *
     * <p>Always {@code false}: there is no way of declaring it read-only, because
     * {@link RowSetMetaData} has no corresponding {@code set}.
     *
     * @param columnIndex the column, from 1
     * @return {@code false}
     * @throws SQLException if the index is not valid
     */
    public boolean isReadOnly(final int columnIndex) throws SQLException {
        col(columnIndex);
        return false;
    }

    /**
     * Whether it can be written.
     *
     * @param columnIndex the column, from 1
     * @return {@code true}, for what was said in {@link #isReadOnly}
     * @throws SQLException if the index is not valid
     */
    public boolean isWritable(final int columnIndex) throws SQLException {
        col(columnIndex);
        return true;
    }

    /**
     * Whether writing is sure to work.
     *
     * @param columnIndex the column, from 1
     * @return {@code true}, for what was said in {@link #isReadOnly}
     * @throws SQLException if the index is not valid
     */
    public boolean isDefinitelyWritable(final int columnIndex) throws SQLException {
        col(columnIndex);
        return true;
    }

    /**
     * The Java class {@code getObject} is going to return for that column.
     *
     * <p>The mapping is JDBC's {@code getObject} table. The types that are not in it fall to {@code
     * Object}, which is the right answer for a type the database defines and Java does not know.
     *
     * <p>JDK 25's {@code RowSetMetaDataImpl} does not follow that table: it answers {@code Byte}
     * for {@code TINYINT}, {@code Short} for {@code SMALLINT}, and {@code java.lang.String} for
     * everything outside its own list, {@code BOOLEAN}, {@code ARRAY} and {@code STRUCT} included.
     *
     * @param columnIndex the column, from 1
     * @return the fully qualified name of the class
     * @throws SQLException if the index is not valid
     */
    public String getColumnClassName(final int columnIndex) throws SQLException {
        // A chain of ifs and not a switch: #503 -- the frozen bin/javac does not fold a `case`
        // label whose constant comes from a .class on the classpath, and all of these do. #503 is
        // closed in the compiler source (target/release/javac emits the lookupswitch), but
        // bin/javac still rejects it; go back to the switch when bin/ is rebuilt.
        final int t = col(columnIndex).colType;
        if (t == Types.BIT || t == Types.BOOLEAN) {
            return "java.lang.Boolean";
        }
        if (t == Types.TINYINT || t == Types.SMALLINT || t == Types.INTEGER) {
            return "java.lang.Integer";
        }
        if (t == Types.BIGINT) {
            return "java.lang.Long";
        }
        if (t == Types.REAL) {
            return "java.lang.Float";
        }
        if (t == Types.FLOAT || t == Types.DOUBLE) {
            return "java.lang.Double";
        }
        if (t == Types.NUMERIC || t == Types.DECIMAL) {
            return "java.math.BigDecimal";
        }
        if (t == Types.CHAR || t == Types.VARCHAR || t == Types.LONGVARCHAR
                || t == Types.NCHAR || t == Types.NVARCHAR || t == Types.LONGNVARCHAR) {
            return "java.lang.String";
        }
        if (t == Types.BINARY || t == Types.VARBINARY || t == Types.LONGVARBINARY) {
            return "byte[]";
        }
        if (t == Types.DATE) {
            return "java.sql.Date";
        }
        if (t == Types.TIME) {
            return "java.sql.Time";
        }
        if (t == Types.TIMESTAMP) {
            return "java.sql.Timestamp";
        }
        if (t == Types.BLOB) {
            return "java.sql.Blob";
        }
        if (t == Types.CLOB) {
            return "java.sql.Clob";
        }
        if (t == Types.NCLOB) {
            return "java.sql.NClob";
        }
        if (t == Types.ARRAY) {
            return "java.sql.Array";
        }
        if (t == Types.REF) {
            return "java.sql.Ref";
        }
        if (t == Types.ROWID) {
            return "java.sql.RowId";
        }
        if (t == Types.SQLXML) {
            return "java.sql.SQLXML";
        }
        if (t == Types.STRUCT) {
            return "java.sql.Struct";
        }
        // A type the database defines and Java does not know: Object is the right answer.
        return "java.lang.Object";
    }
    /**
     * This instance, if it is asked for as an interface it implements.
     *
     * @param <T> the requested type
     * @param iface the interface
     * @return this instance
     * @throws SQLException if it does not implement that interface
     */
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        if (iface != null && iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("this class does not implement " + iface);
    }

    /**
     * Whether {@link #unwrap} is going to manage with that interface.
     *
     * @param iface the interface
     * @return whether it implements it
     * @throws SQLException never
     */
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        return iface != null && iface.isInstance(this);
    }
}
