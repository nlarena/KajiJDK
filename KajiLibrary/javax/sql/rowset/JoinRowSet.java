package javax.sql.rowset;

import java.sql.SQLException;
import java.util.Collection;

import javax.sql.RowSet;

/**
 * Un {@code RowSet} que es la union de varios otros, hecha <strong>sin base de datos</strong>.
 *
 * <h2>Para que sirve unir en el cliente</h2>
 *
 * <p>Para juntar datos que no estan en la misma base. Un conjunto viene de un sistema, otro de un
 * archivo XML, un tercero se armo a mano: ningun {@code JOIN} de SQL puede tocar a los tres, y este
 * si.
 *
 * <p>Tambien sirve cuando los datos ya se trajeron: unir en memoria evita una segunda consulta.
 *
 * <h2>Por que los conjuntos tienen que ser {@link Joinable}</h2>
 *
 * <p>Porque la union necesita saber <strong>por que columna</strong>. Un {@code Joinable} declara
 * su columna de coincidencia, y {@link #addRowSet(Joinable)} usa esa declaracion; las otras
 * sobrecargas la fijan en el momento, para un conjunto que no la traia puesta.
 *
 * <h2>Los tipos de union y el metodo que hay que llamar antes</h2>
 *
 * <p>Los cinco {@code supports*} contestan cuales estan implementados, y no es una formalidad: la
 * especificacion solo <strong>exige</strong> {@link #INNER_JOIN}. Los otros cuatro son opcionales, y
 * llamar a {@link #setJoinType} con uno que la implementacion no tiene falla. Preguntar primero es
 * el uso previsto.
 *
 * <h2>La union no se puede deshacer por partes</h2>
 *
 * <p>No hay un {@code removeRowSet}. Una vez agregado un conjunto, lo unico que queda es armar otro
 * {@code JoinRowSet}. Es una limitacion real de la interfaz y conviene saberla antes de disenar
 * alrededor de ella.
 *
 * @since 1.5
 */
public interface JoinRowSet extends WebRowSet {

    /** Producto cartesiano: cada fila de un lado con cada fila del otro. */
    int CROSS_JOIN = 0;

    /** Solo las filas que coinciden en los dos lados. Es el unico que la especificacion exige. */
    int INNER_JOIN = 1;

    /** Todas las del primero, con nulos donde el segundo no tenga coincidencia. */
    int LEFT_OUTER_JOIN = 2;

    /** Todas las del segundo, con nulos donde el primero no tenga coincidencia. */
    int RIGHT_OUTER_JOIN = 3;

    /** Todas las de los dos lados, con nulos donde falte la coincidencia. */
    int FULL_JOIN = 4;

    /**
     * Agrega un conjunto que ya tiene declarada su columna de coincidencia.
     *
     * @param rowset el conjunto
     * @throws SQLException si no declaro columna de coincidencia
     */
    void addRowSet(Joinable rowset) throws SQLException;

    /**
     * Agrega un conjunto y le fija la columna de coincidencia.
     *
     * @param rowset el conjunto
     * @param columnIdx la columna, desde 1
     * @throws SQLException si el indice no es valido
     */
    void addRowSet(RowSet rowset, int columnIdx) throws SQLException;

    /**
     * Agrega un conjunto y le fija la columna de coincidencia por nombre.
     *
     * @param rowset el conjunto
     * @param columnName el nombre de la columna
     * @throws SQLException si el nombre no existe
     */
    void addRowSet(RowSet rowset, String columnName) throws SQLException;

    /**
     * Agrega varios conjuntos de una, con sus columnas.
     *
     * <p>Los dos arreglos se corresponden posicion a posicion.
     *
     * @param rowset los conjuntos
     * @param columnIdx las columnas, una por conjunto
     * @throws SQLException si los arreglos no tienen el mismo largo o algun indice no es valido
     */
    void addRowSet(RowSet[] rowset, int[] columnIdx) throws SQLException;

    /**
     * Agrega varios conjuntos de una, con sus columnas por nombre.
     *
     * @param rowset los conjuntos
     * @param columnName los nombres, uno por conjunto
     * @throws SQLException si los arreglos no tienen el mismo largo o algun nombre no existe
     */
    void addRowSet(RowSet[] rowset, String[] columnName) throws SQLException;

    /**
     * Los conjuntos que participan de la union.
     *
     * @return la coleccion
     * @throws SQLException si no se pudo obtener
     */
    Collection<?> getRowSets() throws SQLException;

    /**
     * Los nombres de los conjuntos que participan.
     *
     * @return los nombres
     * @throws SQLException si no se pudo obtener
     */
    String[] getRowSetNames() throws SQLException;

    /**
     * El resultado de la union como un {@link CachedRowSet} comun.
     *
     * <p>Es la forma de sacar el resultado de aca: lo que se obtiene ya no recuerda de que
     * conjuntos salio y se comporta como cualquier otro conjunto desconectado.
     *
     * @return el resultado
     * @throws SQLException si no se pudo construir
     */
    CachedRowSet toCachedRowSet() throws SQLException;

    /**
     * Si esta implementacion soporta el producto cartesiano.
     *
     * @return si lo soporta
     */
    boolean supportsCrossJoin();

    /**
     * Si esta implementacion soporta la union interna.
     *
     * @return si lo soporta; la especificacion exige que si
     */
    boolean supportsInnerJoin();

    /**
     * Si esta implementacion soporta la union externa por izquierda.
     *
     * @return si lo soporta
     */
    boolean supportsLeftOuterJoin();

    /**
     * Si esta implementacion soporta la union externa por derecha.
     *
     * @return si lo soporta
     */
    boolean supportsRightOuterJoin();

    /**
     * Si esta implementacion soporta la union externa completa.
     *
     * @return si lo soporta
     */
    boolean supportsFullJoin();

    /**
     * Fija el tipo de union.
     *
     * @param joinType una de las cinco constantes
     * @throws SQLException si esta implementacion no soporta ese tipo
     */
    void setJoinType(int joinType) throws SQLException;

    /**
     * La union expresada como la clausula {@code WHERE} de SQL que le corresponde.
     *
     * <p>Sirve para mostrarle a alguien que se hizo, o para llevar la misma union a una base que
     * pueda ejecutarla mejor.
     *
     * @return la clausula
     * @throws SQLException si no se pudo construir
     */
    String getWhereClause() throws SQLException;

    /**
     * El tipo de union en uso.
     *
     * @return una de las cinco constantes
     * @throws SQLException si no se pudo consultar
     */
    int getJoinType() throws SQLException;
}
