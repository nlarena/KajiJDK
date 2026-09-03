package javax.naming;

/**
 * Se lanza cuando se quiso atar un nombre que ya esta atado. Es la razon por la que existe
 * `rebind`: `bind` se niega a pisar, `rebind` pisa.
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public class NameAlreadyBoundException extends NamingException {

    private static final long serialVersionUID = -8491441000356780586L;

    public NameAlreadyBoundException(String explanation) {
        super(explanation);
    }

    public NameAlreadyBoundException() {
        super();
    }
}
