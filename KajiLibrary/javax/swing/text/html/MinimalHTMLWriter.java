package javax.swing.text.html;

import java.awt.Color;
import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;

import javax.swing.text.AbstractWriter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Escribe como HTML un documento con estilos que no es de HTML.
 *
 * <h2>Para que sirve</h2>
 *
 * <p>Un {@link javax.swing.text.DefaultStyledDocument} no tiene etiquetas: tiene atributos de
 * Swing. Este escritor los traduce a HTML para poder guardar o mandar lo que hay en un
 * {@code JTextPane}.
 *
 * <p>Es "minimo" porque no intenta reconstruir estructura. Escribe un {@code <p>} por parrafo y las
 * etiquetas de caracter que hagan falta, y todo lo que los atributos de Swing no puedan decir en
 * HTML lo pone como estilo en el encabezado.
 *
 * <h2>Lo que no se puede escribir</h2>
 *
 * <p>Un componente o un icono incrustado no tienen equivalente: se escribe un comentario que dice
 * que estaban. Es mejor que perderlos en silencio, aunque no se puedan volver a leer.
 */
public class MinimalHTMLWriter extends AbstractWriter {

    private static final int BOLD = 0x01;
    private static final int ITALIC = 0x02;
    private static final int UNDERLINE = 0x04;

    private int fontMask = 0;
    private int startOffset = 0;
    private int endOffset = 0;
    private boolean inFontTag = false;
    private boolean fontAttrSet = false;
    private AttributeSet fontAttributes;

    /** Escribe el documento entero. */
    public MinimalHTMLWriter(Writer w, StyledDocument doc) {
        super(w, doc);
    }

    /** Escribe ese tramo del documento. */
    public MinimalHTMLWriter(Writer w, StyledDocument doc, int pos, int len) {
        super(w, doc, pos, len);
    }

    /** Escribe el documento: encabezado con los estilos, y despues el cuerpo. */
    public void write() throws IOException, BadLocationException {
        styleNameMapping = new java.util.Hashtable<String, String>();
        writeStartTag("<html>");
        writeHeader();
        writeBody();
        writeEndTag("</html>");
    }

    private java.util.Hashtable<String, String> styleNameMapping;

    /** Los atributos que no tienen etiqueta propia, escritos como CSS. */
    protected void writeAttributes(AttributeSet attr) throws IOException {
        Enumeration<?> names = attr.getAttributeNames();
        while (names.hasMoreElements()) {
            Object name = names.nextElement();
            write("  " + name + ": " + attr.getAttribute(name) + ";");
            writeLineSeparator();
        }
    }

    /** El texto de un elemento hoja, con las entidades escapadas. */
    protected void text(Element elem) throws IOException, BadLocationException {
        String contentStr = getText(elem);
        if (contentStr.length() > 0 && contentStr.charAt(contentStr.length() - 1) == '\n') {
            contentStr = contentStr.substring(0, contentStr.length() - 1);
        }
        if (contentStr.length() > 0) {
            write(contentStr);
        }
    }

    /** Escribe una etiqueta de apertura en su propia linea y sangra lo que viene. */
    protected void writeStartTag(String tag) throws IOException {
        indent();
        write(tag);
        writeLineSeparator();
        incrIndent();
    }

    protected void writeEndTag(String endTag) throws IOException {
        decrIndent();
        indent();
        write(endTag);
        writeLineSeparator();
    }

    /** El encabezado, con los estilos del documento adentro. */
    protected void writeHeader() throws IOException {
        writeStartTag("<head>");
        writeStartTag("<style>");
        writeStartTag("<!--");
        writeStyles();
        writeEndTag("-->");
        writeEndTag("</style>");
        writeEndTag("</head>");
    }

    /**
     * Los estilos con nombre del documento, como reglas de CSS.
     *
     * <p>El estilo por omision no se escribe: no tiene nombre que ponerle a la regla, y sus valores
     * son los que ya valen sin decir nada.
     */
    protected void writeStyles() throws IOException {
        javax.swing.text.StyleContext ctx = null;
        javax.swing.text.Document doc = getDocument();
        if (doc instanceof javax.swing.text.DefaultStyledDocument) {
            Enumeration<?> nombres =
                    ((javax.swing.text.DefaultStyledDocument) doc).getStyleNames();
            while (nombres != null && nombres.hasMoreElements()) {
                String nombre = (String) nombres.nextElement();
                if (javax.swing.text.StyleContext.DEFAULT_STYLE.equals(nombre)) {
                    continue;
                }
                Style s = ((javax.swing.text.DefaultStyledDocument) doc).getStyle(nombre);
                if (s == null || s.getAttributeCount() == 0) {
                    continue;
                }
                indent();
                write("p." + addStyleName(nombre) + " {");
                writeLineSeparator();
                incrIndent();
                writeAttributes(s);
                decrIndent();
                indent();
                write("}");
                writeLineSeparator();
            }
        }
    }

    /** Un nombre de estilo que se pueda escribir en una regla; los espacios molestan. */
    private String addStyleName(String nombre) {
        String limpio = nombre.replace(' ', '-');
        styleNameMapping.put(nombre, limpio);
        return limpio;
    }

    /** El cuerpo: un parrafo por parrafo del documento. */
    protected void writeBody() throws IOException, BadLocationException {
        ElementIterator it = getElementIterator();
        writeStartTag("<body>");
        Element next;
        while ((next = it.next()) != null) {
            if (!inRange(next)) {
                continue;
            }
            if (next instanceof javax.swing.text.AbstractDocument.BranchElement) {
                writeStartParagraph(next);
            } else if (isText(next)) {
                writeContent(next, true);
            } else {
                writeLeaf(next);
            }
            if (esUltimoDelParrafo(it, next)) {
                writeEndParagraph();
            }
        }
        writeEndTag("</body>");
    }

    private boolean esUltimoDelParrafo(ElementIterator it, Element e) {
        Element padre = e.getParentElement();
        return (padre != null
                && padre.getElement(padre.getElementCount() - 1) == e);
    }

    /** Cierra el parrafo y las etiquetas de caracter que quedaron abiertas. */
    protected void writeEndParagraph() throws IOException {
        writeEndMask();
        if (inFontTag()) {
            endFontTag();
        }
        write("</p>");
        writeLineSeparator();
    }

    /** Abre el parrafo, con su clase si el elemento tiene un estilo con nombre. */
    protected void writeStartParagraph(Element elem) throws IOException {
        AttributeSet attr = elem.getAttributes();
        Object resolveAttr = attr.getAttribute(StyleConstants.ResolveAttribute);
        if (resolveAttr instanceof Style) {
            String nombre = ((Style) resolveAttr).getName();
            String limpio = styleNameMapping.get(nombre);
            write("<p class=" + (limpio == null ? nombre : limpio) + ">");
        } else {
            write("<p>");
        }
        writeLineSeparator();
    }

    /** Una hoja que no es texto: un icono o un componente. */
    protected void writeLeaf(Element elem) throws IOException {
        indent();
        if (elem.getName().equals(StyleConstants.IconElementName)) {
            writeImage(elem);
        } else if (elem.getName().equals(StyleConstants.ComponentElementName)) {
            writeComponent(elem);
        }
    }

    /** Un icono; ver la nota de la clase sobre lo que no se puede escribir. */
    protected void writeImage(Element elem) throws IOException {
        write("<!-- icono -->");
    }

    /** Un componente incrustado. */
    protected void writeComponent(Element elem) throws IOException {
        write("<!-- componente -->");
    }

    /** Si el elemento es texto comun. */
    protected boolean isText(Element elem) {
        return (elem.getName().equals(javax.swing.text.AbstractDocument.ContentElementName));
    }

    /** Escribe un tramo de texto con sus etiquetas de caracter alrededor. */
    protected void writeContent(Element elem, boolean needsIndenting) throws IOException,
            BadLocationException {
        AttributeSet attr = elem.getAttributes();
        writeNonHTMLAttributes(attr);
        if (needsIndenting) {
            indent();
        }
        writeHTMLTags(attr);
        text(elem);
    }

    /**
     * Abre y cierra negrita, cursiva y subrayado segun cambien.
     *
     * <p>Se lleva una mascara de tres bits con lo que esta abierto. Comparar la mascara nueva con
     * la vieja dice exactamente que abrir y que cerrar, sin repetir etiquetas ni dejar ninguna sin
     * cerrar.
     */
    protected void writeHTMLTags(AttributeSet attr) throws IOException {
        int oldMask = fontMask;
        setFontMask(attr);
        int endMask = 0;
        int startMask = 0;
        if ((oldMask & BOLD) != 0) {
            if ((fontMask & BOLD) == 0) {
                endMask = endMask | BOLD;
            }
        } else if ((fontMask & BOLD) != 0) {
            startMask = startMask | BOLD;
        }
        if ((oldMask & ITALIC) != 0) {
            if ((fontMask & ITALIC) == 0) {
                endMask = endMask | ITALIC;
            }
        } else if ((fontMask & ITALIC) != 0) {
            startMask = startMask | ITALIC;
        }
        if ((oldMask & UNDERLINE) != 0) {
            if ((fontMask & UNDERLINE) == 0) {
                endMask = endMask | UNDERLINE;
            }
        } else if ((fontMask & UNDERLINE) != 0) {
            startMask = startMask | UNDERLINE;
        }
        writeEndMask(endMask);
        writeStartMask(startMask);
    }

    private void setFontMask(AttributeSet attr) {
        fontMask = 0;
        if (StyleConstants.isBold(attr)) {
            fontMask = fontMask | BOLD;
        }
        if (StyleConstants.isItalic(attr)) {
            fontMask = fontMask | ITALIC;
        }
        if (StyleConstants.isUnderline(attr)) {
            fontMask = fontMask | UNDERLINE;
        }
    }

    private void writeStartMask(int mask) throws IOException {
        if ((mask & UNDERLINE) != 0) {
            write("<u>");
        }
        if ((mask & ITALIC) != 0) {
            write("<i>");
        }
        if ((mask & BOLD) != 0) {
            write("<b>");
        }
    }

    private void writeEndMask(int mask) throws IOException {
        // Al reves de como se abrieron: el HTML no permite cruzarlas.
        if ((mask & BOLD) != 0) {
            write("</b>");
        }
        if ((mask & ITALIC) != 0) {
            write("</i>");
        }
        if ((mask & UNDERLINE) != 0) {
            write("</u>");
        }
    }

    private void writeEndMask() throws IOException {
        writeEndMask(fontMask);
        fontMask = 0;
    }

    /**
     * Lo que no tiene etiqueta de HTML, escrito como un {@code <span>} con estilo.
     *
     * <p>El tamano y el color si tienen etiqueta vieja ({@code <font>}) y van por ahi; lo demas
     * -- un espaciado, una sangria -- solo se puede decir en CSS.
     */
    protected void writeNonHTMLAttributes(AttributeSet attr) throws IOException {
        String color = null;
        Color c = StyleConstants.getForeground(attr);
        if (c != null && !c.equals(Color.black)) {
            color = "#" + hex(c.getRed()) + hex(c.getGreen()) + hex(c.getBlue());
        }
        if (color != null) {
            if (inFontTag()) {
                endFontTag();
            }
            startFontTag("color=\"" + color + "\"");
        } else if (inFontTag()) {
            endFontTag();
        }
    }

    private static String hex(int v) {
        String s = Integer.toHexString(v);
        return (s.length() == 1) ? "0" + s : s;
    }

    /** Si hay una etiqueta {@code <font>} abierta. */
    protected boolean inFontTag() {
        return inFontTag;
    }

    protected void endFontTag() throws IOException {
        write("</font>");
        inFontTag = false;
    }

    protected void startFontTag(String style) throws IOException {
        if (inFontTag()) {
            endFontTag();
        }
        write("<font " + style + ">");
        inFontTag = true;
    }
}
