package javax.security.auth.callback;

/**
 * KajiLibrary's javax.security.auth.callback.TextInputCallback -- pide un texto cualquiera.
 *
 * <p>Es el generico: sirve para lo que no tiene callback propio -- un codigo de un solo uso, el
 * nombre de un dominio, la respuesta a una pregunta de seguridad. Vale lo mismo que en
 * {@link NameCallback} sobre el texto por omision: es una sugerencia, no una respuesta.
 */
public class TextInputCallback implements Callback, java.io.Serializable {

    private static final long serialVersionUID = -8064222478852811804L;

    private final String prompt;
    private final String defaultText;
    private String inputText;

    /**
     * @throws IllegalArgumentException si el prompt es null o vacio
     */
    public TextInputCallback(String prompt) {
        if (prompt == null || prompt.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.prompt = prompt;
        this.defaultText = null;
    }

    /**
     * @throws IllegalArgumentException si el prompt o el texto por omision son null o vacios
     */
    public TextInputCallback(String prompt, String defaultText) {
        if (prompt == null || prompt.length() == 0
                || defaultText == null || defaultText.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.prompt = prompt;
        this.defaultText = defaultText;
    }

    public String getPrompt() {
        return this.prompt;
    }

    /** La sugerencia, o null si no hay. */
    public String getDefaultText() {
        return this.defaultText;
    }

    public void setText(String text) {
        this.inputText = text;
    }

    /** El texto que contestaron, o null si todavia nadie contesto. */
    public String getText() {
        return this.inputText;
    }
}
