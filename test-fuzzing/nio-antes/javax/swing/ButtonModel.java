package javax.swing;

import java.awt.ItemSelectable;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import javax.swing.event.ChangeListener;

/**
 * A button's state, separate from the button.
 *
 * <p>Five bits -- armed, selected, enabled, pressed, with the cursor over it -- plus a mnemonic,
 * a command and a group. The button is a view of this: the look and feel reads the model in
 * order to decide how to paint, and the mouse listener writes into the model, not into the
 * button. The separation makes it possible for a button to be "pressed" from a program
 * ({@code doClick}) exactly as a mouse would press it.
 *
 * <p>A click's sequence is arming on entering, pressing on the mouse button going down, and
 * firing the action on releasing it <em>if it is still armed</em>: moving the mouse outside
 * before releasing disarms, and then releasing fires nothing. It is what makes a regretted click
 * not count.
 */
public interface ButtonModel extends ItemSelectable {

    /** Whether releasing the mouse now would fire the action. */
    boolean isArmed();

    boolean isSelected();

    boolean isEnabled();

    boolean isPressed();

    /** Whether the cursor is over it. */
    boolean isRollover();

    void setArmed(boolean b);

    void setSelected(boolean b);

    void setEnabled(boolean b);

    void setPressed(boolean b);

    void setRollover(boolean b);

    /** The mnemonic, as a {@code KeyEvent} virtual key. */
    void setMnemonic(int key);

    int getMnemonic();

    void setActionCommand(String s);

    String getActionCommand();

    /** The exclusion group it belongs to; the group calls it on adding and removing. */
    void setGroup(ButtonGroup group);

    /** The group, or {@code null}; by default none. */
    default ButtonGroup getGroup() {
        return null;
    }

    void addActionListener(ActionListener l);

    void removeActionListener(ActionListener l);

    void addItemListener(ItemListener l);

    void removeItemListener(ItemListener l);

    void addChangeListener(ChangeListener l);

    void removeChangeListener(ChangeListener l);
}
