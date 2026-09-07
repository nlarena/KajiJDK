package java.awt.color;

/**
 * The colour management engine failed.
 *
 * <p>It is declared because the contract names it. This library throws it nowhere: the ICC parsing
 * here is done in Java and reports its problems as {@link ProfileDataException}, which is the one
 * that describes a malformed profile. See {@link ColorSpace}'s scope note.
 */
public class CMMException extends RuntimeException {

    private static final long serialVersionUID = 5775558044142292260L;

    /** With that message. */
    public CMMException(String s) {
        super(s);
    }
}
