package java.awt;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * A single line of text that can be neither edited nor selected.
 *
 * <p>It is the most passive component of AWT: it shows a text and nothing else. It generates no
 * events of its own and has no state beyond what it says and how it aligns it.
 *
 * <p>In AWT a label does not take the focus either, and that part is **not** enforced here: the JDK
 * leaves it to the peer —a label's peer refuses the focus— and with no peer this one is focusable
 * like any other component, so a traversal can land on it.
 */
public class Label extends Component implements Accessible {

    private static final long serialVersionUID = 3094126758329070636L;

    /** Aligns the text to the left. */
    public static final int LEFT = 0;

    /** Centres it. */
    public static final int CENTER = 1;

    /** Aligns it to the right. */
    public static final int RIGHT = 2;

    private static int labelCounter = 0;

    /** What it says. */
    String text;

    /** How it aligns it. */
    int alignment = LEFT;

    /** An empty label, aligned to the left. */
    public Label() throws HeadlessException {
        this("", LEFT);
    }

    /** A label with that text, aligned to the left. */
    public Label(String text) throws HeadlessException {
        this(text, LEFT);
    }

    /**
     * A label with that text and that alignment.
     *
     * @throws IllegalArgumentException if the alignment is not {@link #LEFT}, {@link #CENTER} or
     *     {@link #RIGHT}
     */
    public Label(String text, int alignment) throws HeadlessException {
        this.text = text;
        this.setAlignment(alignment);
    }

    String constructComponentName() {
        synchronized (Label.class) {
            String n = "label" + labelCounter;
            labelCounter = labelCounter + 1;
            return n;
        }
    }

    /** Declares it showable. */
    public void addNotify() {
        super.addNotify();
    }

    /** How it aligns the text. */
    public int getAlignment() {
        return this.alignment;
    }

    /**
     * Changes the alignment.
     *
     * @throws IllegalArgumentException if it is not one of the three constants
     */
    public synchronized void setAlignment(int alignment) {
        if (alignment != LEFT && alignment != CENTER && alignment != RIGHT) {
            throw new IllegalArgumentException("improper alignment: " + alignment);
        }
        this.alignment = alignment;
    }

    /**
     * What it says.
     *
     * @return the text, or `null` if a `null` was set —the no-argument constructor leaves it empty,
     *     not `null`
     */
    public String getText() {
        return this.text;
    }

    /**
     * Changes what it says.
     *
     * <p>Setting the same text does nothing: the comparison keeps the layout from being invalidated
     * needlessly when something refreshes the label in a loop.
     */
    public void setText(String text) {
        boolean changed;
        synchronized (this) {
            changed = text != this.text && (this.text == null || !this.text.equals(text));
            if (changed) {
                this.text = text;
            }
        }
        if (changed) {
            this.invalidate();
        }
    }

    protected String paramString() {
        String alignmentName = "left";
        if (this.alignment == CENTER) {
            alignmentName = "center";
        } else if (this.alignment == RIGHT) {
            alignmentName = "right";
        }
        return super.paramString() + ",align=" + alignmentName + ",text=" + this.text;
    }

    /** The accessibility information of this label. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTLabel();
        }
        return this.accessibleContext;
    }

    /**
     * The accessibility of a label.
     *
     * <p>Its accessible name is **the text**, not the component's name: for whoever reads it with a
     * screen reader, the label is what it says.
     */
    protected class AccessibleAWTLabel extends AccessibleAWTComponent {

        /** For the subclasses. */
        protected AccessibleAWTLabel() {
        }

        public String getAccessibleName() {
            if (Label.this.getText() == null) {
                return super.getAccessibleName();
            }
            return Label.this.getText();
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.LABEL;
        }
    }
}
