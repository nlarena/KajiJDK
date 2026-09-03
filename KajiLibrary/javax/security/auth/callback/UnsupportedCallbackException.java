package javax.security.auth.callback;

/**
 * KajiLibrary's javax.security.auth.callback.UnsupportedCallbackException -- ese callback no se sabe
 * contestar.
 *
 * <p>Lleva <b>cual</b>, y no solo un mensaje: quien la atrapa suele tener varios callbacks en vuelo
 * y necesita saber cual quedo sin contestar para decidir si puede seguir sin el. Ver
 * {@link CallbackHandler} para la diferencia con un fallo de entrada/salida.
 */
public class UnsupportedCallbackException extends Exception {

    private static final long serialVersionUID = -6873556327310220378L;

    private final Callback callback;

    public UnsupportedCallbackException(Callback callback) {
        super();
        this.callback = callback;
    }

    public UnsupportedCallbackException(Callback callback, String msg) {
        super(msg);
        this.callback = callback;
    }

    /** El callback que no se pudo contestar. */
    public Callback getCallback() {
        return this.callback;
    }
}
