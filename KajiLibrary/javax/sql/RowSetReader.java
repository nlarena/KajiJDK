package javax.sql;

/**
 * KajiLibrary's javax.sql.RowSetReader -- llena un {@link RowSet} desconectado.
 *
 * <p>Un solo metodo, y por eso el conjunto no necesita saber de donde salen sus datos: puede ser una
 * consulta, un archivo o nada de eso.
 */
public interface RowSetReader {

    /** Llena ese conjunto. */
    void readData(RowSetInternal caller) throws java.sql.SQLException;
}
