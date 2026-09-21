package java.sql;

/**
 * KajiLibrary's java.sql.Types -- the catalogue of SQL types, as integers.
 *
 * <p>The numbers follow no order: they come from the X/Open standard and later additions, which is
 * why negative ones live alongside four-digit ones. There is nothing to deduce from them -- they
 * are labels, and the only contract is that these are the values.
 *
 * <p>There is {@link JDBCType}, which is the same catalogue as an enum and therefore with names and
 * with compiler checking. This class stays because the signatures that take an `int sqlType` are
 * older and cannot be changed.
 */
public class Types {

    // A class of constants is not instantiated; the private constructor is what enforces it.
    private Types() {
    }

    public static final int BIT = -7;

    public static final int TINYINT = -6;

    public static final int SMALLINT = 5;

    public static final int INTEGER = 4;

    public static final int BIGINT = -5;

    public static final int FLOAT = 6;

    public static final int REAL = 7;

    public static final int DOUBLE = 8;

    public static final int NUMERIC = 2;

    public static final int DECIMAL = 3;

    public static final int CHAR = 1;

    public static final int VARCHAR = 12;

    public static final int LONGVARCHAR = -1;

    public static final int DATE = 91;

    public static final int TIME = 92;

    public static final int TIMESTAMP = 93;

    public static final int BINARY = -2;

    public static final int VARBINARY = -3;

    public static final int LONGVARBINARY = -4;

    public static final int NULL = 0;

    public static final int OTHER = 1111;

    public static final int JAVA_OBJECT = 2000;

    public static final int DISTINCT = 2001;

    public static final int STRUCT = 2002;

    public static final int ARRAY = 2003;

    public static final int BLOB = 2004;

    public static final int CLOB = 2005;

    public static final int REF = 2006;

    public static final int DATALINK = 70;

    public static final int BOOLEAN = 16;

    public static final int ROWID = -8;

    public static final int NCHAR = -15;

    public static final int NVARCHAR = -9;

    public static final int LONGNVARCHAR = -16;

    public static final int NCLOB = 2011;

    public static final int SQLXML = 2009;

    public static final int REF_CURSOR = 2012;

    public static final int TIME_WITH_TIMEZONE = 2013;

    public static final int TIMESTAMP_WITH_TIMEZONE = 2014;
}
