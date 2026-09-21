package javax.swing.plaf.basic;

import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JList;

/**
 * What {@link BasicComboBoxUI} asks of the list that drops down.
 *
 * <h2>Why it is an interface and not a class</h2>
 *
 * <p>A combo box's drop-down list is the part that changes most from one look and feel to
 * another: it may be a floating window, a panel inside the same window, or something that is not
 * even a list. But what the combo box's look and feel needs of it is always the same -- show it,
 * hide it, tell me whether it is visible, and lend me your listeners so as to forward the events
 * to them --, and that is what this interface says.
 *
 * <h2>The three borrowed listeners</h2>
 *
 * <p>{@link #getMouseListener}, {@link #getMouseMotionListener} and {@link #getKeyListener} are
 * not for the combo box to add them to the list: they are for the combo box to add them
 * <em>to itself</em>. Pressing the combo box's button, dragging downwards and releasing over an
 * item is a single gesture that starts in the combo box and ends in the list, and that is the
 * only way for the two to see the same drag.
 *
 * <h2>{@link #uninstallingUI}</h2>
 *
 * <p>The notice that the combo box is being left without a look and feel. It is where the
 * drop-down window lets go of what it hooked into the combo box's model; without that notice,
 * changing the look and feel would leave the old list listening for ever.
 */
public interface ComboPopup {

    /** It shows the list. */
    void show();

    /** It hides it. */
    void hide();

    boolean isVisible();

    /**
     * The list that is seen; the look and feel needs it in order to know which item ended up under
     * the mouse.
     */
    JList<Object> getList();

    /** See the interface note: they go on the combo box, not on the list. */
    MouseListener getMouseListener();

    MouseMotionListener getMouseMotionListener();

    KeyListener getKeyListener();

    /** See the interface note. */
    void uninstallingUI();
}
