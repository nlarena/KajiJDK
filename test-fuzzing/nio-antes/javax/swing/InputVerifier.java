package javax.swing;

/**
 * It decides whether a component may let go of the focus.
 *
 * <h2>It validates on leaving, not on typing</h2>
 *
 * <p>It is consulted when the focus leaves the component, not on each key. It is what allows a
 * number to be typed one digit at a time -- which goes through invalid states -- without
 * anything complaining until one has finished.
 *
 * <p>A verifier that returns false <strong>locks the focus in</strong>: one cannot go to the
 * next field until one corrects. It is the part to keep in mind, because a verifier with a
 * mistake leaves the user unable to do anything, not even close the window.
 *
 * <h2>Verifying and yielding are two questions</h2>
 *
 * <p>{@link #verify} answers "this is valid". {@link #shouldYieldFocus} answers "let it out",
 * and by default it is the same, but it is overridden so that it also corrects the value, beeps,
 * or marks the field in red. The distinction matters because {@code verify} has to be
 * <em>without effects</em>: it is also called from outside, in order to ask without wanting
 * anything to happen.
 */
public abstract class InputVerifier {

    /** For the subclasses. */
    protected InputVerifier() {
    }

    /** Whether what is in the component is valid; without effects. See the class note. */
    public abstract boolean verify(JComponent input);

    /**
     * Whether the component may let go of the focus.
     *
     * @deprecated Use {@link #shouldYieldFocus(JComponent, JComponent)}, which also knows where
     *     the focus is going.
     */
    @Deprecated
    public boolean shouldYieldFocus(JComponent input) {
        return verify(input);
    }

    /**
     * Whether it is worth verifying that component.
     *
     * <p>True by default. Returning false skips the whole verification, and it is for the case in
     * which the component is switched off or empty on purpose.
     */
    public boolean verifyTarget(JComponent input) {
        return true;
    }

    /**
     * Whether the focus may pass from one component to the other.
     *
     * <p>It receives both: sometimes the answer depends on where it is going. A cancel button has
     * to be able to receive the focus even though the field is wrong.
     */
    public boolean shouldYieldFocus(JComponent source, JComponent target) {
        return shouldYieldFocus(source);
    }
}
