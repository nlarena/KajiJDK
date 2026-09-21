package javax.swing.tree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;

/**
 * Draws a tree row: an icon and a text.
 *
 * <h2>One single component for every row</h2>
 *
 * <p>The tree does not have one component per row: it has <em>this</em> one, and it configures
 * it and draws it once per row. Hence almost every method that in a normal component fires a
 * repaint -- {@link #revalidate}, {@link #repaint}, {@link #invalidate},
 * {@code firePropertyChange} -- is <strong>emptied on purpose</strong>: a renderer that asks to
 * be repainted while it is being drawn would make the tree repaint in a loop.
 *
 * <p>It is the reason this class's list of methods is so long and so boring. It is not extra
 * code; it is the list of things that have to be turned off.
 *
 * <h2>Three icons, not one</h2>
 *
 * <p>Leaf, open folder and closed folder. {@link #getTreeCellRendererComponent} picks them
 * according to what the tree tells it, and not according to what the node knows about itself:
 * whether a folder is open or closed belongs to the view, not to the model.
 *
 * <h2>The colours come from the look and feel</h2>
 *
 * <p>The constructor asks {@link UIManager} for them. With no look and feel installed they stay
 * null, and then the tree is drawn with the component's colours. The six {@code setXxx} change
 * them by hand and always work.
 */
public class DefaultTreeCellRenderer extends JLabel implements TreeCellRenderer {

    /** Whether the row being drawn is chosen. */
    protected boolean selected;

    /** Whether the row being drawn has the focus. */
    protected boolean hasFocus;

    private boolean drawsFocusBorderAroundIcon;
    private boolean drawDashedFocusIndicator;

    /** A closed folder's icon. */
    protected transient Icon closedIcon;

    /** A leaf's icon. */
    protected transient Icon leafIcon;

    /** An open folder's icon. */
    protected transient Icon openIcon;

    /** The chosen text's colour. */
    protected Color textSelectionColor;

    /** The unchosen text's colour. */
    protected Color textNonSelectionColor;

    /** The chosen text's background. */
    protected Color backgroundSelectionColor;

    /** The unchosen text's background. */
    protected Color backgroundNonSelectionColor;

    /** The colour of the box around what is chosen. */
    protected Color borderSelectionColor;

    private transient JTree tree;

    /** A renderer with the look and feel's icons and colours. */
    public DefaultTreeCellRenderer() {
        initialize();
    }

    private void initialize() {
        setHorizontalAlignment(JLabel.LEFT);
        setLeafIcon(UIManager.getIcon("Tree.leafIcon"));
        setClosedIcon(UIManager.getIcon("Tree.closedIcon"));
        setOpenIcon(UIManager.getIcon("Tree.openIcon"));
        setTextSelectionColor(UIManager.getColor("Tree.selectionForeground"));
        setTextNonSelectionColor(UIManager.getColor("Tree.textForeground"));
        setBackgroundSelectionColor(UIManager.getColor("Tree.selectionBackground"));
        setBackgroundNonSelectionColor(UIManager.getColor("Tree.textBackground"));
        setBorderSelectionColor(UIManager.getColor("Tree.selectionBorderColor"));
        Object value = UIManager.get("Tree.drawsFocusBorderAroundIcon");
        drawsFocusBorderAroundIcon = (value != null && ((Boolean) value).booleanValue());
        value = UIManager.get("Tree.drawDashedFocusIndicator");
        drawDashedFocusIndicator = (value != null && ((Boolean) value).booleanValue());
    }

    /** It asks the look and feel again for the icons and colours not set by hand. */
    public void updateUI() {
        super.updateUI();
        if (closedIcon instanceof UIResource) {
            closedIcon = null;
        }
        if (openIcon instanceof UIResource) {
            openIcon = null;
        }
        if (leafIcon instanceof UIResource) {
            leafIcon = null;
        }
        initialize();
    }

    /** The open folder icon the look and feel says. */
    public Icon getDefaultOpenIcon() {
        return UIManager.getIcon("Tree.openIcon");
    }

    /** The closed folder icon the look and feel says. */
    public Icon getDefaultClosedIcon() {
        return UIManager.getIcon("Tree.closedIcon");
    }

    /** The leaf icon the look and feel says. */
    public Icon getDefaultLeafIcon() {
        return UIManager.getIcon("Tree.leafIcon");
    }

    public void setOpenIcon(Icon newIcon) {
        openIcon = newIcon;
    }

    public Icon getOpenIcon() {
        return openIcon;
    }

    public void setClosedIcon(Icon newIcon) {
        closedIcon = newIcon;
    }

    public Icon getClosedIcon() {
        return closedIcon;
    }

    public void setLeafIcon(Icon newIcon) {
        leafIcon = newIcon;
    }

    public Icon getLeafIcon() {
        return leafIcon;
    }

    public void setTextSelectionColor(Color newColor) {
        textSelectionColor = newColor;
    }

    public Color getTextSelectionColor() {
        return textSelectionColor;
    }

    public void setTextNonSelectionColor(Color newColor) {
        textNonSelectionColor = newColor;
    }

    public Color getTextNonSelectionColor() {
        return textNonSelectionColor;
    }

    public void setBackgroundSelectionColor(Color newColor) {
        backgroundSelectionColor = newColor;
    }

    public Color getBackgroundSelectionColor() {
        return backgroundSelectionColor;
    }

    public void setBackgroundNonSelectionColor(Color newColor) {
        backgroundNonSelectionColor = newColor;
    }

    public Color getBackgroundNonSelectionColor() {
        return backgroundNonSelectionColor;
    }

    public void setBorderSelectionColor(Color newColor) {
        borderSelectionColor = newColor;
    }

    public Color getBorderSelectionColor() {
        return borderSelectionColor;
    }

    /**
     * Changes the typeface.
     *
     * <p>A typeface that comes from the look and feel is discarded: see {@link #getFont}.
     */
    public void setFont(Font font) {
        if (font instanceof javax.swing.plaf.FontUIResource) {
            font = null;
        }
        super.setFont(font);
    }

    /**
     * The typeface; if it has none of its own, the tree's.
     *
     * <p>It is what makes changing the tree's typeface show in the rows without touching the
     * renderer.
     */
    public Font getFont() {
        Font font = super.getFont();
        if (font == null && tree != null) {
            font = tree.getFont();
        }
        return font;
    }

    /**
     * Changes the background.
     *
     * <p>A colour that comes from the look and feel is discarded, for the same reason as in
     * {@link #setFont}.
     */
    public void setBackground(Color color) {
        if (color instanceof javax.swing.plaf.ColorUIResource) {
            color = null;
        }
        super.setBackground(color);
    }

    /**
     * It configures itself to draw that row and returns itself.
     *
     * <p>The text comes from {@link JTree#convertValueToText}, not from {@code toString}: it is the
     * tree that decides how a node is written.
     */
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
            boolean expanded, boolean leaf, int row, boolean hasFocus) {
        String stringValue = tree.convertValueToText(value, sel, expanded, leaf, row, hasFocus);
        this.tree = tree;
        this.hasFocus = hasFocus;
        setText(stringValue);
        Color fg;
        if (sel) {
            fg = getTextSelectionColor();
        } else {
            fg = getTextNonSelectionColor();
        }
        setForeground(fg);
        Icon icon;
        if (leaf) {
            icon = getLeafIcon();
        } else if (expanded) {
            icon = getOpenIcon();
        } else {
            icon = getClosedIcon();
        }
        if (!tree.isEnabled()) {
            setEnabled(false);
            LookAndFeel laf = UIManager.getLookAndFeel();
            Icon disabledIcon = (laf == null) ? null : laf.getDisabledIcon(tree, icon);
            if (disabledIcon != null) {
                icon = disabledIcon;
            }
            setDisabledIcon(icon);
        } else {
            setEnabled(true);
            setIcon(icon);
        }
        setComponentOrientation(tree.getComponentOrientation());
        selected = sel;
        return this;
    }

    /**
     * Draws the row's background and then the text.
     *
     * <p>The background covers only what the icon and the text take up, not the whole row: in a
     * tree the selection looks like a painted label, not like a band from side to side.
     */
    public void paint(Graphics g) {
        Color bColor;
        if (selected) {
            bColor = getBackgroundSelectionColor();
        } else {
            bColor = getBackgroundNonSelectionColor();
            if (bColor == null) {
                bColor = getBackground();
            }
        }
        int imageOffset = -1;
        if (bColor != null) {
            imageOffset = getLabelStart();
            g.setColor(bColor);
            if (getComponentOrientation().isLeftToRight()) {
                g.fillRect(imageOffset, 0, getWidth() - imageOffset, getHeight());
            } else {
                g.fillRect(0, 0, getWidth() - imageOffset, getHeight());
            }
        }
        if (hasFocus) {
            if (drawsFocusBorderAroundIcon) {
                imageOffset = 0;
            } else if (imageOffset == -1) {
                imageOffset = getLabelStart();
            }
            Color bsColor = getBorderSelectionColor();
            if (bsColor != null) {
                g.setColor(bsColor);
                if (getComponentOrientation().isLeftToRight()) {
                    g.drawRect(imageOffset, 0, getWidth() - imageOffset - 1, getHeight() - 1);
                } else {
                    g.drawRect(0, 0, getWidth() - imageOffset - 1, getHeight() - 1);
                }
            }
        }
        super.paint(g);
    }

    /** Where the text starts: after the icon and its gap. */
    private int getLabelStart() {
        Icon currentI = getIcon();
        if (currentI != null && getText() != null) {
            return currentI.getIconWidth() + Math.max(0, getIconTextGap() - 1);
        }
        return 0;
    }

    /**
     * What it takes up, with three extra pixels on the right.
     *
     * <p>The three pixels are not cosmetic: without them the last letter ends up stuck to the edge
     * of the selection box.
     */
    public Dimension getPreferredSize() {
        Dimension retDimension = super.getPreferredSize();
        if (retDimension != null) {
            retDimension = new Dimension(retDimension.width + 3, retDimension.height);
        }
        return retDimension;
    }

    /** It does nothing; see the class note. */
    public void validate() {
    }

    /** It does nothing; see the class note. */
    public void invalidate() {
    }

    /** It does nothing; see the class note. */
    public void revalidate() {
    }

    /** It does nothing; see the class note. */
    public void repaint(long tm, int x, int y, int width, int height) {
    }

    /** It does nothing; see the class note. */
    public void repaint(Rectangle r) {
    }

    /** It does nothing; see the class note. */
    public void repaint() {
    }

    /**
     * It only lets through the notice that the text changed.
     *
     * <p>The rest are discarded for what the class note says. The text goes through because the
     * look and feel needs it to measure the row again, and that happens once per row, not in a
     * loop.
     *
     * <p>The JDK also lets the typeface and the colour through <em>when the text is HTML</em>,
     * because then the HTML view has to be rebuilt. That branch asks for {@code BasicHTML}, which
     * this library does not ship; without it the text is still text and the branch would do
     * nothing.
     */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (propertyName == "text") {
            super.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /** It does nothing; see the class note. */
    public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {
    }

    /** It does nothing; see the class note. */
    public void firePropertyChange(String propertyName, char oldValue, char newValue) {
    }

    /** It does nothing; see the class note. */
    public void firePropertyChange(String propertyName, short oldValue, short newValue) {
    }

    /** It does nothing; see the class note. */
    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
    }

    /** It does nothing; see the class note. */
    public void firePropertyChange(String propertyName, long oldValue, long newValue) {
    }

    /** It does nothing; see the class note. */
    public void firePropertyChange(String propertyName, float oldValue, float newValue) {
    }

    /** It does nothing; see the class note. */
    public void firePropertyChange(String propertyName, double oldValue, double newValue) {
    }

    /** It does nothing; see the class note. */
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
    }
}
