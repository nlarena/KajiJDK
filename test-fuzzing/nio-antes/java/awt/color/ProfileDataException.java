package java.awt.color;

/**
 * An ICC profile with invalid data.
 *
 * <p>This one **is** thrown: {@link ICC_Profile} and its two subclasses raise it when a profile does
 * not hold what is being asked of it -- a tag that is missing, or a curve stored the other of the two
 * ways (see {@link ICC_ProfileRGB}'s note on gamma versus table).
 */
public class ProfileDataException extends RuntimeException {

    private static final long serialVersionUID = 7286140888240322498L;

    /** With that message. */
    public ProfileDataException(String s) {
        super(s);
    }
}
