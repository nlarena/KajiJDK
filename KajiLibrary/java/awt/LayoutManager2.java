package java.awt;

/**
 * A layout that also accepts **constraints** per child.
 *
 * <p>{@link LayoutManager} can only receive a name when a child is added, and that is not enough to
 * say "this goes north" or "this spans two columns and stretches". This interface replaces the name
 * with any object, and with that whatever the layout needs can be passed: a string, a
 * {@link GridBagConstraints}, anything.
 *
 * <p>It also adds the **maximum** size and the alignment, which the first one did not have: without
 * a maximum, a layout that distributes spare space does not know when to stop stretching.
 *
 * <p>{@link #invalidateLayout} exists because these layouts usually keep expensive computations
 * between calls; it is the notice that they have to be thrown away.
 */
public interface LayoutManager2 extends LayoutManager {

    /**
     * Tells that a child was added with those constraints.
     *
     * @throws IllegalArgumentException if the constraints are not of the class this layout
     *     understands
     */
    void addLayoutComponent(Component comp, Object constraints);

    /** The maximum the container can make use of. */
    Dimension maximumLayoutSize(Container target);

    /** How the container aligns horizontally within its own. */
    float getLayoutAlignmentX(Container target);

    /** How it aligns vertically. */
    float getLayoutAlignmentY(Container target);

    /** Tells that the stored computations have to be thrown away. */
    void invalidateLayout(Container target);
}
