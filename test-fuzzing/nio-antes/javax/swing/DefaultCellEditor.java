package javax.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.EventObject;

import javax.swing.table.TableCellEditor;
import javax.swing.tree.TreeCellEditor;

/**
 * The cell editor that serves for the three common cases: text, tick and combo box.
 *
 * <h2>One editor, three components, one delegate</h2>
 *
 * <p>The three components have nothing in common: one keeps text, another a boolean, another a
 * chosen element. Instead of three classes, there is one and an {@link EditorDelegate} that
 * knows how to talk to the component it got. The three constructors build the delegate that
 * applies.
 *
 * <p>The delegate is protected and replaceable on purpose: it is the point where this class is
 * taught to handle a component that is none of the three.
 *
 * <h2>When it starts editing</h2>
 *
 * <p>{@link #setClickCountToStart} decides how many clicks are needed. It is two for the text
 * field and <strong>one</strong> for the tick and the combo box, and the difference makes
 * sense: in a text field the first click is used in order to put the caret, whereas a tick has
 * nothing to do with a click other than change.
 *
 * <p>A gesture that is not the mouse's -- a key -- starts the editing without counting clicks.
 */
public class DefaultCellEditor extends AbstractCellEditor
        implements TableCellEditor, TreeCellEditor {

    /** The component the editing is done with. */
    protected JComponent editorComponent;

    /** Who knows how to talk to that component; see the class note. */
    protected EditorDelegate delegate;

    /** How many clicks start the editing. */
    protected int clickCountToStart = 1;

    /**
     * With a text field.
     *
     * <p>Two clicks, for what the class note says.
     */
    public DefaultCellEditor(final JTextField textField) {
        editorComponent = textField;
        this.clickCountToStart = 2;
        delegate = new TextDelegate(this, textField);
        textField.addActionListener(delegate);
    }

    /** With a tick; a single click. */
    public DefaultCellEditor(final JCheckBox checkBox) {
        editorComponent = checkBox;
        delegate = new CheckDelegate(this, checkBox);
        checkBox.addActionListener(delegate);
        checkBox.setRequestFocusEnabled(false);
    }

    /**
     * With a combo box; a single click.
     *
     * <p>The combo box is marked so that pressing Enter does not also fire the window's default
     * button: in a cell, Enter means "I have finished editing".
     */
    public DefaultCellEditor(final JComboBox<?> comboBox) {
        editorComponent = comboBox;
        comboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
        delegate = new ListDelegate(this, comboBox);
        comboBox.addActionListener(delegate);
    }

    /** The component the editing is done with. */
    public Component getComponent() {
        return editorComponent;
    }

    /** How many clicks start the editing; see the class note. */
    public void setClickCountToStart(int count) {
        clickCountToStart = count;
    }

    public int getClickCountToStart() {
        return clickCountToStart;
    }

    public Object getCellEditorValue() {
        return delegate.getCellEditorValue();
    }

    /** Whether that gesture is enough to start editing. */
    public boolean isCellEditable(EventObject anEvent) {
        return delegate.isCellEditable(anEvent);
    }

    public boolean shouldSelectCell(EventObject anEvent) {
        return delegate.shouldSelectCell(anEvent);
    }

    public boolean stopCellEditing() {
        return delegate.stopCellEditing();
    }

    public void cancelCellEditing() {
        delegate.cancelCellEditing();
    }

    /** The component already loaded with that tree row's value. */
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected,
            boolean expanded, boolean leaf, int row) {
        String stringValue = tree.convertValueToText(value, isSelected, expanded, leaf, row,
                false);
        delegate.setValue(stringValue);
        return editorComponent;
    }

    /**
     * The component already loaded with that table cell's value.
     *
     * <p><strong>The tick carries an extra step.</strong> A tick does not fill the whole cell, so
     * the table is asked for that cell's renderer and its border and background colour are copied
     * from it; without that, on starting to edit a flicker is seen where the cell changes
     * appearance. The other two editors fill the cell and do not need it.
     */
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        delegate.setValue(value);
        if (editorComponent instanceof JCheckBox) {
            javax.swing.table.TableCellRenderer renderer = table.getCellRenderer(row, column);
            Component c = renderer.getTableCellRendererComponent(table, value, isSelected, true,
                    row, column);
            if (c != null) {
                editorComponent.setOpaque(true);
                editorComponent.setBackground(c.getBackground());
                if (c instanceof JComponent) {
                    editorComponent.setBorder(((JComponent) c).getBorder());
                }
            } else {
                editorComponent.setOpaque(false);
            }
        }
        return editorComponent;
    }

    /**
     * What has to be known about each component in order to edit with it.
     *
     * <p>It listens to the component -- hence it implements both listeners -- in order to finish
     * the editing when the user presses Enter or chooses from the list.
     */
    protected static class EditorDelegate implements ActionListener, ItemListener, Serializable {

        /** The value that is being edited. */
        protected Object value;

        /**
         * The editor this delegate belongs to.
         *
         * <p>In the JDK this is an inner class and the reference to the outer one is implicit. Here
         * it is a static nested one with the outer one as the first parameter -- which is the same
         * signature the JDK emits -- because this compiler does not analyse `outer.super(...)`; see
         * finding #518.
         */
        final DefaultCellEditor editor;

        /** For the subclasses. */
        protected EditorDelegate(DefaultCellEditor editor) {
            this.editor = editor;
        }

        public Object getCellEditorValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        /**
         * Whether that gesture is enough.
         *
         * <p>A click counts the clicks; any other event -- a key, or nothing -- starts the editing
         * without more ado.
         */
        public boolean isCellEditable(EventObject anEvent) {
            if (anEvent instanceof MouseEvent) {
                return ((MouseEvent) anEvent).getClickCount() >= editor.clickCountToStart;
            }
            return true;
        }

        public boolean shouldSelectCell(EventObject anEvent) {
            return true;
        }

        /**
         * It starts editing. Nobody calls it in this library; it is here because the JDK exposes
         * it.
         */
        public boolean startCellEditing(EventObject anEvent) {
            return true;
        }

        public boolean stopCellEditing() {
            editor.fireEditingStopped();
            return true;
        }

        public void cancelCellEditing() {
            editor.fireEditingCanceled();
        }

        /** The user pressed Enter or chose: the editing is finished. */
        public void actionPerformed(ActionEvent e) {
            editor.stopCellEditing();
        }

        /** The same, for the components that give notice by selection and not by action. */
        public void itemStateChanged(ItemEvent e) {
            editor.stopCellEditing();
        }
    }

    /** The delegate that talks to a text field: the value is what is written. */
    private static class TextDelegate extends EditorDelegate {

        private final JTextField field;

        TextDelegate(DefaultCellEditor editor, JTextField field) {
            super(editor);
            this.field = field;
        }

        public void setValue(Object value) {
            field.setText((value != null) ? value.toString() : "");
        }

        public Object getCellEditorValue() {
            return field.getText();
        }
    }

    /** The delegate that talks to a tick: the value is a boolean. */
    private static class CheckDelegate extends EditorDelegate {

        private final JCheckBox tilde;

        CheckDelegate(DefaultCellEditor editor, JCheckBox tilde) {
            super(editor);
            this.tilde = tilde;
        }

        /** A text serves too: {@code "true"} switches on, any other switches off. */
        public void setValue(Object value) {
            boolean selected = false;
            if (value instanceof Boolean) {
                selected = ((Boolean) value).booleanValue();
            } else if (value instanceof String) {
                selected = value.equals("true");
            }
            tilde.setSelected(selected);
        }

        public Object getCellEditorValue() {
            return Boolean.valueOf(tilde.isSelected());
        }
    }

    /** The delegate that talks to a combo box: the value is what is chosen. */
    private static class ListDelegate extends EditorDelegate {

        private final JComboBox<?> list;

        ListDelegate(DefaultCellEditor editor, JComboBox<?> list) {
            super(editor);
            this.list = list;
        }

        public void setValue(Object value) {
            list.setSelectedItem(value);
        }

        public Object getCellEditorValue() {
            return list.getSelectedItem();
        }

        /**
         * A click on the combo box does not start editing by itself.
         *
         * <p>The combo box already reacts to the click by opening; counting that click as the one
         * that starts the editing would make the drop-down open and close in one go.
         */
        public boolean shouldSelectCell(EventObject anEvent) {
            if (anEvent instanceof MouseEvent) {
                MouseEvent e = (MouseEvent) anEvent;
                return e.getID() != MouseEvent.MOUSE_DRAGGED;
            }
            return true;
        }

        public boolean stopCellEditing() {
            if (list.isEditable()) {
                // An editable combo box may have text half written; it is confirmed first.
                list.actionPerformed(new ActionEvent(this, 0, ""));
            }
            return super.stopCellEditing();
        }
    }
}
