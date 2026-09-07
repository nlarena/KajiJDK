package javax.swing.text;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

/**
 * El juego de edicion para texto con estilos.
 *
 * <h2>Los atributos de entrada</h2>
 *
 * <p>Cuando el cursor se para en un lugar, lo que se escriba ahi tiene que salir con los atributos
 * de ese lugar. Esos atributos viven en {@link #getInputAttributes} y se rearman cada vez que el
 * cursor se mueve, en {@link #createInputAttributes}.
 *
 * <p>Que sean un objeto aparte y no los del elemento es lo que permite que el usuario prenda
 * negrita <em>antes</em> de escribir: la negrita entra en los atributos de entrada, no en el
 * documento, y recien pasa al documento con la primera letra.
 *
 * <h2>Las acciones</h2>
 *
 * <p>Las que hereda de {@link DefaultEditorKit} mueven el cursor y editan texto. Las que agrega
 * cambian atributos: negrita, cursiva, subrayado, tipografia, tamano, color y alineacion. Todas
 * bajan de {@link StyledTextAction}, que sabe encontrar el editor y el documento con estilos a
 * partir del evento.
 */
public class StyledEditorKit extends DefaultEditorKit {

    private Element currentRun;
    private Element currentParagraph;
    private MutableAttributeSet inputAttributes;
    private AttributeTracker inputAttributeUpdater;

    private static final ViewFactory defaultFactory = new StyledViewFactory();

    /** Un juego de edicion con estilos, sin instalar. */
    public StyledEditorKit() {
        createInputAttributes();
    }

    private void createInputAttributes() {
        inputAttributes = new InputAttributes(this);
    }

    public Object clone() {
        StyledEditorKit o = (StyledEditorKit) super.clone();
        o.currentRun = null;
        o.currentParagraph = null;
        o.createInputAttributes();
        o.inputAttributeUpdater = new AttributeTracker(o);
        return o;
    }

    /** Los atributos con los que saldra lo proximo que se escriba. */
    public MutableAttributeSet getInputAttributes() {
        return inputAttributes;
    }

    /** El elemento de caracteres donde esta parado el cursor. */
    public Element getCharacterAttributeRun() {
        return currentRun;
    }

    public Action[] getActions() {
        return TextAction.augmentList(super.getActions(), defaultActions);
    }

    public Document createDefaultDocument() {
        return new DefaultStyledDocument();
    }

    /** Se engancha al cursor del editor para seguir los atributos de entrada. */
    public void install(JEditorPane c) {
        if (inputAttributeUpdater == null) {
            inputAttributeUpdater = new AttributeTracker(this);
        }
        c.addCaretListener(inputAttributeUpdater);
        c.addPropertyChangeListener(inputAttributeUpdater);
        Caret caret = c.getCaret();
        if (caret != null) {
            inputAttributeUpdater.updateInputAttributes(caret.getDot(), caret.getMark(), c);
        }
    }

    public void deinstall(JEditorPane c) {
        if (inputAttributeUpdater != null) {
            c.removeCaretListener(inputAttributeUpdater);
            c.removePropertyChangeListener(inputAttributeUpdater);
        }
        currentRun = null;
        currentParagraph = null;
    }

    public ViewFactory getViewFactory() {
        return defaultFactory;
    }

    /**
     * Rearma los atributos de entrada a partir del elemento donde esta el cursor.
     *
     * <p>Los atributos de parrafo no se copian: si se copiaran, escribir heredaria la alineacion
     * del parrafo como si fuera un atributo de caracter, y despues viajaria con el texto.
     */
    protected void createInputAttributes(Element element, MutableAttributeSet set) {
        if (element.getAttributes().getAttributeCount() > 0
                || element.getEndOffset() - element.getStartOffset() > 1
                || element.getEndOffset() < element.getDocument().getLength()) {
            set.removeAttributes(set);
            set.addAttributes(element.getAttributes());
            set.removeAttribute(StyleConstants.ComponentAttribute);
            set.removeAttribute(StyleConstants.IconAttribute);
            set.removeAttribute(AbstractDocument.ElementNameAttribute);
            set.removeAttribute(StyleConstants.ComposedTextAttribute);
        }
    }

    /** Los atributos de entrada; se avisan al juego cuando cambian. */
    static class InputAttributes extends SimpleAttributeSet {

        private final StyledEditorKit juego;

        InputAttributes(StyledEditorKit juego) {
            this.juego = juego;
        }

        public AttributeSet getResolveParent() {
            return (juego.currentParagraph != null)
                    ? juego.currentParagraph.getAttributes() : null;
        }

        public Object clone() {
            return new SimpleAttributeSet(this);
        }
    }

    /**
     * Sigue al cursor y rearma los atributos de entrada cuando se mueve.
     *
     * <p>Tambien mira el cambio de documento: sin eso, cambiar el documento del editor dejaria los
     * atributos del documento anterior.
     */
    static class AttributeTracker implements CaretListener, PropertyChangeListener,
            java.io.Serializable {

        private final StyledEditorKit juego;

        AttributeTracker(StyledEditorKit juego) {
            this.juego = juego;
        }

        void updateInputAttributes(int dot, int mark, JTextComponent c) {
            // No se toca nada si hay seleccion: la seleccion no define un lugar.
            if (dot != mark) {
                return;
            }
            int start = Math.min(dot, mark);
            Document doc = c.getDocument();
            if (!(doc instanceof StyledDocument)) {
                // El juego se instala antes de que el editor tenga su documento con estilos:
                // `setEditorKit` llama a `install` y recien despues a `setDocument`. En ese hueco
                // el documento es el anterior --uno plano-- y no hay atributos que mirar. Sin esta
                // guarda, armar un JTextPane revienta antes de terminar el constructor.
                return;
            }
            Element run;
            juego.currentParagraph = doc.getDefaultRootElement();
            if (juego.currentParagraph instanceof AbstractDocument.BranchElement) {
                juego.currentParagraph = ((StyledDocument) doc).getParagraphElement(start);
                run = ((StyledDocument) doc).getCharacterElement(start);
            } else {
                run = null;
            }
            if (run != juego.currentRun) {
                juego.currentRun = run;
                juego.createInputAttributes(juego.currentRun, juego.getInputAttributes());
            }
        }

        public void propertyChange(PropertyChangeEvent evt) {
            Object newValue = evt.getNewValue();
            Object source = evt.getSource();
            if ((source instanceof JTextComponent) && (newValue instanceof Document)) {
                JTextComponent c = (JTextComponent) source;
                Caret caret = c.getCaret();
                if (caret != null) {
                    updateInputAttributes(caret.getDot(), caret.getMark(), c);
                }
            }
        }

        public void caretUpdate(CaretEvent e) {
            updateInputAttributes(e.getDot(), e.getMark(), (JTextComponent) e.getSource());
        }
    }

    /** La fabrica de vistas con estilos: parrafos, cajas, componentes e iconos. */
    static class StyledViewFactory implements ViewFactory {

        public View create(Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                if (kind.equals(AbstractDocument.ContentElementName)) {
                    return new LabelView(elem);
                } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                    return new ParagraphView(elem);
                } else if (kind.equals(AbstractDocument.SectionElementName)) {
                    return new BoxView(elem, View.Y_AXIS);
                } else if (kind.equals(StyleConstants.ComponentElementName)) {
                    return new ComponentView(elem);
                } else if (kind.equals(StyleConstants.IconElementName)) {
                    return new IconView(elem);
                }
            }
            // Un elemento que no se conoce se muestra como texto.
            return new LabelView(elem);
        }
    }

    private static final Action[] defaultActions = {
        new FontFamilyAction("font-family-SansSerif", "SansSerif"),
        new FontFamilyAction("font-family-Monospaced", "Monospaced"),
        new FontFamilyAction("font-family-Serif", "Serif"),
        new FontSizeAction("font-size-8", 8),
        new FontSizeAction("font-size-10", 10),
        new FontSizeAction("font-size-12", 12),
        new FontSizeAction("font-size-14", 14),
        new FontSizeAction("font-size-16", 16),
        new FontSizeAction("font-size-18", 18),
        new FontSizeAction("font-size-24", 24),
        new FontSizeAction("font-size-36", 36),
        new FontSizeAction("font-size-48", 48),
        new AlignmentAction("left-justify", StyleConstants.ALIGN_LEFT),
        new AlignmentAction("center-justify", StyleConstants.ALIGN_CENTER),
        new AlignmentAction("right-justify", StyleConstants.ALIGN_RIGHT),
        new BoldAction(),
        new ItalicAction(),
        new UnderlineAction()
    };

    /**
     * La base de las acciones que cambian atributos.
     *
     * <p>Trae lo que toda accion de estilos necesita: encontrar el editor a partir del evento y
     * aplicar atributos al tramo seleccionado o, si no hay seleccion, a los de entrada.
     */
    public abstract static class StyledTextAction extends TextAction {

        /** Una accion con ese nombre. */
        public StyledTextAction(String nm) {
            super(nm);
        }

        /** El editor donde ocurrio el evento. */
        protected final JEditorPane getEditor(ActionEvent e) {
            JTextComponent tcomp = getTextComponent(e);
            if (tcomp instanceof JEditorPane) {
                return (JEditorPane) tcomp;
            }
            return null;
        }

        protected final StyledDocument getStyledDocument(JEditorPane e) {
            Document d = e.getDocument();
            if (d instanceof StyledDocument) {
                return (StyledDocument) d;
            }
            throw new IllegalArgumentException("document must be StyledDocument");
        }

        protected final StyledEditorKit getStyledEditorKit(JEditorPane e) {
            EditorKit k = e.getEditorKit();
            if (k instanceof StyledEditorKit) {
                return (StyledEditorKit) k;
            }
            throw new IllegalArgumentException("EditorKit must be StyledEditorKit");
        }

        /**
         * Aplica atributos de caracter a la seleccion, o a lo proximo que se escriba.
         *
         * <p>Sin seleccion no hay nada que cambiar en el documento, asi que van a los atributos de
         * entrada. Ese es el caso de prender negrita y despues escribir.
         */
        protected final void setCharacterAttributes(JEditorPane editor, AttributeSet attr,
                boolean replace) {
            int p0 = editor.getSelectionStart();
            int p1 = editor.getSelectionEnd();
            if (p0 != p1) {
                StyledDocument doc = getStyledDocument(editor);
                doc.setCharacterAttributes(p0, p1 - p0, attr, replace);
            }
            StyledEditorKit k = getStyledEditorKit(editor);
            MutableAttributeSet inputAttributes = k.getInputAttributes();
            if (replace) {
                inputAttributes.removeAttributes(inputAttributes);
            }
            inputAttributes.addAttributes(attr);
        }

        /** Aplica atributos a los parrafos que toca la seleccion. */
        protected final void setParagraphAttributes(JEditorPane editor, AttributeSet attr,
                boolean replace) {
            int p0 = editor.getSelectionStart();
            int p1 = editor.getSelectionEnd();
            StyledDocument doc = getStyledDocument(editor);
            doc.setParagraphAttributes(p0, p1 - p0, attr, replace);
        }
    }

    /** Prende o apaga la negrita, segun como este el texto donde esta el cursor. */
    public static class BoldAction extends StyledTextAction {

        public BoldAction() {
            super("font-bold");
        }

        public void actionPerformed(ActionEvent e) {
            JEditorPane editor = getEditor(e);
            if (editor != null) {
                StyledEditorKit kit = getStyledEditorKit(editor);
                MutableAttributeSet attr = kit.getInputAttributes();
                boolean bold = !StyleConstants.isBold(attr);
                SimpleAttributeSet sas = new SimpleAttributeSet();
                StyleConstants.setBold(sas, bold);
                setCharacterAttributes(editor, sas, false);
            }
        }
    }

    /** Prende o apaga la cursiva. */
    public static class ItalicAction extends StyledTextAction {

        public ItalicAction() {
            super("font-italic");
        }

        public void actionPerformed(ActionEvent e) {
            JEditorPane editor = getEditor(e);
            if (editor != null) {
                StyledEditorKit kit = getStyledEditorKit(editor);
                MutableAttributeSet attr = kit.getInputAttributes();
                boolean italic = !StyleConstants.isItalic(attr);
                SimpleAttributeSet sas = new SimpleAttributeSet();
                StyleConstants.setItalic(sas, italic);
                setCharacterAttributes(editor, sas, false);
            }
        }
    }

    /** Prende o apaga el subrayado. */
    public static class UnderlineAction extends StyledTextAction {

        public UnderlineAction() {
            super("font-underline");
        }

        public void actionPerformed(ActionEvent e) {
            JEditorPane editor = getEditor(e);
            if (editor != null) {
                StyledEditorKit kit = getStyledEditorKit(editor);
                MutableAttributeSet attr = kit.getInputAttributes();
                boolean underline = !StyleConstants.isUnderline(attr);
                SimpleAttributeSet sas = new SimpleAttributeSet();
                StyleConstants.setUnderline(sas, underline);
                setCharacterAttributes(editor, sas, false);
            }
        }
    }

    /**
     * Pone una tipografia.
     *
     * <p>El nombre puede venir en el evento y no en la accion: asi un menu armado desde datos
     * puede usar una sola accion para todas las tipografias.
     */
    public static class FontFamilyAction extends StyledTextAction {

        private String family;

        /** Una accion con ese nombre que pone esa tipografia. */
        public FontFamilyAction(String nm, String family) {
            super(nm);
            this.family = family;
        }

        public void actionPerformed(ActionEvent e) {
            JEditorPane editor = getEditor(e);
            if (editor != null) {
                String family = this.family;
                if ((e != null) && (e.getSource() == editor)) {
                    String s = e.getActionCommand();
                    if (s != null) {
                        family = s;
                    }
                }
                if (family != null) {
                    SimpleAttributeSet attr = new SimpleAttributeSet();
                    StyleConstants.setFontFamily(attr, family);
                    setCharacterAttributes(editor, attr, false);
                }
            }
        }
    }

    /** Pone un tamano de letra; el evento puede traer otro, como en la tipografia. */
    public static class FontSizeAction extends StyledTextAction {

        private int size;

        public FontSizeAction(String nm, int size) {
            super(nm);
            this.size = size;
        }

        public void actionPerformed(ActionEvent e) {
            JEditorPane editor = getEditor(e);
            if (editor != null) {
                int size = this.size;
                if ((e != null) && (e.getSource() == editor)) {
                    String s = e.getActionCommand();
                    try {
                        size = Integer.parseInt(s, 10);
                    } catch (NumberFormatException nfe) {
                        // El comando no era un numero: queda el de la accion.
                    }
                }
                if (size != 0) {
                    SimpleAttributeSet attr = new SimpleAttributeSet();
                    StyleConstants.setFontSize(attr, size);
                    setCharacterAttributes(editor, attr, false);
                }
            }
        }
    }

    /** Pone un color de letra; el comando puede traerlo como texto. */
    public static class ForegroundAction extends StyledTextAction {

        private Color fg;

        public ForegroundAction(String nm, Color fg) {
            super(nm);
            this.fg = fg;
        }

        public void actionPerformed(ActionEvent e) {
            JEditorPane editor = getEditor(e);
            if (editor != null) {
                Color fg = this.fg;
                if ((e != null) && (e.getSource() == editor)) {
                    String s = e.getActionCommand();
                    try {
                        fg = Color.decode(s);
                    } catch (NumberFormatException nfe) {
                        // El comando no era un color: queda el de la accion.
                    }
                }
                if (fg != null) {
                    SimpleAttributeSet attr = new SimpleAttributeSet();
                    StyleConstants.setForeground(attr, fg);
                    setCharacterAttributes(editor, attr, false);
                }
            }
        }
    }

    /** Alinea los parrafos de la seleccion. */
    public static class AlignmentAction extends StyledTextAction {

        private int a;

        public AlignmentAction(String nm, int a) {
            super(nm);
            this.a = a;
        }

        public void actionPerformed(ActionEvent e) {
            JEditorPane editor = getEditor(e);
            if (editor != null) {
                int a = this.a;
                if ((e != null) && (e.getSource() == editor)) {
                    String s = e.getActionCommand();
                    try {
                        a = Integer.parseInt(s, 10);
                    } catch (NumberFormatException nfe) {
                        // Igual que en el tamano.
                    }
                }
                SimpleAttributeSet attr = new SimpleAttributeSet();
                StyleConstants.setAlignment(attr, a);
                setParagraphAttributes(editor, attr, false);
            }
        }
    }
}
