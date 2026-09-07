import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

/**
 * Documento con estilo contra el JDK: la estructura de tres niveles, los atributos de caracter y
 * de parrafo, el estilo logico y el recorrido del arbol.
 */
public class Doc2 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    /** El arbol entero, en una linea por elemento. */
    static void arbol(Element e, int nivel) {
        String s = "";
        for (int i = 0; i < nivel; i++) {
            s = s + "  ";
        }
        s = s + e.getName() + " [" + e.getStartOffset() + "," + e.getEndOffset() + "]";
        if (e.isLeaf()) {
            s = s + " negrita=" + StyleConstants.isBold(e.getAttributes())
                    + " size=" + StyleConstants.getFontSize(e.getAttributes());
        } else {
            s = s + " hijos=" + e.getElementCount()
                    + " align=" + StyleConstants.getAlignment(e.getAttributes());
        }
        linea(s);
        for (int i = 0; i < e.getElementCount(); i++) {
            arbol(e.getElement(i), nivel + 1);
        }
    }

    public static int run() {
        try {
            DefaultStyledDocument doc = new DefaultStyledDocument();
            SimpleAttributeSet negrita = new SimpleAttributeSet();
            StyleConstants.setBold(negrita, true);

            doc.insertString(0, "hola", null);
            doc.insertString(4, "MUNDO", negrita);
            doc.insertString(9, " chau", null);
            linea("texto=[" + doc.getText(0, doc.getLength()) + "] largo=" + doc.getLength());
            arbol(doc.getDefaultRootElement(), 0);

            Element c = doc.getCharacterElement(5);
            linea("caracter en 5 [" + c.getStartOffset() + "," + c.getEndOffset() + "] negrita="
                    + StyleConstants.isBold(c.getAttributes()));
            Element p = doc.getParagraphElement(5);
            linea("parrafo en 5 [" + p.getStartOffset() + "," + p.getEndOffset() + "] nombre="
                    + p.getName());

            SimpleAttributeSet grande = new SimpleAttributeSet();
            StyleConstants.setFontSize(grande, 20);
            doc.setCharacterAttributes(2, 5, grande, false);
            linea("tras atributos de caracter:");
            arbol(doc.getDefaultRootElement(), 0);

            SimpleAttributeSet centrado = new SimpleAttributeSet();
            StyleConstants.setAlignment(centrado, StyleConstants.ALIGN_CENTER);
            doc.setParagraphAttributes(0, 3, centrado, false);
            linea("alineacion del parrafo="
                    + StyleConstants.getAlignment(doc.getParagraphElement(0).getAttributes()));

            Style titulo = doc.addStyle("titulo", null);
            StyleConstants.setFontSize(titulo, 30);
            doc.setLogicalStyle(0, titulo);
            linea("estilo logico=" + (doc.getLogicalStyle(0) == titulo) + " size heredado="
                    + StyleConstants.getFontSize(doc.getParagraphElement(0).getAttributes()));

            doc.insertString(doc.getLength(), "\nsegunda linea\ntercera", null);
            linea("tras dos fines de linea:");
            arbol(doc.getDefaultRootElement(), 0);

            ElementIterator it = new ElementIterator(doc);
            Element e = it.first();
            int cuenta = 0;
            String nombres = "";
            while (e != null) {
                nombres = nombres + e.getName().charAt(0) + "" + it.depth();
                cuenta++;
                e = it.next();
            }
            linea("recorrido n=" + cuenta + " [" + nombres + "]");

            doc.remove(3, 8);
            linea("tras borrar: texto=[" + doc.getText(0, doc.getLength()) + "]");
            arbol(doc.getDefaultRootElement(), 0);
        } catch (BadLocationException e) {
            linea("EXCEPCION " + e.getMessage());
        }
        return 0;
    }
}
