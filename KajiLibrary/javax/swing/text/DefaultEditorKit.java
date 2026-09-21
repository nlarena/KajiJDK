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
 * The plain text editor kit: the one any component that does not say otherwise uses.
 *
 * <h2>The actions have names</h2>
 *
 * <p>This class's constants are the actions' names, not the actions. A key map ties a
 * combination to a <em>name</em>, and only on installing is it resolved against the actions the
 * kit offers. That detour allows writing the shortcut table without having the actions at hand
 * and changing the kit without redoing the table.
 *
 * <p>The names follow a pattern worth reading once: {@code selection*} is the same action
 * extending the selection instead of moving the cursor, and {@code *Action} is always a suffix.
 * "Move forward one character" and "move forward selecting" are two actions and not one with
 * a parameter, because a key ties one action.
 *
 * <h2>What those that are there do</h2>
 *
 * <p>Those that need a screen or a clipboard --cut, paste, move by pages-- are there as a class
 * and as a name, and do nothing: without keyboard focus there is nobody to fire them, and
 * without a clipboard there would be nowhere to get the text from. Those that work on the
 * document --typing a character, inserting a line ending, a tab-- work.
 */
public class DefaultEditorKit extends EditorKit {

    /** The document property with the line ending the file that was read carried. */
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

    /** Plain text. */
    public String getContentType() {
        return "text/plain";
    }

    /** The factory that builds plain text views: one line per line. */
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

    /** It reads plain text; the three line endings are normalized to one. */
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

    /** The attributes things are written with; in plain text, none. */
    MutableAttributeSet getInputAttributes() {
        return null;
    }

    /**
     * It reads from the stream and inserts.
     *
     * <p>A loose {@code \r\n} or {@code \r} is kept as {@code \n}, and the line ending the file
     * carried is noted in the {@link #EndOfLineStringProperty} property: that way it can be written
     * back as it was.
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

        StringBuilder accumulated = new StringBuilder();
        while ((nch = in.read(buff, 0, buff.length)) != -1) {
            last = 0;
            for (int counter = 0; counter < nch; counter++) {
                char c = buff[counter];
                if (c == '\r') {
                    if (lastWasCR) {
                        isCR = true;
                        accumulated.append(buff, last, counter - last);
                        accumulated.append('\n');
                        last = counter + 1;
                    } else {
                        lastWasCR = true;
                        accumulated.append(buff, last, counter - last);
                        last = counter + 1;
                    }
                } else if (lastWasCR) {
                    if (c == '\n') {
                        isCRLF = true;
                        accumulated.append('\n');
                        last = counter + 1;
                    } else {
                        isCR = true;
                        accumulated.append('\n');
                        last = counter;
                    }
                    lastWasCR = false;
                }
            }
            if (last < nch) {
                accumulated.append(buff, last, nch - last);
            }
        }
        if (lastWasCR) {
            accumulated.append('\n');
            isCR = true;
        }
        doc.insertString(pos, accumulated.toString(), attr);

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

    /** It writes the stretch, putting the line ending the document remembers. */
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
     * It writes the character that was typed.
     *
     * <p>It is a key map's default action: the one that attends to everything that has no shortcut
     * of its own. It filters out the control characters, which are not written.
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

    /** It inserts the text of the event's command. */
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

    /** It inserts a line ending: it is what Enter does. */
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

    /** It inserts a tab. */
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

    /** A sound: the answer to something that cannot be done. */
    public static class BeepAction extends TextAction {

        public BeepAction() {
            super(beepAction);
        }

        public void actionPerformed(ActionEvent e) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    /** Cut; without a clipboard it does nothing. See the class note. */
    public static class CutAction extends TextAction {

        public CutAction() {
            super(cutAction);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    /** Copy; without a clipboard it does nothing. */
    public static class CopyAction extends TextAction {

        public CopyAction() {
            super(copyAction);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    /** Paste; without a clipboard it does nothing. */
    public static class PasteAction extends TextAction {

        public PasteAction() {
            super(pasteAction);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    /** It removes the character before the cursor, or the selection. */
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

    /** It removes the character after the cursor, or the selection. */
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

    /** It makes the component read-only. */
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

    /** It selects everything. */
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

    /** It undoes the selection without moving the cursor. */
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

    /** It flips the component's writing direction. */
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
     * The cursor movement actions.
     *
     * <p>They all need the view tree to know what "one line further down" is, and that tree is
     * held by the UI, which in this library does not exist yet for text. They are there as classes
     * so that the list of actions is complete; when there is a UI, they work.
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

    /** To the beginning of the document. */
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

    /** To the end of the document. */
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

    /** It selects the word, the line or the paragraph; they need the view tree. */
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

    /** It moves forward or back one screen; it needs to know how much is seen. */
    static class VerticalPageAction extends TextAction {

        VerticalPageAction(String nm, int direction, boolean select) {
            super(nm);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }
}
