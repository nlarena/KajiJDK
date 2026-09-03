package javax.security.auth;

/**
 * KajiLibrary's javax.security.auth.RefreshFailedException -- no se pudo renovar una credencial.
 *
 * <p>Es la companera de {@link DestroyFailedException} en el otro extremo de la vida de una
 * credencial: una no se pudo borrar, la otra no se pudo renovar. La renovacion que importa es la de
 * los tickets con vencimiento -- un ticket de Kerberos, por caso -- y fallar ahi no es lo mismo que
 * fallar al usarlo: la credencial vieja puede seguir sirviendo un rato mas.
 */
public class RefreshFailedException extends Exception {

    private static final long serialVersionUID = 5058444488565265840L;

    public RefreshFailedException() {
        super();
    }

    public RefreshFailedException(String msg) {
        super(msg);
    }
}
