package javax.swing;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.accessibility.AccessibleContext;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

/**
 * Un area de texto de varias lineas, sin estilos.
 *
 * <h2>Filas y columnas no son un tamano</h2>
 *
 * <p>{@link #setRows} y {@link #setColumns} no fijan el tamano: fijan el <em>preferido</em>. El
 * area puede terminar mas grande o mas chica segun el acomodador que la contenga. Es la fuente de
 * confusion mas comun con esta clase: pedir veinte columnas y ver diez.
 *
 * <p>Una columna es el ancho de la letra <code>m</code> en la tipografia actual. No es una medida
 * exacta salvo con tipografia de ancho fijo, y por eso el area de texto se usa casi siempre con
 * una.
 *
 * <h2>El corte de linea es de la vista, no del documento</h2>
 *
 * <p>Con {@link #setLineWrap} prendido, una linea larga se ve cortada en varias. El documento no
 * cambia: no hay ningun fin de linea nuevo, y {@link #getLineCount} sigue contando las de verdad.
 * Es lo que se quiere -- guardar el texto tal como se escribio -- y lo que sorprende al contar
 * lineas.
 *
 * <h2>Tampoco se desplaza sola</h2>
 *
 * <p>Como {@link JList}: hay que meterla en un {@link JScrollPane}. Un area sin desplazador crece
 * con el texto hasta desbordar lo que la contiene.
 */
public class JTextArea extends JTextComponent {

    private static final String uiClassID = "TextAreaUI";

    private int rows;
    private int columns;
    private int columnWidth;
    private int rowHeight;
    private boolean wordWrap;
    private boolean wrap;
    private AccessibleContext accessibleContext;

    /** Un area vacia. */
    public JTextArea() {
        this(null, null, 0, 0);
    }

    /** Un area con ese texto. */
    public JTextArea(String text) {
        this(null, text, 0, 0);
    }

    /** Un area de ese tamano preferido; ver la nota de la clase. */
    public JTextArea(int rows, int columns) {
        this(null, null, rows, columns);
    }

    /** Un area con ese texto y ese tamano preferido. */
    public JTextArea(String text, int rows, int columns) {
        this(null, text, rows, columns);
    }

    /** Un area sobre ese documento. */
    public JTextArea(Document doc) {
        this(doc, null, 0, 0);
    }

    /**
     * Un area sobre ese documento, con ese texto y ese tamano.
     *
     * @throws IllegalArgumentException si las filas o las columnas son negativas.
     */
    public JTextArea(Document doc, String text, int rows, int columns) {
        super();
        this.rows = rows;
        this.columns = columns;
        if (doc == null) {
            doc = createDefaultModel();
        }
        setDocument(doc);
        if (text != null) {
            setText(text);
            select(0, 0);
        }
        if (rows < 0) {
            throw new IllegalArgumentException("rows: " + rows);
        }
        if (columns < 0) {
            throw new IllegalArgumentException("columns: " + columns);
        }
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** Un documento de texto plano; es todo lo que un area necesita. */
    protected Document createDefaultModel() {
        return new PlainDocument();
    }

    /**
     * Cada cuantas columnas cae una marca de tabulacion.
     *
     * <p>Se guarda como propiedad del documento, no del area: quien dibuja el texto es la vista, y
     * la vista mira el documento.
     */
    public void setTabSize(int size) {
        Document doc = getDocument();
        if (doc != null) {
            int old = getTabSize();
            doc.putProperty(PlainDocument.tabSizeAttribute, Integer.valueOf(size));
            firePropertyChange("tabSize", old, size);
        }
    }

    public int getTabSize() {
        int size = 8;
        Document doc = getDocument();
        if (doc != null) {
            Integer i = (Integer) doc.getProperty(PlainDocument.tabSizeAttribute);
            if (i != null) {
                size = i.intValue();
            }
        }
        return size;
    }

    /** Si las lineas largas se ven cortadas; ver la nota de la clase. */
    public void setLineWrap(boolean wrap) {
        boolean old = this.wrap;
        this.wrap = wrap;
        firePropertyChange("lineWrap", old, wrap);
    }

    public boolean getLineWrap() {
        return wrap;
    }

    /** Si el corte respeta las palabras o parte donde caiga. */
    public void setWrapStyleWord(boolean word) {
        boolean old = this.wordWrap;
        this.wordWrap = word;
        firePropertyChange("wrapStyleWord", old, word);
    }

    public boolean getWrapStyleWord() {
        return wordWrap;
    }

    /**
     * En que linea cae esa posicion.
     *
     * @throws BadLocationException si la posicion no existe.
     */
    public int getLineOfOffset(int offset) throws BadLocationException {
        Document doc = getDocument();
        if (offset < 0) {
            throw new BadLocationException("Can't translate offset to line", -1);
        }
        if (offset > doc.getLength()) {
            throw new BadLocationException("Can't translate offset to line",
                    doc.getLength() + 1);
        }
        Element map = getDocument().getDefaultRootElement();
        return map.getElementIndex(offset);
    }

    /** Cuantas lineas tiene el texto; las de verdad, no las que se ven. */
    public int getLineCount() {
        Element map = getDocument().getDefaultRootElement();
        return map.getElementCount();
    }

    /**
     * Donde empieza esa linea.
     *
     * @throws BadLocationException si la linea no existe.
     */
    public int getLineStartOffset(int line) throws BadLocationException {
        int lineCount = getLineCount();
        if (line < 0) {
            throw new BadLocationException("Negative line", -1);
        }
        if (line >= lineCount) {
            throw new BadLocationException("No such line", getDocument().getLength() + 1);
        }
        Element map = getDocument().getDefaultRootElement();
        Element lineElem = map.getElement(line);
        return lineElem.getStartOffset();
    }

    /**
     * Donde termina esa linea, contando el fin de linea.
     *
     * @throws BadLocationException si la linea no existe.
     */
    public int getLineEndOffset(int line) throws BadLocationException {
        int lineCount = getLineCount();
        if (line < 0) {
            throw new BadLocationException("Negative line", -1);
        }
        if (line >= lineCount) {
            throw new BadLocationException("No such line", getDocument().getLength() + 1);
        }
        Element map = getDocument().getDefaultRootElement();
        Element lineElem = map.getElement(line);
        int endOffset = lineElem.getEndOffset();
        // La ultima linea no tiene fin de linea que descontar.
        return ((line == lineCount - 1) ? (endOffset - 1) : endOffset);
    }

    /** Mete texto en esa posicion. */
    public void insert(String str, int pos) {
        Document doc = getDocument();
        if (doc != null) {
            try {
                doc.insertString(pos, str, null);
            } catch (BadLocationException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    /** Agrega texto al final. */
    public void append(String str) {
        Document doc = getDocument();
        if (doc != null) {
            try {
                doc.insertString(doc.getLength(), str, null);
            } catch (BadLocationException e) {
                // El documento no puede estar mas corto que su propio largo.
            }
        }
    }

    /**
     * Reemplaza el texto entre esas dos posiciones.
     *
     * @throws IllegalArgumentException si el fin es anterior al inicio.
     */
    public void replaceRange(String str, int start, int end) {
        if (end < start) {
            throw new IllegalArgumentException("end before start");
        }
        Document doc = getDocument();
        if (doc != null) {
            try {
                if (doc instanceof javax.swing.text.AbstractDocument) {
                    ((javax.swing.text.AbstractDocument) doc).replace(start, end - start, str,
                            null);
                } else {
                    doc.remove(start, end - start);
                    if (str != null && str.length() > 0) {
                        doc.insertString(start, str, null);
                    }
                }
            } catch (BadLocationException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public int getRows() {
        return rows;
    }

    /**
     * Cuantas filas se prefieren.
     *
     * @throws IllegalArgumentException si es negativo.
     */
    public void setRows(int rows) {
        int oldVal = this.rows;
        if (rows < 0) {
            throw new IllegalArgumentException("rows less than zero.");
        }
        if (rows != oldVal) {
            this.rows = rows;
            invalidate();
        }
    }

    /** El alto de una fila: el de la tipografia. */
    protected int getRowHeight() {
        if (rowHeight == 0) {
            FontMetrics metrics = getFontMetrics(getFont());
            rowHeight = metrics.getHeight();
        }
        return rowHeight;
    }

    public int getColumns() {
        return columns;
    }

    /**
     * Cuantas columnas se prefieren.
     *
     * @throws IllegalArgumentException si es negativo.
     */
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

    /** El ancho de una columna: el de la letra {@code m}; ver la nota de la clase. */
    protected int getColumnWidth() {
        if (columnWidth == 0) {
            FontMetrics metrics = getFontMetrics(getFont());
            columnWidth = metrics.charWidth('m');
        }
        return columnWidth;
    }

    /**
     * El tamano preferido.
     *
     * <p>Es el que pida el texto, pero nunca menos de las filas y columnas pedidas. De ahi que un
     * area vacia con veinte columnas ya ocupe lugar.
     */
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d = (d == null) ? new Dimension(400, 400) : d;
        Insets insets = getInsets();

        if (columns != 0) {
            d.width = Math.max(d.width, columns * getColumnWidth() + insets.left + insets.right);
        }
        if (rows != 0) {
            d.height = Math.max(d.height, rows * getRowHeight() + insets.top + insets.bottom);
        }
        return d;
    }

    /** Cambiar la tipografia invalida el alto y el ancho medidos. */
    public void setFont(Font f) {
        super.setFont(f);
        rowHeight = 0;
        columnWidth = 0;
    }

    protected String paramString() {
        return super.paramString();
    }

    /**
     * Si el area se estira al ancho del desplazador.
     *
     * <p>Con corte de linea si: cortar en el ancho del desplazador es justamente lo que se pidio.
     * Sin corte no, porque entonces la barra horizontal tiene sentido.
     */
    public boolean getScrollableTracksViewportWidth() {
        return (wrap) ? true : super.getScrollableTracksViewportWidth();
    }

    /** Cuanto pedirle al desplazador: las filas y columnas preferidas. */
    public Dimension getPreferredScrollableViewportSize() {
        Dimension size = super.getPreferredScrollableViewportSize();
        size = (size == null) ? new Dimension(400, 400) : size;
        Insets insets = getInsets();
        size.width = (columns == 0) ? size.width
                : columns * getColumnWidth() + insets.left + insets.right;
        size.height = (rows == 0) ? size.height
                : rows * getRowHeight() + insets.top + insets.bottom;
        return size;
    }

    /** De a cuanto avanza la rueda: una fila o una columna. */
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation,
            int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            return getRowHeight();
        }
        if (orientation == SwingConstants.HORIZONTAL) {
            return getColumnWidth();
        }
        throw new IllegalArgumentException("Invalid orientation: " + orientation);
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
