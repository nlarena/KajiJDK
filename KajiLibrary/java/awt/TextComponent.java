package java.awt;

import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.awt.im.InputMethodRequests;
import java.util.EventListener;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleText;

/**
 * Lo que tienen en común un campo de texto y un área de texto: el texto, la selección y el cursor.
 *
 * <p>No se puede instanciar —su constructor es de paquete— porque un componente de texto sin decidir
 * si es de un renglón o de varios no es nada.
 *
 * <p>La selección se guarda como dos posiciones, y la clase se encarga de que estén ordenadas y
 * dentro del texto. El cursor no es un tercer número: es el principio de la selección.
 */
public class TextComponent extends Component implements Accessible {

    private static final long serialVersionUID = -2214773872412987419L;

    /** Lo que dice. */
    String text;

    /** Si se puede escribir en él. */
    boolean editable = true;

    /** Dónde arranca la selección. */
    int selectionStart;

    /** Dónde termina. */
    int selectionEnd;

    /** Si alguien de afuera le fijó el color de fondo. */
    boolean backgroundSetByClientCode = false;

    /** Los oyentes, encadenados. */
    protected transient TextListener textListener;

    /** Con ese texto; `null` cuenta como vacío. */
    TextComponent(String text) throws HeadlessException {
        this.text = text == null ? "" : text;
    }

    /**
     * Prende o apaga los métodos de entrada.
     *
     * <p>Sin pantalla no hay ninguno activo, pero el pedido se acepta sin romper: un programa que lo
     * llama en el armado no tiene por qué fallar por eso.
     */
    public void enableInputMethods(boolean enable) {
        super.enableInputMethods(enable);
    }

    boolean areInputMethodsEnabled() {
        return false;
    }

    /**
     * Lo que el método de entrada necesita saber del componente.
     *
     * @return `null` siempre: sin método de entrada activo no hay nada que contestar, y contestar
     *     algo vacío haría creer que la composición sobre el componente funciona
     */
    public InputMethodRequests getInputMethodRequests() {
        return null;
    }

    /** Lo declara mostrable. */
    public void addNotify() {
        super.addNotify();
    }

    /** Lo declara no mostrable. */
    public void removeNotify() {
        super.removeNotify();
    }

    /**
     * Cambia el texto.
     *
     * <p>La selección se recorta al texto nuevo: dejarla apuntando afuera sería un estado inválido
     * que después explota en cualquier `getSelectedText`.
     *
     * @param t el texto; `null` cuenta como vacío
     */
    public synchronized void setText(String t) {
        this.text = t == null ? "" : t;
        if (this.selectionStart > this.text.length()) {
            this.selectionStart = this.text.length();
        }
        if (this.selectionEnd > this.text.length()) {
            this.selectionEnd = this.text.length();
        }
    }

    /** Lo que dice. */
    public synchronized String getText() {
        return this.text;
    }

    /**
     * Lo que está seleccionado.
     *
     * @return el tramo seleccionado, o la cadena vacía si no hay selección
     */
    public synchronized String getSelectedText() {
        return this.getText().substring(this.getSelectionStart(), this.getSelectionEnd());
    }

    /** Si se puede escribir en él. */
    public boolean isEditable() {
        return this.editable;
    }

    /** Lo deja escribir o no. */
    public synchronized void setEditable(boolean b) {
        this.editable = b;
    }

    /**
     * El color de fondo.
     *
     * <p>Un componente de texto **no editable** hereda el fondo del padre aunque a él nunca se lo
     * hayan puesto, y ése es el motivo de que esto esté redefinido: el color de un campo de sólo
     * lectura tiene que verse como el del panel que lo contiene.
     */
    public Color getBackground() {
        if (!this.editable && !this.backgroundSetByClientCode) {
            Container p = this.getParent();
            if (p != null) {
                return p.getBackground();
            }
        }
        return super.getBackground();
    }

    /** Le fija el color de fondo. */
    public void setBackground(Color c) {
        this.backgroundSetByClientCode = true;
        super.setBackground(c);
    }

    /** Dónde arranca la selección. */
    public synchronized int getSelectionStart() {
        return this.selectionStart;
    }

    /**
     * Mueve el principio de la selección.
     *
     * <p>Pasarlo del final la arrastra: la selección nunca queda dada vuelta.
     */
    public synchronized void setSelectionStart(int selectionStart) {
        this.select(selectionStart, this.getSelectionEnd());
    }

    /** Dónde termina la selección. */
    public synchronized int getSelectionEnd() {
        return this.selectionEnd;
    }

    /** Mueve el final de la selección; ponerlo antes del principio lo arrastra. */
    public synchronized void setSelectionEnd(int selectionEnd) {
        this.select(this.getSelectionStart(), selectionEnd);
    }

    /**
     * Selecciona ese tramo.
     *
     * <p>Las posiciones se recortan al texto y se ordenan, así que cualquier par sirve. Es
     * deliberadamente tolerante: el llamador típico calcula posiciones a partir de una búsqueda y no
     * tiene por qué validar.
     */
    public synchronized void select(int selectionStart, int selectionEnd) {
        String t = this.getText();
        if (selectionStart > t.length()) {
            selectionStart = t.length();
        }
        if (selectionStart < 0) {
            selectionStart = 0;
        }
        if (selectionEnd > t.length()) {
            selectionEnd = t.length();
        }
        if (selectionEnd < selectionStart) {
            selectionEnd = selectionStart;
        }
        this.selectionStart = selectionStart;
        this.selectionEnd = selectionEnd;
    }

    /** Selecciona todo. */
    public synchronized void selectAll() {
        this.select(0, this.getText().length());
    }

    /**
     * Pone el cursor ahí.
     *
     * <p>Es la selección vacía en esa posición, no un tercer estado.
     *
     * @throws IllegalArgumentException si la posición es negativa
     */
    public synchronized void setCaretPosition(int position) {
        if (position < 0) {
            throw new IllegalArgumentException("position less than zero.");
        }
        int n = this.getText().length();
        if (position > n) {
            position = n;
        }
        this.select(position, position);
    }

    /**
     * Dónde está el cursor.
     *
     * <p>Es el **principio** de la selección, no el final. Suena al revés —al arrastrar, el cursor
     * va donde está el mouse, que es el final— pero es lo que devuelve AWT sin ventana, y se
     * comprobó contra el JDK.
     */
    public synchronized int getCaretPosition() {
        return this.getSelectionStart();
    }

    /** Agrega un oyente; `null` no hace nada. */
    public synchronized void addTextListener(TextListener l) {
        if (l == null) {
            return;
        }
        this.textListener = AWTEventMulticaster.add(this.textListener, l);
        this.enableEvents(AWTEvent.TEXT_EVENT_MASK);
    }

    /** Saca un oyente. */
    public synchronized void removeTextListener(TextListener l) {
        if (l == null) {
            return;
        }
        this.textListener = AWTEventMulticaster.remove(this.textListener, l);
    }

    /** Los oyentes puestos. */
    public synchronized TextListener[] getTextListeners() {
        return AWTEventMulticaster.getListeners(this.textListener, TextListener.class);
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (listenerType == TextListener.class) {
            return AWTEventMulticaster.getListeners(this.textListener, listenerType);
        }
        return super.getListeners(listenerType);
    }

    protected void processEvent(AWTEvent e) {
        if (e instanceof TextEvent) {
            this.processTextEvent((TextEvent) e);
            return;
        }
        super.processEvent(e);
    }

    /** Les avisa a los oyentes de texto. */
    protected void processTextEvent(TextEvent e) {
        TextListener l = this.textListener;
        if (l != null) {
            l.textValueChanged(e);
        }
    }

    protected String paramString() {
        String s = super.paramString() + ",text=" + this.getText();
        if (this.editable) {
            s = s + ",editable";
        }
        return s + ",selection=" + this.getSelectionStart() + "-" + this.getSelectionEnd();
    }

    /** La accesibilidad del componente de texto. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTTextComponent();
        }
        return this.accessibleContext;
    }

    /**
     * La accesibilidad de un componente de texto.
     *
     * <p>Implementa {@link AccessibleText}, que es lo que le permite a un lector de pantalla recorrer
     * el contenido por letra, palabra o renglón. Lo que **no** implementa es
     * {@link AccessibleText#getCharacterBounds}, que devuelve `null`, ni
     * {@link AccessibleText#getCharacterAttribute}: los dos necesitan la tipografía medida sobre una
     * pantalla, y sin ella cualquier respuesta sería inventada.
     */
    protected class AccessibleAWTTextComponent extends AccessibleAWTComponent
            implements AccessibleText {

        /** Para las subclases. */
        protected AccessibleAWTTextComponent() {
        }

        public AccessibleText getAccessibleText() {
            return this;
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.TEXT;
        }

        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = super.getAccessibleStateSet();
            if (TextComponent.this.isEditable()) {
                s.add(AccessibleState.EDITABLE);
            }
            s.add(AccessibleState.SINGLE_LINE);
            return s;
        }

        /**
         * Qué letra hay en ese punto de la pantalla.
         *
         * @return -1 siempre: sin tipografía medida no hay forma de saberlo
         */
        public int getIndexAtPoint(Point p) {
            return -1;
        }

        /**
         * Dónde cae esa letra en la pantalla.
         *
         * @return `null` siempre, por lo mismo
         */
        public Rectangle getCharacterBounds(int i) {
            return null;
        }

        /** Cuántas letras tiene. */
        public int getCharCount() {
            return TextComponent.this.getText().length();
        }

        /** Dónde está el cursor. */
        public int getCaretPosition() {
            return TextComponent.this.getCaretPosition();
        }

        /**
         * Los atributos de esa letra.
         *
         * @return `null` siempre: el texto de AWT no tiene atributos por letra
         */
        public javax.swing.text.AttributeSet getCharacterAttribute(int i) {
            return null;
        }

        public int getSelectionStart() {
            return TextComponent.this.getSelectionStart();
        }

        public int getSelectionEnd() {
            return TextComponent.this.getSelectionEnd();
        }

        public String getSelectedText() {
            String s = TextComponent.this.getSelectedText();
            return s.isEmpty() ? null : s;
        }

        /**
         * La letra, palabra o renglón que está en esa posición.
         *
         * @return el tramo, o `null` si la posición no existe o la parte no es una de las tres
         */
        public String getAtIndex(int part, int index) {
            return this.tramo(part, index, 0);
        }

        /** Lo mismo, pero lo que viene después. */
        public String getAfterIndex(int part, int index) {
            return this.tramo(part, index, 1);
        }

        /** Lo mismo, pero lo que viene antes. */
        public String getBeforeIndex(int part, int index) {
            return this.tramo(part, index, -1);
        }

        /**
         * Recorta la letra, palabra o renglón que está en esa posición, corrida en esa dirección.
         *
         * @param direccion -1 el anterior, 0 el de la posición, 1 el siguiente
         */
        private String tramo(int part, int index, int direccion) {
            String t = TextComponent.this.getText();
            if (index < 0 || index >= t.length()) {
                return null;
            }
            if (part == AccessibleText.CHARACTER) {
                int i = index + direccion;
                if (i < 0 || i >= t.length()) {
                    return null;
                }
                return t.substring(i, i + 1);
            }
            if (part == AccessibleText.WORD) {
                return this.palabra(t, index, direccion);
            }
            if (part == AccessibleText.SENTENCE) {
                return this.renglon(t, index, direccion);
            }
            return null;
        }

        /** La palabra de esa posición, corrida en esa dirección. */
        private String palabra(String t, int index, int direccion) {
            int desde = index;
            int hasta = index;
            while (desde > 0 && !Character.isWhitespace(t.charAt(desde - 1))) {
                desde = desde - 1;
            }
            while (hasta < t.length() && !Character.isWhitespace(t.charAt(hasta))) {
                hasta = hasta + 1;
            }
            if (direccion == 0) {
                return desde == hasta ? null : t.substring(desde, hasta);
            }
            if (direccion > 0) {
                int i = hasta;
                while (i < t.length() && Character.isWhitespace(t.charAt(i))) {
                    i = i + 1;
                }
                if (i >= t.length()) {
                    return null;
                }
                int j = i;
                while (j < t.length() && !Character.isWhitespace(t.charAt(j))) {
                    j = j + 1;
                }
                return t.substring(i, j);
            }
            int i = desde;
            while (i > 0 && Character.isWhitespace(t.charAt(i - 1))) {
                i = i - 1;
            }
            if (i <= 0) {
                return null;
            }
            int j = i;
            while (j > 0 && !Character.isWhitespace(t.charAt(j - 1))) {
                j = j - 1;
            }
            return t.substring(j, i);
        }

        /** El renglón de esa posición, corrido en esa dirección. */
        private String renglon(String t, int index, int direccion) {
            int desde = t.lastIndexOf('\n', index - 1) + 1;
            int hasta = t.indexOf('\n', index);
            if (hasta < 0) {
                hasta = t.length();
            } else {
                hasta = hasta + 1;
            }
            if (direccion == 0) {
                return t.substring(desde, hasta);
            }
            if (direccion > 0) {
                if (hasta >= t.length()) {
                    return null;
                }
                int fin = t.indexOf('\n', hasta);
                return fin < 0 ? t.substring(hasta) : t.substring(hasta, fin + 1);
            }
            if (desde <= 0) {
                return null;
            }
            int inicio = t.lastIndexOf('\n', desde - 2) + 1;
            return t.substring(inicio, desde);
        }
    }
}
