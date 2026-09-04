package java.rmi;

/**
 * KajiLibrary's java.rmi.RMISecurityException -- obsoleta desde 1.2.
 *
 * <p>Ya no la lanza nadie: donde antes salia esta, ahora sale {@link SecurityException} a secas. Se
 * mantiene solo para que el codigo viejo compile.
 *
 * <p>El segundo constructor toma dos cadenas y nunca quedo claro que significaba la segunda; tampoco
 * importa ya.
 */
@Deprecated
public class RMISecurityException extends SecurityException {

    private static final long serialVersionUID = -8433406075740433514L;

    /** @param name el mensaje */
    @Deprecated
    public RMISecurityException(String name) {
        super(name);
    }

    /**
     * @param name el mensaje
     * @param arg sin uso
     */
    @Deprecated
    public RMISecurityException(String name, String arg) {
        this(name);
    }
}
