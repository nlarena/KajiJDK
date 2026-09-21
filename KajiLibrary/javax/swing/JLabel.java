package javax.swing;

import java.awt.Component;

import javax.swing.plaf.LabelUI;
import javax.swing.plaf.basic.BasicLabelUI;

/**
 * A text, an icon, or both: the simplest component that shows something.
 *
 * <h2>Six knobs for one thing</h2>
 *
 * <p>A label has two alignments and two positions, and the difference is the one that confuses:
 * the <em>alignment</em> says where the icon-text set goes within the label's area; the text's
 * <em>position</em> says where the text goes <em>with respect to the icon</em>. With the four
 * plus the gap ({@link #setIconTextGap}) any arrangement is described, and who resolves it is
 * {@code SwingUtilities.layoutCompoundLabel}, the same one for buttons and cells.
 *
 * <p>By default: aligned to the start ({@code LEADING}), centred vertically, and the text after
 * the icon ({@code TRAILING}) -- that is, icon on the left and text on the right in a language
 * that is read that way.
 *
 * <h2>What it does about {@code UIManager}</h2>
 *
 * <p>{@link #updateUI} installs {@link BasicLabelUI} directly. In the JDK it asks the
 * {@code UIManager}, which may answer with Metal's look and feel, Windows' or whichever is
 * installed; here there is a single look and feel, and asking would be a ceremony with a single
 * answer.
 *
 * <h2>What is not there</h2>
 *
 * <p>Three things, and none is an oversight. {@code getDisabledIcon} does not make a grey
 * version of the icon when one was not fixed: that asks for an image to filter, and an
 * {@link Icon} does not promise to have one. {@code imageUpdate} is not there because its only
 * reason is to repaint when an asynchronous image finishes loading, and here images are not
 * loaded asynchronously. And accessibility, which is a subsystem of its own.
 */
public class JLabel extends JComponent implements SwingConstants {

    private static final long serialVersionUID = 5296049245363908046L;

    /** The client property a component knows which label names it with. */
    static final String LABELED_BY_PROPERTY = "labeledBy";

    /** The component this label describes, or {@code null}. */
    protected Component labelFor;

    private int mnemonic = '\0';
    private int mnemonicIndex = -1;
    private String text = "";
    private Icon defaultIcon;
    private Icon disabledIcon;
    private boolean disabledIconSet;
    private int verticalAlignment = CENTER;
    private int horizontalAlignment = LEADING;
    private int verticalTextPosition = CENTER;
    private int horizontalTextPosition = TRAILING;
    private int iconTextGap = 4;

    /** With text, icon and horizontal alignment. */
    public JLabel(String text, Icon icon, int horizontalAlignment) {
        setText(text);
        setIcon(icon);
        setHorizontalAlignment(horizontalAlignment);
        updateUI();
        setAlignmentX(LEFT_ALIGNMENT);
    }

    /** With text and horizontal alignment. */
    public JLabel(String text, int horizontalAlignment) {
        this(text, null, horizontalAlignment);
    }

    /** With text, aligned to the start. */
    public JLabel(String text) {
        this(text, null, LEADING);
    }

    /** With icon and horizontal alignment. */
    public JLabel(Icon image, int horizontalAlignment) {
        this(null, image, horizontalAlignment);
    }

    /** With icon, centred. */
    public JLabel(Icon image) {
        this(null, image, CENTER);
    }

    /** Empty. */
    public JLabel() {
        this("", null, LEADING);
    }

    /** The installed look and feel. */
    public LabelUI getUI() {
        return (LabelUI) this.ui;
    }

    /** It installs a label look and feel. */
    public void setUI(LabelUI ui) {
        super.setUI(ui);
    }

    /** It installs the basic look and feel; see the class note about {@code UIManager}. */
    public void updateUI() {
        setUI((LabelUI) BasicLabelUI.createUI(this));
    }

    public String getUIClassID() {
        return "LabelUI";
    }

    /** The text, or {@code null}. */
    public String getText() {
        return this.text;
    }

    /**
     * It changes the text.
     *
     * <p>If there was a mnemonic, it is looked up again in the new text: the underlined letter has
     * to go on being the mnemonic's, not the one that was at that position.
     */
    public void setText(String text) {
        String old = this.text;
        this.text = text;
        firePropertyChange("text", old, text);
        setDisplayedMnemonicIndex(SwingUtilities.findDisplayedMnemonicIndex(text, getDisplayedMnemonic()));
        if (text == null || old == null || !text.equals(old)) {
            revalidate();
            repaint();
        }
    }

    /** The icon, or {@code null}. */
    public Icon getIcon() {
        return this.defaultIcon;
    }

    /** It changes the icon; it asks for a relayout only if it changed size. */
    public void setIcon(Icon icon) {
        Icon old = this.defaultIcon;
        this.defaultIcon = icon;
        firePropertyChange("icon", old, icon);
        if (old != icon) {
            if (old == null || icon == null
                    || old.getIconWidth() != icon.getIconWidth()
                    || old.getIconHeight() != icon.getIconHeight()) {
                revalidate();
            }
            repaint();
        }
    }

    /** The icon for when it is disabled, or {@code null}; see the class note. */
    public Icon getDisabledIcon() {
        return this.disabledIcon;
    }

    /** It fixes the icon for when it is disabled. */
    public void setDisabledIcon(Icon disabledIcon) {
        Icon old = this.disabledIcon;
        this.disabledIcon = disabledIcon;
        this.disabledIconSet = disabledIcon != null;
        firePropertyChange("disabledIcon", old, disabledIcon);
        if (disabledIcon != old) {
            if (disabledIcon == null || old == null
                    || disabledIcon.getIconWidth() != old.getIconWidth()
                    || disabledIcon.getIconHeight() != old.getIconHeight()) {
                revalidate();
            }
            if (!isEnabled()) {
                repaint();
            }
        }
    }

    /**
     * It fixes the mnemonic as a key code, and looks up which letter to underline.
     *
     * <p>It is an {@code int} and not a {@code char} because it is a {@code KeyEvent} code, which
     * does not always correspond to a character.
     */
    public void setDisplayedMnemonic(int key) {
        int old = this.mnemonic;
        this.mnemonic = key;
        firePropertyChange("displayedMnemonic", old, this.mnemonic);
        setDisplayedMnemonicIndex(SwingUtilities.findDisplayedMnemonicIndex(getText(), this.mnemonic));
        if (key != old) {
            revalidate();
            repaint();
        }
    }

    /** It fixes the mnemonic as a character; it is kept in upper case, which is the key code. */
    public void setDisplayedMnemonic(char aChar) {
        setDisplayedMnemonic((int) Character.toUpperCase(aChar));
    }

    /** The mnemonic, as a key code; {@code 0} if there is none. */
    public int getDisplayedMnemonic() {
        return this.mnemonic;
    }

    /**
     * It fixes which character is underlined, by position.
     *
     * <p>For when the automatic search chooses wrongly: in {@code "Save As"} with mnemonic
     * {@code A}, the first {@code A} is the one in {@code As}, and perhaps the one in {@code Save}
     * was wanted.
     *
     * @throws IllegalArgumentException if the position does not fall in the text, save {@code -1}
     */
    public void setDisplayedMnemonicIndex(int index) {
        int old = this.mnemonicIndex;
        if (index == -1) {
            this.mnemonicIndex = -1;
        } else {
            String t = getText();
            int length = t == null ? 0 : t.length();
            if (index < -1 || index >= length) {
                throw new IllegalArgumentException("index == " + String.valueOf(index));
            }
            this.mnemonicIndex = index;
        }
        firePropertyChange("displayedMnemonicIndex", old, index);
        if (index != old) {
            revalidate();
            repaint();
        }
    }

    /** The position of the underlined character, or {@code -1}. */
    public int getDisplayedMnemonicIndex() {
        return this.mnemonicIndex;
    }

    /**
     * It validates a horizontal key.
     *
     * @throws IllegalArgumentException with the given message if it is not one of the five valid
     *     ones
     */
    protected int checkHorizontalKey(int key, String message) {
        if (key == LEFT || key == CENTER || key == RIGHT || key == LEADING || key == TRAILING) {
            return key;
        }
        throw new IllegalArgumentException(message);
    }

    /**
     * It validates a vertical key.
     *
     * @throws IllegalArgumentException with the given message if it is not one of the three valid
     *     ones
     */
    protected int checkVerticalKey(int key, String message) {
        if (key == TOP || key == CENTER || key == BOTTOM) {
            return key;
        }
        throw new IllegalArgumentException(message);
    }

    /** The pixels between the icon and the text. */
    public int getIconTextGap() {
        return this.iconTextGap;
    }

    /** It changes the gap between icon and text. */
    public void setIconTextGap(int iconTextGap) {
        int old = this.iconTextGap;
        this.iconTextGap = iconTextGap;
        firePropertyChange("iconTextGap", old, iconTextGap);
        if (iconTextGap != old) {
            revalidate();
            repaint();
        }
    }

    /** Where the set goes vertically: {@code TOP}, {@code CENTER} or {@code BOTTOM}. */
    public int getVerticalAlignment() {
        return this.verticalAlignment;
    }

    public void setVerticalAlignment(int alignment) {
        if (alignment == this.verticalAlignment) {
            return;
        }
        int old = this.verticalAlignment;
        this.verticalAlignment = checkVerticalKey(alignment, "verticalAlignment");
        firePropertyChange("verticalAlignment", old, this.verticalAlignment);
        repaint();
    }

    /** Where the set goes horizontally. */
    public int getHorizontalAlignment() {
        return this.horizontalAlignment;
    }

    public void setHorizontalAlignment(int alignment) {
        if (alignment == this.horizontalAlignment) {
            return;
        }
        int old = this.horizontalAlignment;
        this.horizontalAlignment = checkHorizontalKey(alignment, "horizontalAlignment");
        firePropertyChange("horizontalAlignment", old, this.horizontalAlignment);
        repaint();
    }

    /** Where the text goes with respect to the icon, vertically. */
    public int getVerticalTextPosition() {
        return this.verticalTextPosition;
    }

    public void setVerticalTextPosition(int textPosition) {
        if (textPosition == this.verticalTextPosition) {
            return;
        }
        int old = this.verticalTextPosition;
        this.verticalTextPosition = checkVerticalKey(textPosition, "verticalTextPosition");
        firePropertyChange("verticalTextPosition", old, this.verticalTextPosition);
        revalidate();
        repaint();
    }

    /** Where the text goes with respect to the icon, horizontally. */
    public int getHorizontalTextPosition() {
        return this.horizontalTextPosition;
    }

    public void setHorizontalTextPosition(int textPosition) {
        int old = this.horizontalTextPosition;
        this.horizontalTextPosition = checkHorizontalKey(textPosition, "horizontalTextPosition");
        firePropertyChange("horizontalTextPosition", old, this.horizontalTextPosition);
        revalidate();
        repaint();
    }

    protected String paramString() {
        String textStr = this.text != null ? this.text : "";
        String iconStr = this.defaultIcon != null && this.defaultIcon != this
                ? this.defaultIcon.toString() : "";
        String disabledIconStr = this.disabledIcon != null && this.disabledIcon != this
                ? this.disabledIcon.toString() : "";
        String labelForStr = this.labelFor != null ? this.labelFor.toString() : "";
        return super.paramString()
                + ",defaultIcon=" + iconStr
                + ",disabledIcon=" + disabledIconStr
                + ",horizontalAlignment=" + horizontalName(this.horizontalAlignment)
                + ",horizontalTextPosition=" + horizontalName(this.horizontalTextPosition)
                + ",iconTextGap=" + String.valueOf(this.iconTextGap)
                + ",labelFor=" + labelForStr
                + ",text=" + textStr
                + ",verticalAlignment=" + verticalName(this.verticalAlignment)
                + ",verticalTextPosition=" + verticalName(this.verticalTextPosition);
    }

    private static String horizontalName(int k) {
        if (k == LEFT) {
            return "LEFT";
        }
        if (k == CENTER) {
            return "CENTER";
        }
        if (k == RIGHT) {
            return "RIGHT";
        }
        if (k == LEADING) {
            return "LEADING";
        }
        if (k == TRAILING) {
            return "TRAILING";
        }
        return "";
    }

    private static String verticalName(int k) {
        if (k == TOP) {
            return "TOP";
        }
        if (k == CENTER) {
            return "CENTER";
        }
        if (k == BOTTOM) {
            return "BOTTOM";
        }
        return "";
    }

    /** The component this label describes, or {@code null}. */
    public Component getLabelFor() {
        return this.labelFor;
    }

    /**
     * It says which component this label describes.
     *
     * <p>The component learns about it through the client property {@code labeledBy}: it is how a
     * screen reader, or the mnemonic, finds the field from its label.
     */
    public void setLabelFor(Component c) {
        Component old = this.labelFor;
        this.labelFor = c;
        firePropertyChange("labelFor", old, c);
        if (old instanceof JComponent) {
            ((JComponent) old).putClientProperty(LABELED_BY_PROPERTY, null);
        }
        if (c instanceof JComponent) {
            ((JComponent) c).putClientProperty(LABELED_BY_PROPERTY, this);
        }
    }
}
