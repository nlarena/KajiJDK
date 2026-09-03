package javax.security.auth.callback;

/**
 * KajiLibrary's javax.security.auth.callback.TextOutputCallback -- el unico que no pregunta nada.
 *
 * <p>Lleva un mensaje para mostrarle al usuario y su gravedad. Existe por lo mismo que los demas:
 * un modulo de login que quiera avisar "la clave vence en tres dias" no puede escribir en la
 * terminal, porque no sabe si hay una. Lo manda por el mismo canal que sus preguntas.
 */
public class TextOutputCallback implements Callback, java.io.Serializable {

    private static final long serialVersionUID = 1689502495511663102L;

    /** Un aviso informativo. */
    public static final int INFORMATION = 0;

    /** Una advertencia. */
    public static final int WARNING = 1;

    /** Un error. */
    public static final int ERROR = 2;

    private final int messageType;
    private final String message;

    /**
     * @throws IllegalArgumentException si el tipo no es uno de los tres, o el mensaje es null o
     *     vacio: un mensaje vacio no le dice nada a nadie
     */
    public TextOutputCallback(int messageType, String message) {
        if ((messageType != INFORMATION && messageType != WARNING && messageType != ERROR)
                || message == null || message.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.messageType = messageType;
        this.message = message;
    }

    public int getMessageType() {
        return this.messageType;
    }

    public String getMessage() {
        return this.message;
    }
}
