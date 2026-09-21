package java.nio.file;

// The string cannot be turned into a path.
//
// **It is not an `IOException`.** It inherits from `IllegalArgumentException` because the problem
// is in the argument and not on the disk: the string is badly written, and that is known without
// touching anything. That is why `Path.of` declares no `throws`.
//
// It keeps the **index** of the character that broke it, which is what allows underlining the exact
// position in an error message instead of repeating the whole string.
public class InvalidPathException extends IllegalArgumentException {

    private static final long serialVersionUID = 4355821422286746137L;

    private final String input;
    private final String reason;
    private final int index;

    /**
     * @param input the string that is no good
     * @param reason why it is no good
     * @param index the position of the offending character, or -1 if it is not known
     * @throws IllegalArgumentException if `index` is less than -1
     * @throws NullPointerException if `input` or `reason` is `null`
     */
    public InvalidPathException(String input, String reason, int index) {
        super(reason);
        if (input == null || reason == null) {
            throw new NullPointerException();
        }
        if (index < -1) {
            throw new IllegalArgumentException();
        }
        this.input = input;
        this.reason = reason;
        this.index = index;
    }

    /** Like the other, with the index at -1: where the problem is is not known. */
    public InvalidPathException(String input, String reason) {
        this(input, reason, -1);
    }

    /** The string that was to be converted. */
    public String getInput() {
        return this.input;
    }

    /**
     * The explanation.
     *
     * <p>It comes from a field of its own and not from `super.getMessage()` --which is where the
     * JDK takes it from-- because of a bug in **this VM**: an `invokespecial` to a method the named
     * superclass *inherits* rather than declares runs the body with the wrong constant pool and
     * blows up with `getfield: bad FieldRef`. `getMessage()` is declared in `Throwable`, not in
     * `IllegalArgumentException`, so it falls exactly into that case. Keeping the reason separately
     * gives the same result and does not depend on it.
     */
    public String getReason() {
        return this.reason;
    }

    /** The offending character's position, or -1. */
    public int getIndex() {
        return this.index;
    }

    /** `reason: string` and, if it is known, ` at index N` in between. */
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getReason());
        if (this.index > -1) {
            sb.append(" at index ");
            sb.append(this.index);
        }
        sb.append(": ");
        sb.append(this.input);
        return sb.toString();
    }
}
