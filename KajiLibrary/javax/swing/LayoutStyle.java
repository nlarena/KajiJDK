package javax.swing;

import java.awt.Container;

/**
 * How much space goes between two components, according to the system's rules.
 *
 * <h2>The spacing is not a preference, it is a rule of the platform</h2>
 *
 * <p>Each system has its style guide, and there it says how many pixels go between a label and
 * its field, between two buttons of the same bar, and between a dialog's edge and its content.
 * They are not the same numbers on Windows, on macOS and on GNOME, and an application that fixes
 * them by hand looks out of place on two of the three.
 *
 * <p>This class is where the installed look and feel answers those two questions --
 * {@link #getPreferredGap} between two components and {@link #getContainerGap} against the edge
 * -- and it is what {@link GroupLayout} consults when it is asked for a "whatever applies"
 * space.
 *
 * <h2>Three kinds of neighbourhood</h2>
 *
 * <p>{@link ComponentPlacement#RELATED} for two things that go together -- the label and its
 * field --, {@link ComponentPlacement#UNRELATED} for two different groups, and
 * {@link ComponentPlacement#INDENT} for what hangs from something else, such as the check box
 * that only makes sense if the one above is ticked.
 *
 * <h2>Where the instance comes from</h2>
 *
 * <p>From {@link #setInstance} if somebody set it, and otherwise from the installed look and
 * feel. With no look and feel there is no style guide to consult, and {@link #getInstance}
 * returns one that gives the JDK's numbers -- six pixels between related things, twelve between
 * groups and twelve against the edge --, measured against its own. A real look and feel replaces
 * them with its platform's.
 */
public abstract class LayoutStyle {

    private static LayoutStyle instance;

    /** It fixes the instance; null gives the decision back to the look and feel. */
    public static void setInstance(LayoutStyle style) {
        synchronized (LayoutStyle.class) {
            instance = style;
        }
    }

    /**
     * The instance in use.
     *
     * <p>See the class note: with no look and feel installed, one with the usual numbers.
     */
    public static LayoutStyle getInstance() {
        LayoutStyle style;
        synchronized (LayoutStyle.class) {
            style = instance;
        }
        if (style != null) {
            return style;
        }
        LookAndFeel laf = UIManager.getLookAndFeel();
        if (laf != null) {
            LayoutStyle fromLookAndFeel = laf.getLayoutStyle();
            if (fromLookAndFeel != null) {
                return fromLookAndFeel;
            }
        }
        return DefaultLayoutStyle.SHARED;
    }

    /** For the subclasses. */
    public LayoutStyle() {
    }

    /**
     * The space that goes between those two components.
     *
     * @param position which side the second is on with respect to the first; one of
     *     {@link SwingConstants}' constants
     * @throws IllegalArgumentException if some component or the position is invalid
     */
    public abstract int getPreferredGap(JComponent component1, JComponent component2,
            ComponentPlacement type, int position, Container parent);

    /**
     * The space that goes between that component and its container's edge.
     *
     * @throws IllegalArgumentException if the component or the position is invalid
     */
    public abstract int getContainerGap(JComponent component, int position, Container parent);

    /** What relation there is between the two components; see the class note. */
    public enum ComponentPlacement {

        /** They go together: a label and its field. */
        RELATED,

        /** They are different groups. */
        UNRELATED,

        /** The second hangs from the first. */
        INDENT;
    }

    /**
     * The one that is used with no look and feel installed.
     *
     * <p>The numbers are the ones almost every guide shares. They are not measured against any
     * platform in particular, and that is why this class is not public: whoever wants their
     * system's has to install the look and feel that knows them.
     */
    private static class DefaultLayoutStyle extends LayoutStyle {

        static final LayoutStyle SHARED = new DefaultLayoutStyle();

        public int getPreferredGap(JComponent component1, JComponent component2,
                ComponentPlacement type, int position, Container parent) {
            // A null component comes out as NullPointerException, not as IllegalArgumentException:
                        // the JDK does not check it and blows up on using it. It is measured.
            component1.getWidth();
            component2.getWidth();
            if (type == null) {
                throw new NullPointerException("type");
            }
            checkPosition(position);
            if (type == ComponentPlacement.INDENT
                    && (position == SwingConstants.EAST || position == SwingConstants.WEST)) {
                return 12;
            }
            return (type == ComponentPlacement.UNRELATED) ? 12 : 6;
        }

        public int getContainerGap(JComponent component, int position, Container parent) {
            component.getWidth();
            checkPosition(position);
            return 12;
        }

        private static void checkPosition(int position) {
            if (position != SwingConstants.NORTH && position != SwingConstants.SOUTH
                    && position != SwingConstants.EAST && position != SwingConstants.WEST) {
                throw new IllegalArgumentException("Invalid position");
            }
        }
    }
}
