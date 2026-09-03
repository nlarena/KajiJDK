package javax.naming;

/**
 * Se lanza cuando la operacion devolvio un resultado incompleto porque no se pudo seguir --por
 * ejemplo, una parte del arbol vive en otro servidor al que no se llego--. Lo ya entregado sirve;
 * lo que falta no se sabe.
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public class PartialResultException extends NamingException {

    private static final long serialVersionUID = 2572144970049426786L;

    public PartialResultException(String explanation) {
        super(explanation);
    }

    public PartialResultException() {
        super();
    }
}
