package java.sql;

/**
 * KajiLibrary's java.sql.JDBCType -- el catalogo de {@link Types} como enum.
 *
 * <p>La ventaja sobre los enteros no es cosmetica: un `int` acepta cualquier valor, asi que
 * `setNull(1, 4)` y `setNull(1, 400)` compilan igual y el segundo falla en tiempo de ejecucion. Con
 * el enum el compilador lo atrapa, y ademas se puede imprimir con nombre.
 *
 * <p>Implementa {@link SQLType}, que es lo que permite que las firmas nuevas acepten tanto estos como
 * los tipos propios de un proveedor.
 */
public enum JDBCType implements SQLType {

    /** El tipo SQL `BIT`. */
    BIT(Types.BIT),

    /** El tipo SQL `TINYINT`. */
    TINYINT(Types.TINYINT),

    /** El tipo SQL `SMALLINT`. */
    SMALLINT(Types.SMALLINT),

    /** El tipo SQL `INTEGER`. */
    INTEGER(Types.INTEGER),

    /** El tipo SQL `BIGINT`. */
    BIGINT(Types.BIGINT),

    /** El tipo SQL `FLOAT`. */
    FLOAT(Types.FLOAT),

    /** El tipo SQL `REAL`. */
    REAL(Types.REAL),

    /** El tipo SQL `DOUBLE`. */
    DOUBLE(Types.DOUBLE),

    /** El tipo SQL `NUMERIC`. */
    NUMERIC(Types.NUMERIC),

    /** El tipo SQL `DECIMAL`. */
    DECIMAL(Types.DECIMAL),

    /** El tipo SQL `CHAR`. */
    CHAR(Types.CHAR),

    /** El tipo SQL `VARCHAR`. */
    VARCHAR(Types.VARCHAR),

    /** El tipo SQL `LONGVARCHAR`. */
    LONGVARCHAR(Types.LONGVARCHAR),

    /** El tipo SQL `DATE`. */
    DATE(Types.DATE),

    /** El tipo SQL `TIME`. */
    TIME(Types.TIME),

    /** El tipo SQL `TIMESTAMP`. */
    TIMESTAMP(Types.TIMESTAMP),

    /** El tipo SQL `BINARY`. */
    BINARY(Types.BINARY),

    /** El tipo SQL `VARBINARY`. */
    VARBINARY(Types.VARBINARY),

    /** El tipo SQL `LONGVARBINARY`. */
    LONGVARBINARY(Types.LONGVARBINARY),

    /** El tipo SQL `NULL`. */
    NULL(Types.NULL),

    /** El tipo SQL `OTHER`. */
    OTHER(Types.OTHER),

    /** El tipo SQL `JAVA_OBJECT`. */
    JAVA_OBJECT(Types.JAVA_OBJECT),

    /** El tipo SQL `DISTINCT`. */
    DISTINCT(Types.DISTINCT),

    /** El tipo SQL `STRUCT`. */
    STRUCT(Types.STRUCT),

    /** El tipo SQL `ARRAY`. */
    ARRAY(Types.ARRAY),

    /** El tipo SQL `BLOB`. */
    BLOB(Types.BLOB),

    /** El tipo SQL `CLOB`. */
    CLOB(Types.CLOB),

    /** El tipo SQL `REF`. */
    REF(Types.REF),

    /** El tipo SQL `DATALINK`. */
    DATALINK(Types.DATALINK),

    /** El tipo SQL `BOOLEAN`. */
    BOOLEAN(Types.BOOLEAN),

    /** El tipo SQL `ROWID`. */
    ROWID(Types.ROWID),

    /** El tipo SQL `NCHAR`. */
    NCHAR(Types.NCHAR),

    /** El tipo SQL `NVARCHAR`. */
    NVARCHAR(Types.NVARCHAR),

    /** El tipo SQL `LONGNVARCHAR`. */
    LONGNVARCHAR(Types.LONGNVARCHAR),

    /** El tipo SQL `NCLOB`. */
    NCLOB(Types.NCLOB),

    /** El tipo SQL `SQLXML`. */
    SQLXML(Types.SQLXML),

    /** El tipo SQL `REF_CURSOR`. */
    REF_CURSOR(Types.REF_CURSOR),

    /** El tipo SQL `TIME_WITH_TIMEZONE`. */
    TIME_WITH_TIMEZONE(Types.TIME_WITH_TIMEZONE),

    /** El tipo SQL `TIMESTAMP_WITH_TIMEZONE`. */
    TIMESTAMP_WITH_TIMEZONE(Types.TIMESTAMP_WITH_TIMEZONE);

    private final Integer type;

    JDBCType(final Integer type) {
        this.type = type;
    }

    /** El nombre del tipo; para estos, el del enum. */
    public String getName() {
        return this.name();
    }

    /** `"java.sql"`: son los tipos del estandar, no los de un proveedor. */
    public String getVendor() {
        return "java.sql";
    }

    /** El entero de {@link Types} que le corresponde. */
    public Integer getVendorTypeNumber() {
        return this.type;
    }

    /**
     * El `JDBCType` de ese entero de {@link Types}.
     *
     * @throws IllegalArgumentException si el entero no es ninguno
     */
    public static JDBCType valueOf(int type) {
        JDBCType[] todos = JDBCType.values();
        int i = 0;
        while (i < todos.length) {
            if (todos[i].type.intValue() == type) {
                return todos[i];
            }
            i = i + 1;
        }
        throw new IllegalArgumentException("Type:" + type + " is not a valid Types.java value.");
    }
}
