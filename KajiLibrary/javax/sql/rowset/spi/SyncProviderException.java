package javax.sql.rowset.spi;

import java.sql.SQLException;

/**
 * La sincronizacion fallo, y adentro viene <strong>que</strong> fallo.
 *
 * <h2>Por que esta excepcion lleva datos</h2>
 *
 * <p>Porque un fallo de sincronizacion casi nunca es total. De doscientas filas modificadas, ciento
 * noventa y ocho se escribieron bien y dos chocaron con cambios de otro. Una excepcion con un
 * mensaje obligaria a rehacer todo; el {@link SyncResolver} que viene adentro permite resolver
 * <strong>solo esas dos</strong> y volver a intentar.
 *
 * <p>Es la razon de que {@code acceptChanges} lance esto y no una {@code SQLException} comun: el
 * que llama necesita las filas, no un mensaje.
 *
 * @since 1.5
 */
public class SyncProviderException extends SQLException {

    private static final long serialVersionUID = -3985215347103826532L;

    private SyncResolver syncResolver;

    /** Sin detalle y sin resolvedor. */
    public SyncProviderException() {
        super();
    }

    /**
     * Con un mensaje.
     *
     * @param msg el mensaje
     */
    public SyncProviderException(String msg) {
        super(msg);
    }

    /**
     * Con el resolvedor que trae las filas en conflicto.
     *
     * @param syncResolver el resolvedor
     * @throws IllegalArgumentException si es {@code null}
     */
    public SyncProviderException(SyncResolver syncResolver) {
        super();
        if (syncResolver == null) {
            throw new IllegalArgumentException("el SyncResolver no puede ser null");
        }
        this.syncResolver = syncResolver;
    }

    /**
     * El resolvedor con las filas en conflicto.
     *
     * @return el resolvedor, o {@code null} si esta excepcion no trae ninguno
     */
    public SyncResolver getSyncResolver() {
        return syncResolver;
    }

    /**
     * Fija el resolvedor.
     *
     * @param syncResolver el resolvedor
     * @throws IllegalArgumentException si es {@code null}
     */
    public void setSyncResolver(SyncResolver syncResolver) {
        if (syncResolver == null) {
            throw new IllegalArgumentException("el SyncResolver no puede ser null");
        }
        this.syncResolver = syncResolver;
    }
}
