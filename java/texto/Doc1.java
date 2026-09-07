import java.awt.Color;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.GapContent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.PlainDocument;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StringContent;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;
import javax.swing.undo.UndoManager;

/**
 * Atributos, estilos, contenido y documento plano contra el JDK, por numeros.
 *
 * Cubre lo que no se ve: como se comparten los conjuntos de atributos, como se acomodan las
 * posiciones al editar, como queda la lista de lineas y que dice cada evento.
 */
public class Doc1 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    /** Anota los eventos que llegan; nombrado y no anonimo (#499). */
    static class Espia implements DocumentListener {
        StringBuilder log = new StringBuilder();

        public void insertUpdate(DocumentEvent e) {
            log.append("i" + e.getOffset() + "+" + e.getLength() + " ");
        }

        public void removeUpdate(DocumentEvent e) {
            log.append("r" + e.getOffset() + "+" + e.getLength() + " ");
        }

        public void changedUpdate(DocumentEvent e) {
            log.append("c" + e.getOffset() + "+" + e.getLength() + " ");
        }
    }

    static void atributos() {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setBold(a, true);
        StyleConstants.setFontSize(a, 14);
        StyleConstants.setForeground(a, Color.red);
        linea("attr n=" + a.getAttributeCount() + " bold=" + StyleConstants.isBold(a)
                + " size=" + StyleConstants.getFontSize(a) + " italic=" + StyleConstants.isItalic(a)
                + " familia=" + StyleConstants.getFontFamily(a)
                + " fg=" + StyleConstants.getForeground(a).getRGB());
        SimpleAttributeSet b = new SimpleAttributeSet(a);
        linea("copia igual=" + a.isEqual(b) + " equals=" + a.equals(b) + " contiene=" + a.containsAttributes(b));
        b.removeAttribute(StyleConstants.Bold);
        linea("sin negrita n=" + b.getAttributeCount() + " igual=" + a.isEqual(b)
                + " bold=" + StyleConstants.isBold(b));
        b.setResolveParent(a);
        linea("con padre bold=" + StyleConstants.isBold(b) + " n=" + b.getAttributeCount()
                + " definido=" + b.isDefined(StyleConstants.Bold));
        linea("vacio n=" + SimpleAttributeSet.EMPTY.getAttributeCount()
                + " igual=" + SimpleAttributeSet.EMPTY.isEqual(new SimpleAttributeSet()));

        TabStop t1 = new TabStop(20.0f);
        TabStop t2 = new TabStop(50.0f, TabStop.ALIGN_RIGHT, TabStop.LEAD_DOTS);
        TabSet ts = new TabSet(new TabStop[] {t1, t2});
        linea("tabs n=" + ts.getTabCount() + " despues30=" + ts.getTabAfter(30.0f).getPosition()
                + " indice=" + ts.getTabIndex(t2) + " indiceDespues20=" + ts.getTabIndexAfter(20.0f)
                + " t2=" + t2.toString());
    }

    static void estilos() {
        StyleContext ctx = new StyleContext();
        Style raiz = ctx.getStyle(StyleContext.DEFAULT_STYLE);
        linea("estilo por omision=" + (raiz != null) + " nombre=" + raiz.getName());
        Style titulo = ctx.addStyle("titulo", raiz);
        StyleConstants.setFontSize(titulo, 24);
        StyleConstants.setBold(titulo, true);
        linea("titulo size=" + StyleConstants.getFontSize(titulo)
                + " padre=" + (titulo.getResolveParent() == raiz));
        StyleConstants.setFontSize(raiz, 11);
        Style hijo = ctx.addStyle("hijo", titulo);
        linea("hijo hereda size=" + StyleConstants.getFontSize(hijo)
                + " bold=" + StyleConstants.isBold(hijo));

        // Dos conjuntos con los mismos atributos tienen que ser el mismo objeto.
        MutableAttributeSet x = new SimpleAttributeSet();
        StyleConstants.setItalic(x, true);
        AttributeSet i1 = ctx.addAttribute(ctx.getEmptySet(), StyleConstants.Italic, Boolean.TRUE);
        AttributeSet i2 = ctx.addAttribute(ctx.getEmptySet(), StyleConstants.Italic, Boolean.TRUE);
        linea("compartido=" + (i1 == i2) + " n=" + i1.getAttributeCount()
                + " italic=" + StyleConstants.isItalic(i1));
        AttributeSet grande = ctx.getEmptySet();
        for (int k = 0; k < 12; k++) {
            grande = ctx.addAttribute(grande, "k" + k, Integer.valueOf(k));
        }
        linea("grande n=" + grande.getAttributeCount() + " compartido="
                + (grande == ctx.addAttribute(ctx.getEmptySet(), "k0", Integer.valueOf(0))));
        ctx.removeStyle("titulo");
        linea("tras quitar titulo=" + (ctx.getStyle("titulo") == null));
    }

    static void contenido() throws BadLocationException {
        for (int cual = 0; cual < 2; cual++) {
            javax.swing.text.AbstractDocument.Content c =
                    (cual == 0) ? new GapContent() : new StringContent();
            String nombre = (cual == 0) ? "gap" : "string";
            c.insertString(0, "hola mundo");
            Position p = c.createPosition(5);
            c.insertString(0, ">> ");
            linea(nombre + " largo=" + c.length() + " pos=" + p.getOffset()
                    + " texto=[" + c.getString(0, c.length() - 1) + "]");
            c.remove(0, 3);
            linea(nombre + " tras borrar pos=" + p.getOffset() + " largo=" + c.length());
            Segment s = new Segment();
            c.getChars(0, 4, s);
            linea(nombre + " segmento=[" + s.toString() + "] count=" + s.count);
        }
    }

    static void documento() throws BadLocationException {
        PlainDocument doc = new PlainDocument();
        Espia espia = new Espia();
        doc.addDocumentListener(espia);
        UndoManager undo = new UndoManager();
        doc.addUndoableEditListener(undo);

        doc.insertString(0, "uno\ndos\ntres", null);
        Element raiz = doc.getDefaultRootElement();
        linea("lineas=" + raiz.getElementCount() + " largo=" + doc.getLength()
                + " nombre=" + raiz.getName() + " hoja=" + raiz.isLeaf());
        for (int i = 0; i < raiz.getElementCount(); i++) {
            Element l = raiz.getElement(i);
            linea("  linea" + i + " [" + l.getStartOffset() + "," + l.getEndOffset() + "] "
                    + l.getName() + " hoja=" + l.isLeaf() + " padre=" + (l.getParentElement() == raiz));
        }
        linea("indice de 5=" + raiz.getElementIndex(5) + " parrafo de 5="
                + doc.getParagraphElement(5).getStartOffset());

        Position p = doc.createPosition(9);
        doc.insertString(0, "cero\n", null);
        linea("tras insertar al principio lineas=" + raiz.getElementCount()
                + " pos=" + p.getOffset() + " texto=[" + doc.getText(0, doc.getLength()) + "]");

        doc.remove(2, 6);
        linea("tras borrar lineas=" + raiz.getElementCount()
                + " texto=[" + doc.getText(0, doc.getLength()) + "] pos=" + p.getOffset());

        linea("eventos=" + espia.log.toString().trim());
        linea("deshacer=" + undo.canUndo());
        undo.undo();
        linea("tras deshacer texto=[" + doc.getText(0, doc.getLength()) + "] lineas="
                + raiz.getElementCount());
        undo.redo();
        linea("tras rehacer texto=[" + doc.getText(0, doc.getLength()) + "]");

        doc.putProperty("titulo", "prueba");
        linea("propiedad=" + doc.getProperty("titulo") + " tab="
                + doc.getProperty(PlainDocument.tabSizeAttribute));
        linea("raices=" + doc.getRootElements().length + " bidi="
                + doc.getRootElements()[1].getName() + " hijos="
                + doc.getRootElements()[1].getElementCount());

        Element bidi = doc.getRootElements()[1].getElement(0);
        linea("bidi tramo=[" + bidi.getStartOffset() + "," + bidi.getEndOffset() + "] nivel="
                + StyleConstants.getBidiLevel(bidi.getAttributes()));
    }

    public static int run() {
        try {
            atributos();
            estilos();
            contenido();
            documento();
        } catch (Exception e) {
            linea("EXCEPCION " + e.getClass().getName() + " " + e.getMessage());
        }
        return 0;
    }
}
