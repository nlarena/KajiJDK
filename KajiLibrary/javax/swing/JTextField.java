package javax.swing;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.accessibility.AccessibleContext;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

/**
 * A single-line text field.
 *
 * <h2>One line, and what that implies</h2>
 *
 * <p>The document is a {@link PlainDocument} whose line ends are filtered out: it is not that
 * the field does not show them, it is that it does not let them in. That decision is in
 * {@link #createDefaultModel} and it is what makes pasting a text of several lines into a field
 * paste a single one.
 *
 * <p>Enter fires an {@link ActionEvent} instead of typing: it is the only key a field treats
 * differently, and it is what allows a form to be sent from the keyboard.
 *
 * <h2>The preferred width</h2>
 *
 * <p>It is asked for in <em>columns</em>, not in pixels, and a column is the width of the
 * current typeface's letter "m". It is an old measurement and it is still the best there is:
 * a field of twenty columns goes on fitting twenty characters when somebody changes the letter
 * size.
 */
public class JTextField extends JTextComponent implements SwingConstants {

    /** The name of the action Enter fires. */
    public static final String notifyAction = "notify-field-accept";

    private static final String uiClassID = "TextFieldUI";

    private int columns;
    private int columnWidth;
    private int horizontalAlignment = LEADING;
    private transient BoundedRangeModel visibility;
    private Action action;
    private String command;
    private PropertyChangeListener actionPropertyChangeListener;

    /** An empty field. */
    public JTextField() {
        this(null, null, 0);
    }

    public JTextField(String text) {
        this(null, text, 0);
    }

    /** An empty field of that width in columns. */
    public JTextField(int columns) {
        this(null, null, columns);
    }

    public JTextField(String text, int columns) {
        this(null, text, columns);
    }

    /** The constructor all the others reach. */
    public JTextField(Document doc, String text, int columns) {
        if (columns < 0) {
            throw new IllegalArgumentException("columns less than zero.");
        }
        visibility = new DefaultBoundedRangeModel();
        visibility.addChangeListener(new ScrollRepainter(this));
        this.columns = columns;
        if (doc == null) {
            doc = createDefaultModel();
        }
        setDocument(doc);
        if (text != null) {
            setText(text);
        }
    }

    public String getUIClassID() {
        return uiClassID;
    }

    public void setDocument(Document doc) {
        if (doc != null) {
            doc.putProperty("filterNewlines", Boolean.TRUE);
        }
        super.setDocument(doc);
    }

    /** Yes: a field has a size of its own and the layout comes here. */
    public boolean isValidateRoot() {
        return false;
    }

    public int getHorizontalAlignment() {
        return horizontalAlignment;
    }

    /** How the text is aligned when there is room left over. */
    public void setHorizontalAlignment(int alignment) {
        if (alignment == horizontalAlignment) {
            return;
        }
        int oldValue = horizontalAlignment;
        if ((alignment == LEFT) || (alignment == CENTER) || (alignment == RIGHT)
                || (alignment == LEADING) || (alignment == TRAILING)) {
            horizontalAlignment = alignment;
        } else {
            throw new IllegalArgumentException("horizontalAlignment");
        }
        firePropertyChange("horizontalAlignment", oldValue, horizontalAlignment);
        invalidate();
        repaint();
    }

    /** A plain text document with no line ends; see the class note. */
    protected Document createDefaultModel() {
        return new PlainDocument();
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        int oldVal = this.columns;
        if (columns < 0) {
            throw new IllegalArgumentException("columns less than zero.");
        }
        if (columns != oldVal) {
            this.columns = columns;
            invalidate();
        }
    }

    /** A column's width: that of the letter "m"; see the class note. */
    protected int getColumnWidth() {
        if (columnWidth == 0) {
            FontMetrics metrics = getFontMetrics(getFont());
            columnWidth = metrics.charWidth('m');
        }
        return columnWidth;
    }

    /** The width of the columns asked for, or the text's if none were asked for. */
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        if (columns != 0) {
            Insets insets = getInsets();
            size.width = columns * getColumnWidth() + insets.left + insets.right;
        }
        return size;
    }

    /** Changing the typeface changes the column's width. */
    public void setFont(Font f) {
        super.setFont(f);
        columnWidth = 0;
    }

    public synchronized void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public synchronized void removeActionListener(ActionListener l) {
        if ((action != null) && (action == l)) {
            setAction(null);
        } else {
            listenerList.remove(ActionListener.class, l);
        }
    }

    public synchronized ActionListener[] getActionListeners() {
        return listenerList.getListeners(ActionListener.class);
    }

    /** It gives notice that Enter was pressed. */
    protected void fireActionPerformed() {
        Object[] listeners = listenerList.getListenerList();
        int modifiers = 0;
        ActionEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ActionListener.class) {
                if (e == null) {
                    e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                            (command != null) ? command : getText(),
                            java.awt.EventQueue.getMostRecentEventTime(), modifiers);
                }
                ((ActionListener) listeners[i + 1]).actionPerformed(e);
            }
        }
    }

    public void setActionCommand(String command) {
        this.command = command;
    }

    /** It ties the field to an action, like a button. */
    public void setAction(Action a) {
        Action oldValue = getAction();
        if (action == null || !action.equals(a)) {
            action = a;
            if (oldValue != null) {
                removeActionListener(oldValue);
                oldValue.removePropertyChangeListener(actionPropertyChangeListener);
                actionPropertyChangeListener = null;
            }
            configurePropertiesFromAction(action);
            if (action != null) {
                addActionListener(action);
                actionPropertyChangeListener = createActionPropertyChangeListener(action);
                action.addPropertyChangeListener(actionPropertyChangeListener);
            }
            firePropertyChange("action", oldValue, action);
        }
    }

    public Action getAction() {
        return action;
    }

    protected void configurePropertiesFromAction(Action a) {
        setActionCommandFromAction(a);
        setEnabled((a != null) ? a.isEnabled() : true);
        setToolTipText((a != null) ? (String) a.getValue(Action.SHORT_DESCRIPTION) : null);
    }

    private void setActionCommandFromAction(Action a) {
        setActionCommand((a != null) ? (String) a.getValue(Action.ACTION_COMMAND_KEY) : null);
    }

    protected void actionPropertyChanged(Action action, String propertyName) {
        if (Action.ACTION_COMMAND_KEY.equals(propertyName)) {
            setActionCommandFromAction(action);
        } else if ("enabled".equals(propertyName)) {
            setEnabled(action.isEnabled());
        } else if (Action.SHORT_DESCRIPTION.equals(propertyName)) {
            setToolTipText((String) action.getValue(Action.SHORT_DESCRIPTION));
        }
    }

    protected PropertyChangeListener createActionPropertyChangeListener(Action a) {
        return new ActionListenerImpl(this, a);
    }

    /** The component's plus Enter's. */
    public Action[] getActions() {
        return super.getActions();
    }

    /** It fires Enter's action; it is what the key calls. */
    public void postActionEvent() {
        fireActionPerformed();
    }

    /**
     * The model of what is seen when the text does not fit.
     *
     * <p>It is a {@link BoundedRangeModel} and not a pair of integers because that way a scroll
     * bar can be plugged into it without writing anything.
     */
    public BoundedRangeModel getHorizontalVisibility() {
        return visibility;
    }

    public int getScrollOffset() {
        return visibility.getValue();
    }

    public void setScrollOffset(int scrollOffset) {
        visibility.setValue(scrollOffset);
    }

    /** It scrolls so that that rectangle is seen. */
    public void scrollRectToVisible(Rectangle r) {
        Insets i = getInsets();
        int x0 = r.x + visibility.getValue() - i.left;
        int x1 = x0 + r.width;
        if (x0 < visibility.getValue()) {
            setScrollOffset(x0);
        } else if (x1 > visibility.getValue() + visibility.getExtent()) {
            setScrollOffset(x1 - visibility.getExtent());
        }
    }

    boolean hasActionListener() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ActionListener.class) {
                return true;
            }
        }
        return false;
    }

    protected String paramString() {
        String horizontalAlignmentString;
        if (horizontalAlignment == LEFT) {
            horizontalAlignmentString = "LEFT";
        } else if (horizontalAlignment == CENTER) {
            horizontalAlignmentString = "CENTER";
        } else if (horizontalAlignment == RIGHT) {
            horizontalAlignmentString = "RIGHT";
        } else if (horizontalAlignment == LEADING) {
            horizontalAlignmentString = "LEADING";
        } else if (horizontalAlignment == TRAILING) {
            horizontalAlignmentString = "TRAILING";
        } else {
            horizontalAlignmentString = "";
        }
        String commandString = (command != null ? command : "");
        return super.paramString() + ",columns=" + columns + ",columnWidth=" + columnWidth
                + ",command=" + commandString + ",horizontalAlignment="
                + horizontalAlignmentString;
    }

    /** With no accessibility context: there is no assistive technology on this VM. */
    public AccessibleContext getAccessibleContext() {
        return null;
    }

    /** It repaints when what is seen changes; named and not anonymous (#499). */
    static class ScrollRepainter implements ChangeListener, java.io.Serializable {

        private final JTextField field;

        ScrollRepainter(JTextField field) {
            this.field = field;
        }

        public void stateChanged(ChangeEvent e) {
            field.repaint();
        }
    }

    /** It follows the tied action; named and not anonymous (#499). */
    static class ActionListenerImpl implements PropertyChangeListener, java.io.Serializable {

        private final JTextField field;
        private final Action action;

        ActionListenerImpl(JTextField field, Action action) {
            this.field = field;
            this.action = action;
        }

        public void propertyChange(PropertyChangeEvent e) {
            field.actionPropertyChanged(action, e.getPropertyName());
        }
    }
}
