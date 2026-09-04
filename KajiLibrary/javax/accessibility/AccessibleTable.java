package javax.accessibility;

/**
 * Lo implementa lo que muestra datos en **filas y columnas**.
 *
 * <p>Lo que lo separa de una lista de listas son las cabeceras y las descripciones: sin ellas, quien
 * no ve la pantalla escucha un número suelto y no tiene forma de saber de qué columna es. Las
 * cabeceras son a su vez tablas, lo que permite cabeceras de varios niveles.
 *
 * <p>La **extensión** de una celda —{@link #getAccessibleRowExtentAt} y su par— es cuántas filas o
 * columnas ocupa. Es lo que hace que una celda combinada se anuncie como una y no como cuatro
 * celdas repetidas.
 */
public interface AccessibleTable {

    /** El título de la tabla, o `null` si no tiene. */
    Accessible getAccessibleCaption();

    /** Cambia el título. */
    void setAccessibleCaption(Accessible a);

    /** El resumen de la tabla, o `null` si no tiene. */
    Accessible getAccessibleSummary();

    /** Cambia el resumen. */
    void setAccessibleSummary(Accessible a);

    /** Cuántas filas hay. */
    int getAccessibleRowCount();

    /** Cuántas columnas hay. */
    int getAccessibleColumnCount();

    /**
     * La celda de esa posición.
     *
     * @return la celda, o `null` si la posición no existe
     */
    Accessible getAccessibleAt(int r, int c);

    /** Cuántas filas ocupa esa celda. */
    int getAccessibleRowExtentAt(int r, int c);

    /** Cuántas columnas ocupa esa celda. */
    int getAccessibleColumnExtentAt(int r, int c);

    /** La cabecera de filas, ella misma una tabla, o `null` si no hay. */
    AccessibleTable getAccessibleRowHeader();

    /** Cambia la cabecera de filas. */
    void setAccessibleRowHeader(AccessibleTable table);

    /** La cabecera de columnas, o `null` si no hay. */
    AccessibleTable getAccessibleColumnHeader();

    /** Cambia la cabecera de columnas. */
    void setAccessibleColumnHeader(AccessibleTable table);

    /** Qué describe a esa fila, o `null` si nada. */
    Accessible getAccessibleRowDescription(int r);

    /** Cambia la descripción de esa fila. */
    void setAccessibleRowDescription(int r, Accessible a);

    /** Qué describe a esa columna, o `null` si nada. */
    Accessible getAccessibleColumnDescription(int c);

    /** Cambia la descripción de esa columna. */
    void setAccessibleColumnDescription(int c, Accessible a);

    /** Si esa celda está elegida. */
    boolean isAccessibleSelected(int r, int c);

    /** Si esa fila entera está elegida. */
    boolean isAccessibleRowSelected(int r);

    /** Si esa columna entera está elegida. */
    boolean isAccessibleColumnSelected(int c);

    /** Qué filas están elegidas. */
    int[] getSelectedAccessibleRows();

    /** Qué columnas están elegidas. */
    int[] getSelectedAccessibleColumns();
}
