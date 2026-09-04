package java.awt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

/**
 * Un renglón donde escribir.
 *
 * <p>Dispara un {@link ActionEvent} cuando el usuario aprieta Enter, que es lo que lo hace útil para
 * un buscador o un cuadro de diálogo: el campo mismo avisa que terminaron de escribir.
 *
 * <p>Tiene un modo de contraseña: {@link #setEchoChar} hace que muestre siempre el mismo carácter en
 * vez de lo que se escribió. Ojo con que **el texto se guarda igual**, en claro, y
 * {@link #getText} lo devuelve tal cual: el eco es cosmético, no es cifrado.
 */
public class TextField extends TextComponent {

    private static final long serialVersionUID = -2966288784432217853L;

    private static int textFieldCounter = 0;

    /** Cuántas letras de ancho pide. */
    int columns;

    /** Qué muestra en vez de lo escrito, o 0 si muestra lo escrito. */
    char echoChar;

    /** Los oyentes de acción, encadenados. */
    transient ActionListener actionListener;

    /** Un campo vacío. */
    public TextField() throws HeadlessException {
        this("", 0);
    }

    /** Un campo con ese texto, del ancho del texto. */
    public TextField(String text) throws HeadlessException {
        this(text, text == null ? 0 : text.length());
    }

    /** Un campo vacío de ese ancho. */
    public TextField(int columns) throws HeadlessException {
        this("", columns);
    }

    /**
     * Un campo con ese texto y ese ancho.
     *
     * <p>Un ancho negativo se toma como cero, en vez de romper: el ancho es una sugerencia de
     * distribución, no un invariante.
     */
    public TextField(String text, int columns) throws HeadlessException {
        super(text);
        this.columns = Math.max(0, columns);
    }

    String constructComponentName() {
        synchronized (TextField.class) {
            String n = "textfield" + textFieldCounter;
            textFieldCounter = textFieldCounter + 1;
            return n;
        }
    }

    /** Lo declara mostrable. */
    public void addNotify() {
        super.addNotify();
    }

    /**
     * Qué muestra en vez de lo escrito.
     *
     * @return el carácter de eco, o 0 si muestra lo escrito
     */
    public char getEchoChar() {
        return this.echoChar;
    }

    /**
     * Hace que muestre siempre ese carácter.
     *
     * @param c el carácter, o 0 para volver a mostrar lo escrito
     */
    public void setEchoChar(char c) {
        this.setEchoCharacter(c);
    }

    /**
     * Hace que muestre siempre ese carácter.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #setEchoChar}.
     */
    @Deprecated
    public synchronized void setEchoCharacter(char c) {
        this.echoChar = c;
    }

    /** Cambia el texto y manda el cursor al principio. */
    public void setText(String t) {
        super.setText(t);
    }

    /** Si está en modo de eco. */
    public boolean echoCharIsSet() {
        return this.echoChar != 0;
    }

    /** Cuántas letras de ancho pide. */
    public int getColumns() {
        return this.columns;
    }

    /**
     * Cambia el ancho pedido.
     *
     * @throws IllegalArgumentException si es negativo
     */
    public void setColumns(int columns) {
        synchronized (this) {
            if (columns < 0) {
                throw new IllegalArgumentException("columns less than zero.");
            }
            this.columns = columns;
        }
    }

    /** Lo que necesitaría un campo de ese ancho. */
    public Dimension getPreferredSize(int columns) {
        return this.getSize();
    }

    /**
     * Lo que necesitaría un campo de ese ancho.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #getPreferredSize(int)}.
     */
    @Deprecated
    public Dimension preferredSize(int columns) {
        return this.getPreferredSize(columns);
    }

    public Dimension getPreferredSize() {
        return this.columns > 0 ? this.getPreferredSize(this.columns) : super.getPreferredSize();
    }

    /**
     * Lo que necesita.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #getPreferredSize()}.
     */
    @Deprecated
    public Dimension preferredSize() {
        return this.getPreferredSize();
    }

    /** Lo mínimo para un campo de ese ancho. */
    public Dimension getMinimumSize(int columns) {
        return this.getSize();
    }

    /**
     * Lo mínimo para ese ancho.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #getMinimumSize(int)}.
     */
    @Deprecated
    public Dimension minimumSize(int columns) {
        return this.getMinimumSize(columns);
    }

    public Dimension getMinimumSize() {
        return this.columns > 0 ? this.getMinimumSize(this.columns) : super.getMinimumSize();
    }

    /**
     * Lo mínimo que necesita.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #getMinimumSize()}.
     */
    @Deprecated
    public Dimension minimumSize() {
        return this.getMinimumSize();
    }

    /** Agrega un oyente de acción; `null` no hace nada. */
    public synchronized void addActionListener(ActionListener l) {
        if (l == null) {
            return;
        }
        this.actionListener = AWTEventMulticaster.add(this.actionListener, l);
        this.enableEvents(AWTEvent.ACTION_EVENT_MASK);
    }

    /** Saca un oyente de acción. */
    public synchronized void removeActionListener(ActionListener l) {
        if (l == null) {
            return;
        }
        this.actionListener = AWTEventMulticaster.remove(this.actionListener, l);
    }

    /** Los oyentes de acción. */
    public synchronized ActionListener[] getActionListeners() {
        return AWTEventMulticaster.getListeners(this.actionListener, ActionListener.class);
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (listenerType == ActionListener.class) {
            return AWTEventMulticaster.getListeners(this.actionListener, listenerType);
        }
        return super.getListeners(listenerType);
    }

    protected void processEvent(AWTEvent e) {
        if (e instanceof ActionEvent) {
            this.processActionEvent((ActionEvent) e);
            return;
        }
        super.processEvent(e);
    }

    /** Les avisa a los oyentes de acción. */
    protected void processActionEvent(ActionEvent e) {
        ActionListener l = this.actionListener;
        if (l != null) {
            l.actionPerformed(e);
        }
    }

    protected String paramString() {
        String s = super.paramString();
        if (this.echoChar != 0) {
            s = s + ",echo=" + this.echoChar;
        }
        return s;
    }

    /** La accesibilidad del campo. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTTextField();
        }
        return this.accessibleContext;
    }

    /**
     * La accesibilidad de un campo de texto.
     *
     * <p>Lo único que agrega es el estado `SINGLE_LINE`, que ya viene de
     * {@link TextComponent.AccessibleAWTTextComponent}: es de un renglón por definición.
     */
    protected class AccessibleAWTTextField extends AccessibleAWTTextComponent {

        /** Para las subclases. */
        protected AccessibleAWTTextField() {
        }

        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = super.getAccessibleStateSet();
            s.add(AccessibleState.SINGLE_LINE);
            return s;
        }
    }
}
