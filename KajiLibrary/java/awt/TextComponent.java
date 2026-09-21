package java.awt;

import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.awt.im.InputMethodRequests;
import java.util.EventListener;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleText;

/**
 * What a text field and a text area have in common: the text, the selection and the caret.
 *
 * <p>It cannot be instantiated —its constructor is package-private— because a text component that
 * has not decided whether it is one line or several is nothing.
 *
 * <p>The selection is kept as two positions, and the class takes care that they are in order and
 * inside the text. The caret is not a third number: it is the start of the selection.
 */
public class TextComponent extends Component implements Accessible {

    private static final long serialVersionUID = -2214773872412987419L;

    /** What it says. */
    String text;

    /** Whether it can be typed into. */
    boolean editable = true;

    /** Where the selection starts. */
    int selectionStart;

    /** Where it ends. */
    int selectionEnd;

    /** Whether someone from outside set its background colour. */
    boolean backgroundSetByClientCode = false;

    /** The listeners, chained. */
    protected transient TextListener textListener;

    /** With that text; `null` counts as empty. */
    TextComponent(String text) throws HeadlessException {
        this.text = text == null ? "" : text;
    }

    /**
     * Turns the input methods on or off.
     *
     * <p>Without a screen none of them is active, but the request is accepted without breaking: a
     * program that calls it while building its interface has no reason to fail because of that.
     */
    public void enableInputMethods(boolean enable) {
        super.enableInputMethods(enable);
    }

    boolean areInputMethodsEnabled() {
        return false;
    }

    /**
     * What the input method needs to know about the component.
     *
     * @return `null` always: with no active input method there is nothing to answer, and answering
     *     something empty would make it look as though composing over the component works
     */
    public InputMethodRequests getInputMethodRequests() {
        return null;
    }

    /** Declares it showable. */
    public void addNotify() {
        super.addNotify();
    }

    /** Declares it no longer showable. */
    public void removeNotify() {
        super.removeNotify();
    }

    /**
     * Changes the text.
     *
     * <p>The selection is clamped to the new text: leaving it pointing outside would be an invalid
     * state that later blows up in any `getSelectedText`.
     *
     * @param t the text; `null` counts as empty
     */
    public synchronized void setText(String t) {
        this.text = t == null ? "" : t;
        if (this.selectionStart > this.text.length()) {
            this.selectionStart = this.text.length();
        }
        if (this.selectionEnd > this.text.length()) {
            this.selectionEnd = this.text.length();
        }
    }

    /** What it says. */
    public synchronized String getText() {
        return this.text;
    }

    /**
     * What is selected.
     *
     * @return the selected stretch, or the empty string if there is no selection
     */
    public synchronized String getSelectedText() {
        return this.getText().substring(this.getSelectionStart(), this.getSelectionEnd());
    }

    /** Whether it can be typed into. */
    public boolean isEditable() {
        return this.editable;
    }

    /** Lets it be typed into or not. */
    public synchronized void setEditable(boolean b) {
        this.editable = b;
    }

    /**
     * The background colour.
     *
     * <p>A **non-editable** text component inherits the parent's background even if one was never
     * set on it, and that is the reason this is overridden: the colour of a read-only field has to
     * look like that of the panel holding it.
     */
    public Color getBackground() {
        if (!this.editable && !this.backgroundSetByClientCode) {
            Container p = this.getParent();
            if (p != null) {
                return p.getBackground();
            }
        }
        return super.getBackground();
    }

    /** Sets its background colour. */
    public void setBackground(Color c) {
        this.backgroundSetByClientCode = true;
        super.setBackground(c);
    }

    /** Where the selection starts. */
    public synchronized int getSelectionStart() {
        return this.selectionStart;
    }

    /**
     * Moves the start of the selection.
     *
     * <p>Moving it past the end drags the end with it: the selection never ends up reversed.
     */
    public synchronized void setSelectionStart(int selectionStart) {
        this.select(selectionStart, this.getSelectionEnd());
    }

    /** Where the selection ends. */
    public synchronized int getSelectionEnd() {
        return this.selectionEnd;
    }

    /** Moves the end of the selection; putting it before the start pulls it up to the start. */
    public synchronized void setSelectionEnd(int selectionEnd) {
        this.select(this.getSelectionStart(), selectionEnd);
    }

    /**
     * Selects that stretch.
     *
     * <p>The positions are clamped to the text, so any pair works. It is deliberately tolerant: the
     * typical caller works positions out from a search and has no reason to validate them.
     *
     * <p>A reversed pair is not swapped: the end is pulled up to the start and what is left is an
     * empty selection there. It is what the JDK does, and it is why `select(5, 2)` selects nothing
     * instead of the same as `select(2, 5)`.
     */
    public synchronized void select(int selectionStart, int selectionEnd) {
        String t = this.getText();
        if (selectionStart > t.length()) {
            selectionStart = t.length();
        }
        if (selectionStart < 0) {
            selectionStart = 0;
        }
        if (selectionEnd > t.length()) {
            selectionEnd = t.length();
        }
        if (selectionEnd < selectionStart) {
            selectionEnd = selectionStart;
        }
        this.selectionStart = selectionStart;
        this.selectionEnd = selectionEnd;
    }

    /** Selects everything. */
    public synchronized void selectAll() {
        this.select(0, this.getText().length());
    }

    /**
     * Puts the caret there.
     *
     * <p>It is the empty selection at that position, not a third state.
     *
     * @throws IllegalArgumentException if the position is negative
     */
    public synchronized void setCaretPosition(int position) {
        if (position < 0) {
            throw new IllegalArgumentException("position less than zero.");
        }
        int n = this.getText().length();
        if (position > n) {
            position = n;
        }
        this.select(position, position);
    }

    /**
     * Where the caret is.
     *
     * <p>It is the **start** of the selection, not the end. It sounds backwards —while dragging,
     * the caret goes where the mouse is, which is the end— but it is what AWT answers without a
     * window, and it was checked against the JDK.
     */
    public synchronized int getCaretPosition() {
        return this.getSelectionStart();
    }

    /** Adds a listener; `null` does nothing. */
    public synchronized void addTextListener(TextListener l) {
        if (l == null) {
            return;
        }
        this.textListener = AWTEventMulticaster.add(this.textListener, l);
        this.enableEvents(AWTEvent.TEXT_EVENT_MASK);
    }

    /** Removes a listener. */
    public synchronized void removeTextListener(TextListener l) {
        if (l == null) {
            return;
        }
        this.textListener = AWTEventMulticaster.remove(this.textListener, l);
    }

    /** The listeners that are set. */
    public synchronized TextListener[] getTextListeners() {
        return AWTEventMulticaster.getListeners(this.textListener, TextListener.class);
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (listenerType == TextListener.class) {
            return AWTEventMulticaster.getListeners(this.textListener, listenerType);
        }
        return super.getListeners(listenerType);
    }

    protected void processEvent(AWTEvent e) {
        if (e instanceof TextEvent) {
            this.processTextEvent((TextEvent) e);
            return;
        }
        super.processEvent(e);
    }

    /** Tells the text listeners. */
    protected void processTextEvent(TextEvent e) {
        TextListener l = this.textListener;
        if (l != null) {
            l.textValueChanged(e);
        }
    }

    protected String paramString() {
        String s = super.paramString() + ",text=" + this.getText();
        if (this.editable) {
            s = s + ",editable";
        }
        return s + ",selection=" + this.getSelectionStart() + "-" + this.getSelectionEnd();
    }

    /** The accessibility information of this text component. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTTextComponent();
        }
        return this.accessibleContext;
    }

    /**
     * The accessibility of a text component.
     *
     * <p>It implements {@link AccessibleText}, which is what lets a screen reader walk the content
     * by letter, word or line. What it does **not** implement is {@link
     * AccessibleText#getCharacterBounds}, which returns `null`, nor {@link
     * AccessibleText#getCharacterAttribute}: both need the typography measured on a screen, and
     * without it any answer would be invented.
     *
     * <p>{@link AccessibleText#SENTENCE} is answered with the **line**. Telling where a sentence
     * ends needs text analysis that is not done here, and for a field or an area the line is the
     * unit that the reader ends up walking.
     */
    protected class AccessibleAWTTextComponent extends AccessibleAWTComponent
            implements AccessibleText {

        /** For the subclasses. */
        protected AccessibleAWTTextComponent() {
        }

        public AccessibleText getAccessibleText() {
            return this;
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.TEXT;
        }

        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = super.getAccessibleStateSet();
            if (TextComponent.this.isEditable()) {
                s.add(AccessibleState.EDITABLE);
            }
            s.add(AccessibleState.SINGLE_LINE);
            return s;
        }

        /**
         * Which letter is at that point of the screen.
         *
         * @return -1 always: without measured typography there is no way to know
         */
        public int getIndexAtPoint(Point p) {
            return -1;
        }

        /**
         * Where that letter falls on the screen.
         *
         * @return `null` always, for the same reason
         */
        public Rectangle getCharacterBounds(int i) {
            return null;
        }

        /** How many letters it has. */
        public int getCharCount() {
            return TextComponent.this.getText().length();
        }

        /** Where the caret is. */
        public int getCaretPosition() {
            return TextComponent.this.getCaretPosition();
        }

        /**
         * The attributes of that letter.
         *
         * @return `null` always: AWT text has no per-letter attributes
         */
        public javax.swing.text.AttributeSet getCharacterAttribute(int i) {
            return null;
        }

        public int getSelectionStart() {
            return TextComponent.this.getSelectionStart();
        }

        public int getSelectionEnd() {
            return TextComponent.this.getSelectionEnd();
        }

        public String getSelectedText() {
            String s = TextComponent.this.getSelectedText();
            return s.isEmpty() ? null : s;
        }

        /**
         * The letter, word or line at that position.
         *
         * @return the stretch, or `null` if the position does not exist or the part is not one of
         *     the three
         */
        public String getAtIndex(int part, int index) {
            return this.segment(part, index, 0);
        }

        /** The same, but what comes after. */
        public String getAfterIndex(int part, int index) {
            return this.segment(part, index, 1);
        }

        /** The same, but what comes before. */
        public String getBeforeIndex(int part, int index) {
            return this.segment(part, index, -1);
        }

        /**
         * Takes the letter, word or line at that position, shifted in that direction.
         *
         * @param direction -1 the previous one, 0 the one at the position, 1 the next one
         */
        private String segment(int part, int index, int direction) {
            String t = TextComponent.this.getText();
            if (index < 0 || index >= t.length()) {
                return null;
            }
            if (part == AccessibleText.CHARACTER) {
                int i = index + direction;
                if (i < 0 || i >= t.length()) {
                    return null;
                }
                return t.substring(i, i + 1);
            }
            if (part == AccessibleText.WORD) {
                return this.word(t, index, direction);
            }
            if (part == AccessibleText.SENTENCE) {
                return this.line(t, index, direction);
            }
            return null;
        }

        /** The word at that position, shifted in that direction. */
        private String word(String t, int index, int direction) {
            int from = index;
            int to = index;
            while (from > 0 && !Character.isWhitespace(t.charAt(from - 1))) {
                from = from - 1;
            }
            while (to < t.length() && !Character.isWhitespace(t.charAt(to))) {
                to = to + 1;
            }
            if (direction == 0) {
                return from == to ? null : t.substring(from, to);
            }
            if (direction > 0) {
                int i = to;
                while (i < t.length() && Character.isWhitespace(t.charAt(i))) {
                    i = i + 1;
                }
                if (i >= t.length()) {
                    return null;
                }
                int j = i;
                while (j < t.length() && !Character.isWhitespace(t.charAt(j))) {
                    j = j + 1;
                }
                return t.substring(i, j);
            }
            int i = from;
            while (i > 0 && Character.isWhitespace(t.charAt(i - 1))) {
                i = i - 1;
            }
            if (i <= 0) {
                return null;
            }
            int j = i;
            while (j > 0 && !Character.isWhitespace(t.charAt(j - 1))) {
                j = j - 1;
            }
            return t.substring(j, i);
        }

        /** The line at that position, shifted in that direction. */
        private String line(String t, int index, int direction) {
            int from = t.lastIndexOf('\n', index - 1) + 1;
            int to = t.indexOf('\n', index);
            if (to < 0) {
                to = t.length();
            } else {
                to = to + 1;
            }
            if (direction == 0) {
                return t.substring(from, to);
            }
            if (direction > 0) {
                if (to >= t.length()) {
                    return null;
                }
                int nextEnd = t.indexOf('\n', to);
                return nextEnd < 0 ? t.substring(to) : t.substring(to, nextEnd + 1);
            }
            if (from <= 0) {
                return null;
            }
            int prevStart = t.lastIndexOf('\n', from - 2) + 1;
            return t.substring(prevStart, from);
        }
    }
}
