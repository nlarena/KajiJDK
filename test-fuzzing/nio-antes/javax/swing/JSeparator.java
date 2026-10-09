package javax.swing;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.SeparatorUI;

/**
 * A line that separates groups of things.
 *
 * <h2>A component for a line</h2>
 *
 * <p>It could be a border or two lines drawn by hand. It is a component because that way the
 * layout places it along with what it separates: in a tool bar that is reordered, the line moves
 * with the buttons without anybody recomputing anything.
 *
 * <p>Besides, the look and feel draws it as the system requires -- one line, two, a gap --
 * without whoever put it there having to know which.
 */
public class JSeparator extends JComponent implements SwingConstants, Accessible {

    private static final String uiClassID = "SeparatorUI";

    private int orientation = HORIZONTAL;
    private AccessibleContext accessibleContext;

    /** A horizontal line. */
    public JSeparator() {
        this(HORIZONTAL);
    }

    /** A line with that orientation. */
    public JSeparator(int orientation) {
        checkOrientation(orientation);
        this.orientation = orientation;
        setFocusable(false);
        updateUI();
    }

    public SeparatorUI getUI() {
        return (SeparatorUI) ui;
    }

    public void setUI(SeparatorUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    public int getOrientation() {
        return this.orientation;
    }

    /**
     * Whether the line goes lying down or standing up.
     *
     * @throws IllegalArgumentException if it is not one of the two.
     */
    public void setOrientation(int orientation) {
        if (this.orientation == orientation) {
            return;
        }
        int oldValue = this.orientation;
        checkOrientation(orientation);
        this.orientation = orientation;
        firePropertyChange("orientation", oldValue, orientation);
        revalidate();
        repaint();
    }

    private static void checkOrientation(int orientation) {
        if (orientation != VERTICAL && orientation != HORIZONTAL) {
            throw new IllegalArgumentException("orientation must be one of: VERTICAL, HORIZONTAL");
        }
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
