package javax.naming;

/**
 * Se lanza cuando la configuracion del cliente o del proveedor esta mal: una propiedad con un
 * valor imposible, una clase de fabrica que no se puede cargar, un `PROVIDER_URL` inservible.
 * Reintentar nunca ayuda; hay que cambiar la configuracion.
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public class ConfigurationException extends NamingException {

    private static final long serialVersionUID = -2535156726228855704L;

    public ConfigurationException(String explanation) {
        super(explanation);
    }

    public ConfigurationException() {
        super();
    }
}
