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
 * The editor kit for styled text.
 *
 * <h2>The input attributes</h2>
 *
 * <p>When the cursor stops in a place, what is typed there has to come out with that place's
 * attributes. Those attributes live in {@link #getInputAttributes} and are rebuilt every time
 * the cursor moves, in {@link #createInputAttributes}.
 *
 * <p>That they are a separate object and not the element's is what allows the user to turn bold
 * on <em>before</em> typing: the bold goes into the input attributes, not into the document, and
 * only passes to the document with the first letter.
 *
 * <h2>The actions</h2>
 *
 * <p>Those it inherits from {@link DefaultEditorKit} move the cursor and edit text. Those it
 * adds change attributes: bold, italic, underline, typeface, size, colour and alignment. They
 * all descend from {@link StyledTextAction}, which knows how to find the editor and the styled
 * document from the event.
 */
public class StyledEditorKit extends DefaultEditorKit {

    private Element currentRun;
    private Element currentParagraph;
    private MutableAttributeSet inputAttributes;
    private AttributeTracker inputAttributeUpdater;

    private static final ViewFactory defaultFactory = new StyledViewFactory();

    /** A styled editor kit, not installed. */
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

    /** The attributes the next thing typed will come out with. */
    public MutableAttributeSet getInputAttributes() {
        return inputAttributes;
    }

    /** The character element the cursor is standing in. */
    public Element getCharacterAttributeRun() {
        return currentRun;
    }

    public Action[] getActions() {
        return TextAction.augmentList(super.getActions(), defaultActions);
    }

    public Document createDefaultDocument() {
        return new DefaultStyledDocument();
    }

    /** It hooks itself to the editor's cursor to follow the input attributes. */
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
     * It rebuilds the input attributes from the element the cursor is in.
     *
     * <p>The paragraph attributes are not copied: if they were, typing would inherit the
     * paragraph's alignment as if it were a character attribute, and afterwards it would travel
     * with the text.
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

    /** The input attributes; the kit is told when they change. */
    static class InputAttributes extends SimpleAttributeSet {

        private final StyledEditorKit kit;

        InputAttributes(StyledEditorKit kit) {
            this.kit = kit;
        }

        public AttributeSet getResolveParent() {
            return (kit.currentParagraph != null)
                    ? kit.currentParagraph.getAttributes() : null;
        }

        public Object clone() {
            return new SimpleAttributeSet(this);
        }
    }

    /**
     * It follows the cursor and rebuilds the input attributes when it moves.
     *
     * <p>It also watches the change of document: without that, changing the editor's document would
     * leave the previous document's attributes.
     */
    static class AttributeTracker implements CaretListener, PropertyChangeListener,
            java.io.Serializable {

        private final StyledEditorKit kit;

        AttributeTracker(StyledEditorKit kit) {
            this.kit = kit;
        }

        void updateInputAttributes(int dot, int mark, JTextComponent c) {
            // Nothing is touched if there is a selection: a selection does not define a place.
            if (dot != mark) {
                return;
            }
            int start = Math.min(dot, mark);
            Document doc = c.getDocument();
            if (!(doc instanceof StyledDocument)) {
                // The kit is installed before the editor has its styled document:
                                // `setEditorKit` calls `install` and only afterwards `setDocument`.
                                // In that gap the document is the previous one --a plain one-- and
                                // there are no attributes to look at. Without this guard, building
                                // a JTextPane blows up before the constructor finishes.
                return;
            }
            Element run;
            kit.currentParagraph = doc.getDefaultRootElement();
            if (kit.currentParagraph instanceof AbstractDocument.BranchElement) {
                kit.currentParagraph = ((StyledDocument) doc).getParagraphElement(start);
                run = ((StyledDocument) doc).getCharacterElement(start);
            } else {
                run = null;
            }
            if (run != kit.currentRun) {
                kit.currentRun = run;
                kit.createInputAttributes(kit.currentRun, kit.getInputAttributes());
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

    /** The styled view factory: paragraphs, boxes, components and icons. */
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
            // An element that is not known is shown as text.
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
     * The base of the actions that change attributes.
     *
     * <p>It brings what every style action needs: finding the editor from the event and applying
     * attributes to the selected stretch or, if there is no selection, to the input ones.
     */
    public abstract static class StyledTextAction extends TextAction {

        /** An action with that name. */
        public StyledTextAction(String nm) {
            super(nm);
        }

        /** The editor where the event happened. */
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
         * It applies character attributes to the selection, or to the next thing typed.
         *
         * <p>With no selection there is nothing to change in the document, so they go to the input
         * attributes. That is the case of turning bold on and then typing.
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

        /** It applies attributes to the paragraphs the selection touches. */
        protected final void setParagraphAttributes(JEditorPane editor, AttributeSet attr,
                boolean replace) {
            int p0 = editor.getSelectionStart();
            int p1 = editor.getSelectionEnd();
            StyledDocument doc = getStyledDocument(editor);
            doc.setParagraphAttributes(p0, p1 - p0, attr, replace);
        }
    }

    /** It turns bold on or off, according to how the text where the cursor is stands. */
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

    /** It turns italics on or off. */
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

    /** It turns the underline on or off. */
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
     * It sets a typeface.
     *
     * <p>The name may come in the event and not in the action: that way a menu built from data can
     * use a single action for every typeface.
     */
    public static class FontFamilyAction extends StyledTextAction {

        private String family;

        /** An action with that name that sets that typeface. */
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

    /** It sets a letter size; the event may bring another, as with the typeface. */
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
                        // The command was not a number: the action's is left.
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

    /** It sets a letter colour; the command may bring it as text. */
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
                        // The command was not a colour: the action's is left.
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

    /** It aligns the selection's paragraphs. */
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
                        // The same as with the size.
                    }
                }
                SimpleAttributeSet attr = new SimpleAttributeSet();
                StyleConstants.setAlignment(attr, a);
                setParagraphAttributes(editor, attr, false);
            }
        }
    }
}
