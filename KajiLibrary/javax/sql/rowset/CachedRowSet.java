package javax.sql.rowset;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Collection;

import javax.sql.RowSet;
import javax.sql.RowSetEvent;
import javax.sql.RowSetMetaData;
import javax.sql.rowset.spi.SyncProvider;
import javax.sql.rowset.spi.SyncProviderException;

/**
 * Un conjunto de filas que vive <strong>desconectado</strong> de la base.
 *
 * <h2>Que gana con desconectarse</h2>
 *
 * <p>Que no ocupa una conexion mientras alguien lo mira. Una conexion es un recurso escaso y caro; un
 * {@code ResultSet} comun la retiene desde que se abre hasta que se cierra, y en una aplicacion con
 * miles de usuarios eso no escala. Este conjunto se llena, suelta la conexion, y despues se puede
 * recorrer, modificar y hasta serializar y mandar por la red.
 *
 * <p>Tambien es {@code Serializable} y desplazable en los dos sentidos, cosas que un
 * {@code ResultSet} de solo avance no puede ofrecer justamente porque esta atado al cursor del
 * servidor.
 *
 * <h2>Lo que cuesta: los conflictos</h2>
 *
 * <p>Entre que se leyo y que se escribe pasa tiempo, y en ese tiempo otro pudo tocar las mismas
 * filas. Por eso {@link #acceptChanges} lanza {@link SyncProviderException} y no una
 * {@code SQLException} comun: adentro viene el
 * {@link javax.sql.rowset.spi.SyncResolver} con las filas que chocaron, para resolverlas una por
 * una en vez de perder el lote entero.
 *
 * <p>Ese es el nucleo del diseno: la desconexion no elimina el problema de la concurrencia, lo
 * <strong>mueve</strong> del servidor al cliente y lo hace explicito.
 *
 * <h2>Las filas originales</h2>
 *
 * <p>El conjunto guarda dos versiones de cada fila modificada: la que se cargo y la que el usuario
 * dejo. La original es lo que permite detectar conflictos —hay contra que comparar— y tambien lo
 * que hace posible {@link #undoUpdate} y {@link #restoreOriginal}. Sin ella, deshacer significaria
 * volver a consultar.
 *
 * <h2>La paginacion</h2>
 *
 * <p>{@link #setPageSize} y {@link #nextPage} sirven para un resultado que no entra en memoria: se
 * trae de a tantas filas. La contrapartida es que las paginas se leen en momentos distintos, asi que
 * dos paginas del mismo recorrido pueden no ser consistentes entre si.
 *
 * @since 1.5
 */
public interface CachedRowSet extends RowSet, Joinable {

    /**
     * Si {@link #acceptChanges()} confirma la transaccion por su cuenta.
     *
     * <p>Es {@code true} y es una constante, no una propiedad: el comportamiento esta fijado por la
     * especificacion.
     */
    boolean COMMIT_ON_ACCEPT_CHANGES = true;

    /**
     * Llena el conjunto con lo que haya en un {@code ResultSet} ya abierto.
     *
     * @param data el resultado del cual copiar
     * @throws SQLException si no se pudo leer
     */
    void populate(ResultSet data) throws SQLException;

    /**
     * Se conecta con la conexion dada, ejecuta la consulta y se desconecta.
     *
     * <p>La conexion la cierra el que llama, no este metodo: se la prestaron.
     *
     * @param conn la conexion a usar
     * @throws SQLException si la consulta fallo
     */
    void execute(Connection conn) throws SQLException;

    /**
     * Devuelve los cambios al origen.
     *
     * @throws SyncProviderException si hubo conflictos; trae adentro el resolvedor
     */
    void acceptChanges() throws SyncProviderException;

    /**
     * Devuelve los cambios al origen usando la conexion dada.
     *
     * @param con la conexion a usar
     * @throws SyncProviderException si hubo conflictos; trae adentro el resolvedor
     */
    void acceptChanges(Connection con) throws SyncProviderException;

    /**
     * Descarta todos los cambios y vuelve al contenido con el que se cargo.
     *
     * @throws SQLException si no se pudo restaurar
     */
    void restoreOriginal() throws SQLException;

    /**
     * Suelta el contenido, dejando el conjunto vacio pero con sus propiedades.
     *
     * @throws SQLException si no se pudo liberar
     */
    void release() throws SQLException;

    /**
     * Deshace el borrado de la fila actual.
     *
     * @throws SQLException si la fila actual no estaba borrada
     */
    void undoDelete() throws SQLException;

    /**
     * Deshace la insercion de la fila actual.
     *
     * @throws SQLException si la fila actual no era una insercion
     */
    void undoInsert() throws SQLException;

    /**
     * Deshace la modificacion de la fila actual.
     *
     * @throws SQLException si la fila actual no estaba modificada
     */
    void undoUpdate() throws SQLException;

    /**
     * Si esa columna de la fila actual fue modificada.
     *
     * @param idx la columna, desde 1
     * @return si cambio
     * @throws SQLException si el indice no es valido
     */
    boolean columnUpdated(int idx) throws SQLException;

    /**
     * Si esa columna de la fila actual fue modificada.
     *
     * @param columnName el nombre de la columna
     * @return si cambio
     * @throws SQLException si el nombre no existe
     */
    boolean columnUpdated(String columnName) throws SQLException;

    /**
     * El conjunto entero como una coleccion de filas.
     *
     * @return la coleccion
     * @throws SQLException si no se pudo construir
     */
    Collection<?> toCollection() throws SQLException;

    /**
     * Una columna entera como coleccion de valores.
     *
     * @param column la columna, desde 1
     * @return la coleccion
     * @throws SQLException si el indice no es valido
     */
    Collection<?> toCollection(int column) throws SQLException;

    /**
     * Una columna entera como coleccion de valores.
     *
     * @param column el nombre de la columna
     * @return la coleccion
     * @throws SQLException si el nombre no existe
     */
    Collection<?> toCollection(String column) throws SQLException;

    /**
     * El proveedor que sincroniza este conjunto con su origen.
     *
     * @return el proveedor
     * @throws SQLException si no se pudo obtener
     */
    SyncProvider getSyncProvider() throws SQLException;

    /**
     * Cambia el proveedor de sincronizacion.
     *
     * @param provider el identificador del proveedor
     * @throws SQLException si no esta registrado o no se pudo instanciar
     */
    void setSyncProvider(String provider) throws SQLException;

    /**
     * Cuantas filas hay.
     *
     * @return la cantidad
     */
    int size();

    /**
     * Fija los metadatos de las columnas.
     *
     * <p>Hace falta cuando el conjunto se llena a mano y no desde un {@code ResultSet}: sin
     * metadatos no hay nombres ni tipos de columna, y casi nada del resto de la interfaz funciona.
     *
     * @param md los metadatos
     * @throws SQLException si no se pudieron fijar
     */
    void setMetaData(RowSetMetaData md) throws SQLException;

    /**
     * El contenido original de todo el conjunto, como {@code ResultSet}.
     *
     * @return el contenido original
     * @throws SQLException si no se pudo construir
     */
    ResultSet getOriginal() throws SQLException;

    /**
     * El contenido original de la fila actual.
     *
     * @return la fila original
     * @throws SQLException si no hay fila actual
     */
    ResultSet getOriginalRow() throws SQLException;

    /**
     * Declara que la fila actual pasa a ser la original.
     *
     * <p>Es lo que se hace despues de sincronizar bien: lo que se acaba de escribir es ahora lo que
     * hay en el origen, asi que es contra eso que hay que comparar la proxima vez.
     *
     * @throws SQLException si no hay fila actual
     */
    void setOriginalRow() throws SQLException;

    /**
     * La tabla contra la cual se escriben los cambios.
     *
     * @return el nombre de la tabla
     * @throws SQLException si no se pudo obtener
     */
    String getTableName() throws SQLException;

    /**
     * Fija la tabla contra la cual escribir.
     *
     * <p>Hace falta cuando la consulta toco varias tablas: el conjunto no puede adivinar en cual
     * escribir, y sin esto {@link #acceptChanges} no tiene destino.
     *
     * @param tabName el nombre de la tabla
     * @throws SQLException si el nombre es invalido
     */
    void setTableName(String tabName) throws SQLException;

    /**
     * Las columnas que identifican una fila.
     *
     * @return los indices, desde 1
     * @throws SQLException si no se pudieron obtener
     */
    int[] getKeyColumns() throws SQLException;

    /**
     * Fija las columnas que identifican una fila.
     *
     * <p>Es lo que el escritor usa para armar el {@code WHERE} al actualizar. Sin claves tendria
     * que comparar todas las columnas, que es mas lento y falla con las que no se pueden comparar.
     *
     * @param keys los indices, desde 1
     * @throws SQLException si algun indice no es valido
     */
    void setKeyColumns(int[] keys) throws SQLException;

    /**
     * Otro conjunto que comparte los datos de este.
     *
     * <p>Comparten las filas y tienen <strong>cursores distintos</strong>: dos recorridos
     * independientes sobre los mismos datos, sin copiarlos. Modificar por uno se ve por el otro.
     *
     * @return el conjunto compartido
     * @throws SQLException si no se pudo crear
     */
    RowSet createShared() throws SQLException;

    /**
     * Una copia independiente, con datos y estado.
     *
     * @return la copia
     * @throws SQLException si no se pudo copiar
     */
    CachedRowSet createCopy() throws SQLException;

    /**
     * Una copia con las columnas pero sin las filas.
     *
     * @return la copia vacia
     * @throws SQLException si no se pudo copiar
     */
    CachedRowSet createCopySchema() throws SQLException;

    /**
     * Una copia sin las restricciones del original.
     *
     * <p>Sin la marca de solo lectura, sin el tipo de cursor, sin el nivel de aislamiento. Sirve
     * para trabajar con los datos sin arrastrar limitaciones que venian de como se los consulto.
     *
     * @return la copia sin restricciones
     * @throws SQLException si no se pudo copiar
     */
    CachedRowSet createCopyNoConstraints() throws SQLException;

    /**
     * Los avisos acumulados.
     *
     * @return el primero de la cadena, o {@code null}
     * @throws SQLException si no se pudieron obtener
     */
    RowSetWarning getRowSetWarnings() throws SQLException;

    /**
     * Si las filas borradas se siguen viendo al recorrer.
     *
     * @return si se muestran
     * @throws SQLException si no se pudo consultar
     */
    boolean getShowDeleted() throws SQLException;

    /**
     * Muestra o esconde las filas borradas.
     *
     * <p>Las filas borradas no desaparecen hasta sincronizar —hay que recordar que borrarlas para
     * escribirlo despues—, asi que la pregunta es solo si el recorrido pasa por ellas.
     *
     * @param b si mostrarlas
     * @throws SQLException si no se pudo cambiar
     */
    void setShowDeleted(boolean b) throws SQLException;

    /**
     * Confirma la transaccion de la conexion subyacente.
     *
     * @throws SQLException si no se pudo confirmar
     */
    void commit() throws SQLException;

    /**
     * Deshace la transaccion de la conexion subyacente.
     *
     * @throws SQLException si no se pudo deshacer
     */
    void rollback() throws SQLException;

    /**
     * Deshace hasta el punto de resguardo dado.
     *
     * @param s el punto de resguardo
     * @throws SQLException si no se pudo deshacer
     */
    void rollback(Savepoint s) throws SQLException;

    /**
     * Aviso de que el conjunto se lleno, con la fila donde arranco la pagina.
     *
     * @param event el evento
     * @param numRows la fila inicial de la pagina
     * @throws SQLException si no se pudo procesar
     */
    void rowSetPopulated(RowSetEvent event, int numRows) throws SQLException;

    /**
     * Llena el conjunto desde una fila en adelante.
     *
     * @param startRow la fila del resultado por la cual empezar, desde 1
     * @param rs el resultado del cual copiar
     * @throws SQLException si no se pudo leer
     */
    void populate(ResultSet rs, int startRow) throws SQLException;

    /**
     * Cuantas filas trae cada pagina.
     *
     * @param size el tamano; cero desactiva la paginacion
     * @throws SQLException si el tamano es negativo o supera el maximo de filas
     */
    void setPageSize(int size) throws SQLException;

    /**
     * El tamano de pagina.
     *
     * @return el tamano
     */
    int getPageSize();

    /**
     * Trae la pagina siguiente.
     *
     * @return {@code true} si habia otra
     * @throws SQLException si no se pudo leer
     */
    boolean nextPage() throws SQLException;

    /**
     * Trae la pagina anterior.
     *
     * @return {@code true} si habia otra
     * @throws SQLException si no se pudo leer
     */
    boolean previousPage() throws SQLException;
}
