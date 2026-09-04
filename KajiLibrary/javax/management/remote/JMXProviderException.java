package javax.management.remote;

import java.io.IOException;

/**
 * KajiLibrary's javax.management.remote.JMXProviderException -- hay proveedor para ese protocolo pero
 * no se pudo usar.
 *
 * <p>La distincion con {@code MalformedURLException} es la que hace util a esta clase, y es sutil:
 *
 * <ul>
 *   <li>si <b>no hay</b> proveedor para el protocolo, {@link JMXConnectorFactory} lanza
 *       {@code MalformedURLException} con "Unsupported protocol";
 *   <li>si <b>lo hay</b> y algo salio mal --no se pudo cargar la clase, no tiene el constructor
 *       esperado, fallo al armarse-- lanza esta.
 * </ul>
 *
 * <p>La primera significa "pediste algo que no existe"; esta significa "existe y esta roto". Un
 * programa que reintenta con otro protocolo solo deberia hacerlo con la primera.
 *
 * <p>Redefine {@link #getCause} porque es de 2003 y guarda la causa en un campo propio.
 */
public class JMXProviderException extends IOException {

    private static final long serialVersionUID = -3166703627550447198L;

    /** La original. */
    private Throwable cause = null;

    /** Sin detalle. */
    public JMXProviderException() {
    }

    /** Con mensaje. */
    public JMXProviderException(String message) {
        super(message);
    }

    /** Con mensaje y causa. */
    public JMXProviderException(String message, Throwable cause) {
        super(message);
        this.cause = cause;
    }

    /** La causa. */
    @Override
    public Throwable getCause() {
        return this.cause;
    }
}
