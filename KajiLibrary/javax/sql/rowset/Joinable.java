package javax.sql.rowset;

import java.sql.SQLException;

/**
 * Lo que un {@code RowSet} tiene que saber para poder participar de un {@link JoinRowSet}.
 *
 * <h2>Que es una columna de coincidencia</h2>
 *
 * <p>La columna por la cual este conjunto se une a otro: el equivalente del {@code ON} de un
 * {@code JOIN} de SQL. Un conjunto de empleados con la columna {@code id_departamento} marcada se
 * puede unir a uno de departamentos con {@code id} marcada.
 *
 * <p>La marca vive en el conjunto y no en la union, y esa es la decision de diseno de esta
 * interfaz: cada conjunto declara por donde se deja unir, y el {@link JoinRowSet} solo los junta.
 * Asi un mismo conjunto entra en varias uniones sin repetir la configuracion.
 *
 * <h2>Por que se puede marcar mas de una</h2>
 *
 * <p>Porque una clave puede ser compuesta. Las versiones que toman arreglos fijan varias columnas
 * de una, y el orden importa: se corresponden posicion a posicion con las del otro conjunto.
 *
 * <h2>Marcar por nombre o por indice</h2>
 *
 * <p>Las dos formas existen y no son equivalentes en el peor caso: un conjunto sin metadatos de
 * nombre no puede resolver el nombre, y ahi la version por indice es la unica que sirve.
 *
 * @since 1.5
 */
public interface Joinable {

    /**
     * Marca una columna como de coincidencia.
     *
     * @param columnIdx la columna, desde 1
     * @throws SQLException si el indice no es valido
     */
    void setMatchColumn(int columnIdx) throws SQLException;

    /**
     * Marca varias columnas, en orden.
     *
     * @param columnIdxes las columnas, desde 1
     * @throws SQLException si algun indice no es valido
     */
    void setMatchColumn(int[] columnIdxes) throws SQLException;

    /**
     * Marca una columna por nombre.
     *
     * @param columnName el nombre
     * @throws SQLException si el nombre no existe
     */
    void setMatchColumn(String columnName) throws SQLException;

    /**
     * Marca varias columnas por nombre, en orden.
     *
     * @param columnNames los nombres
     * @throws SQLException si algun nombre no existe
     */
    void setMatchColumn(String[] columnNames) throws SQLException;

    /**
     * Los indices de las columnas marcadas.
     *
     * @return los indices
     * @throws SQLException si no hay ninguna marcada
     */
    int[] getMatchColumnIndexes() throws SQLException;

    /**
     * Los nombres de las columnas marcadas.
     *
     * @return los nombres
     * @throws SQLException si no hay ninguna marcada
     */
    String[] getMatchColumnNames() throws SQLException;

    /**
     * Desmarca una columna.
     *
     * @param columnIdx la columna, desde 1
     * @throws SQLException si esa columna no estaba marcada
     */
    void unsetMatchColumn(int columnIdx) throws SQLException;

    /**
     * Desmarca varias columnas.
     *
     * @param columnIdxes las columnas, desde 1
     * @throws SQLException si alguna no estaba marcada
     */
    void unsetMatchColumn(int[] columnIdxes) throws SQLException;

    /**
     * Desmarca una columna por nombre.
     *
     * @param columnName el nombre
     * @throws SQLException si esa columna no estaba marcada
     */
    void unsetMatchColumn(String columnName) throws SQLException;

    /**
     * Desmarca varias columnas por nombre.
     *
     * @param columnName los nombres
     * @throws SQLException si alguna no estaba marcada
     */
    void unsetMatchColumn(String[] columnName) throws SQLException;
}
