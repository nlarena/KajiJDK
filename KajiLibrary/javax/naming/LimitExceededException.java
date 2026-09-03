package javax.naming;

/**
 * Se lanza cuando la operacion se corto por un limite pactado --de resultados o de tiempo-- y
 * no por un error.

 * <p>No es abstracta, a diferencia de `NamingSecurityException`: un proveedor puede haber chocado
 * con un limite que no es ni de tamano ni de tiempo, y entonces lanza esta. Lo que llega es
 * **parcial**, no vacio.
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public class LimitExceededException extends NamingException {

    private static final long serialVersionUID = -776898738660207856L;

    public LimitExceededException(String explanation) {
        super(explanation);
    }

    public LimitExceededException() {
        super();
    }
}
