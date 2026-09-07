package javax.swing.text;

import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;

/**
 * La base de los que escriben un documento a texto.
 *
 * <h2>Que resuelve, y que no</h2>
 *
 * <p>No sabe nada del formato de salida: eso lo pone {@link #write()}, que cada subclase escribe.
 * Lo que trae hecho es todo lo aburrido que igual hay que hacer bien: recorrer los elementos del
 * rango pedido, sangrar, cortar lineas en el ancho maximo sin partir palabras, y sacar el texto de
 * un elemento hoja.
 *
 * <h2>El corte de linea</h2>
 *
 * <p>Cuando el corte esta prendido, cada escritura busca un espacio hacia atras y corta ahi. Si no
 * hay ningun espacio, la linea se pasa de largo: partir una palabra al medio seria peor que una
 * linea larga, porque cambiaria el contenido.
 *
 * <p>La sangria se escribe recien cuando hay algo que poner en la linea. Escribirla antes dejaria
 * lineas con espacios y nada mas.
 */
public abstract class AbstractWriter {

    private ElementIterator it;
    private Writer out;
    private int indentLevel = 0;
    private int indentSpace = 2;
    private Document doc = null;
    private int maxLineLength = 100;
    private int currLength = 0;
    private int startOffset = 0;
    private int endOffset = 0;
    private int offsetIndent = 0;

    private String lineSeparator;
    private boolean canWrapLines;
    private boolean isLineEmpty;
    private char[] indentChars;
    private char[] tempChars;
    private char[] newlineChars;
    private Segment segment;

    /** El fin de linea que se usa si nadie pide otro. */
    protected static final char NEWLINE = '\n';

    /** Escribe el documento entero. */
    protected AbstractWriter(Writer w, Document doc) {
        this(w, doc, 0, doc.getLength());
    }

    /** Escribe el tramo pedido del documento. */
    protected AbstractWriter(Writer w, Document doc, int pos, int len) {
        this.doc = doc;
        it = new ElementIterator(doc.getDefaultRootElement());
        out = w;
        startOffset = pos;
        endOffset = pos + len;
        Object docNewline = doc.getProperty(DefaultEditorKit.EndOfLineStringProperty);
        if (docNewline instanceof String) {
            setLineSeparator((String) docNewline);
        } else {
            String newline = null;
            try {
                newline = System.getProperty("line.separator");
            } catch (SecurityException se) {
                // Sin permiso para leerla: se usa la de siempre.
            }
            if (newline == null) {
                newline = "\n";
            }
            setLineSeparator(newline);
        }
        canWrapLines = true;
    }

    /** Escribe el arbol que cuelga de ese elemento. */
    protected AbstractWriter(Writer w, Element root) {
        this(w, root, 0, root.getEndOffset());
    }

    /** Escribe el tramo pedido del arbol de ese elemento. */
    protected AbstractWriter(Writer w, Element root, int pos, int len) {
        this.doc = root.getDocument();
        it = new ElementIterator(root);
        out = w;
        startOffset = pos;
        endOffset = pos + len;
        canWrapLines = true;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    protected ElementIterator getElementIterator() {
        return it;
    }

    protected Writer getWriter() {
        return out;
    }

    protected Document getDocument() {
        return doc;
    }

    /** Si ese elemento se pisa con el tramo que se esta escribiendo. */
    protected boolean inRange(Element next) {
        int startOffset = getStartOffset();
        int endOffset = getEndOffset();
        if ((next.getStartOffset() >= startOffset && next.getStartOffset() < endOffset)
                || (startOffset >= next.getStartOffset()
                        && startOffset < next.getEndOffset())) {
            return true;
        }
        return false;
    }

    /** Lo que hace la subclase: escribir el documento en su formato. */
    protected abstract void write() throws IOException, BadLocationException;

    /** El texto de ese elemento, recortado al tramo pedido. */
    protected String getText(Element elem) throws BadLocationException {
        return doc.getText(elem.getStartOffset(), elem.getEndOffset() - elem.getStartOffset());
    }

    /**
     * Escribe el texto de un elemento hoja, recortado al tramo.
     *
     * <p>Recortar aca y no en {@link #getText} es lo que permite escribir media hoja cuando el
     * tramo empieza o termina en el medio de una.
     */
    protected void text(Element elem) throws BadLocationException, IOException {
        int start = Math.max(getStartOffset(), elem.getStartOffset());
        int end = Math.min(getEndOffset(), elem.getEndOffset());
        if (start < end) {
            if (segment == null) {
                segment = new Segment();
            }
            getDocument().getText(start, end - start, segment);
            if (segment.count > 0) {
                write(segment.array, segment.offset, segment.count);
            }
        }
    }

    /** El ancho maximo de linea; ver la nota de la clase. */
    protected void setLineLength(int l) {
        maxLineLength = l;
    }

    protected int getLineLength() {
        return maxLineLength;
    }

    protected void setCurrentLineLength(int length) {
        currLength = length;
        isLineEmpty = (currLength == 0);
    }

    protected int getCurrentLineLength() {
        return currLength;
    }

    /** Si en esta linea no se escribio nada todavia (la sangria no cuenta). */
    protected boolean isLineEmpty() {
        return isLineEmpty;
    }

    protected void setCanWrapLines(boolean newValue) {
        canWrapLines = newValue;
    }

    protected boolean getCanWrapLines() {
        return canWrapLines;
    }

    /** Cuantos espacios vale un nivel de sangria. */
    protected void setIndentSpace(int space) {
        indentSpace = space;
    }

    protected int getIndentSpace() {
        return indentSpace;
    }

    public void setLineSeparator(String value) {
        lineSeparator = value;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    protected void incrIndent() {
        // El nivel sube salvo que la sangria ya se pase del ancho de linea.
        if (offsetIndent > 0 || ((indentLevel + 1) * getIndentSpace()) < getLineLength()) {
            indentLevel++;
        } else {
            offsetIndent++;
        }
    }

    protected void decrIndent() {
        if (offsetIndent > 0) {
            offsetIndent--;
        } else {
            indentLevel--;
        }
    }

    protected int getIndentLevel() {
        return indentLevel;
    }

    /** Escribe la sangria de la linea que viene. */
    protected void indent() throws IOException {
        int max = getIndentLevel() * getIndentSpace();
        if (indentChars == null || max > indentChars.length) {
            indentChars = new char[max];
            for (int counter = 0; counter < max; counter++) {
                indentChars[counter] = ' ';
            }
        }
        int length = getCurrentLineLength();
        boolean wasEmpty = isLineEmpty();
        output(indentChars, 0, max);
        if (wasEmpty && length == 0) {
            isLineEmpty = true;
        }
    }

    protected void write(char ch) throws IOException {
        if (tempChars == null) {
            tempChars = new char[128];
        }
        tempChars[0] = ch;
        write(tempChars, 0, 1);
    }

    protected void write(String content) throws IOException {
        if (content == null) {
            return;
        }
        int size = content.length();
        if (tempChars == null || tempChars.length < size) {
            tempChars = new char[size];
        }
        content.getChars(0, size, tempChars, 0);
        write(tempChars, 0, size);
    }

    protected void writeLineSeparator() throws IOException {
        String newline = getLineSeparator();
        int length = newline.length();
        if (newlineChars == null || newlineChars.length < length) {
            newlineChars = new char[length];
        }
        newline.getChars(0, length, newlineChars, 0);
        output(newlineChars, 0, length);
        setCurrentLineLength(0);
    }

    /**
     * Escribe ese texto, cortando lineas si hace falta.
     *
     * <p>Toda escritura pasa por aca: es donde vive el corte y donde se cuenta el largo. Lo que va
     * de verdad al {@link Writer} sale por {@link #output}.
     */
    protected void write(char[] chars, int startIndex, int length) throws IOException {
        if (!getCanWrapLines()) {
            // Sin corte: se busca solo el fin de linea para no perder la cuenta.
            int lastIndex = startIndex;
            int endIndex = startIndex + length;
            int newlineIndex = indexOf(chars, NEWLINE, startIndex, endIndex);
            while (newlineIndex != -1) {
                if (newlineIndex > lastIndex) {
                    output(chars, lastIndex, newlineIndex - lastIndex);
                }
                writeLineSeparator();
                lastIndex = newlineIndex + 1;
                newlineIndex = indexOf(chars, NEWLINE, lastIndex, endIndex);
            }
            if (lastIndex < endIndex) {
                output(chars, lastIndex, endIndex - lastIndex);
            }
        } else {
            int lastIndex = startIndex;
            int endIndex = startIndex + length;
            int lineLength = getCurrentLineLength();
            int maxLength = getLineLength();

            while (lastIndex < endIndex) {
                int newlineIndex = indexOf(chars, NEWLINE, lastIndex, endIndex);
                boolean needsNewline = false;
                boolean forceNewLine = false;

                lineLength = getCurrentLineLength();
                if (newlineIndex != -1 && (lineLength + (newlineIndex - lastIndex))
                        < maxLength) {
                    if (newlineIndex > lastIndex) {
                        output(chars, lastIndex, newlineIndex - lastIndex);
                    }
                    lastIndex = newlineIndex + 1;
                    forceNewLine = true;
                } else if (newlineIndex == -1 && (lineLength + (endIndex - lastIndex))
                        < maxLength) {
                    if (endIndex > lastIndex) {
                        output(chars, lastIndex, endIndex - lastIndex);
                    }
                    lastIndex = endIndex;
                } else {
                    // No entra: se busca un espacio hacia atras.
                    int breakPoint = -1;
                    int maxBreak = Math.min(endIndex - lastIndex,
                            maxLength - lineLength - 1);
                    int counter;
                    for (counter = 0; counter < maxBreak; counter++) {
                        if (Character.isWhitespace(chars[counter + lastIndex])) {
                            breakPoint = counter;
                        }
                    }
                    if (breakPoint != -1) {
                        breakPoint = breakPoint + lastIndex + 1;
                        output(chars, lastIndex, breakPoint - lastIndex);
                        lastIndex = breakPoint;
                    } else {
                        // Ni un espacio: la linea se pasa. Ver la nota de la clase.
                        boolean done = false;
                        int check = lastIndex + maxBreak;
                        maxBreak = endIndex;
                        for (counter = check; counter < maxBreak; counter++) {
                            if (Character.isWhitespace(chars[counter])) {
                                breakPoint = counter;
                                done = true;
                                break;
                            }
                        }
                        if (!done) {
                            output(chars, lastIndex, endIndex - lastIndex);
                            lastIndex = endIndex;
                        } else {
                            if (breakPoint < endIndex && chars[breakPoint] == NEWLINE) {
                                output(chars, lastIndex, breakPoint - lastIndex);
                                forceNewLine = true;
                                lastIndex = breakPoint + 1;
                            } else {
                                output(chars, lastIndex, breakPoint - lastIndex + 1);
                                lastIndex = breakPoint + 1;
                            }
                        }
                    }
                    needsNewline = true;
                }
                if (forceNewLine || needsNewline) {
                    writeLineSeparator();
                    if (lastIndex < endIndex) {
                        indent();
                    }
                }
            }
        }
    }

    /** Escribe los atributos de ese conjunto, uno por linea. */
    protected void writeAttributes(AttributeSet attr) throws IOException {
        Enumeration<?> names = attr.getAttributeNames();
        while (names.hasMoreElements()) {
            Object name = names.nextElement();
            write(" " + name + "=" + attr.getAttribute(name));
        }
    }

    /**
     * La unica salida de verdad al {@link Writer}.
     *
     * <p>Una subclase que quiera espiar o cambiar lo que sale solo tiene que sobrescribir esto, sin
     * repetir la logica de corte de {@link #write(char[], int, int)}.
     */
    protected void output(char[] content, int start, int length) throws IOException {
        getWriter().write(content, start, length);
        setCurrentLineLength(getCurrentLineLength() + length);
    }

    private static int indexOf(char[] chars, char sChar, int start, int end) {
        while (start < end) {
            if (chars[start] == sChar) {
                return start;
            }
            start++;
        }
        return -1;
    }
}
