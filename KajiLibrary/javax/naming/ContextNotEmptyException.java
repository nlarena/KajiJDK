package javax.naming;

/**
 * Se lanza al intentar destruir un contexto que todavia tiene cosas adentro. JNDI no borra en
 * cascada: el que llama tiene que vaciarlo primero.
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public class ContextNotEmptyException extends NamingException {

    private static final long serialVersionUID = 1090963683348219877L;

    public ContextNotEmptyException(String explanation) {
        super(explanation);
    }

    public ContextNotEmptyException() {
        super();
    }
}
