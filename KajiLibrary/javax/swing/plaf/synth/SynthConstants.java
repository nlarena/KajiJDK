package javax.swing.plaf.synth;

/**
 * The states a component may be in.
 *
 * <h2>They are flags, not values</h2>
 *
 * <p>They are combined with {@code |}: a button may be at once {@link #ENABLED},
 * {@link #MOUSE_OVER} and {@link #FOCUSED}. That is why they are powers of two and why there is
 * a {@code SELECTED} separate from {@code PRESSED}, which are different things even though they
 * look alike.
 *
 * <p>The complete state is what {@link SynthContext#getComponentState} receives and what decides
 * which colour and which border the component gets at that moment.
 *
 * @since 1.5
 */
public interface SynthConstants {

    /** It can be used. */
    int ENABLED = 1;

    /** The pointer is over it. */
    int MOUSE_OVER = 2;

    /** It is pressed right now. */
    int PRESSED = 4;

    /** It cannot be used. */
    int DISABLED = 8;

    /** It has the keyboard focus. */
    int FOCUSED = 256;

    /** It is selected; it is not the same as being pressed. */
    int SELECTED = 512;

    /** It is the default option, the one that answers the enter key. */
    int DEFAULT = 1024;
}
