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
 * Un campo de texto de una sola linea.
 *
 * <h2>Una linea, y lo que eso implica</h2>
 *
 * <p>El documento es un {@link PlainDocument} al que se le filtran los fines de linea: no es que
 * el campo no los muestre, es que no los deja entrar. Esa decision esta en
 * {@link #createDefaultModel} y es lo que hace que pegar un texto de varias lineas en un campo
 * pegue una sola.
 *
 * <p>Enter dispara un {@link ActionEvent} en vez de escribir: es la unica tecla que un campo trata
 * distinto, y es lo que permite que un formulario se envie desde el teclado.
 *
 * <h2>El ancho preferido</h2>
 *
 * <p>Se pide en <em>columnas</em>, no en pixeles, y una columna es el ancho de la letra "m" de la
 * fuente actual. Es una medida vieja y sigue siendo la mejor que hay: un campo de veinte columnas
 * sigue entrando veinte caracteres cuando alguien cambia el tamano de la letra.
 */
public class JTextField extends JTextComponent implements SwingConstants {

    /** El nombre de la accion que dispara Enter. */
    public static final String notifyAction = "notify-field-accept";

    private static final String uiClassID = "TextFieldUI";

    private int columns;
    private int columnWidth;
    private int horizontalAlignment = LEADING;
    private transient BoundedRangeModel visibility;
    private Action action;
    private String command;
    private PropertyChangeListener actionPropertyChangeListener;

    /** Un campo vacio. */
    public JTextField() {
        this(null, null, 0);
    }

    public JTextField(String text) {
        this(null, text, 0);
    }

    /** Un campo vacio de ese ancho en columnas. */
    public JTextField(int columns) {
        this(null, null, columns);
    }

    public JTextField(String text, int columns) {
        this(null, text, columns);
    }

    /** El constructor al que llegan todos los demas. */
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

    /** Si: un campo tiene tamano propio y el maquetado para aca. */
    public boolean isValidateRoot() {
        return false;
    }

    public int getHorizontalAlignment() {
        return horizontalAlignment;
    }

    /** Como se alinea el texto cuando sobra lugar. */
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

    /** Un documento de texto plano sin fines de linea; ver la nota de la clase. */
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

    /** El ancho de una columna: el de la letra "m"; ver la nota de la clase. */
    protected int getColumnWidth() {
        if (columnWidth == 0) {
            FontMetrics metrics = getFontMetrics(getFont());
            columnWidth = metrics.charWidth('m');
        }
        return columnWidth;
    }

    /** El ancho de las columnas pedidas, o el del texto si no se pidieron. */
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        if (columns != 0) {
            Insets insets = getInsets();
            size.width = columns * getColumnWidth() + insets.left + insets.right;
        }
        return size;
    }

    /** Cambiar la fuente cambia el ancho de columna. */
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

    /** Avisa que se apreto Enter. */
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

    /** Ata el campo a una accion, como un boton. */
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
        return new EscuchaDeAccion(this, a);
    }

    /** Las del componente mas la de Enter. */
    public Action[] getActions() {
        return super.getActions();
    }

    /** Dispara la accion de Enter; es lo que llama la tecla. */
    public void postActionEvent() {
        fireActionPerformed();
    }

    /**
     * El modelo de lo que se ve cuando el texto no entra.
     *
     * <p>Es un {@link BoundedRangeModel} y no un par de enteros porque asi se le puede enchufar
     * una barra de desplazamiento sin escribir nada.
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

    /** Desplaza para que ese rectangulo se vea. */
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

    /** Sin contexto de accesibilidad: no hay tecnologia asistiva en esta VM. */
    public AccessibleContext getAccessibleContext() {
        return null;
    }

    /** Repinta cuando cambia lo que se ve; nombrada y no anonima (#499). */
    static class ScrollRepainter implements ChangeListener, java.io.Serializable {

        private final JTextField campo;

        ScrollRepainter(JTextField campo) {
            this.campo = campo;
        }

        public void stateChanged(ChangeEvent e) {
            campo.repaint();
        }
    }

    /** Sigue a la accion atada; nombrada y no anonima (#499). */
    static class EscuchaDeAccion implements PropertyChangeListener, java.io.Serializable {

        private final JTextField campo;
        private final Action accion;

        EscuchaDeAccion(JTextField campo, Action accion) {
            this.campo = campo;
            this.accion = accion;
        }

        public void propertyChange(PropertyChangeEvent e) {
            campo.actionPropertyChanged(accion, e.getPropertyName());
        }
    }
}
