package javax.security.auth.callback;

/**
 * KajiLibrary's javax.security.auth.callback.TextInputCallback -- asks for any text.
 *
 * <p>It is the generic one: it serves for what has no callback of its own -- a one-time code, a
 * domain's name, the answer to a security question. What was said in {@link NameCallback} about the
 * default text holds too: it is a suggestion, not an answer.
 */
public class TextInputCallback implements Callback, java.io.Serializable {

    private static final long serialVersionUID = -8064222478852811804L;

    private final String prompt;
    private final String defaultText;
    private String inputText;

    /**
     * @throws IllegalArgumentException if the prompt is null or empty
     */
    public TextInputCallback(String prompt) {
        if (prompt == null || prompt.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.prompt = prompt;
        this.defaultText = null;
    }

    /**
     * @throws IllegalArgumentException if the prompt or the default text is null or empty
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

    /** The suggestion, or null if there is none. */
    public String getDefaultText() {
        return this.defaultText;
    }

    public void setText(String text) {
        this.inputText = text;
    }

    /** The text that was answered, or null if nobody answered yet. */
    public String getText() {
        return this.inputText;
    }
}
