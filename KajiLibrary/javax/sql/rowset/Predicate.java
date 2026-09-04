package javax.sql.rowset;

import java.sql.SQLException;

import javax.sql.RowSet;

/**
 * El filtro de un {@link FilteredRowSet}: decide que filas se ven.
 *
 * <h2>Por que hay tres metodos</h2>
 *
 * <p>Porque el filtrado ocurre en dos momentos. {@link #evaluate(RowSet)} se llama con el conjunto
 * posicionado en una fila y decide si esa fila se muestra: es el filtro de <strong>lectura</strong>.
 *
 * <p>Los otros dos reciben un valor suelto y la columna donde va a escribirse, y son el filtro de
 * <strong>escritura</strong>: contestan si ese valor <em>seguiria</em> perteneciendo al conjunto
 * filtrado. Sirven para rechazar una insercion o una modificacion que haria desaparecer la fila del
 * propio filtro que la contiene — una fila que se escribe y en el acto se vuelve invisible es casi
 * siempre un error de quien la escribe.
 *
 * <h2>Un filtro es un corte, no una consulta</h2>
 *
 * <p>El conjunto sigue teniendo todas las filas: el filtro solo esconde las que no pasan. Sacarlo
 * las vuelve a mostrar, sin volver a consultar el origen. Es la diferencia con cambiar el
 * {@code WHERE} de la consulta, que obligaria a reconectarse.
 *
 * @since 1.5
 */
public interface Predicate {

    /**
     * Si la fila actual del conjunto pasa el filtro.
     *
     * @param rs el conjunto, posicionado en la fila a evaluar
     * @return {@code true} si la fila se muestra
     */
    boolean evaluate(RowSet rs);

    /**
     * Si ese valor seria aceptable en esa columna.
     *
     * @param value el valor
     * @param column la columna, desde 1
     * @return {@code true} si el valor pasa el filtro
     * @throws SQLException si la columna no existe
     */
    boolean evaluate(Object value, int column) throws SQLException;

    /**
     * Si ese valor seria aceptable en esa columna.
     *
     * @param value el valor
     * @param columnName el nombre de la columna
     * @return {@code true} si el valor pasa el filtro
     * @throws SQLException si la columna no existe
     */
    boolean evaluate(Object value, String columnName) throws SQLException;
}
