package javax.swing.text;

import java.util.Vector;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent$EventType;
import javax.swing.undo.UndoableEdit;

/**
 * Un documento sin estilos: solo texto, y una estructura de una sola capa de lineas.
 *
 * <h2>La estructura es la lista de lineas</h2>
 *
 * <p>La raiz tiene un hijo por linea, y cada hijo va desde el principio de la linea hasta despues
 * de su fin de linea. No hay parrafos ni tramos con estilo: es lo que hace que este documento sea
 * barato y lo que usa un area de texto comun.
 *
 * <p>Toda la clase es mantener esa lista cuando se inserta o se borra. Insertar un texto con dos
 * fines de linea parte una linea en tres; borrar un tramo que cruza fines de linea junta las que
 * quedaron a medias en una sola.
 *
 * <p>{@link #tabSizeAttribute} y {@link #lineLimitAttribute} son propiedades del documento, no
 * atributos de texto: el que dibuja las lee para saber cada cuanto va una tabulacion y donde
 * cortar las lineas.
 */
public class PlainDocument extends AbstractDocument {

    /** La cantidad de espacios de una tabulacion. */
    public static final String tabSizeAttribute = "tabSize";

    /** El ancho maximo de una linea, en caracteres. */
    public static final String lineLimitAttribute = "lineLimit";

    private AbstractElement defaultRoot;
    private Vector<Element> added = new Vector<Element>();
    private Vector<Element> removed = new Vector<Element>();

    /** Un documento vacio sobre un contenido con hueco. */
    public PlainDocument() {
        this(new GapContent());
    }

    /** Un documento sobre ese contenido. */
    public PlainDocument(Content c) {
        super(c);
        putProperty(tabSizeAttribute, Integer.valueOf(8));
        defaultRoot = createDefaultRoot();
    }

    /**
     * Inserta texto.
     *
     * <p>Los atributos se ignoran —este documento no los guarda— salvo que traigan la marca de
     * texto internacional, que se propaga como propiedad del documento.
     */
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        super.insertString(offs, str, a);
    }

    public Element getDefaultRootElement() {
        return defaultRoot;
    }

    /** La raiz con una sola linea, que es lo que hay en un documento vacio. */
    protected AbstractElement createDefaultRoot() {
        BranchElement map = (BranchElement) createBranchElement(null, null);
        Element line = createLeafElement(map, null, 0, 1);
        Element[] lines = new Element[1];
        lines[0] = line;
        map.replace(0, 0, lines);
        return map;
    }

    /** En este documento un parrafo es una linea. */
    public Element getParagraphElement(int pos) {
        Element lineMap = getDefaultRootElement();
        int lineIndex = lineMap.getElementIndex(pos);
        return lineMap.getElement(lineIndex);
    }

    /**
     * Parte lineas si el texto insertado trae fines de linea.
     *
     * <p>La linea donde cayo la insercion se reemplaza por tantas como fines de linea haya, mas el
     * resto. Que se reemplace la linea entera y no se la corte a mano es lo que hace que el evento
     * lleve la lista exacta de lo que se fue y lo que llego.
     */
    protected void insertUpdate(DefaultDocumentEvent chng, AttributeSet attr) {
        removed.removeAllElements();
        added.removeAllElements();
        BranchElement lineMap = (BranchElement) getDefaultRootElement();
        int offset = chng.getOffset();
        int length = chng.getLength();
        if (offset > 0) {
            offset = offset - 1;
            length = length + 1;
        }
        int index = lineMap.getElementIndex(offset);
        Element rmCandidate = lineMap.getElement(index);
        int rmOffs0 = rmCandidate.getStartOffset();
        int rmOffs1 = rmCandidate.getEndOffset();
        int lastOffset = rmOffs0;
        try {
            Segment s = new Segment();
            getText(offset, length, s);
            boolean hasBreaks = false;
            for (int i = 0; i < length; i++) {
                char c = s.array[s.offset + i];
                if (c == '\n') {
                    int breakOffset = offset + i + 1;
                    added.addElement(createLeafElement(lineMap, null, lastOffset, breakOffset));
                    lastOffset = breakOffset;
                    hasBreaks = true;
                }
            }
            if (hasBreaks) {
                removed.addElement(rmCandidate);
                if ((rmOffs1 > lastOffset) || (rmOffs1 == getLength() + 1)) {
                    added.addElement(createLeafElement(lineMap, null, lastOffset, rmOffs1));
                }
            }
        } catch (BadLocationException e) {
            throw new Error("Internal error: " + e.toString());
        }

        if (added.size() > 0 || removed.size() > 0) {
            Element[] aelems = new Element[added.size()];
            added.copyInto(aelems);
            Element[] relems = new Element[removed.size()];
            removed.copyInto(relems);
            ElementEdit ee = new ElementEdit(lineMap, index, relems, aelems);
            chng.addEdit(ee);
            lineMap.replace(index, relems.length, aelems);
        }
        super.insertUpdate(chng, attr);
    }

    /**
     * Junta las lineas que el borrado dejo a medias.
     *
     * <p>Solo hace algo si el tramo borrado cruzaba al menos un fin de linea: si no, la linea
     * sigue siendo la misma, solo que mas corta, y sus posiciones ya se acomodaron solas.
     */
    protected void removeUpdate(DefaultDocumentEvent chng) {
        removed.removeAllElements();
        BranchElement map = (BranchElement) getDefaultRootElement();
        int offset = chng.getOffset();
        int length = chng.getLength();
        int line0 = map.getElementIndex(offset);
        int line1 = map.getElementIndex(offset + length);
        if (line0 != line1) {
            for (int i = line0; i <= line1; i++) {
                removed.addElement(map.getElement(i));
            }
            int p0 = map.getElement(line0).getStartOffset();
            int p1 = map.getElement(line1).getEndOffset();
            Element[] aelems = new Element[1];
            aelems[0] = createLeafElement(map, null, p0, p1);
            Element[] relems = new Element[removed.size()];
            removed.copyInto(relems);
            ElementEdit ee = new ElementEdit(map, line0, relems, aelems);
            chng.addEdit(ee);
            map.replace(line0, relems.length, aelems);
        }
        super.removeUpdate(chng);
    }
}
