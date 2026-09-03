package javax.sql;

/**
 * KajiLibrary's javax.sql.RowSetListener -- se entera de lo que le pasa a un {@link RowSet}.
 *
 * <p>Los tres avisos van de menos a mas grande, y estan separados porque cuestan distinto reaccionar:
 * mover el cursor puede pedir solo repintar una fila, cambiar una fila pide repintarla, y cambiar el
 * conjunto entero pide volver a dibujar todo.
 */
public interface RowSetListener extends java.util.EventListener {

    /** El cursor se movio. */
    void cursorMoved(RowSetEvent event);

    /** La fila actual cambio. */
    void rowChanged(RowSetEvent event);

    /** Cambio el conjunto entero. */
    void rowSetChanged(RowSetEvent event);
}
