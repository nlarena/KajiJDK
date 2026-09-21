package javax.security.auth.callback;

/**
 * KajiLibrary's javax.security.auth.callback.NameCallback -- asks for a user name.
 *
 * <p>The default name, when there is one, is a <b>suggestion</b> and not an answer: whoever answers
 * decides whether to use it. That is why {@link #getName()} returns null until somebody calls
 * {@link #setName} even if there is a default -- confusing the two is reading a name nobody
 * confirmed.
 */
public class NameCallback implements Callback, java.io.Serializable {

    private static final long serialVersionUID = 3770938795909392253L;

    private final String prompt;
    private final String defaultName;
    private String inputName;

    /**
     * @throws IllegalArgumentException if the prompt is null or empty: an empty prompt leaves the
     *     user not knowing what is being asked of them
     */
    public NameCallback(String prompt) {
        if (prompt == null || prompt.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.prompt = prompt;
        this.defaultName = null;
    }

    /**
     * @throws IllegalArgumentException if the prompt or the default name is null or empty
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

    /** The suggestion, or null if there is none. It is not the answer; see the class note. */
    public String getDefaultName() {
        return this.defaultName;
    }

    public void setName(String name) {
        this.inputName = name;
    }

    /** The name that was answered, or null if nobody answered yet. */
    public String getName() {
        return this.inputName;
    }
}
