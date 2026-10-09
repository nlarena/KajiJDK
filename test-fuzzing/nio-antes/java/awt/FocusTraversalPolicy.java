package java.awt;

/**
 * In what order the tab key traverses the components of a container.
 *
 * <p>The default order —the visual one, top to bottom and left to right— is almost always right,
 * but not always: a form in two columns is usually meant to be traversed by column and not by row.
 * This class is where that is changed.
 *
 * <p>The five abstract methods are not redundant. {@link #getDefaultComponent} is whose turn the
 * focus is when the container receives it **for the first time**, and {@link #getFirstComponent} is
 * the first one of the traversal: in a dialog, the first one is usually a text field and the
 * default one the OK button.
 */
public abstract class FocusTraversalPolicy {

    /** For the subclasses. */
    protected FocusTraversalPolicy() {
    }

    /**
     * Whose turn the focus is after that component.
     *
     * @throws IllegalArgumentException if either of the two is missing, or if the component is not
     *     in that container
     */
    public abstract Component getComponentAfter(Container aContainer, Component aComponent);

    /**
     * Whose turn it was before.
     *
     * @throws IllegalArgumentException if either of the two is missing, or if the component is not
     *     in that container
     */
    public abstract Component getComponentBefore(Container aContainer, Component aComponent);

    /**
     * The first one of the traversal.
     *
     * @throws IllegalArgumentException if the container is `null`
     */
    public abstract Component getFirstComponent(Container aContainer);

    /**
     * The last one of the traversal.
     *
     * @throws IllegalArgumentException if the container is `null`
     */
    public abstract Component getLastComponent(Container aContainer);

    /**
     * Whose turn it is when the container receives the focus.
     *
     * @throws IllegalArgumentException if the container is `null`
     */
    public abstract Component getDefaultComponent(Container aContainer);

    /**
     * Whose turn it is the first time the window is shown.
     *
     * <p>By default, the same as {@link #getDefaultComponent}. It is kept apart so that a policy
     * can tell the first time from the following ones, which is what is needed when the dialog has
     * to start with the focus on a field but go back to the button afterwards.
     *
     * @throws IllegalArgumentException if the window is `null`
     */
    public Component getInitialComponent(Window window) {
        if (window == null) {
            throw new IllegalArgumentException("window cannot be equal to null.");
        }
        Component def = this.getDefaultComponent(window);
        if (def == null && window.isFocusableWindow()) {
            return window;
        }
        return def;
    }
}
