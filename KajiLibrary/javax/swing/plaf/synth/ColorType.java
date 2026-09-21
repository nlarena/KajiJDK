package javax.swing.plaf.synth;

/**
 * Which colour of a component is being asked for.
 *
 * <h2>Why several are needed</h2>
 *
 * <p>A component does not have one colour: it has the background's, the text's, the text's
 * background when it is selected, and the focus box's. Asking for them by name --and not through
 * different methods-- is what allows writing a look and feel as a table instead of as code.
 *
 * <h2>The identifier</h2>
 *
 * <p>Each type has a consecutive number, and {@link #MAX_COUNT} says how many there are. That is
 * what allows keeping a style's colours in an indexed array instead of in a map, which for
 * something consulted on every repaint is not the same thing.
 *
 * <p>One of one's own can be defined by inheriting: the constructor assigns it the next number.
 * That is why {@link #MAX_COUNT} is read at start-up and is not a compile-time constant.
 *
 * @since 1.5
 */
public class ColorType {

    private static int next;

    /** The foreground's colour. */
    public static final ColorType FOREGROUND = new ColorType("Foreground");

    /** The background's colour. */
    public static final ColorType BACKGROUND = new ColorType("Background");

    /** The text's colour. */
    public static final ColorType TEXT_FOREGROUND = new ColorType("TextForeground");

    /** The text's background colour. */
    public static final ColorType TEXT_BACKGROUND = new ColorType("TextBackground");

    /** The colour the focus is marked with. */
    public static final ColorType FOCUS = new ColorType("Focus");

    /** How many types are defined. */
    public static final int MAX_COUNT = Math.max(FOREGROUND.getID(),
            Math.max(BACKGROUND.getID(), Math.max(TEXT_FOREGROUND.getID(),
                    Math.max(TEXT_BACKGROUND.getID(), FOCUS.getID())))) + 1;

    private final String description;
    private final int id;

    /**
     * A colour type with that name.
     *
     * @param description what it is called
     * @throws NullPointerException if {@code description} is {@code null}
     */
    protected ColorType(String description) {
        if (description == null) {
            throw new NullPointerException("ColorType must have a valid description");
        }
        this.description = description;
        synchronized (ColorType.class) {
            this.id = next++;
        }
    }

    /**
     * This type's number.
     *
     * @return the number, between zero and {@link #MAX_COUNT} minus one
     */
    public final int getID() {
        return id;
    }

    /**
     * The name.
     *
     * @return what it is called
     */
    @Override
    public String toString() {
        return description;
    }
}
