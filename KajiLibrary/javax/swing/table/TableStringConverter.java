package javax.swing.table;

/**
 * Convierte el valor de una celda a texto, para ordenar y filtrar.
 *
 * <h2>Por que no alcanza con {@code toString}</h2>
 *
 * <p>El {@code toString} de un objeto es para el programador; lo que la tabla muestra es para el
 * usuario, y muchas veces no son lo mismo -- una fecha, un importe, un enumerado con nombres
 * traducidos --. Ordenar o filtrar por el primero da un resultado que no se parece a lo que se ve.
 *
 * <p>Un convertidor propio resuelve eso sin tocar el modelo: el modelo sigue guardando objetos y la
 * vista los ordena por como se leen.
 */
public abstract class TableStringConverter {

    /** Para las subclases. */
    protected TableStringConverter() {
    }

    /**
     * El texto de esa celda.
     *
     * <p>Los indices son del <strong>modelo</strong>, no de la vista: se llama mientras se decide
     * el orden, cuando el de la vista todavia no existe.
     */
    public abstract String toString(TableModel model, int row, int column);
}
