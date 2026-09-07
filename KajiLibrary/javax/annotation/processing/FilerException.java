package javax.annotation.processing;

import java.io.IOException;

// The error a {@link Filer} throws when it is asked for something that violates its contract:
// creating the same type twice, writing over a source that already existed, or a name that is not
// valid. It is an `IOException` and not an unchecked one on purpose: the `Filer` does I/O, and
// whoever generates code has to decide what to do if it cannot write.
public class FilerException extends IOException {

    // An explicit `static final`, as in the rest of the library: the value is the real JDK's so that a
    // serialized stream crosses in both directions.
    static final long serialVersionUID = 8426423106453163293L;

    /**
     * @param s the reason the `Filer` operation could not be done
     */
    public FilerException(String s) {
        super(s);
    }
}
