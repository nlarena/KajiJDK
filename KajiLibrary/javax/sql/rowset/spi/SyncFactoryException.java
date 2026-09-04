package javax.sql.rowset.spi;

import java.sql.SQLException;

/**
 * La fabrica de proveedores no pudo entregar lo que se le pidio.
 *
 * <p>Se distingue de {@link SyncProviderException} en <strong>cuando</strong> pasa: esta es al
 * conseguir el proveedor —no esta registrado, la clase no carga, el nombre esta mal— y la otra es
 * despues, mientras el proveedor ya conseguido sincroniza. Una es de configuracion, la otra de
 * datos.
 *
 * @since 1.5
 */
public class SyncFactoryException extends SQLException {

    private static final long serialVersionUID = -4354595476433200352L;

    /** Sin detalle. */
    public SyncFactoryException() {
        super();
    }

    /**
     * Con un mensaje.
     *
     * @param msg el mensaje
     */
    public SyncFactoryException(String msg) {
        super(msg);
    }
}
