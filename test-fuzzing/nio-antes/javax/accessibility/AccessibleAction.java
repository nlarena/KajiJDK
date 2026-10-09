package javax.accessibility;

/**
 * Implemented by what can be **done**: pressing, toggling, following a link.
 *
 * <p>An object may have several actions and 0 is the main one -- the one that happens on a double
 * click. Describing them with a string and running them by number is what allows an assistive
 * technology to offer "press the OK button" without knowing what a button is.
 */
public interface AccessibleAction {

    /** Toggle the expanded state of a node. */
    String TOGGLE_EXPAND = "toggleexpand";

    /** Increase the value. */
    String INCREMENT = "increment";

    /** Decrease the value. */
    String DECREMENT = "decrement";

    /** Start playing. */
    String CLICK = "click";

    /** Open the context menu. */
    String TOGGLE_POPUP = "toggle popup";

    /** How many actions there are. */
    int getAccessibleActionCount();

    /**
     * What that action does, in words.
     *
     * @return the description, or `null` if the number does not exist
     */
    String getAccessibleActionDescription(int i);

    /**
     * Runs that action.
     *
     * @return `true` if it ran
     */
    boolean doAccessibleAction(int i);
}
