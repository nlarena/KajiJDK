package java.awt.im;

import java.awt.AWTEvent;
import java.awt.Component;
import java.util.Locale;

/**
 * The state of writing for a window: which input method is active and what it is composing.
 *
 * <p>There is **one per window**, not one per text field, and that choice shows: when focus moves
 * from one field to another in the same window, the input method keeps its state. It is what one
 * expects — the conversion dictionary does not reset for moving between fields.
 *
 * <p><strong>This implementation has no input method behind it.</strong> Without a window system
 * there is none to activate: {@link #selectInputMethod} answers `false`, composition stays off and
 * {@link #getLocale} returns `null`. They are all true answers about this context, not filler:
 * there is no input method, none could be chosen, there is no active locale. {@link
 * #endComposition} does nothing, which is exactly right when no composition is in progress. This
 * note also put {@link #reconvert} among the methods that do nothing; it throws {@link
 * UnsupportedOperationException}, as its javadoc says and as the JDK's base class does.
 */
public class InputContext {

    private boolean compositionEnabled;

    /** For subclasses. */
    protected InputContext() {
    }

    /** A new context. */
    public static InputContext getInstance() {
        return new InputContext();
    }

    /**
     * Selects an input method for that locale.
     *
     * @return `false` always: there is none installed to choose
     * @throws NullPointerException if the locale is `null`
     */
    public boolean selectInputMethod(Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        return false;
    }

    /**
     * The locale of the active input method.
     *
     * @return `null` always: there is none active
     */
    public Locale getLocale() {
        return null;
    }

    /** Narrows which characters can be typed; with no input method there is nothing to narrow. */
    public void setCharacterSubsets(Character.Subset[] subsets) {
    }

    /**
     * Turns composition on or off.
     *
     * @throws UnsupportedOperationException if asked to turn it on: there is no input method that
     *     can compose, and saying it was turned on would lie about the state
     */
    public void setCompositionEnabled(boolean enable) {
        if (enable) {
            throw new UnsupportedOperationException("no input method is installed");
        }
        this.compositionEnabled = false;
    }

    /**
     * Whether composition is on.
     *
     * @return `false` always
     */
    public boolean isCompositionEnabled() {
        return this.compositionEnabled;
    }

    /**
     * Asks for the already committed text to be converted again.
     *
     * @throws UnsupportedOperationException always: there is no input method to reconvert
     */
    public void reconvert() {
        throw new UnsupportedOperationException("no input method is installed");
    }

    /** Passes an event to the input method; with none, it does nothing. */
    public void dispatchEvent(AWTEvent event) {
    }

    /**
     * Tells that the component stopped existing; with no input method, there is no state to drop.
     */
    public void removeNotify(Component client) {
    }

    /** Ends the composition in progress; there is none. */
    public void endComposition() {
    }

    /** Releases the resources; there are none. */
    public void dispose() {
    }

    /**
     * The input method's control object.
     *
     * @return `null` always: there is no input method to control
     */
    public Object getInputMethodControlObject() {
        return null;
    }
}
