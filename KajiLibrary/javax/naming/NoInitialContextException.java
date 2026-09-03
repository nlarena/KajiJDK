package javax.naming;

/**
 * Se lanza cuando no hay proveedor inicial que pueda atender la operacion.

 * <p>En este JDK es la excepcion mas probable de todo el paquete: sin ninguna implementacion de
 * `javax.naming.spi.InitialContextFactory` instalada, **todas** las operaciones de
 * `InitialContext` terminan aca. Eso no es una limitacion de KajiLibrary sino el comportamiento
 * del JDK real en las mismas condiciones; ver la cabecera de `InitialContext`.
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public class NoInitialContextException extends NamingException {

    private static final long serialVersionUID = -3413733186901258623L;

    public NoInitialContextException(String explanation) {
        super(explanation);
    }

    public NoInitialContextException() {
        super();
    }
}
