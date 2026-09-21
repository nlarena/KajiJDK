package javax.security.auth.callback;

/**
 * KajiLibrary's javax.security.auth.callback.PasswordCallback -- asks for a password.
 *
 * <h2>Why it is char[] and not String</h2>
 *
 * <p>A {@code String} is immutable and lives until the collector picks it up: a password kept there
 * stays in memory for a time nobody controls, and shows up whole in a dump. A {@code char[]} can be
 * <b>overwritten</b> as soon as it was used, and that is what {@link #clearPassword()} does. It is
 * the same reason {@code Console.readPassword()} also returns an array.
 *
 * <p>The array is copied on the way in and on the way out. Without the copy on the way in, whoever
 * called {@code setPassword} could overwrite theirs and leave this object with a broken password;
 * without the one on the way out, whoever reads the password could overwrite it and break a second
 * read.
 *
 * <h2>The detail of clearPassword that gets forgotten</h2>
 *
 * <p><b>It does not set the array to null: it fills it with spaces.</b> After calling it, {@link
 * #getPassword()} still returns an array of the same length, full of blanks. It is the JDK's and it
 * makes sense -- erasing is overwriting the bytes, not letting go of the reference, which would
 * leave the bytes where they were -- but it surprises whoever expects a null.
 */
public class PasswordCallback implements Callback, java.io.Serializable {

    private static final long serialVersionUID = 2267422647454909926L;

    private final String prompt;
    private final boolean echoOn;
    private char[] inputPassword;

    /**
     * @param echoOn whether what the user types can be shown on screen. Almost always false; true
     *     is for the cases where there is no secret to protect
     * @throws IllegalArgumentException if the prompt is null or empty
     */
    public PasswordCallback(String prompt, boolean echoOn) {
        if (prompt == null || prompt.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.prompt = prompt;
        this.echoOn = echoOn;
    }

    public String getPrompt() {
        return this.prompt;
    }

    public boolean isEchoOn() {
        return this.echoOn;
    }

    /** Keeps a <b>copy</b> of the password. See the class note. */
    public void setPassword(char[] password) {
        this.inputPassword = password == null ? null : copy(password);
    }

    /** A <b>copy</b> of the password, or null if nobody answered yet. */
    public char[] getPassword() {
        return this.inputPassword == null ? null : copy(this.inputPassword);
    }

    /**
     * Overwrites the password with spaces.
     *
     * <p>It does not set it to null; see the class note. Calling it twice, or with no password set,
     * does nothing.
     */
    public void clearPassword() {
        if (this.inputPassword != null) {
            int i = 0;
            while (i < this.inputPassword.length) {
                this.inputPassword[i] = ' ';
                i = i + 1;
            }
        }
    }

    private static char[] copy(char[] a) {
        char[] c = new char[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }
}
