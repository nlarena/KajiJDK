package javax.swing.plaf.nimbus;

import javax.swing.JComponent;

/**
 * A state of one's own, so a look and feel can draw something Swing does not name.
 *
 * <h2>What for</h2>
 *
 * <p>The states in {@code SynthConstants} -- pressed, disabled, focused -- are the ones Swing knows
 * about. A look and feel may want to tell more apart: a button inside a toolbar, a progress bar that
 * already finished, a text field that is not inside a scroll pane. None of those is a Swing state,
 * and all three change how the thing looks.
 *
 * <p>A state of one's own is defined by subclassing and answering {@link #isInState}. Nimbus defines
 * a couple of dozen that way.
 *
 * <h2>Why a question and not a flag</h2>
 *
 * <p>Because the state is not stored anywhere: it is worked out by looking at the component at the
 * moment of drawing it. Storing it would mean keeping it up to date on every change, which is
 * exactly the kind of duplicated state that drifts.
 *
 * @param <T> the kind of component that can be asked
 * @since 1.7
 */
public abstract class State<T extends JComponent> {

    private final String name;

    /**
     * A state with that name.
     *
     * <p>The name is what identifies it in the look and feel's description, and that is why it
     * cannot repeat among the states of one component.
     *
     * @param name what it is called
     */
    protected State(String name) {
        this.name = name;
    }

    /**
     * The name.
     *
     * @return what it is called
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Whether the component is in this state right now.
     *
     * <p>It is called while drawing, so it has to be cheap: look at the component's properties, do
     * not walk trees or consult anything outside.
     *
     * @param c the component
     * @return true if it is in this state
     */
    protected abstract boolean isInState(T c);
}
