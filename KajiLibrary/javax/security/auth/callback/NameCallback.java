package javax.security.auth.callback;

/**
 * KajiLibrary's javax.security.auth.callback.NameCallback -- pide un nombre de usuario.
 *
 * <p>El nombre por omision, cuando lo hay, es una <b>sugerencia</b> y no una respuesta: quien
 * contesta decide si la usa. Por eso {@link #getName()} devuelve null hasta que alguien llame a
 * {@link #setName} aunque haya default -- confundir los dos es leer un nombre que nadie confirmo.
 */
public class NameCallback implements Callback, java.io.Serializable {

    private static final long serialVersionUID = 3770938795909392253L;

    private final String prompt;
    private final String defaultName;
    private String inputName;

    /**
     * @throws IllegalArgumentException si el prompt es null o vacio: un prompt vacio deja al usuario
     *     sin saber que se le esta pidiendo
     */
    public NameCallback(String prompt) {
        if (prompt == null || prompt.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.prompt = prompt;
        this.defaultName = null;
    }

    /**
     * @throws IllegalArgumentException si el prompt o el nombre por omision son null o vacios
     */
    public NameCallback(String prompt, String defaultName) {
        if (prompt == null || prompt.length() == 0
                || defaultName == null || defaultName.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.prompt = prompt;
        this.defaultName = defaultName;
    }

    public String getPrompt() {
        return this.prompt;
    }

    /** La sugerencia, o null si no hay. No es la respuesta; ver la nota de la clase. */
    public String getDefaultName() {
        return this.defaultName;
    }

    public void setName(String name) {
        this.inputName = name;
    }

    /** El nombre que contestaron, o null si todavia nadie contesto. */
    public String getName() {
        return this.inputName;
    }
}
