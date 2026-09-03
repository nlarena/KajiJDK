package javax.naming;

/**
 * Se lanza cuando lo que hay en el enlace no es un nombre valido. La tira, entre otros,
 * `LinkRef.getLinkName()` cuando la referencia no tiene la direccion `LinkAddress` que deberia.
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public class MalformedLinkException extends LinkException {

    private static final long serialVersionUID = -3066740437737830242L;

    public MalformedLinkException(String explanation) {
        super(explanation);
    }

    public MalformedLinkException() {
        super();
    }
}
