package javax.swing.plaf;

import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.Popup;

/**
 * A {@link JPopupMenu}'s look and feel.
 *
 * <h2>Two questions that are not geometry</h2>
 *
 * <p>Which gesture opens a context menu depends on the system: on Windows it is releasing the
 * right button and on others it is pressing it. That is known by the look and feel, not by the
 * menu, and that is why {@link #isPopupTrigger} is here.
 *
 * <p>{@link #getPopup} assembles the little window. That is not the menu's either: the decision
 * to draw it inside the window or in one of its own depends on whether it fits, and that is
 * measured by the look and feel.
 *
 * <p>Both have a body, unlike the other looks and feels: there is a reasonable default answer and
 * forcing it to be written in every look and feel would be repeating it.
 */
public abstract class PopupMenuUI extends ComponentUI {

    protected PopupMenuUI() {
    }

    /** Whether that event is the gesture that opens a context menu. */
    public boolean isPopupTrigger(MouseEvent e) {
        return e.isPopupTrigger();
    }

    /** The little window for that menu, at that point on the screen. */
    public Popup getPopup(JPopupMenu popup, int x, int y) {
        javax.swing.PopupFactory f = javax.swing.PopupFactory.getSharedInstance();
        return f.getPopup(popup.getInvoker(), popup, x, y);
    }
}
