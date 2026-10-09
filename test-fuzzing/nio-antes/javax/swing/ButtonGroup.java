package javax.swing;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

/**
 * A group of buttons of which at most one is selected.
 *
 * <p>It is the radio buttons' mutual exclusion, and it lives outside them: the group paints
 * nothing and has no parent, it only remembers who is selected and deselects the previous one
 * when another is selected. A button belongs to the group by its <em>model</em>, not by itself,
 * which is why {@link #setSelected} and {@link #isSelected} receive a {@link ButtonModel}.
 *
 * <p>The selection, once made, cannot be taken away by clicking: a selected radio button stays
 * selected even though it is pressed again. The only way for none to be selected is
 * {@link #clearSelection}.
 */
public class ButtonGroup implements Serializable {

    /** The buttons, in the order they were added. */
    protected Vector<AbstractButton> buttons = new Vector<AbstractButton>();

    /** The selected model, or {@code null}. */
    ButtonModel selection = null;

    public ButtonGroup() {
    }

    /**
     * It adds a button.
     *
     * <p>If it is already selected and the group has no selection, it becomes the selection; if
     * the group already had one, the one that arrives is deselected. It is the group's rule applied
     * at the moment of entering.
     */
    public void add(AbstractButton b) {
        if (b == null) {
            return;
        }
        buttons.addElement(b);
        if (b.isSelected()) {
            if (selection == null) {
                selection = b.getModel();
            } else {
                b.setSelected(false);
            }
        }
        b.getModel().setGroup(this);
    }

    /** It removes a button; if it was the selection, the group is left without one. */
    public void remove(AbstractButton b) {
        if (b == null) {
            return;
        }
        buttons.removeElement(b);
        if (b.getModel() == selection) {
            selection = null;
        }
        b.getModel().setGroup(null);
    }

    /** It leaves the group with no selection, deselecting the one that had it. */
    public void clearSelection() {
        if (selection != null) {
            ButtonModel old = selection;
            selection = null;
            old.setSelected(false);
        }
    }

    public Enumeration<AbstractButton> getElements() {
        return buttons.elements();
    }

    /** The selected model, or {@code null}. */
    public ButtonModel getSelection() {
        return selection;
    }

    /**
     * It selects that model, deselecting the one that was selected.
     *
     * <p>It only selects: asking for {@code false} does nothing, which is the "it cannot be taken
     * away by clicking" rule stated in the API. The model itself calls it when it is selected.
     */
    public void setSelected(ButtonModel m, boolean b) {
        if (b && m != null && m != selection) {
            ButtonModel old = selection;
            selection = m;
            if (old != null) {
                old.setSelected(false);
            }
            m.setSelected(true);
        }
    }

    public boolean isSelected(ButtonModel m) {
        return m == selection;
    }

    public int getButtonCount() {
        if (buttons == null) {
            return 0;
        }
        return buttons.size();
    }
}
