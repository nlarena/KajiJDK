package org.w3c.dom.ranges;

/**
 * KajiLibrary's org.w3c.dom.ranges.RangeException -- an impossible range operation.
 *
 * <p>It is <b>unchecked</b>, just like {@code DOMException}, and for the same reason: the two
 * errors it reports are program errors and not data errors. Forcing one to catch what cannot happen
 * in correct code would dirty every call.
 *
 * <p>The code goes in a public field and not in an accessor. It is the DOM convention -- the
 * interfaces are defined in IDL, which has no properties -- and it is reproduced as it is even
 * though today it would not be written like that.
 */
public class RangeException extends RuntimeException {

    private static final long serialVersionUID = 2427623564573316628L;

    /**
     * The two ends do not delimit a range: they are in different trees, or the end comes before the
     * start.
     */
    public static final short BAD_BOUNDARYPOINTS_ERR = 1;

    /** That type of node cannot be the container of an end, or cannot be selected. */
    public static final short INVALID_NODE_TYPE_ERR = 2;

    /** Which of the two. Public by the DOM convention; see the note of the class. */
    public short code;

    public RangeException(short code, String message) {
        super(message);
        this.code = code;
    }
}
