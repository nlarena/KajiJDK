package java.awt;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

/**
 * Un rectángulo de varios renglones donde escribir.
 *
 * <p>A diferencia de {@link TextField}, Enter mete un salto de línea en vez de disparar un evento:
 * no hay forma de "confirmar" un área de texto, y por eso no tiene oyentes de acción.
 *
 * <p>Las barras de desplazamiento se eligen en el constructor y **no se pueden cambiar después**;
 * {@link #getScrollbarVisibility} sólo informa qué se pidió. Es una limitación real de AWT.
 */
public class TextArea extends TextComponent {

    private static final long serialVersionUID = 3692302836626095722L;

    private static int textAreaCounter = 0;

    /** Cuántos renglones de alto pide. */
    int rows;

    /** Cuántas letras de ancho pide. */
    int columns;

    /** Las dos barras. */
    public static final int SCROLLBARS_BOTH = 0;

    /** Sólo la vertical. */
    public static final int SCROLLBARS_VERTICAL_ONLY = 1;

    /** Sólo la horizontal. */
    public static final int SCROLLBARS_HORIZONTAL_ONLY = 2;

    /** Ninguna. */
    public static final int SCROLLBARS_NONE = 3;

    /** Cuáles se pidieron. */
    private int scrollbarVisibility;

    /** Un área vacía con las dos barras. */
    public TextArea() throws HeadlessException {
        this("", 0, 0, SCROLLBARS_BOTH);
    }

    /** Un área con ese texto y las dos barras. */
    public TextArea(String text) throws HeadlessException {
        this(text, 0, 0, SCROLLBARS_BOTH);
    }

    /** Un área vacía de ese tamaño, con las dos barras. */
    public TextArea(int rows, int columns) throws HeadlessException {
        this("", rows, columns, SCROLLBARS_BOTH);
    }

    /** Un área con ese texto y ese tamaño, con las dos barras. */
    public TextArea(String text, int rows, int columns) throws HeadlessException {
        this(text, rows, columns, SCROLLBARS_BOTH);
    }

    /**
     * Un área con todo dicho.
     *
     * <p>Un valor de barras que no sea una de las cuatro constantes se toma como
     * {@link #SCROLLBARS_BOTH}, que es lo que hace el JDK: es un pedido de apariencia, no algo que
     * justifique una excepción.
     */
    public TextArea(String text, int rows, int columns, int scrollbars) throws HeadlessException {
        super(text);
        this.rows = Math.max(0, rows);
        this.columns = Math.max(0, columns);
        if (scrollbars >= SCROLLBARS_BOTH && scrollbars <= SCROLLBARS_NONE) {
            this.scrollbarVisibility = scrollbars;
        } else {
            this.scrollbarVisibility = SCROLLBARS_BOTH;
        }
    }

    String constructComponentName() {
        synchronized (TextArea.class) {
            String n = "text" + textAreaCounter;
            textAreaCounter = textAreaCounter + 1;
            return n;
        }
    }

    /** La declara mostrable. */
    public void addNotify() {
        super.addNotify();
    }

    /**
     * Mete ese texto en esa posición.
     *
     * @throws StringIndexOutOfBoundsException si la posición cae fuera del texto
     */
    public void insert(String str, int pos) {
        this.insertText(str, pos);
    }

    /**
     * Mete texto en esa posición.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #insert}.
     */
    @Deprecated
    public synchronized void insertText(String str, int pos) {
        String t = this.getText();
        this.setText(t.substring(0, pos) + str + t.substring(pos));
    }

    /** Pega ese texto al final. */
    public void append(String str) {
        this.appendText(str);
    }

    /**
     * Pega texto al final.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #append}.
     */
    @Deprecated
    public synchronized void appendText(String str) {
        this.insertText(str, this.getText().length());
    }

    /**
     * Reemplaza ese tramo por ese texto.
     *
     * <p><strong>No</strong> recorta ni ordena las posiciones, a diferencia de
     * {@link TextComponent#select}: un tramo dado vuelta o pasado del texto tira. Ahí la tolerancia
     * tiene sentido —la selección la calcula una búsqueda— y acá no: reemplazar el tramo equivocado
     * en silencio es peor que no reemplazar nada.
     *
     * @throws StringIndexOutOfBoundsException si el tramo no es válido
     */
    public void replaceRange(String str, int start, int end) {
        this.replaceText(str, start, end);
    }

    /**
     * Reemplaza un tramo.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #replaceRange}.
     */
    @Deprecated
    public synchronized void replaceText(String str, int start, int end) {
        String t = this.getText();
        this.setText(t.substring(0, start) + str + t.substring(end));
    }

    /** Cuántos renglones de alto pide. */
    public int getRows() {
        return this.rows;
    }

    /**
     * Cambia el alto pedido.
     *
     * @throws IllegalArgumentException si es negativo
     */
    public void setRows(int rows) {
        if (rows < 0) {
            throw new IllegalArgumentException("rows less than zero.");
        }
        this.rows = rows;
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
        if (columns < 0) {
            throw new IllegalArgumentException("columns less than zero.");
        }
        this.columns = columns;
    }

    /** Qué barras se pidieron al construirla. */
    public int getScrollbarVisibility() {
        return this.scrollbarVisibility;
    }

    /** Lo que necesitaría un área de ese tamaño. */
    public Dimension getPreferredSize(int rows, int columns) {
        return this.getSize();
    }

    /**
     * Lo que necesitaría un área de ese tamaño.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #getPreferredSize(int, int)}.
     */
    @Deprecated
    public Dimension preferredSize(int rows, int columns) {
        return this.getPreferredSize(rows, columns);
    }

    public Dimension getPreferredSize() {
        if (this.rows > 0 && this.columns > 0) {
            return this.getPreferredSize(this.rows, this.columns);
        }
        return super.getPreferredSize();
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

    /** Lo mínimo para un área de ese tamaño. */
    public Dimension getMinimumSize(int rows, int columns) {
        return this.getSize();
    }

    /**
     * Lo mínimo para ese tamaño.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #getMinimumSize(int, int)}.
     */
    @Deprecated
    public Dimension minimumSize(int rows, int columns) {
        return this.getMinimumSize(rows, columns);
    }

    public Dimension getMinimumSize() {
        if (this.rows > 0 && this.columns > 0) {
            return this.getMinimumSize(this.rows, this.columns);
        }
        return super.getMinimumSize();
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

    protected String paramString() {
        String barras = "both";
        if (this.scrollbarVisibility == SCROLLBARS_VERTICAL_ONLY) {
            barras = "vertical";
        } else if (this.scrollbarVisibility == SCROLLBARS_HORIZONTAL_ONLY) {
            barras = "horizontal";
        } else if (this.scrollbarVisibility == SCROLLBARS_NONE) {
            barras = "none";
        }
        return super.paramString() + ",rows=" + this.rows + ",columns=" + this.columns
                + ",scrollbarVisibility=" + barras;
    }

    /** La accesibilidad del área. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTTextArea();
        }
        return this.accessibleContext;
    }

    /**
     * La accesibilidad de un área de texto.
     *
     * <p>Informa `MULTI_LINE` en vez de `SINGLE_LINE`, que es la diferencia que le importa a un
     * lector de pantalla: le dice que tiene que ofrecer navegación por renglón.
     */
    protected class AccessibleAWTTextArea extends AccessibleAWTTextComponent {

        /** Para las subclases. */
        protected AccessibleAWTTextArea() {
        }

        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = super.getAccessibleStateSet();
            s.remove(AccessibleState.SINGLE_LINE);
            s.add(AccessibleState.MULTI_LINE);
            return s;
        }
    }
}
