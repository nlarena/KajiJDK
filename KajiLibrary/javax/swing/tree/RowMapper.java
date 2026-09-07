package javax.swing.tree;

/**
 * Traduce caminos del arbol a numeros de fila.
 *
 * <h2>Por que no lo sabe el modelo de seleccion</h2>
 *
 * <p>La seleccion de un arbol se guarda como caminos, no como filas: un camino sigue siendo el
 * mismo si se despliega algo mas arriba, y la fila no. Pero para dibujar hay que saber la fila, y
 * eso depende de que este desplegado, que es cosa de la vista.
 *
 * <p>Esta interfaz es el puente. El modelo de seleccion tiene uno puesto y le pregunta cuando le
 * piden filas; un camino que no se ve -- porque su padre esta plegado -- devuelve -1.
 */
public interface RowMapper {

    /** Las filas de esos caminos; -1 para los que no se ven. */
    int[] getRowsForPaths(TreePath[] path);
}
