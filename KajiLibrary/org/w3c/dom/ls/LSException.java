package org.w3c.dom.ls;

/**
 * KajiLibrary's org.w3c.dom.ls.LSException -- loading or saving failed.
 *
 * <p>It is one of the W3C exceptions, with the same design as {@code DOMException}: a numeric code
 * in a <b>public</b> field instead of a hierarchy of subclasses. It comes from the specification
 * being written in IDL and having to be translatable into languages with no inheritance of
 * exceptions.
 *
 * <p>Only two codes, and they separate the two directions: {@link #PARSE_ERR} when reading,
 * {@link #SERIALIZE_ERR} when writing. It is little information on purpose -- the detail of
 * <b>what</b> was wrong does not go here but to the error handler, which receives it while the
 * analysis is still standing at the bad point and can say on which line it was.
 */
public class LSException extends RuntimeException {

    private static final long serialVersionUID = 5371691160978884690L;

    /** The code; public because of the translation of the IDL. See the note of the class. */
    public short code;

    /** The document could not be read. */
    public static final short PARSE_ERR = 81;

    /** The document could not be written. */
    public static final short SERIALIZE_ERR = 82;

    /**
     * @param code one of the two above
     * @param message what happened
     */
    public LSException(short code, String message) {
        super(message);
        this.code = code;
    }
}
