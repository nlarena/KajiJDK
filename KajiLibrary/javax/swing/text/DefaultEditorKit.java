package javax.swing.text;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import javax.swing.Action;

/**
 * El juego de edicion de texto plano: el que usa cualquier componente que no diga otra cosa.
 *
 * <h2>Las acciones tienen nombre</h2>
 *
 * <p>Las constantes de esta clase son los nombres de las acciones, no las acciones. Un mapa de
 * teclas ata una combinacion a un <em>nombre</em>, y recien al instalarse se resuelve contra las
 * acciones que el juego ofrece. Esa vuelta permite escribir la tabla de atajos sin tener las
 * acciones a mano y cambiar el juego sin rehacer la tabla.
 *
 * <p>Los nombres siguen un patron que conviene leer una vez: {@code selection*} es la misma accion
 * extendiendo la seleccion en vez de mover el cursor, y {@code *Action} es siempre un sufijo.
 * "Avanzar un caracter" y "avanzar seleccionando" son dos acciones y no una con parametro, porque
 * una tecla ata una accion.
 *
 * <h2>Que hacen las que estan</h2>
 *
 * <p>Las que necesitan pantalla o portapapeles —cortar, pegar, avanzar de a paginas— estan como
 * clase y como nombre, y no hacen nada: sin foco de teclado no hay quien las dispare, y sin
 * portapapeles no habria de donde sacar el texto. Las que trabajan sobre el documento —escribir un
 * caracter, insertar un fin de linea, una tabulacion— funcionan.
 */
public class DefaultEditorKit extends EditorKit {

    /** La propiedad del documento con el fin de linea que traia el archivo leido. */
    public static final String EndOfLineStringProperty = "__EndOfLine__";

    public static final String insertContentAction = "insert-content";
    public static final String insertBreakAction = "insert-break";
    public static final String insertTabAction = "insert-tab";
    public static final String deletePrevCharAction = "delete-previous";
    public static final String deleteNextCharAction = "delete-next";
    public static final String deleteNextWordAction = "delete-next-word";
    public static final String deletePrevWordAction = "delete-previous-word";
    public static final String readOnlyAction = "set-read-only";
    public static final String writableAction = "set-writable";
    public static final String cutAction = "cut-to-clipboard";
    public static final String copyAction = "copy-to-clipboard";
    public static final String pasteAction = "paste-from-clipboard";
    public static final String beepAction = "beep";
    public static final String pageUpAction = "page-up";
    public static final String pageDownAction = "page-down";
    public static final String selectionPageUpAction = "selection-page-up";
    public static final String selectionPageDownAction = "selection-page-down";
    public static final String selectionPageLeftAction = "selection-page-left";
    public static final String selectionPageRightAction = "selection-page-right";
    public static final String forwardAction = "caret-forward";
    public static final String backwardAction = "caret-backward";
    public static final String selectionForwardAction = "selection-forward";
    public static final String selectionBackwardAction = "selection-backward";
    public static final String upAction = "caret-up";
    public static final String downAction = "caret-down";
    public static final String selectionUpAction = "selection-up";
    public static final String selectionDownAction = "selection-down";
    public static final String beginWordAction = "caret-begin-word";
    public static final String endWordAction = "caret-end-word";
    public static final String selectionBeginWordAction = "selection-begin-word";
    public static final String selectionEndWordAction = "selection-end-word";
    public static final String previousWordAction = "caret-previous-word";
    public static final String nextWordAction = "caret-next-word";
    public static final String selectionPreviousWordAction = "selection-previous-word";
    public static final String selectionNextWordAction = "selection-next-word";
    public static final String beginLineAction = "caret-begin-line";
    public static final String endLineAction = "caret-end-line";
    public static final String beginLineUpAction = "caret-begin-line-up";
    public static final String endLineDownAction = "caret-end-line-down";
    public static final String selectionBeginLineAction = "selection-begin-line";
    public static final String selectionEndLineAction = "selection-end-line";
    public static final String beginParagraphAction = "caret-begin-paragraph";
    public static final String endParagraphAction = "caret-end-paragraph";
    public static final String selectionBeginParagraphAction = "selection-begin-paragraph";
    public static final String selectionEndParagraphAction = "selection-end-paragraph";
    public static final String beginAction = "caret-begin";
    public static final String endAction = "caret-end";
    public static final String selectionBeginAction = "selection-begin";
    public static final String selectionEndAction = "selection-end";
    public static final String selectWordAction = "select-word";
    public static final String selectLineAction = "select-line";
    public static final String selectParagraphAction = "select-paragraph";
    public static final String selectAllAction = "select-all";
    public static final String unselectAction = "unselect";
    public static final String toggleComponentOrientationAction =
            "toggle-componentOrientation";
    public static final String defaultKeyTypedAction = "default-typed";

    private static final Action[] defaultActions = {
        new InsertContentAction(), new DeletePrevCharAction(), new DeleteNextCharAction(),
        new ReadOnlyAction(), new WritableAction(), new CutAction(), new CopyAction(),
        new PasteAction(), new VerticalPageAction(pageUpAction, -1, false),
        new VerticalPageAction(pageDownAction, 1, false),
        new VerticalPageAction(selectionPageUpAction, -1, true),
        new VerticalPageAction(selectionPageDownAction, 1, true),
        new InsertBreakAction(), new BeepAction(), new NextVisualPositionAction(forwardAction,
        false), new NextVisualPositionAction(backwardAction, false),
        new BeginWordAction(beginWordAction, false), new EndWordAction(endWordAction, false),
        new PreviousWordAction(previousWordAction, false),
        new NextWordAction(nextWordAction, false), new BeginLineAction(beginLineAction, false),
        new EndLineAction(endLineAction, false),
        new BeginParagraphAction(beginParagraphAction, false),
        new EndParagraphAction(endParagraphAction, false), new BeginAction(beginAction, false),
        new EndAction(endAction, false), new SelectWordAction(), new SelectLineAction(),
        new SelectParagraphAction(), new SelectAllAction(), new UnselectAction(),
        new ToggleComponentOrientationAction(), new DefaultKeyTypedAction(),
        new InsertTabAction() };

    public DefaultEditorKit() {
    }

    /** Texto plano. */
    public String getContentType() {
        return "text/plain";
    }

    /** La fabrica que arma vistas de texto plano: una linea por linea. */
    public ViewFactory getViewFactory() {
        return null;
    }

    public Action[] getActions() {
        return defaultActions;
    }

    public Caret createCaret() {
        return null;
    }

    public Document createDefaultDocument() {
        return new PlainDocument();
    }

    /** Lee texto plano; los tres fines de linea se normalizan a uno. */
    public void read(InputStream in, Document doc, int pos) throws IOException,
            BadLocationException {
        read(new InputStreamReader(in), doc, pos);
    }

    public void write(OutputStream out, Document doc, int pos, int len) throws IOException,
            BadLocationException {
        OutputStreamWriter osw = new OutputStreamWriter(out);
        write(osw, doc, pos, len);
        osw.flush();
    }

    /** Los atributos con los que se escribe; en texto plano, ninguno. */
    MutableAttributeSet getInputAttributes() {
        return null;
    }

    /**
     * Lee del flujo e inserta.
     *
     * <p>Un {@code \r\n} o un {@code \r} sueltos se guardan como {@code \n}, y el fin de linea que
     * traia el archivo queda anotado en la propiedad {@link #EndOfLineStringProperty}: asi se
     * puede escribir de vuelta como estaba.
     */
    public void read(Reader in, Document doc, int pos) throws IOException, BadLocationException {
        char[] buff = new char[4096];
        int nch;
        boolean lastWasCR = false;
        boolean isCRLF = false;
        boolean isCR = false;
        int last;
        boolean wasEmpty = (doc.getLength() == 0);
        AttributeSet attr = null;

        StringBuilder acumulado = new StringBuilder();
        while ((nch = in.read(buff, 0, buff.length)) != -1) {
            last = 0;
            for (int counter = 0; counter < nch; counter++) {
                char c = buff[counter];
                if (c == '\r') {
                    if (lastWasCR) {
                        isCR = true;
                        acumulado.append(buff, last, counter - last);
                        acumulado.append('\n');
                        last = counter + 1;
                    } else {
                        lastWasCR = true;
                        acumulado.append(buff, last, counter - last);
                        last = counter + 1;
                    }
                } else if (lastWasCR) {
                    if (c == '\n') {
                        isCRLF = true;
                        acumulado.append('\n');
                        last = counter + 1;
                    } else {
                        isCR = true;
                        acumulado.append('\n');
                        last = counter;
                    }
                    lastWasCR = false;
                }
            }
            if (last < nch) {
                acumulado.append(buff, last, nch - last);
            }
        }
        if (lastWasCR) {
            acumulado.append('\n');
            isCR = true;
        }
        doc.insertString(pos, acumulado.toString(), attr);

        if (wasEmpty) {
            if (isCRLF) {
                doc.putProperty(EndOfLineStringProperty, "\r\n");
            } else if (isCR) {
                doc.putProperty(EndOfLineStringProperty, "\r");
            } else {
                doc.putProperty(EndOfLineStringProperty, "\n");
            }
        }
    }

    /** Escribe el tramo, poniendo el fin de linea que el documento recuerda. */
    public void write(Writer out, Document doc, int pos, int len) throws IOException,
            BadLocationException {
        if ((pos < 0) || ((pos + len) > doc.getLength())) {
            throw new BadLocationException("DefaultEditorKit.write", pos);
        }
        Segment data = new Segment();
        int nleft = len;
        int offs = pos;
        Object endOfLineProperty = doc.getProperty(EndOfLineStringProperty);
        String endOfLine = (endOfLineProperty instanceof String) ? (String) endOfLineProperty
                : null;

        while (nleft > 0) {
            int n = Math.min(nleft, 4096);
            doc.getText(offs, n, data);
            if (endOfLine == null || "\n".equals(endOfLine)) {
                out.write(data.array, data.offset, data.count);
            } else {
                for (int i = 0; i < data.count; i++) {
                    char c = data.array[data.offset + i];
                    if (c == '\n') {
                        out.write(endOfLine);
                    } else {
                        out.write(c);
                    }
                }
            }
            offs = offs + n;
            nleft = nleft - n;
        }
        out.flush();
    }

    /**
     * Escribe el caracter que se tecleo.
     *
     * <p>Es la accion por omision de un mapa de teclas: la que atiende todo lo que no tiene un
     * atajo propio. Filtra los caracteres de control, que no se escriben.
     */
    public static class DefaultKeyTypedAction extends TextAction {

        public DefaultKeyTypedAction() {
            super(defaultKeyTypedAction);
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if ((target != null) && (e != null)) {
                if (!target.isEditable() || !target.isEnabled()) {
                    return;
                }
                String content = e.getActionCommand();
                int mod = e.getModifiers();
                if ((content != null) && (content.length() > 0)) {
                    char c = content.charAt(0);
                    if ((c >= 0x20) && (c != 0x7F)) {
                        target.replaceSelection(content);
                    }
                }
            }
        }
    }

    /** Inserta el texto del comando del evento. */
    public static class InsertContentAction extends TextAction {

        public InsertContentAction() {
            super(insertContentAction);
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if ((target != null) && (e != null)) {
                if (!target.isEditable() || !target.isEnabled()) {
                    return;
                }
                String content = e.getActionCommand();
                if (content != null) {
                    target.replaceSelection(content);
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        }
    }

    /** Inserta un fin de linea: es lo que hace Enter. */
    public static class InsertBreakAction extends TextAction {

        public InsertBreakAction() {
            super(insertBreakAction);
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    return;
                }
                target.replaceSelection("\n");
            }
        }
    }

    /** Inserta una tabulacion. */
    public static class InsertTabAction extends TextAction {

        public InsertTabAction() {
            super(insertTabAction);
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    return;
                }
                target.replaceSelection("\t");
            }
        }
    }

    /** Un sonido: la respuesta a algo que no se puede hacer. */
    public static class BeepAction extends TextAction {

        public BeepAction() {
            super(beepAction);
        }

        public void actionPerformed(ActionEvent e) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    /** Cortar; sin portapapeles no hace nada. Ver la nota de la clase. */
    public static class CutAction extends TextAction {

        public CutAction() {
            super(cutAction);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    /** Copiar; sin portapapeles no hace nada. */
    public static class CopyAction extends TextAction {

        public CopyAction() {
            super(copyAction);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    /** Pegar; sin portapapeles no hace nada. */
    public static class PasteAction extends TextAction {

        public PasteAction() {
            super(pasteAction);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    /** Borra el caracter de antes del cursor, o la seleccion. */
    static class DeletePrevCharAction extends TextAction {

        DeletePrevCharAction() {
            super(deletePrevCharAction);
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if ((target != null) && target.isEditable()) {
                try {
                    Document doc = target.getDocument();
                    Caret caret = target.getCaret();
                    int dot = caret.getDot();
                    int mark = caret.getMark();
                    if (dot != mark) {
                        doc.remove(Math.min(dot, mark), Math.abs(dot - mark));
                    } else if (dot > 0) {
                        doc.remove(dot - 1, 1);
                    }
                } catch (BadLocationException bl) {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        }
    }

    /** Borra el caracter que sigue al cursor, o la seleccion. */
    static class DeleteNextCharAction extends TextAction {

        DeleteNextCharAction() {
            super(deleteNextCharAction);
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if ((target != null) && target.isEditable()) {
                try {
                    Document doc = target.getDocument();
                    Caret caret = target.getCaret();
                    int dot = caret.getDot();
                    int mark = caret.getMark();
                    if (dot != mark) {
                        doc.remove(Math.min(dot, mark), Math.abs(dot - mark));
                    } else if (dot < doc.getLength()) {
                        doc.remove(dot, 1);
                    }
                } catch (BadLocationException bl) {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        }
    }

    /** Pone el componente de solo lectura. */
    static class ReadOnlyAction extends TextAction {

        ReadOnlyAction() {
            super(readOnlyAction);
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if (target != null) {
                target.setEditable(false);
            }
        }
    }

    static class WritableAction extends TextAction {

        WritableAction() {
            super(writableAction);
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if (target != null) {
                target.setEditable(true);
            }
        }
    }

    /** Selecciona todo. */
    static class SelectAllAction extends TextAction {

        SelectAllAction() {
            super(selectAllAction);
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if (target != null) {
                target.selectAll();
            }
        }
    }

    /** Deshace la seleccion sin mover el cursor. */
    static class UnselectAction extends TextAction {

        UnselectAction() {
            super(unselectAction);
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if (target != null) {
                target.setCaretPosition(target.getCaretPosition());
            }
        }
    }

    /** Da vuelta el sentido de escritura del componente. */
    static class ToggleComponentOrientationAction extends TextAction {

        ToggleComponentOrientationAction() {
            super(toggleComponentOrientationAction);
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if (target != null) {
                java.awt.ComponentOrientation last = target.getComponentOrientation();
                java.awt.ComponentOrientation next;
                if (last == java.awt.ComponentOrientation.RIGHT_TO_LEFT) {
                    next = java.awt.ComponentOrientation.LEFT_TO_RIGHT;
                } else {
                    next = java.awt.ComponentOrientation.RIGHT_TO_LEFT;
                }
                target.setComponentOrientation(next);
                target.repaint();
            }
        }
    }

    /**
     * Las acciones de movimiento del cursor.
     *
     * <p>Todas necesitan el arbol de vistas para saber que es "una linea mas abajo", y ese arbol
     * lo tiene el UI, que en esta biblioteca no existe todavia para texto. Estan como clase para
     * que la lista de acciones este completa; cuando haya UI, funcionan.
     */
    static class NextVisualPositionAction extends TextAction {

        NextVisualPositionAction(String nm, boolean select) {
            super(nm);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    static class BeginWordAction extends TextAction {

        BeginWordAction(String nm, boolean select) {
            super(nm);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    static class EndWordAction extends TextAction {

        EndWordAction(String nm, boolean select) {
            super(nm);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    static class PreviousWordAction extends TextAction {

        PreviousWordAction(String nm, boolean select) {
            super(nm);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    static class NextWordAction extends TextAction {

        NextWordAction(String nm, boolean select) {
            super(nm);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    static class BeginLineAction extends TextAction {

        BeginLineAction(String nm, boolean select) {
            super(nm);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    static class EndLineAction extends TextAction {

        EndLineAction(String nm, boolean select) {
            super(nm);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    static class BeginParagraphAction extends TextAction {

        BeginParagraphAction(String nm, boolean select) {
            super(nm);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    static class EndParagraphAction extends TextAction {

        EndParagraphAction(String nm, boolean select) {
            super(nm);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    /** Al principio del documento. */
    static class BeginAction extends TextAction {

        private final boolean select;

        BeginAction(String nm, boolean select) {
            super(nm);
            this.select = select;
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if (target != null) {
                if (select) {
                    target.moveCaretPosition(0);
                } else {
                    target.setCaretPosition(0);
                }
            }
        }
    }

    /** Al final del documento. */
    static class EndAction extends TextAction {

        private final boolean select;

        EndAction(String nm, boolean select) {
            super(nm);
            this.select = select;
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if (target != null) {
                int dot = target.getDocument().getLength();
                if (select) {
                    target.moveCaretPosition(dot);
                } else {
                    target.setCaretPosition(dot);
                }
            }
        }
    }

    /** Selecciona la palabra, la linea o el parrafo; necesitan el arbol de vistas. */
    static class SelectWordAction extends TextAction {

        SelectWordAction() {
            super(selectWordAction);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    static class SelectLineAction extends TextAction {

        SelectLineAction() {
            super(selectLineAction);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    static class SelectParagraphAction extends TextAction {

        SelectParagraphAction() {
            super(selectParagraphAction);
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if (target != null) {
                Element p = Utilities.getParagraphElement(target, target.getCaretPosition());
                if (p != null) {
                    target.setCaretPosition(p.getStartOffset());
                    target.moveCaretPosition(p.getEndOffset());
                }
            }
        }
    }

    /** Avanza o retrocede una pantalla; necesita saber cuanto se ve. */
    static class VerticalPageAction extends TextAction {

        VerticalPageAction(String nm, int direction, boolean select) {
            super(nm);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }
}
