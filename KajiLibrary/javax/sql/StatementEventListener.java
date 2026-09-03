package javax.sql;

/**
 * KajiLibrary's javax.sql.StatementEventListener -- lo escucha un pool de **sentencias**.
 *
 * <p>Misma idea que {@link ConnectionEventListener} un nivel mas abajo: preparar una sentencia
 * cuesta, asi que un pool las guarda tambien a ellas, y necesita enterarse de cuando la aplicacion
 * cerro la suya.
 */
public interface StatementEventListener extends java.util.EventListener {

    /** La aplicacion cerro la sentencia: se puede reutilizar. */
    void statementClosed(StatementEvent event);

    /** La sentencia dejo de ser valida: hay que descartarla. */
    void statementErrorOccurred(StatementEvent event);
}
