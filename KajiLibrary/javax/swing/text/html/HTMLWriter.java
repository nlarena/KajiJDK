package javax.swing.text.html;

import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.text.AbstractWriter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.StyleConstants;

/**
 * Escribe un {@link HTMLDocument} de vuelta como HTML.
 *
 * <h2>El problema: el arbol no es el HTML</h2>
 *
 * <p>Un documento guarda parrafos y tramos de caracteres con atributos. El HTML tiene etiquetas
 * anidadas. No son lo mismo: un tramo con negrita y cursiva es <em>un</em> elemento del documento y
 * <em>dos</em> etiquetas anidadas en el HTML.
 *
 * <p>Por eso el escritor lleva la cuenta de las etiquetas que tiene abiertas
 * ({@link #writeEmbeddedTags} y {@link #closeOutUnwantedEmbeddedTags}): al pasar de un tramo al
 * siguiente abre las que aparecen y cierra las que dejaron de estar, en el orden que corresponde.
 *
 * <h2>Elementos que no se escriben</h2>
 *
 * <p>El analizador inventa elementos que el autor no escribio -- el parrafo implicito, el
 * <code>&lt;html&gt;</code> que faltaba -- y los marca. {@link #synthesizedElement} los reconoce y
 * los saltea, para que el HTML que sale se parezca al que entro.
 */
public class HTMLWriter extends AbstractWriter {

    private Vector<HTML.Tag> tags = new Vector<HTML.Tag>(10);
    private Vector<Object> tagValues = new Vector<Object>(10);
    private Vector<HTML.Tag> tagsToRemove = new Vector<HTML.Tag>(10);
    private boolean wroteHead = false;
    private boolean replaceEntities = false;
    private boolean inContent = false;
    private boolean inPre = false;
    private boolean indentNext = false;
    private char[] tempChars;

    /** Escribe el documento entero. */
    public HTMLWriter(Writer w, HTMLDocument doc) {
        this(w, doc, 0, doc.getLength());
    }

    /** Escribe ese tramo del documento. */
    public HTMLWriter(Writer w, HTMLDocument doc, int pos, int len) {
        super(w, doc, pos, len);
    }

    /**
     * Escribe el documento.
     *
     * <p>Recorre el arbol de elementos con un {@link ElementIterator}, abriendo y cerrando
     * etiquetas segun el nivel.
     */
    public void write() throws IOException, BadLocationException {
        ElementIterator it = getElementIterator();
        Element current = it.current();
        Element next;

        setCurrentLineLength(0);
        while (current != null) {
            if (!inRange(current)) {
                current = it.next();
                continue;
            }
            if (current instanceof javax.swing.text.AbstractDocument.BranchElement) {
                if (!synthesizedElement(current)) {
                    startTag(current);
                }
            } else {
                if (matchNameAttribute(current.getAttributes(), HTML.Tag.CONTENT)) {
                    text(current);
                } else if (matchNameAttribute(current.getAttributes(), HTML.Tag.COMMENT)) {
                    comment(current);
                } else {
                    emptyTag(current);
                }
            }
            next = it.next();
            if (next == null) {
                cerrarHasta(0, current);
                break;
            }
            int nivelActual = nivel(current);
            int nivelSiguiente = nivel(next);
            if (nivelSiguiente <= nivelActual) {
                cerrarHasta(nivelSiguiente, current);
            }
            current = next;
        }
        closeOutUnwantedEmbeddedTags(null);
    }

    private static int nivel(Element e) {
        int n = 0;
        for (Element p = e.getParentElement(); p != null; p = p.getParentElement()) {
            n++;
        }
        return n;
    }

    /** Cierra las etiquetas abiertas desde ese elemento hasta ese nivel. */
    private void cerrarHasta(int nivelDestino, Element desde) throws IOException {
        Element e = desde;
        if (!(e instanceof javax.swing.text.AbstractDocument.BranchElement)) {
            e = e.getParentElement();
        }
        while (e != null && nivel(e) >= nivelDestino) {
            if (!synthesizedElement(e)) {
                endTag(e);
            }
            e = e.getParentElement();
        }
    }

    /**
     * Escribe los atributos de un conjunto.
     *
     * <p>No escribe los internos: el nombre del elemento y las marcas que puso el analizador no son
     * atributos de HTML, y escribirlos daria un documento que no se puede volver a leer.
     */
    protected void writeAttributes(AttributeSet attr) throws IOException {
        Enumeration<?> names = attr.getAttributeNames();
        while (names.hasMoreElements()) {
            Object name = names.nextElement();
            if (name instanceof HTML.Tag
                    || name instanceof StyleConstants
                    || name == HTMLEditorKit.ParserCallback.IMPLIED
                    || name == javax.swing.text.AbstractDocument.ElementNameAttribute) {
                continue;
            }
            Object value = attr.getAttribute(name);
            write(" " + name + "=\"" + value + "\"");
        }
    }

    /** Escribe una etiqueta sin cierre, como {@code <br>} o {@code <img>}. */
    protected void emptyTag(Element elem) throws BadLocationException, IOException {
        AttributeSet attr = elem.getAttributes();
        closeOutUnwantedEmbeddedTags(attr);
        writeEmbeddedTags(attr);
        Object nombre = attr.getAttribute(StyleConstants.NameAttribute);
        if (nombre instanceof HTML.Tag) {
            write('<');
            write(nombre.toString());
            writeAttributes(attr);
            write('>');
        }
    }

    /** Si esa etiqueta arma bloque. */
    protected boolean isBlockTag(AttributeSet attr) {
        Object o = attr.getAttribute(StyleConstants.NameAttribute);
        if (o instanceof HTML.Tag) {
            return ((HTML.Tag) o).isBlock();
        }
        return false;
    }

    /** Escribe la etiqueta de apertura de un elemento con hijos. */
    protected void startTag(Element elem) throws IOException, BadLocationException {
        AttributeSet attr = elem.getAttributes();
        Object nombre = attr.getAttribute(StyleConstants.NameAttribute);
        if (!(nombre instanceof HTML.Tag)) {
            return;
        }
        HTML.Tag tag = (HTML.Tag) nombre;
        if (tag == HTML.Tag.PRE) {
            inPre = true;
        }
        if (isBlockTag(attr) && !isLineEmpty()) {
            writeLineSeparator();
        }
        indent();
        write('<');
        write(tag.toString());
        writeAttributes(attr);
        write('>');
        if (tag.isBlock()) {
            writeLineSeparator();
        }
        incrIndent();
    }

    /** El contenido de un {@code <textarea>}, tal cual, sin cortar lineas. */
    protected void textAreaContent(AttributeSet attr) throws BadLocationException, IOException {
        Object modelo = attr.getAttribute(StyleConstants.ModelAttribute);
        if (modelo instanceof javax.swing.text.Document) {
            javax.swing.text.Document doc = (javax.swing.text.Document) modelo;
            String texto = doc.getText(0, doc.getLength());
            setCanWrapLines(false);
            write(texto);
            setCanWrapLines(true);
        }
    }

    /** El texto de un elemento hoja, con las entidades escapadas. */
    protected void text(Element elem) throws BadLocationException, IOException {
        int start = Math.max(getStartOffset(), elem.getStartOffset());
        int end = Math.min(getEndOffset(), elem.getEndOffset());
        if (start >= end) {
            return;
        }
        String s = getDocument().getText(start, end - start);
        inContent = true;
        if (inPre) {
            setCanWrapLines(false);
        }
        write(escapar(s));
        if (inPre) {
            setCanWrapLines(true);
        }
    }

    /**
     * Escapa lo que en HTML no se puede escribir tal cual.
     *
     * <p>Son cuatro: los dos angulos, el ampersand y la comilla doble. El ampersand va primero, si
     * no se escaparia el que acaba de escribir el reemplazo anterior.
     */
    private static String escapar(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&') {
                sb.append("&amp;");
            } else if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else if (c == '"') {
                sb.append("&quot;");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Las opciones de un {@code <select>}.
     *
     * <p>Salen del modelo del control, no del documento: el usuario pudo haber cambiado la
     * seleccion, y lo que hay que escribir es lo que se ve.
     */
    protected void selectContent(AttributeSet attr) throws IOException {
        Object modelo = attr.getAttribute(StyleConstants.ModelAttribute);
        incrIndent();
        if (modelo instanceof javax.swing.ComboBoxModel) {
            javax.swing.ComboBoxModel<?> m = (javax.swing.ComboBoxModel<?>) modelo;
            for (int i = 0; i < m.getSize(); i++) {
                Object o = m.getElementAt(i);
                if (o instanceof Option) {
                    writeOption((Option) o);
                }
            }
        } else if (modelo instanceof javax.swing.ListModel) {
            javax.swing.ListModel<?> m = (javax.swing.ListModel<?>) modelo;
            for (int i = 0; i < m.getSize(); i++) {
                Object o = m.getElementAt(i);
                if (o instanceof Option) {
                    writeOption((Option) o);
                }
            }
        }
        decrIndent();
    }

    /** Una opcion de lista, con su marca de seleccionada si la tiene. */
    protected void writeOption(Option option) throws IOException {
        indent();
        write('<');
        write("option");
        Object value = option.getAttributes().getAttribute(HTML.Attribute.VALUE);
        if (value != null) {
            write(" value=" + value);
        }
        if (option.isSelected()) {
            write(" selected");
        }
        write('>');
        if (option.getLabel() != null) {
            write(option.getLabel());
        }
        writeLineSeparator();
    }

    /** La etiqueta de cierre. */
    protected void endTag(Element elem) throws IOException {
        AttributeSet attr = elem.getAttributes();
        Object nombre = attr.getAttribute(StyleConstants.NameAttribute);
        if (!(nombre instanceof HTML.Tag)) {
            return;
        }
        HTML.Tag tag = (HTML.Tag) nombre;
        if (tag == HTML.Tag.PRE) {
            inPre = false;
        }
        decrIndent();
        if (tag.isBlock() && !isLineEmpty()) {
            writeLineSeparator();
        }
        indent();
        write('<');
        write('/');
        write(tag.toString());
        write('>');
        if (tag.isBlock()) {
            writeLineSeparator();
        }
    }

    /** Un comentario, con sus delimitadores. */
    protected void comment(Element elem) throws BadLocationException, IOException {
        AttributeSet as = elem.getAttributes();
        Object o = as.getAttribute(HTML.Attribute.COMMENT);
        if (o instanceof String) {
            indent();
            write("<!--");
            write((String) o);
            write("-->");
            writeLineSeparator();
        }
    }

    /** Si el elemento lo invento el analizador; ver la nota de la clase. */
    protected boolean synthesizedElement(Element elem) {
        Object o = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
        if (o == HTML.Tag.IMPLIED) {
            return true;
        }
        return elem.getAttributes().getAttribute(
                HTMLEditorKit.ParserCallback.IMPLIED) != null;
    }

    /** Si el nombre del elemento es esa etiqueta. */
    protected boolean matchNameAttribute(AttributeSet attr, HTML.Tag tag) {
        Object o = attr.getAttribute(StyleConstants.NameAttribute);
        return (o instanceof HTML.Tag && o == tag);
    }

    /**
     * Abre las etiquetas de caracter que este tramo tiene y el anterior no.
     *
     * <p>Son las que no salen de un elemento del arbol sino de un atributo del tramo: negrita,
     * cursiva, un enlace. Ver la nota de la clase.
     */
    protected void writeEmbeddedTags(AttributeSet attr) throws IOException {
        Enumeration<?> names = attr.getAttributeNames();
        while (names.hasMoreElements()) {
            Object name = names.nextElement();
            if (name instanceof HTML.Tag) {
                HTML.Tag tag = (HTML.Tag) name;
                if (tag == HTML.Tag.FORM || tags.contains(tag)) {
                    continue;
                }
                write('<');
                write(tag.toString());
                Object o = attr.getAttribute(tag);
                if (o instanceof AttributeSet) {
                    writeAttributes((AttributeSet) o);
                }
                write('>');
                tags.addElement(tag);
                tagValues.addElement(o);
            }
        }
    }

    /**
     * Cierra las etiquetas de caracter que ya no corresponden.
     *
     * <p>Se cierran en orden inverso al de apertura, y si una del medio dejo de valer se cierran
     * tambien las de adentro y se vuelven a abrir. No hay otra forma: el HTML no permite cerrar una
     * etiqueta salteando las que tiene adentro.
     */
    protected void closeOutUnwantedEmbeddedTags(AttributeSet attr) throws IOException {
        tagsToRemove.removeAllElements();
        for (int i = 0; i < tags.size(); i++) {
            HTML.Tag tag = tags.elementAt(i);
            Object value = tagValues.elementAt(i);
            if (attr == null || !valorIgual(attr.getAttribute(tag), value)) {
                tagsToRemove.addElement(tag);
            }
        }
        if (tagsToRemove.size() == 0) {
            return;
        }
        int desde = tags.size();
        for (int i = 0; i < tags.size(); i++) {
            if (tagsToRemove.contains(tags.elementAt(i))) {
                desde = i;
                break;
            }
        }
        // Se cierra desde la ultima hasta la primera que dejo de valer.
        Vector<HTML.Tag> reabrir = new Vector<HTML.Tag>();
        Vector<Object> reabrirVal = new Vector<Object>();
        for (int i = tags.size() - 1; i >= desde; i--) {
            HTML.Tag tag = tags.elementAt(i);
            write('<');
            write('/');
            write(tag.toString());
            write('>');
            if (!tagsToRemove.contains(tag)) {
                reabrir.insertElementAt(tag, 0);
                reabrirVal.insertElementAt(tagValues.elementAt(i), 0);
            }
        }
        while (tags.size() > desde) {
            tags.removeElementAt(tags.size() - 1);
            tagValues.removeElementAt(tagValues.size() - 1);
        }
        for (int i = 0; i < reabrir.size(); i++) {
            HTML.Tag tag = reabrir.elementAt(i);
            write('<');
            write(tag.toString());
            write('>');
            tags.addElement(tag);
            tagValues.addElement(reabrirVal.elementAt(i));
        }
    }

    private static boolean valorIgual(Object a, Object b) {
        return (a == null) ? (b == null) : a.equals(b);
    }

    protected void writeLineSeparator() throws IOException {
        boolean pre = inPre;
        inPre = false;
        super.writeLineSeparator();
        inPre = pre;
    }

    protected void output(char[] chars, int start, int length) throws IOException {
        super.output(chars, start, length);
    }
}
