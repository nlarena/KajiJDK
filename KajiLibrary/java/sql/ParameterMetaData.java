package java.sql;

/**
 * KajiLibrary's java.sql.ParameterMetaData -- what each hole of a {@link PreparedStatement}
 * expects.
 *
 * <p>The mirror of {@link ResultSetMetaData} on the input side, and it serves the same purpose:
 * writing code that fills a statement without knowing in advance how many parameters it has or of
 * which type.
 *
 * <p>{@link #getParameterMode} is what has no equivalent on the other side -- a stored procedure
 * can have output parameters, which are **read** after executing it.
 */
public interface ParameterMetaData extends Wrapper {

    /** The parameter does not allow nulls. */
    int parameterNoNulls = 0;

    /** It allows them. */
    int parameterNullable = 1;

    /** It is not known. */
    int parameterNullableUnknown = 2;

    /** It is not known which way it goes. */
    int parameterModeUnknown = 0;

    /** Input only. */
    int parameterModeIn = 1;

    /** Input and output. */
    int parameterModeInOut = 2;

    /** Output only. */
    int parameterModeOut = 4;

    /** How many parameters there are. */
    int getParameterCount() throws SQLException;

    /** One of `parameterNoNulls`, `parameterNullable`, `parameterNullableUnknown`. */
    int isNullable(int param) throws SQLException;

    boolean isSigned(int param) throws SQLException;

    int getPrecision(int param) throws SQLException;

    int getScale(int param) throws SQLException;

    /** The SQL type, as a code. */
    int getParameterType(int param) throws SQLException;

    /** The SQL type, as the vendor calls it. */
    String getParameterTypeName(int param) throws SQLException;

    /** The Java class to pass it. */
    String getParameterClassName(int param) throws SQLException;

    /** One of the `parameterMode*`. */
    int getParameterMode(int param) throws SQLException;
}
