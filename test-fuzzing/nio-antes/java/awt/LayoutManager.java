package java.awt;

/**
 * What decides where each child of a container goes and how big it is.
 *
 * <p>Separating the layout from the container is the design decision that makes AWT work on screens
 * and fonts that did not exist when the program was written: the container does not know where its
 * children go, it asks someone else, and changing that someone else changes the whole interface.
 *
 * <p>{@link #layoutContainer} is the method that does the work; the two size queries answer **how
 * much the container needs**, and they are what let the decision propagate up the tree. (This note
 * said three.)
 *
 * <p>{@link #addLayoutComponent} takes a name and not an object, and that signature has aged: it
 * serves {@link CardLayout} —where the name identifies the card— and little else.
 * {@link LayoutManager2} replaces it with one that takes any object.
 */
public interface LayoutManager {

    /** Tells that a child was added with that name. */
    void addLayoutComponent(String name, Component comp);

    /** Tells that a child was removed. */
    void removeLayoutComponent(Component comp);

    /** How much the container needs for its children to be comfortable. */
    Dimension preferredLayoutSize(Container parent);

    /** The minimum the container can work with. */
    Dimension minimumLayoutSize(Container parent);

    /** Places and sizes the children. */
    void layoutContainer(Container parent);
}
