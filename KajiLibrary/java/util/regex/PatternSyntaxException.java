package java.util.regex;

// KajiLibrary's java.util.regex.PatternSyntaxException — signals a syntax error in a
// regular-expression pattern. It carries the offending pattern, a human-readable
// description of the error, and the index at which it was found (-1 if unknown). The
// message (built lazily by getMessage) reproduces the pattern with a caret under the
// error, the way real javac's PatternSyntaxException does. A KajiLibrary subset.
public class PatternSyntaxException extends IllegalArgumentException {

    private final String desc;
    private final String pattern;
    private final int index;

    public PatternSyntaxException(String desc, String regex, int index) {
        this.desc = desc;
        this.pattern = regex;
        this.index = index;
    }

    public String getDescription() {
        return this.desc;
    }

    public int getIndex() {
        return this.index;
    }

    public String getPattern() {
        return this.pattern;
    }

    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.desc);
        if (this.index >= 0) {
            sb.append(" near index ");
            sb.append(this.index);
        }
        sb.append('\n');
        sb.append(this.pattern);
        if (this.index >= 0) {
            sb.append('\n');
            for (int i = 0; i < this.index; i++) {
                sb.append(' ');
            }
            sb.append('^');
        }
        return sb.toString();
    }
}
