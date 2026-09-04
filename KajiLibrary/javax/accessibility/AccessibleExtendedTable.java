package javax.accessibility;

/**
 * Una tabla accesible que además traduce entre **índice lineal** y fila y columna.
 *
 * <p>Los hijos de un objeto accesible se numeran de corrido, y una tabla se recorre por coordenadas.
 * Estos tres métodos son el puente entre las dos numeraciones, y sin ellos hay que reconstruirlo a
 * mano en cada recorrido — mal, porque las celdas combinadas rompen la cuenta ingenua.
 */
public interface AccessibleExtendedTable extends AccessibleTable {

    /** Qué fila corresponde a ese índice lineal. */
    int getAccessibleRow(int index);

    /** Qué columna corresponde a ese índice lineal. */
    int getAccessibleColumn(int index);

    /** Qué índice lineal corresponde a esa posición. */
    int getAccessibleIndex(int r, int c);
}
