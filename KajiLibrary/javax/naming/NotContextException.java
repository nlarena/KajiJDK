package javax.naming;

/**
 * Se lanza cuando la resolucion llego a un objeto que no es un contexto y todavia quedaba
 * nombre por resolver --o cuando se pidio una operacion de contexto sobre algo que no lo es--.
 * Resolver `a/b/c` donde `a/b` es un archivo y no un directorio da esto.
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public class NotContextException extends NamingException {

    private static final long serialVersionUID = 849752551644540417L;

    public NotContextException(String explanation) {
        super(explanation);
    }

    public NotContextException() {
        super();
    }
}
