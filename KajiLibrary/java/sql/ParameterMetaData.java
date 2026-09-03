package java.sql;

/**
 * KajiLibrary's java.sql.ParameterMetaData -- que espera cada hueco de un
 * {@link PreparedStatement}.
 *
 * <p>El espejo de {@link ResultSetMetaData} del lado de la entrada, y sirve para lo mismo: escribir
 * codigo que llena una sentencia sin saber de antemano cuantos parametros tiene ni de que tipo.
 *
 * <p>{@link #getParameterMode} es lo que no tiene equivalente del otro lado -- un procedimiento
 * almacenado puede tener parametros de salida, que se **leen** despues de ejecutarlo.
 */
public interface ParameterMetaData extends Wrapper {

    /** El parametro no admite nulos. */
    int parameterNoNulls = 0;

    /** Los admite. */
    int parameterNullable = 1;

    /** No se sabe. */
    int parameterNullableUnknown = 2;

    /** No se sabe en que direccion va. */
    int parameterModeUnknown = 0;

    /** Solo entrada. */
    int parameterModeIn = 1;

    /** Entrada y salida. */
    int parameterModeInOut = 2;

    /** Solo salida. */
    int parameterModeOut = 4;

    /** Cuantos parametros hay. */
    int getParameterCount() throws SQLException;

    /** Uno de los `parameterNullable*`. */
    int isNullable(int param) throws SQLException;

    boolean isSigned(int param) throws SQLException;

    int getPrecision(int param) throws SQLException;

    int getScale(int param) throws SQLException;

    /** El tipo SQL, como codigo. */
    int getParameterType(int param) throws SQLException;

    /** El tipo SQL, como lo llama el proveedor. */
    String getParameterTypeName(int param) throws SQLException;

    /** La clase Java que hay que pasarle. */
    String getParameterClassName(int param) throws SQLException;

    /** Uno de los `parameterMode*`. */
    int getParameterMode(int param) throws SQLException;
}
