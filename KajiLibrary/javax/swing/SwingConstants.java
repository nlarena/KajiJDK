package javax.swing;

/**
 * The position and orientation constants Swing's components share.
 *
 * <p>It is an interface with no methods, and a component <em>implements</em> it in order to be
 * able to write plain {@code CENTER} instead of {@code SwingConstants.CENTER}. It is a use of
 * interfaces that today is considered to be in bad taste -- it puts constants into every
 * class's public API -- and that the JDK keeps for compatibility: it is here because
 * {@code JLabel} and all the others carry it in their signature.
 *
 * <p>The cardinal points and {@link #LEADING}/{@link #TRAILING} are two vocabularies about the
 * same thing. The difference matters: {@code LEFT} is always the left, and {@code LEADING} is
 * the side the text <em>starts</em> on, which in a language that is read right to left is the
 * right.
 */
public interface SwingConstants {

    /** The centre, in both directions. */
    public static final int CENTER = 0;

    /** At the top. */
    public static final int TOP = 1;

    /** On the left, whatever the orientation. */
    public static final int LEFT = 2;

    /** At the bottom. */
    public static final int BOTTOM = 3;

    /** On the right, whatever the orientation. */
    public static final int RIGHT = 4;

    /** North: at the top and in the centre. */
    public static final int NORTH = 1;

    /** Noreste. */
    public static final int NORTH_EAST = 2;

    /** East. */
    public static final int EAST = 3;

    /** Sureste. */
    public static final int SOUTH_EAST = 4;

    /** Sur. */
    public static final int SOUTH = 5;

    /** Suroeste. */
    public static final int SOUTH_WEST = 6;

    /** Oeste. */
    public static final int WEST = 7;

    /** Noroeste. */
    public static final int NORTH_WEST = 8;

    /** Horizontal. */
    public static final int HORIZONTAL = 0;

    /** Vertical. */
    public static final int VERTICAL = 1;

    /** The side the text starts on, according to the component's orientation. */
    public static final int LEADING = 10;

    /** The side the text ends on, according to the component's orientation. */
    public static final int TRAILING = 11;

    /** The next one, in a sequence. */
    public static final int NEXT = 12;

    /** The previous one, in a sequence. */
    public static final int PREVIOUS = 13;
}
