package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.LineUnavailableException -- the line exists but cannot be used
 * now.
 *
 * <p>It is the distinction that makes this class useful: it does not mean the system does not
 * support what was asked --that is what {@link IllegalArgumentException} is for-- but that at
 * <b>this moment</b> there is no resource.
 *
 * <p>The usual cause is that another program took the device, or that the mixer's simultaneous
 * lines ran out. Retrying later may work, and that is why it is checked.
 */
public class LineUnavailableException extends Exception {

    private static final long serialVersionUID = -2046718279487432130L;

    /** Without detail. */
    public LineUnavailableException() {
        super();
    }

    /** With a message. */
    public LineUnavailableException(String message) {
        super(message);
    }
}
