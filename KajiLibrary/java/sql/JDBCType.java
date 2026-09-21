package java.sql;

/**
 * KajiLibrary's java.sql.JDBCType -- {@link Types}'s catalogue as an enum.
 *
 * <p>The advantage over the integers is not cosmetic: an `int` accepts any value, so `setNull(1,
 * 4)` and `setNull(1, 400)` compile alike and the second fails at run time. With the enum the
 * compiler catches it, and on top of that it can be printed by name.
 *
 * <p>It implements {@link SQLType}, which is what allows the newer signatures to accept both these
 * and a vendor's own types.
 */
public enum JDBCType implements SQLType {

    /** The SQL type `BIT`. */
    BIT(Types.BIT),

    /** The SQL type `TINYINT`. */
    TINYINT(Types.TINYINT),

    /** The SQL type `SMALLINT`. */
    SMALLINT(Types.SMALLINT),

    /** The SQL type `INTEGER`. */
    INTEGER(Types.INTEGER),

    /** The SQL type `BIGINT`. */
    BIGINT(Types.BIGINT),

    /** The SQL type `FLOAT`. */
    FLOAT(Types.FLOAT),

    /** The SQL type `REAL`. */
    REAL(Types.REAL),

    /** The SQL type `DOUBLE`. */
    DOUBLE(Types.DOUBLE),

    /** The SQL type `NUMERIC`. */
    NUMERIC(Types.NUMERIC),

    /** The SQL type `DECIMAL`. */
    DECIMAL(Types.DECIMAL),

    /** The SQL type `CHAR`. */
    CHAR(Types.CHAR),

    /** The SQL type `VARCHAR`. */
    VARCHAR(Types.VARCHAR),

    /** The SQL type `LONGVARCHAR`. */
    LONGVARCHAR(Types.LONGVARCHAR),

    /** The SQL type `DATE`. */
    DATE(Types.DATE),

    /** The SQL type `TIME`. */
    TIME(Types.TIME),

    /** The SQL type `TIMESTAMP`. */
    TIMESTAMP(Types.TIMESTAMP),

    /** The SQL type `BINARY`. */
    BINARY(Types.BINARY),

    /** The SQL type `VARBINARY`. */
    VARBINARY(Types.VARBINARY),

    /** The SQL type `LONGVARBINARY`. */
    LONGVARBINARY(Types.LONGVARBINARY),

    /** The SQL type `NULL`. */
    NULL(Types.NULL),

    /** The SQL type `OTHER`. */
    OTHER(Types.OTHER),

    /** The SQL type `JAVA_OBJECT`. */
    JAVA_OBJECT(Types.JAVA_OBJECT),

    /** The SQL type `DISTINCT`. */
    DISTINCT(Types.DISTINCT),

    /** The SQL type `STRUCT`. */
    STRUCT(Types.STRUCT),

    /** The SQL type `ARRAY`. */
    ARRAY(Types.ARRAY),

    /** The SQL type `BLOB`. */
    BLOB(Types.BLOB),

    /** The SQL type `CLOB`. */
    CLOB(Types.CLOB),

    /** The SQL type `REF`. */
    REF(Types.REF),

    /** The SQL type `DATALINK`. */
    DATALINK(Types.DATALINK),

    /** The SQL type `BOOLEAN`. */
    BOOLEAN(Types.BOOLEAN),

    /** The SQL type `ROWID`. */
    ROWID(Types.ROWID),

    /** The SQL type `NCHAR`. */
    NCHAR(Types.NCHAR),

    /** The SQL type `NVARCHAR`. */
    NVARCHAR(Types.NVARCHAR),

    /** The SQL type `LONGNVARCHAR`. */
    LONGNVARCHAR(Types.LONGNVARCHAR),

    /** The SQL type `NCLOB`. */
    NCLOB(Types.NCLOB),

    /** The SQL type `SQLXML`. */
    SQLXML(Types.SQLXML),

    /** The SQL type `REF_CURSOR`. */
    REF_CURSOR(Types.REF_CURSOR),

    /** The SQL type `TIME_WITH_TIMEZONE`. */
    TIME_WITH_TIMEZONE(Types.TIME_WITH_TIMEZONE),

    /** The SQL type `TIMESTAMP_WITH_TIMEZONE`. */
    TIMESTAMP_WITH_TIMEZONE(Types.TIMESTAMP_WITH_TIMEZONE);

    private final Integer type;

    JDBCType(final Integer type) {
        this.type = type;
    }

    /** The type's name; for these, the enum's. */
    public String getName() {
        return this.name();
    }

    /** `"java.sql"`: they are the standard's types, not a vendor's. */
    public String getVendor() {
        return "java.sql";
    }

    /** The {@link Types} integer that matches it. */
    public Integer getVendorTypeNumber() {
        return this.type;
    }

    /**
     * The `JDBCType` for that {@link Types} integer.
     *
     * @throws IllegalArgumentException if the integer is none of them
     */
    public static JDBCType valueOf(int type) {
        JDBCType[] all = JDBCType.values();
        int i = 0;
        while (i < all.length) {
            if (all[i].type.intValue() == type) {
                return all[i];
            }
            i = i + 1;
        }
        throw new IllegalArgumentException("Type:" + type + " is not a valid Types.java value.");
    }
}
