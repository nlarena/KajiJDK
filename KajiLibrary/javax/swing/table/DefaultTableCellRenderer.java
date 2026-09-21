package javax.swing.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * Draws a table cell as a label.
 *
 * <h2>One single component for every cell</h2>
 *
 * <p>The same as in the tree: the table does not have one component per cell, it has
 * <em>this</em> one, and it configures it and draws it once per cell. Hence {@link #revalidate},
 * {@link #repaint}, {@link #invalidate} and almost every {@code firePropertyChange} are
 * <strong>emptied on purpose</strong>: a renderer that asks to be repainted while it is being
 * drawn would leave the table in a loop.
 *
 * <h2>The colours are not kept, they take turns</h2>
 *
 * <p>Each cell asks the table for its colour -- selection's or normal -- and the renderer applies
 * it. But if somebody set a colour on the renderer <em>by hand</em>, that one wins: that is why
 * the colours that come from the look and feel are marked and told apart from those the program
 * set.
 *
 * <h2>The cell with the focus carries a border</h2>
 *
 * <p>And the others carry an empty border of the same size, not none. Otherwise the focused cell
 * would measure differently from the others and the text would jump a pixel as the focus moved.
 */
public class DefaultTableCellRenderer extends JLabel implements TableCellRenderer,
        java.io.Serializable {

    /** The border without focus: empty, but the same size as the other. See the class note. */
    protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    private static final Border NO_FOCUS = new EmptyBorder(1, 1, 1, 1);

    private Color unselectedForeground;
    private Color unselectedBackground;

    /** A renderer aligned to the left and opaque. */
    public DefaultTableCellRenderer() {
        super();
        setOpaque(true);
        setBorder(getNoFocusBorder());
        setName("Table.cellRenderer");
    }

    private Border getNoFocusBorder() {
        Border border = UIManager.getBorder("Table.cellNoFocusBorder");
        if (border != null) {
            return border;
        }
        return NO_FOCUS;
    }

    /**
     * The text's colour when the cell is not chosen.
     *
     * <p>Null gives the decision back to the table; see the class note.
     */
    public void setForeground(Color c) {
        super.setForeground(c);
        unselectedForeground = c;
    }

    /** The background when the cell is not chosen; null gives it back to the table. */
    public void setBackground(Color c) {
        super.setBackground(c);
        unselectedBackground = c;
    }

    /** It asks the look and feel for the colours again and forgets those set by hand. */
    public void updateUI() {
        super.updateUI();
        setForeground(null);
        setBackground(null);
    }

    /**
     * It configures itself to draw that cell and returns itself.
     *
     * <p>The text comes from {@link #setValue}, which a subclass can change to format numbers or
     * dates without touching anything else.
     */
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        if (table == null) {
            return this;
        }
        if (isSelected) {
            super.setForeground(table.getSelectionForeground());
            super.setBackground(table.getSelectionBackground());
        } else {
            Color background = (unselectedBackground != null) ? unselectedBackground
                    : table.getBackground();
            super.setForeground((unselectedForeground != null) ? unselectedForeground
                    : table.getForeground());
            super.setBackground(background);
        }
        setFont(table.getFont());
        if (hasFocus) {
            Border border = UIManager.getBorder("Table.focusCellHighlightBorder");
            if (border == null) {
                border = NO_FOCUS;
            }
            setBorder(border);
            if (!isSelected && table.isCellEditable(row, column)) {
                Color col = UIManager.getColor("Table.focusCellForeground");
                if (col != null) {
                    super.setForeground(col);
                }
                col = UIManager.getColor("Table.focusCellBackground");
                if (col != null) {
                    super.setBackground(col);
                }
            }
        } else {
            setBorder(getNoFocusBorder());
        }
        setValue(value);
        return this;
    }

    /**
     * Puts the value in as text.
     *
     * <p>It is this class's extension point: a subclass that wants to show an amount with two
     * decimals changes this and nothing else.
     */
    protected void setValue(Object value) {
        setText((value == null) ? "" : value.toString());
    }

    /** It does nothing; see the class note. */
    public void invalidate() {
    }

    /** It does nothing; see the class note. */
    public void validate() {
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
     * <p>The JDK also lets through the typeface and the colour when the text is HTML; that branch
     * asks for {@code BasicHTML}, which this library does not ship.
     */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (propertyName == "text") {
            super.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /** It does nothing; see the class note. */
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
    }

    /**
     * A renderer that is also a look and feel resource.
     *
     * <p>Marking it like that is how the look and feel says "I set this one": on changing look
     * and feel it is replaced, and one set by the program is kept.
     */
    public static class UIResource extends DefaultTableCellRenderer
            implements javax.swing.plaf.UIResource {

        /** A base renderer marked as the look and feel's. */
        public UIResource() {
            super();
        }
    }
}
