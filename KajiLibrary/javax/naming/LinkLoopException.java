package javax.naming;

/**
 * Se lanza cuando seguir los enlaces vuelve a pasar por el mismo punto, o cuando se paso el
 * limite de saltos. Sin esto, un enlace que se apunta a si mismo colgaria al que resuelve.
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public class LinkLoopException extends LinkException {

    private static final long serialVersionUID = -3119189944325198009L;

    public LinkLoopException(String explanation) {
        super(explanation);
    }

    public LinkLoopException() {
        super();
    }
}
