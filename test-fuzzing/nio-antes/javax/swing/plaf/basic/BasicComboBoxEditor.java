package javax.swing.plaf.basic;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.Method;

import javax.swing.ComboBoxEditor;
import javax.swing.JTextField;
import javax.swing.border.Border;

/**
 * The text field of an editable combo box.
 *
 * <h2>Returning the type, not the text</h2>
 *
 * <p>It is the only difficult thing about this class. The field keeps text, but the combo box
 * may have items that are not strings -- numbers, dates, colours --, and if on editing one the
 * combo box got a string back, the model would end up with mixed types.
 *
 * <p>So {@link #getItem} remembers the last value it was given and what class it was. If the
 * text did not change, it returns <em>that very object</em>. And if it changed, it looks up by
 * reflection a {@code valueOf(String)} in the old value's class and uses it to build a new one
 * of the same type. If it does not find it, or if it fails, it returns the text: it is worse,
 * but it is better than breaking.
 *
 * <h2>The field with no border</h2>
 *
 * <p>The field goes inside the combo box, which already has a border of its own; one more would
 * look like a frame inside another. That is why {@link #createEditorComponent} takes away its
 * entry border, and that is why the field does not write the text again if it is the same one
 * it already has: rewriting it would move the caret while somebody is typing.
 *
 * <p>The class is called {@code BorderlessTextField} and its {@code setBorder}
 * <em>looks like</em> it filters out the look and feel's borders, but it filters none out; see
 * that method's note.
 */
public class BasicComboBoxEditor implements ComboBoxEditor, FocusListener {

    protected JTextField editor;
    private Object oldValue;

    public BasicComboBoxEditor() {
        editor = createEditorComponent();
    }

    public Component getEditorComponent() {
        return editor;
    }

    /** See the class note: nine columns and no border. */
    protected JTextField createEditorComponent() {
        JTextField field = new BorderlessTextField("", 9);
        field.setBorder(null);
        return field;
    }

    /**
     * It puts that value into the field.
     *
     * <p>A null value leaves the field empty and does <em>not</em> forget the previous value: the
     * previous one's type is what {@link #getItem} needs in order to return something of the right
     * type.
     */
    public void setItem(Object anObject) {
        String text;
        if (anObject != null) {
            if (anObject instanceof String) {
                text = (String) anObject;
            } else {
                text = anObject.toString();
            }
            if (text == null) {
                text = "";
            }
            oldValue = anObject;
        } else {
            text = "";
        }
        if (!text.equals(editor.getText())) {
            editor.setText(text);
        }
    }

    /** See the class note. */
    public Object getItem() {
        Object newValue = editor.getText();
        if (oldValue != null && !(oldValue instanceof String)) {
            if (newValue.equals(oldValue.toString())) {
                return oldValue;
            }
            Class<?> cls = oldValue.getClass();
            try {
                Method method = cls.getMethod("valueOf", new Class<?>[] {String.class});
                newValue = method.invoke(oldValue, new Object[] {editor.getText()});
            } catch (Exception ex) {
                // With no `valueOf` there is no way of recovering the type, and returning the text
                // is the
                                // only thing left. See the class note.
            }
        }
        return newValue;
    }

    public void selectAll() {
        editor.selectAll();
        editor.requestFocus();
    }

    /** It does nothing: the combo box learns about the focus on its own. */
    public void focusGained(FocusEvent e) {
    }

    /** The same. */
    public void focusLost(FocusEvent e) {
    }

    public void addActionListener(ActionListener l) {
        editor.addActionListener(l);
    }

    public void removeActionListener(ActionListener l) {
        editor.removeActionListener(l);
    }

    /** See the class note. */
    static class BorderlessTextField extends JTextField {

        public BorderlessTextField(String value, int n) {
            super(value, n);
        }

        /**
         * It does not rewrite the same text: doing so would move the caret while somebody is
         * typing.
         */
        public void setText(String s) {
            if (getText().equals(s)) {
                return;
            }
            super.setText(s);
        }

        /**
         * It accepts any border, and the check that looks as though it rejects some rejects none.
         *
         * <p>The JDK's intention was to reject the borders the look and feel sets -- hence the
         * class's name --, and the check says {@code b instanceof UIResource}. But inside {@link
         * BasicComboBoxEditor} the name {@code UIResource} is not {@link
         * javax.swing.plaf.UIResource}: it is {@link BasicComboBoxEditor.UIResource}, the nested
         * class right here. A border is never an instance of <em>that</em> one, so the check always
         * passes and the border is always set.
         *
         * <p>It is measured -- an {@code EmptyBorderUIResource} goes in without trouble -- and it
         * is copied with the same type, not with the one the intention called for: changing it
         * would leave this library's combo boxes without the border the JDK does give them.
         */
        public void setBorder(Border b) {
            if (!(b instanceof BasicComboBoxEditor.UIResource)) {
                super.setBorder(b);
            }
        }
    }

    /**
     * The same editor, marked as set by the look and feel.
     *
     * <p>See {@link BasicComboBoxRenderer.UIResource}.
     */
    public static class UIResource extends BasicComboBoxEditor
            implements javax.swing.plaf.UIResource {

        public UIResource() {
        }
    }
}
