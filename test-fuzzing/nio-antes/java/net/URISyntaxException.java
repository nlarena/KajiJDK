package java.net;

// Checked exception that says WHERE a URI string stopped being a URI: it keeps the whole input,
// the reason, and the index of the character that broke it. `getMessage()` joins them.
//
// KajiLibrary: the same surface as the JDK's (6 members). The only detour is in `getMessage()`: the
// JDK builds "reason at index N: input" and our `String` has no `valueOf(int)`, so the number is
// written out by hand with `digits()` — ten lines of arithmetic over `char`, which is the same thing
// any `Integer.toString` would do.
public class URISyntaxException extends Exception {

    private final String input;
    private final String reason;
    private final int index;

    public URISyntaxException(String input, String reason, int index) {
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

    public URISyntaxException(String input, String reason) {
        this(input, reason, -1);
    }

    public String getInput() {
        return this.input;
    }

    public String getReason() {
        return this.reason;
    }

    // -1 when the reason does not point at a concrete position.
    public int getIndex() {
        return this.index;
    }

    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.reason);
        if (this.index > -1) {
            sb.append(" at index ");
            sb.append(digits(this.index));
        }
        sb.append(": ");
        sb.append(this.input);
        return sb.toString();
    }

    // `String.valueOf(int)` does not exist in this library; this is its minimal equivalent.
    private static String digits(int value) {
        if (value == 0) {
            return "0";
        }
        StringBuilder rev = new StringBuilder();
        int n = value;
        while (n > 0) {
            int d = n % 10;
            rev.append((char) ('0' + d));
            n = n / 10;
        }
        StringBuilder out = new StringBuilder();
        int i = rev.length() - 1;
        while (i >= 0) {
            out.append(rev.charAt(i));
            i = i - 1;
        }
        return out.toString();
    }
}
