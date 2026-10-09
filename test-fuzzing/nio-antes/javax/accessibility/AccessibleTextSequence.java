package javax.accessibility;

/**
 * A stretch of text with its contents.
 *
 * <p>Like {@link AccessibleAttributeSequence}, it is a record of public fields: it is returned many
 * times when walking a document and gains nothing from encapsulation.
 */
public class AccessibleTextSequence {

    /** Where the stretch starts. */
    public int startIndex;

    /** Where it ends. */
    public int endIndex;

    /** The text of the stretch. */
    public String text;

    /** An empty stretch. */
    public AccessibleTextSequence() {
    }

    /** With the stretch and its text. */
    public AccessibleTextSequence(int start, int end, String txt) {
        this.startIndex = start;
        this.endIndex = end;
        this.text = txt;
    }
}
