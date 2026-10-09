package javax.swing;

import java.awt.Component;

/**
 * Who builds the little windows of the drop-downs.
 *
 * <h2>Why there is a factory and not a constructor</h2>
 *
 * <p>There are two ways of showing a drop-down and the choice depends on the case; see
 * {@link Popup}'s note. Concentrating it in a factory also allows two things a constructor would
 * not give: reusing the little windows instead of building one for each opening, and a program
 * replacing the whole factory in order to change how every drop-down looks.
 *
 * <h2>The shared one</h2>
 *
 * <p>{@link #getSharedInstance} returns a single one for the whole program. That it is shared is
 * what makes the reuse worthwhile: one factory per menu would have nothing to reuse.
 */
public class PopupFactory {

    private static PopupFactory shared = new PopupFactory();

    /** A new factory; the usual thing is to use the shared one. */
    public PopupFactory() {
    }

    /**
     * It changes the whole program's factory.
     *
     * @throws IllegalArgumentException if it is null.
     */
    public static void setSharedInstance(PopupFactory factory) {
        if (factory == null) {
            throw new IllegalArgumentException("PopupFactory can not be null");
        }
        shared = factory;
    }

    public static PopupFactory getSharedInstance() {
        return shared;
    }

    /**
     * A little window with that content, at that point of the screen.
     *
     * @throws IllegalArgumentException if the content is null.
     */
    public Popup getPopup(Component owner, Component contents, int x, int y) {
        return getPopup(owner, contents, x, y, false);
    }

    /**
     * The same, saying whether the lightweight form is advisable.
     *
     * <p>The flag is a request, not an order: if the content does not fit in the window, the heavy
     * one is used all the same. The other way round would be worse -- a clipped menu --, and that
     * is why the decision is not left entirely to the caller.
     */
    protected Popup getPopup(Component owner, Component contents, int x, int y,
            boolean isHeavyWeightPopup) {
        if (contents == null) {
            throw new IllegalArgumentException(
                    "Popup.getPopup must be passed non-null contents");
        }
        return new Popup(owner, contents, x, y);
    }
}
