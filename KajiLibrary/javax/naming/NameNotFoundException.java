package javax.naming;

/**
 * Se lanza cuando el componente que se estaba resolviendo no esta atado a nada. El nombre
 * resuelto y el restante de la excepcion dicen exactamente donde se corto.
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public class NameNotFoundException extends NamingException {

    private static final long serialVersionUID = -8007156725367842053L;

    public NameNotFoundException(String explanation) {
        super(explanation);
    }

    public NameNotFoundException() {
        super();
    }
}
