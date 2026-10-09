package java.awt.im.spi;

import java.awt.AWTEvent;
import java.awt.Rectangle;
import java.util.Locale;

/**
 * An input method: what turns keystrokes into text when one key is not enough.
 *
 * <p>Writing Japanese, Chinese or Korean is not one key per character: the user types a phonetic
 * reading, the input method offers candidates, and only when one is chosen is the text produced.
 * Everything in this interface comes from that.
 *
 * <h2>The composed text</h2>
 *
 * <p>Between the first keystroke and the choice there is an intermediate state --the **composed
 * text**-- that the client shows but that is not yet part of the document. It is what is seen
 * underlined while choosing. The input method reports it through
 * {@link InputMethodContext#dispatchInputMethodEvent}, and the text becomes final when it is
 * *committed*.
 *
 * <p>That is why {@link #endComposition} shows up in so many places: every time focus leaves, or
 * the client needs the real text, something has to be decided about what was half composed.
 *
 * <h2>Active and inactive</h2>
 *
 * <p>An input method is {@link #activate activated} when the client using it takes focus and
 * {@link #deactivate deactivated} when it loses it. In between it can stay alive: deactivation
 * releases nothing; that is what {@link #dispose} is for.
 *
 * @see InputMethodDescriptor
 */
public interface InputMethod {

    /**
     * Gives the input method the context through which it communicates with the client.
     *
     * <p>It is called only once, right after creation and before anything else.
     */
    void setInputMethodContext(InputMethodContext context);

    /**
     * Changes the locale being written.
     *
     * @return `true` if it could; `false` if it does not support that locale
     */
    boolean setLocale(Locale locale);

    /** The locale being written, or `null` if there is none yet. */
    Locale getLocale();

    /**
     * Restricts the characters that can be produced to those subsets.
     *
     * <p>With `null` there is no restriction. (This javadoc said an empty array means the same; the
     * contract only says so of `null`.)
     */
    void setCharacterSubsets(Character.Subset[] subsets);

    /**
     * Turns composition on or off.
     *
     * <p>When off, keystrokes pass straight through. It is how one switches between writing
     * Japanese and writing Latin characters without changing input method.
     *
     * @throws UnsupportedOperationException if this input method cannot be turned off
     */
    void setCompositionEnabled(boolean enable);

    /**
     * Whether composition is on.
     *
     * @throws UnsupportedOperationException if this input method cannot answer it
     */
    boolean isCompositionEnabled();

    /**
     * Puts the already committed text around the cursor back into composition.
     *
     * <p>It is for correcting: the user chose the wrong candidate, and instead of deleting and
     * typing again asks to reconvert what was already written.
     *
     * @throws UnsupportedOperationException if this input method cannot reconvert
     */
    void reconvert();

    /**
     * Hands an event to the input method.
     *
     * <p>What arrives is every {@code InputEvent}, key and mouse events included; whichever event
     * the input method consumes stays with it, and the client does not see it. (This javadoc said
     * only the events the method asked for arrive.)
     */
    void dispatchEvent(AWTEvent event);

    /**
     * Tells that the client's window moved, changed size or visibility.
     *
     * <p>It serves to relocate the candidate window, which has to follow the text. It only arrives
     * if the method asked for it with {@link InputMethodContext#enableClientWindowNotification}.
     *
     * @param bounds the new position on screen, or `null` if the window is no longer visible
     */
    void notifyClientWindowChange(Rectangle bounds);

    /** Activates it: its client has just taken focus. */
    void activate();

    /**
     * Deactivates it: its client lost focus.
     *
     * @param isTemporary whether focus left only briefly --a menu, a dialog-- in which case the
     *     composition state is worth keeping instead of throwing it away
     */
    void deactivate(boolean isTemporary);

    /** Hides the windows it opened: the candidate window, the status window. */
    void hideWindows();

    /**
     * Tells that a client component was removed from its containment hierarchy, or that input
     * method support was disabled for it. It is only called while the input method is inactive.
     * (This javadoc said the composed text has to be dropped without committing it; the contract
     * says nothing of the kind.)
     */
    void removeNotify();

    /**
     * Ends the composition in progress.
     *
     * <p>Depending on the platform and possibly on user preferences, the uncommitted text is
     * committed or deleted; this javadoc said it is always committed. Any change goes out through
     * the usual event, so the client learns of it the same way as if the user had chosen it.
     */
    void endComposition();

    /**
     * Releases the input method's resources.
     *
     * <p>It is called with the method already deactivated, and after this it is not used again.
     */
    void dispose();

    /**
     * A control object specific to this implementation, or `null` if there is none.
     *
     * <p>It is the escape hatch: whatever an input method wants to expose that this interface does
     * not cover goes out here, and the client that understands it casts it.
     */
    Object getControlObject();
}
