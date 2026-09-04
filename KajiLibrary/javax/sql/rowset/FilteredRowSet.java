package javax.sql.rowset;

import java.sql.SQLException;

/**
 * Un {@link WebRowSet} con un filtro puesto: solo se ven las filas que el filtro acepta.
 *
 * <h2>Por que filtrar en el cliente y no en la consulta</h2>
 *
 * <p>Porque el conjunto ya esta desconectado. Cambiar el {@code WHERE} obligaria a volver a la base;
 * poner un filtro es inmediato y no cuesta una conexion. Para recorrer los mismos datos con varios
 * criterios —lo que hace una interfaz con columnas ordenables y cajas de busqueda— es la diferencia
 * entre una consulta por interaccion y ninguna.
 *
 * <h2>El filtro esconde, no borra</h2>
 *
 * <p>Las filas que no pasan siguen ahi. Sacar el filtro las vuelve a mostrar, y
 * {@link CachedRowSet#acceptChanges} sincroniza <strong>todas</strong> las modificadas, incluidas
 * las que el filtro estaba escondiendo. Pensarlo como un borrado lleva a perder cambios de vista y
 * a sorprenderse cuando aparecen en la base.
 *
 * @since 1.5
 */
public interface FilteredRowSet extends WebRowSet {

    /**
     * Pone o cambia el filtro.
     *
     * @param p el filtro, o {@code null} para sacarlo
     * @throws SQLException si no se pudo aplicar
     */
    void setFilter(Predicate p) throws SQLException;

    /**
     * El filtro puesto.
     *
     * @return el filtro, o {@code null} si no hay
     */
    Predicate getFilter();
}
