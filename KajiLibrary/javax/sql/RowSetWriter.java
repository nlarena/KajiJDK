package javax.sql;

/**
 * KajiLibrary's javax.sql.RowSetWriter -- devuelve a la base los cambios de un {@link RowSet}.
 *
 * <p>Devuelve `boolean` y no `void`, que es lo que lo distingue de {@link RowSetReader}: `false`
 * significa **conflicto** --alguien mas cambio esas filas desde que se leyeron-- y no un error. Es la
 * unica respuesta honesta cuando dos escrituras se pisan.
 */
public interface RowSetWriter {

    /** Escribe los cambios; `false` si hubo conflicto. */
    boolean writeData(RowSetInternal caller) throws java.sql.SQLException;
}
