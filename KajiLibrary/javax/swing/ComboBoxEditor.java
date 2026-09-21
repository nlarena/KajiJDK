package javax.swing;

import java.awt.Component;
import java.awt.event.ActionListener;

/**
 * The editable part of a combo box.
 *
 * <p>An editable combo box is two things stuck together: the list and a field to type in. This
 * is the field, and it is separate so as to be able to replace it -- with a formatted one, with
 * an autocompleting one -- without touching the list.
 *
 * <p>{@link #selectAll} is in the interface because the list calls it on opening: selecting
 * everything makes typing replace what was there, which is what is expected when choosing from
 * a list.
 */
public interface ComboBoxEditor {

    /** The component that is typed in. */
    Component getEditorComponent();

    /** It puts that value in to be edited. */
    void setItem(Object anObject);

    /** What is written now. */
    Object getItem();

    void selectAll();

    /** It gives notice when the user finishes editing, normally with Enter. */
    void addActionListener(ActionListener l);

    void removeActionListener(ActionListener l);
}
