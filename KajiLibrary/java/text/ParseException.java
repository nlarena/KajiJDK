package java.text;

// KajiLibrary's java.text.ParseException — thrown by the `parse` methods that do NOT take a
// ParsePosition. It carries the offset where parsing failed, which is the same information the
// position-taking form reports through `setErrorIndex`: the two APIs differ in how they deliver
// the failure, not in what they know.
public class ParseException extends Exception {

    private final int errorOffset;

    public ParseException(String s, int errorOffset) {
        super(s);
        this.errorOffset = errorOffset;
    }

    public int getErrorOffset() {
        return this.errorOffset;
    }
}
