package javax.security.auth.callback;

/**
 * KajiLibrary's javax.security.auth.callback.ConfirmationCallback -- asks for a confirmation.
 *
 * <p>It has two modes and it is as well to tell them apart before reading anything else, because
 * the same constants mean different things in each:
 *
 * <ul>
 *   <li><b>With {@code optionType}</b> -- the set of buttons is chosen by whoever asks among three
 *       predefined ones, and the answer is one of the constants {@link #YES}, {@link #NO},
 *       {@link #CANCEL} or {@link #OK}. Which of them is valid depends on the set: asking for
 *       {@link #OK_CANCEL_OPTION} and answering {@code YES} is an error. (The note said four; the
 *       fourth option type, {@link #UNSPECIFIED_OPTION}, is the other mode.)
 *   <li><b>With its own options</b> -- whoever asks passes the texts and the answer is an
 *       <b>index</b> into that array. In this mode {@link #getOptionType()} returns
 *       {@link #UNSPECIFIED_OPTION}, which is how the two are told apart from outside.
 * </ul>
 *
 * <h2>Two surprising details</h2>
 *
 * <ol>
 *   <li>{@link #getSelectedIndex()} <b>does not throw</b> if nobody answered yet: it returns 0. And
 *       0 is {@link #YES} and also the first index of an own list, so it is a valid answer. Whoever
 *       needs to tell "they did not answer" from "they said yes" has to keep track on their side;
 *       it is the JDK's and there is no way around it from the API.
 *   <li>The default is validated <b>against the chosen set</b>. It is the only thing that prevents
 *       building a callback that asks "yes or no" and suggests "cancel".
 * </ol>
 */
public class ConfirmationCallback implements Callback, java.io.Serializable {

    private static final long serialVersionUID = -9095656433782481624L;

    /** There is no predefined set: the options are its own. See the class note. */
    public static final int UNSPECIFIED_OPTION = -1;

    /** Yes / No. */
    public static final int YES_NO_OPTION = 0;

    /** Yes / No / Cancel. */
    public static final int YES_NO_CANCEL_OPTION = 1;

    /** OK / Cancel. */
    public static final int OK_CANCEL_OPTION = 2;

    /** The "yes" answer. It is 0, which is also the first index of an own list. */
    public static final int YES = 0;

    /** The "no" answer. */
    public static final int NO = 1;

    /** The "cancel" answer. */
    public static final int CANCEL = 2;

    /** The "OK" answer. */
    public static final int OK = 3;

    /** Severity: informative. */
    public static final int INFORMATION = 0;

    /** Severity: warning. */
    public static final int WARNING = 1;

    /** Severity: error. */
    public static final int ERROR = 2;

    private final String prompt;
    private final int messageType;
    private final int optionType;
    private final String[] options;
    private final int defaultOption;
    private int selection;

    /** With a predefined set and no prompt text. */
    public ConfirmationCallback(int messageType, int optionType, int defaultOption) {
        this(null, false, messageType, optionType, null, defaultOption, false);
    }

    /** With its own options and no prompt text. */
    public ConfirmationCallback(int messageType, String[] options, int defaultOption) {
        this(null, false, messageType, UNSPECIFIED_OPTION, options, defaultOption, true);
    }

    /** With a predefined set and prompt text. */
    public ConfirmationCallback(String prompt, int messageType, int optionType,
            int defaultOption) {
        this(prompt, true, messageType, optionType, null, defaultOption, false);
    }

    /** With its own options and prompt text. */
    public ConfirmationCallback(String prompt, int messageType, String[] options,
            int defaultOption) {
        this(prompt, true, messageType, UNSPECIFIED_OPTION, options, defaultOption, true);
    }

    private ConfirmationCallback(String prompt, boolean hasPrompt, int messageType, int optionType,
            String[] options, int defaultOption, boolean ownOptions) {
        // The prompt is optional for the object but **not** for the constructor that asks for it:
        // the two that carry it require a real text, and the other two pass null on purpose.
        // Without this distinction, passing null to the one that asks for it would be accepted
        // silently and the user would see a confirmation with no question.
        if (hasPrompt && (prompt == null || prompt.length() == 0)) {
            throw new IllegalArgumentException("Invalid prompt");
        }
        if (messageType != INFORMATION && messageType != WARNING && messageType != ERROR) {
            throw new IllegalArgumentException("Invalid msgType");
        }
        if (ownOptions) {
            if (options == null || options.length == 0 || defaultOption < 0
                    || defaultOption >= options.length) {
                throw new IllegalArgumentException("Invalid options and/or default option");
            }
            int i = 0;
            while (i < options.length) {
                if (options[i] == null || options[i].length() == 0) {
                    throw new IllegalArgumentException("Invalid option value");
                }
                i = i + 1;
            }
            this.options = copy(options);
        } else {
            if (optionType != YES_NO_OPTION && optionType != YES_NO_CANCEL_OPTION
                    && optionType != OK_CANCEL_OPTION) {
                throw new IllegalArgumentException("Invalid optionType");
            }
            // The default has to be an answer THAT set of buttons can give. It is the only thing
            // that prevents asking "yes or no" and suggesting "cancel".
            if (!validDefault(optionType, defaultOption)) {
                throw new IllegalArgumentException("Invalid default option");
            }
            this.options = null;
        }
        this.prompt = prompt;
        this.messageType = messageType;
        this.optionType = optionType;
        this.defaultOption = defaultOption;
    }

    private static boolean validDefault(int optionType, int defaultOption) {
        if (optionType == YES_NO_OPTION) {
            return defaultOption == YES || defaultOption == NO;
        }
        if (optionType == YES_NO_CANCEL_OPTION) {
            return defaultOption == YES || defaultOption == NO || defaultOption == CANCEL;
        }
        return defaultOption == OK || defaultOption == CANCEL;
    }

    /** The question's text, or null if none was given. */
    public String getPrompt() {
        return this.prompt;
    }

    public int getMessageType() {
        return this.messageType;
    }

    /** The set of buttons, or {@link #UNSPECIFIED_OPTION} if the options are its own. */
    public int getOptionType() {
        return this.optionType;
    }

    /** The own options, or null if a predefined set was used. A copy. */
    public String[] getOptions() {
        return this.options == null ? null : copy(this.options);
    }

    public int getDefaultOption() {
        return this.defaultOption;
    }

    /** Answers. It is one of the constants, or an index if the options are its own. */
    public void setSelectedIndex(int selection) {
        this.selection = selection;
    }

    /** What was answered. <b>0 if nobody answered yet</b>; see the class note. */
    public int getSelectedIndex() {
        return this.selection;
    }

    private static String[] copy(String[] a) {
        String[] c = new String[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }
}
