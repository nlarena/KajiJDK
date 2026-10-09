package javax.swing.text;

import java.io.IOException;

/**
 * "The character set was not the one you said, we have to start again."
 *
 * <p>An HTML reader throws it when it finds, with the document already started, a tag that
 * declares another encoding. It is an exception and not an error because the reader <em>can</em>
 * handle it: it closes the stream, opens it again with the encoding it says and starts over.
 *
 * <p>{@link #keyEqualsCharSet} tells the two ways of writing that declaration in HTML apart, and
 * it is needed because the text that is kept is different in each case.
 */
public class ChangedCharSetException extends IOException {

    String charSetSpec;
    boolean charSetKey;

    public ChangedCharSetException(String charSetSpec, boolean charSetKey) {
        this.charSetSpec = charSetSpec;
        this.charSetKey = charSetKey;
    }

    /** What the declaration said. */
    public String getCharSetSpec() {
        return charSetSpec;
    }

    /** Whether the declaration came as {@code charset=...} and not as the whole attribute. */
    public boolean keyEqualsCharSet() {
        return charSetKey;
    }
}
