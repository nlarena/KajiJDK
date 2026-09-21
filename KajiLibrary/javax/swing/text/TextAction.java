package javax.swing.text;

import java.awt.event.ActionEvent;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;

/**
 * An action that works on the text component that fired it.
 *
 * <h2>Why it does not keep the component</h2>
 *
 * <p>An editing action --"delete the previous word"-- holds for any text component, and putting
 * it in a shared menu means that which one it will act on is not known beforehand. That is why
 * {@link #getTextComponent} takes it from the event: the one that fired the action.
 *
 * <p>When the event does not say where it came from --an action invoked from code--, it falls
 * back on {@link #getFocusedComponent}, which in this VM returns {@code null} because there is
 * no keyboard focus.
 */
public abstract class TextAction extends AbstractAction {

    /** An action with that name; the name is what ties it to a key. */
    public TextAction(String name) {
        super(name);
    }

    /** The component to act on; see the class note. */
    protected final JTextComponent getTextComponent(ActionEvent e) {
        if (e != null) {
            Object o = e.getSource();
            if (o instanceof JTextComponent) {
                return (JTextComponent) o;
            }
        }
        return getFocusedComponent();
    }

    /**
     * It joins two lists of actions, the second one winning.
     *
     * <p>It is how an editor kit adds its own to those it inherits without repeating: two actions
     * with the same name are the same one, and the one from the lower list stays.
     */
    public static final Action[] augmentList(Action[] list1, Action[] list2) {
        Hashtable<String, Action> h = new Hashtable<String, Action>();
        for (int i = 0; i < list1.length; i++) {
            Action a = list1[i];
            String value = (String) a.getValue(Action.NAME);
            h.put((value != null ? value : ""), a);
        }
        for (int i = 0; i < list2.length; i++) {
            Action a = list2[i];
            String value = (String) a.getValue(Action.NAME);
            h.put((value != null ? value : ""), a);
        }
        Action[] actions = new Action[h.size()];
        int index = 0;
        for (java.util.Enumeration<Action> e = h.elements(); e.hasMoreElements();) {
            actions[index] = e.nextElement();
            index = index + 1;
        }
        return actions;
    }

    /** {@code null}: this VM has no keyboard focus; see the class note. */
    protected final JTextComponent getFocusedComponent() {
        return JTextComponent.getFocusedComponent();
    }
}
