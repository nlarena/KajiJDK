package javax.sql.rowset.spi;

import javax.sql.RowSetReader;
import javax.sql.RowSetWriter;

/**
 * Quien sabe llevar y traer los datos entre un {@code RowSet} desconectado y su origen.
 *
 * <h2>El problema que resuelve</h2>
 *
 * <p>Un {@code CachedRowSet} se llena, se desconecta, viaja, se modifica y vuelve. Entre que se leyo
 * y que se escribe pueden pasar minutos, y en ese rato otro pudo cambiar las mismas filas. Alguien
 * tiene que decidir que hacer con eso, y ese alguien es el proveedor de sincronizacion.
 *
 * <h2>Los grados, que son una escala de cuanto se controla</h2>
 *
 * <p>{@link #GRADE_NONE} no chequea nada: lo que se escribe pisa lo que haya. Los dos
 * {@code GRADE_CHECK_*} comparan al escribir —solo las filas modificadas, o todas— y fallan si algo
 * cambio. Los dos {@code GRADE_LOCK_*} directamente impiden que cambie, tomando candados en el
 * origen.
 *
 * <p>La escala es de menor a mayor seguridad y tambien de menor a mayor costo, y esa es la decision
 * que hay detras: los candados dan la garantia mas fuerte y son los que peor escalan, porque
 * mantienen bloqueado el origen mientras el {@code RowSet} anda desconectado por ahi. Por eso el
 * grado por omision de los proveedores es de chequeo y no de candado.
 *
 * <h2>Por que un {@code RowSet} no habla directo con la base</h2>
 *
 * <p>Porque asi el mismo {@code RowSet} sirve para una base SQL, para un archivo XML o para lo que
 * sea: cambiar el proveedor cambia el origen sin tocar el codigo que usa las filas. El lector y el
 * escritor que devuelven {@link #getRowSetReader} y {@link #getRowSetWriter} son las dos mitades de
 * ese acoplamiento.
 *
 * @since 1.5
 */
public abstract class SyncProvider {

    /** No se chequea nada al escribir. */
    public static final int GRADE_NONE = 1;

    /** Al escribir se chequea que las filas modificadas no hayan cambiado en el origen. */
    public static final int GRADE_CHECK_MODIFIED_AT_COMMIT = 2;

    /** Al escribir se chequean todas las filas, no solo las modificadas. */
    public static final int GRADE_CHECK_ALL_AT_COMMIT = 3;

    /** Se toma un candado sobre las filas al modificarlas. */
    public static final int GRADE_LOCK_WHEN_MODIFIED = 4;

    /** Se toma un candado sobre las filas al cargarlas. */
    public static final int GRADE_LOCK_WHEN_LOADED = 5;

    /** No se toma ningun candado en el origen. */
    public static final int DATASOURCE_NO_LOCK = 1;

    /** Se toman candados de fila. */
    public static final int DATASOURCE_ROW_LOCK = 2;

    /** Se toman candados de tabla. */
    public static final int DATASOURCE_TABLE_LOCK = 3;

    /** Se toma un candado sobre toda la base. */
    public static final int DATASOURCE_DB_LOCK = 4;

    /** El proveedor puede sincronizar contra una vista actualizable. */
    public static final int UPDATABLE_VIEW_SYNC = 5;

    /** El proveedor no puede sincronizar contra una vista. */
    public static final int NONUPDATABLE_VIEW_SYNC = 6;

    /** Para las subclases. */
    public SyncProvider() {
    }

    /**
     * El identificador unico de este proveedor, en forma de nombre de paquete invertido.
     *
     * <p>Es lo que se le pasa a {@link SyncFactory#getInstance} para pedirlo, asi que tiene que ser
     * unico entre todos los proveedores instalados.
     *
     * @return el identificador
     */
    public abstract String getProviderID();

    /**
     * El lector que llena el {@code RowSet} desde el origen.
     *
     * @return el lector
     */
    public abstract RowSetReader getRowSetReader();

    /**
     * El escritor que devuelve los cambios al origen.
     *
     * @return el escritor
     */
    public abstract RowSetWriter getRowSetWriter();

    /**
     * El grado de sincronizacion que este proveedor ofrece.
     *
     * @return una de las constantes {@code GRADE_}
     */
    public abstract int getProviderGrade();

    /**
     * Pide un nivel de candado en el origen.
     *
     * <p>Es un pedido, no una orden: un proveedor que no sepa tomar ese candado tiene que fallar y
     * no bajar en silencio a uno mas debil. Bajar sin avisar le daria al que llama una garantia que
     * cree tener y no tiene.
     *
     * @param datasourceLock una de las constantes {@code DATASOURCE_}
     * @throws SyncProviderException si el proveedor no soporta ese nivel
     */
    public abstract void setDataSourceLock(int datasourceLock) throws SyncProviderException;

    /**
     * El nivel de candado que se esta usando.
     *
     * @return una de las constantes {@code DATASOURCE_}
     * @throws SyncProviderException si no se pudo averiguar
     */
    public abstract int getDataSourceLock() throws SyncProviderException;

    /**
     * Si puede sincronizar contra una vista.
     *
     * @return {@link #UPDATABLE_VIEW_SYNC} o {@link #NONUPDATABLE_VIEW_SYNC}
     */
    public abstract int supportsUpdatableView();

    /**
     * La version de este proveedor.
     *
     * @return la version
     */
    public abstract String getVersion();

    /**
     * Quien lo hizo.
     *
     * @return el nombre del proveedor
     */
    public abstract String getVendor();
}
