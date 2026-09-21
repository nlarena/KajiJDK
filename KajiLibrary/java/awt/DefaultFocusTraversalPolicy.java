package java.awt;

/**
 * The default traversal policy: it follows the order in which the children were added.
 *
 * <p>In the JDK what it adds to {@link ContainerOrderFocusTraversalPolicy} is **how it decides
 * whether a component takes part in the traversal**: besides `isFocusable()` it looks at whether
 * the component overrode the old `isFocusTraversable()` and, if nobody overrode it, at whether the
 * peer accepts the focus. The reason is compatibility: before 1.4 a component said whether it
 * entered the traversal by overriding `isFocusTraversable()`, and that override is still respected
 * as long as nobody has said otherwise with the newer API.
 *
 * <p><strong>Here it adds nothing.</strong> Against what this note used to claim, {@link #accept}
 * consults neither `isFocusTraversable` nor whether the focusability was set by hand: it asks the
 * same four questions as the policy it inherits from —visible, displayable, enabled and focusable—
 * so it behaves exactly like its parent. Both halves of the JDK rule rest on the peer, and without
 * a windowing system there is no peer to ask.
 */
public class DefaultFocusTraversalPolicy extends ContainerOrderFocusTraversalPolicy {

    private static final long serialVersionUID = 8876966522510157497L;

    /** A default policy. */
    public DefaultFocusTraversalPolicy() {
    }

    /**
     * Whether that component takes part in the traversal.
     *
     * <p>It has to be visible, enabled and displayable, besides accepting the focus.
     */
    protected boolean accept(Component aComponent) {
        if (!aComponent.isVisible() || !aComponent.isDisplayable() || !aComponent.isEnabled()) {
            return false;
        }
        return aComponent.isFocusable();
    }
}
