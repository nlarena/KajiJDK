package javax.security.auth.callback;

/**
 * KajiLibrary's javax.security.auth.callback.ChoiceCallback -- asks to choose from a list.
 *
 * <p>It serves for what cannot be typed by hand: which of the store's certificates to use, which of
 * three domains to authenticate with. The list is built by whoever asks, and whoever answers
 * returns <b>indices</b>, not texts: that way there is no need to look up again which one was
 * chosen.
 *
 * <h2>Multiple selection is decided at construction</h2>
 *
 * <p>{@code allowMultipleSelections} is final and not a plea: if it is false,
 * {@link #setSelectedIndexes} throws {@code UnsupportedOperationException} instead of keeping the
 * first. Whoever asked said "only one" and keeping one of several would be choosing for them.
 *
 * <p>{@link #setSelectedIndex}, on the other hand, always works -- a single choice fits both forms
 * -- and leaves a one-element array.
 */
public class ChoiceCallback implements Callback, java.io.Serializable {

    private static final long serialVersionUID = -3975664071579892167L;

    private final String prompt;
    private final String[] choices;
    private final int defaultChoice;
    private final boolean multipleSelectionsAllowed;
    private int[] selections;

    /**
     * @param defaultChoice the suggested index; it has to fall inside the list
     * @throws IllegalArgumentException if the prompt is null or empty, if the list is empty, if any
     *     choice is null or empty, or if the default falls outside
     */
    public ChoiceCallback(String prompt, String[] choices, int defaultChoice,
            boolean multipleSelectionsAllowed) {
        if (prompt == null || prompt.length() == 0 || choices == null || choices.length == 0
                || defaultChoice < 0 || defaultChoice >= choices.length) {
            throw new IllegalArgumentException();
        }
        int i = 0;
        while (i < choices.length) {
            // An empty choice would look like a blank line in the list: the user would not know
            // what they are choosing.
            if (choices[i] == null || choices[i].length() == 0) {
                throw new IllegalArgumentException();
            }
            i = i + 1;
        }
        this.prompt = prompt;
        this.choices = copy(choices);
        this.defaultChoice = defaultChoice;
        this.multipleSelectionsAllowed = multipleSelectionsAllowed;
    }

    public String getPrompt() {
        return this.prompt;
    }

    /**
     * The choices. A copy: touching what comes out of here does not change the list asked about.
     */
    public String[] getChoices() {
        return copy(this.choices);
    }

    public int getDefaultChoice() {
        return this.defaultChoice;
    }

    public boolean allowMultipleSelections() {
        return this.multipleSelectionsAllowed;
    }

    /** Answers with a single choice. It works with or without multiple selection. */
    public void setSelectedIndex(int selection) {
        this.selections = new int[] {selection};
    }

    /**
     * Answers with several.
     *
     * @throws UnsupportedOperationException if it was built with single selection. See the class
     *     note: keeping one of several would be choosing for whoever asked
     */
    public void setSelectedIndexes(int[] selections) {
        if (!this.multipleSelectionsAllowed) {
            throw new UnsupportedOperationException();
        }
        this.selections = selections == null ? null : copyInts(selections);
    }

    /** The chosen indices, or null if nobody answered yet. */
    public int[] getSelectedIndexes() {
        return this.selections == null ? null : copyInts(this.selections);
    }

    private static String[] copy(String[] a) {
        String[] c = new String[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }

    private static int[] copyInts(int[] a) {
        int[] c = new int[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }
}
