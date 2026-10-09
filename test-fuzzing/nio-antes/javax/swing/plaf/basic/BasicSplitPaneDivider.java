package javax.swing.plaf.basic;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JSplitPane;
import javax.swing.border.Border;

/**
 * The bar that separates a {@link JSplitPane}'s two halves.
 *
 * <h2>It is a container, not a painted component</h2>
 *
 * <p>It inherits from {@link Container} because it may have children: the two "one touch"
 * little buttons that fold the pane to one side in one go. With that option not switched on it
 * has none, and there it is just a rectangle with a border.
 *
 * <h2>Dragging in two forms</h2>
 *
 * <p>Dragging the divider may lay the two children out on every movement -- "continuous" --
 * or draw a shadow and lay them out only on releasing. The second exists because laying out two
 * trees of components sixty times a second is expensive; the first because it looks much
 * better. Who decides is {@code JSplitPane.setContinuousLayout}, and {@link DragController}
 * implements both: it moves the shadow or it moves the divider, and tells the look and feel.
 *
 * <h2>The border is set and not changed</h2>
 *
 * <p>{@link #setBorder} accepts any, but the one that matters is the one the look and feel
 * sets: it draws the line on each side, and the insets that give the divider a pixel of air
 * come from it. Measured: insets (0, 1, 0, 1) horizontally.
 *
 * <h2>Size</h2>
 *
 * <p>The thickness is {@link #getDividerSize} and the length is zero: it is stretched by the
 * pane's layout. A horizontal divider measures 10 x 1 and a vertical one 1 x 10.
 */
public class BasicSplitPaneDivider extends Container implements PropertyChangeListener {

    /** How much the one-touch little buttons measure. */
    protected static final int ONE_TOUCH_SIZE = 6;

    /** How far they shift from the edge. */
    protected static final int ONE_TOUCH_OFFSET = 2;

    protected DragController dragger;
    protected BasicSplitPaneUI splitPaneUI;
    protected int dividerSize = 0;
    protected Component hiddenDivider;
    protected JSplitPane splitPane;
    protected MouseHandler mouseHandler;
    protected int orientation;
    protected JButton leftButton;
    protected JButton rightButton;

    private Border border;
    private boolean mouseOver;

    /** For that look and feel; it keeps its pane and its orientation. */
    public BasicSplitPaneDivider(BasicSplitPaneUI ui) {
        setLayout(null);
        setBasicSplitPaneUI(ui);
        orientation = splitPane.getOrientation();
        setCursor((orientation == JSplitPane.HORIZONTAL_SPLIT)
                ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.E_RESIZE_CURSOR)
                : java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.S_RESIZE_CURSOR));
        setBackground(ui.getSplitPane().getBackground());
    }

    /**
     * It changes look and feel; it stops listening to the previous pane and starts with the new
     * one.
     */
    public void setBasicSplitPaneUI(BasicSplitPaneUI newUI) {
        if (splitPane != null) {
            splitPane.removePropertyChangeListener(this);
            if (mouseHandler != null) {
                splitPane.removeMouseListener(mouseHandler);
                splitPane.removeMouseMotionListener(mouseHandler);
                removeMouseListener(mouseHandler);
                removeMouseMotionListener(mouseHandler);
                mouseHandler = null;
            }
        }
        splitPaneUI = newUI;
        if (newUI != null) {
            splitPane = newUI.getSplitPane();
            if (splitPane != null) {
                if (mouseHandler == null) {
                    mouseHandler = new MouseHandler(this);
                }
                splitPane.addMouseListener(mouseHandler);
                splitPane.addMouseMotionListener(mouseHandler);
                addMouseListener(mouseHandler);
                addMouseMotionListener(mouseHandler);
                splitPane.addPropertyChangeListener(this);
                if (splitPane.isOneTouchExpandable()) {
                    oneTouchExpandableChanged();
                }
            }
        } else {
            splitPane = null;
        }
    }

    public BasicSplitPaneUI getBasicSplitPaneUI() {
        return splitPaneUI;
    }

    /** The thickness; less than zero is taken as zero. */
    public void setDividerSize(int newSize) {
        dividerSize = newSize;
    }

    public int getDividerSize() {
        return dividerSize;
    }

    public void setBorder(Border border) {
        Border oldBorder = this.border;
        this.border = border;
        firePropertyChange("border", oldBorder, border);
    }

    public Border getBorder() {
        return border;
    }

    /** The border's, or zero if there is no border. */
    public Insets getInsets() {
        Border b = getBorder();
        if (b != null) {
            return b.getBorderInsets(this);
        }
        return super.getInsets();
    }

    /** Whether the mouse is over it; a look and feel may draw it differently. */
    protected void setMouseOver(boolean mouseOver) {
        this.mouseOver = mouseOver;
    }

    public boolean isMouseOver() {
        return mouseOver;
    }

    /** The thickness across and nothing lengthwise; see the class note. */
    public Dimension getPreferredSize() {
        if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
            return new Dimension(getDividerSize(), 1);
        }
        return new Dimension(1, getDividerSize());
    }

    /** The same as the preferred one: the divider does not shrink. */
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (leftButton != null) {
            leftButton.setEnabled(enabled);
        }
        if (rightButton != null) {
            rightButton.setEnabled(enabled);
        }
        setCursor(enabled
                ? ((orientation == JSplitPane.HORIZONTAL_SPLIT)
                        ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.E_RESIZE_CURSOR)
                        : java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.S_RESIZE_CURSOR))
                : java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /** It reacts to the orientation, to the size and to the one-touch option. */
    public void propertyChange(PropertyChangeEvent e) {
        if (e.getSource() == splitPane) {
            String name = e.getPropertyName();
            if (JSplitPane.ORIENTATION_PROPERTY.equals(name)) {
                orientation = splitPane.getOrientation();
                setCursor((orientation == JSplitPane.HORIZONTAL_SPLIT)
                        ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.E_RESIZE_CURSOR)
                        : java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.S_RESIZE_CURSOR));
                invalidate();
                validate();
            } else if (JSplitPane.ONE_TOUCH_EXPANDABLE_PROPERTY.equals(name)) {
                oneTouchExpandableChanged();
            }
        }
    }

    /** The background, the border, and the little buttons if they are there. */
    public void paint(Graphics g) {
        super.paint(g);
        Border b = getBorder();
        if (b != null) {
            Dimension size = getSize();
            b.paintBorder(this, g, 0, 0, size.width, size.height);
        }
    }

    /**
     * It puts the two one-touch little buttons in the first time they are needed.
     *
     * <p>And it never takes them out: switching the option off does not remove them from the
     * divider. It looks like an oversight and it is measured -- with the option off the divider
     * goes on having two children --, and it has its logic: switching it on again costs nothing,
     * and the buttons are not drawn if the layout gives them no room.
     */
    protected void oneTouchExpandableChanged() {
        if (splitPane.isOneTouchExpandable() && leftButton == null && rightButton == null) {
            leftButton = createLeftOneTouchButton();
            rightButton = createRightOneTouchButton();
            if (leftButton != null && rightButton != null) {
                add(leftButton);
                add(rightButton);
            }
        }
        invalidate();
        validate();
    }

    /** The little button that folds upwards or to the left. */
    protected JButton createLeftOneTouchButton() {
        return new OneTouchButton(this, true);
    }

    /** And the one that folds the other way. */
    protected JButton createRightOneTouchButton() {
        return new OneTouchButton(this, false);
    }

    /** It tells the look and feel that the drag begins. */
    protected void prepareForDragging() {
        splitPaneUI.startDragging();
    }

    /** And that it is going along. */
    protected void dragDividerTo(int location) {
        splitPaneUI.dragDividerTo(location);
    }

    /** And that it ended. */
    protected void finishDraggingTo(int location) {
        splitPaneUI.finishDraggingTo(location);
    }

    /**
     * The one that follows a horizontal drag.
     *
     * <p>It keeps how much there is between where it was pressed and where the divider begins, so
     * that the divider does not jump on being grabbed by the middle. And it keeps the caps: how
     * far it can be taken without shrinking a child below its minimum.
     *
     * <p>It is static and takes the divider as the first parameter. That is the signature the JDK
     * generates for an inner class, and it is the only one that compiles here; see finding #518.
     */
    protected static class DragController {

        final BasicSplitPaneDivider divisor;
        int initialX;
        int maxX;
        int minX;
        int offset;

        protected DragController(BasicSplitPaneDivider divisor, MouseEvent e) {
            this.divisor = divisor;
            JSplitPane splitPane = divisor.getBasicSplitPaneUI().getSplitPane();
            Component leftC = splitPane.getLeftComponent();
            Component rightC = splitPane.getRightComponent();
            initialX = divisor.getLocation().x;
            offset = e.getX();
            if (leftC == null || rightC == null || !leftC.isVisible() || !rightC.isVisible()) {
                maxX = -1;
                return;
            }
            Insets insets = splitPane.getInsets();
            minX = (insets != null) ? insets.left : 0;
            minX += leftC.getMinimumSize().width;
            maxX = splitPane.getWidth() - ((insets != null) ? insets.right : 0)
                    - rightC.getMinimumSize().width - divisor.getDividerSize();
            if (maxX < minX) {
                minX = 0;
                maxX = splitPane.getWidth() - divisor.getDividerSize();
            }
        }

        /** Whether the drag makes sense; if not, it is ignored. */
        protected boolean isValid() {
            return maxX > 0;
        }

        /** Which position that event corresponds to. */
        protected int positionForMouseEvent(MouseEvent e) {
            int newX = (e.getSource() == divisor) ? (e.getX() + divisor.getLocation().x) : e.getX();
            return Math.min(maxX, Math.max(minX, newX - offset));
        }

        /** The same with loose coordinates. */
        protected int getNeededLocation(int x, int y) {
            return Math.min(maxX, Math.max(minX, x - offset));
        }

        protected void continueDrag(int newX, int newY) {
            divisor.dragDividerTo(getNeededLocation(newX, newY));
        }

        protected void continueDrag(MouseEvent e) {
            divisor.dragDividerTo(positionForMouseEvent(e));
        }

        protected void completeDrag(int x, int y) {
            divisor.finishDraggingTo(getNeededLocation(x, y));
        }

        protected void completeDrag(MouseEvent e) {
            divisor.finishDraggingTo(positionForMouseEvent(e));
        }
    }

    /** The same for the other axis; see {@link DragController}. */
    protected static class VerticalDragController extends DragController {

        protected VerticalDragController(BasicSplitPaneDivider divisor, MouseEvent e) {
            super(divisor, e);
            JSplitPane splitPane = divisor.getBasicSplitPaneUI().getSplitPane();
            Component leftC = splitPane.getLeftComponent();
            Component rightC = splitPane.getRightComponent();
            initialX = divisor.getLocation().y;
            offset = e.getY();
            if (leftC == null || rightC == null || !leftC.isVisible() || !rightC.isVisible()) {
                maxX = -1;
                return;
            }
            Insets insets = splitPane.getInsets();
            minX = (insets != null) ? insets.top : 0;
            minX += leftC.getMinimumSize().height;
            maxX = splitPane.getHeight() - ((insets != null) ? insets.bottom : 0)
                    - rightC.getMinimumSize().height - divisor.getDividerSize();
            if (maxX < minX) {
                minX = 0;
                maxX = splitPane.getHeight() - divisor.getDividerSize();
            }
        }

        protected int getNeededLocation(int x, int y) {
            return Math.min(maxX, Math.max(minX, y - offset));
        }

        protected int positionForMouseEvent(MouseEvent e) {
            int newY = (e.getSource() == divisor) ? (e.getY() + divisor.getLocation().y) : e.getY();
            return Math.min(maxX, Math.max(minX, newY - offset));
        }
    }

    /** The one that translates the mouse events into dragging; see the class note. */
    protected static class MouseHandler extends MouseAdapter implements MouseMotionListener {

        private final BasicSplitPaneDivider divisor;

        protected MouseHandler(BasicSplitPaneDivider divisor) {
            this.divisor = divisor;
        }

        public void mousePressed(MouseEvent e) {
            JSplitPane splitPane = divisor.splitPane;
            if ((e.getSource() != divisor && e.getSource() != splitPane)
                    || divisor.dragger != null || splitPane == null || !splitPane.isEnabled()) {
                return;
            }
            if (splitPane.getLeftComponent() == null || splitPane.getRightComponent() == null) {
                return;
            }
            DragController d = (divisor.orientation == JSplitPane.HORIZONTAL_SPLIT)
                    ? new DragController(divisor, e)
                    : new VerticalDragController(divisor, e);
            if (!d.isValid()) {
                return;
            }
            divisor.dragger = d;
            divisor.prepareForDragging();
            d.continueDrag(e);
        }

        public void mouseReleased(MouseEvent e) {
            DragController d = divisor.dragger;
            if (d == null) {
                return;
            }
            if (e.getSource() == divisor.splitPane) {
                d.completeDrag(e.getX(), e.getY());
            } else if (e.getSource() == divisor) {
                java.awt.Point ourLoc = divisor.getLocation();
                d.completeDrag(e.getX() + ourLoc.x, e.getY() + ourLoc.y);
            }
            divisor.dragger = null;
        }

        public void mouseDragged(MouseEvent e) {
            DragController d = divisor.dragger;
            if (d == null) {
                return;
            }
            if (e.getSource() == divisor.splitPane) {
                d.continueDrag(e.getX(), e.getY());
            } else if (e.getSource() == divisor) {
                java.awt.Point ourLoc = divisor.getLocation();
                d.continueDrag(e.getX() + ourLoc.x, e.getY() + ourLoc.y);
            }
        }

        public void mouseMoved(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
            if (e.getSource() == divisor) {
                divisor.setMouseOver(true);
            }
        }

        public void mouseExited(MouseEvent e) {
            if (e.getSource() == divisor) {
                divisor.setMouseOver(false);
            }
        }
    }

    /**
     * A one-touch little button.
     *
     * <p>It paints neither border nor background: the only thing seen is the little arrow, and it
     * is drawn by the look and feel. The basic one draws none, and it is said: without it the
     * button is an invisible little square that works all the same.
     */
    private static class OneTouchButton extends JButton {

        private final BasicSplitPaneDivider divisor;
        private final boolean towardsStart;

        OneTouchButton(BasicSplitPaneDivider divisor, boolean towardsStart) {
            this.divisor = divisor;
            this.towardsStart = towardsStart;
            setMinimumSize(new Dimension(ONE_TOUCH_SIZE, ONE_TOUCH_SIZE));
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
            setFocusPainted(false);
            setBorderPainted(false);
            setRequestFocusEnabled(false);
            addActionListener(new OneTouchListener(this));
        }

        void collapse() {
            JSplitPane splitPane = divisor.splitPane;
            Insets insets = splitPane.getInsets();
            if (towardsStart) {
                int border = 0;
                if (insets != null) {
                    border = (divisor.orientation == JSplitPane.HORIZONTAL_SPLIT)
                            ? insets.left : insets.top;
                }
                splitPane.setDividerLocation(border);
            } else {
                splitPane.setDividerLocation(
                        divisor.getBasicSplitPaneUI().getMaximumDividerLocation(splitPane));
            }
        }

        public boolean isFocusTraversable() {
            return false;
        }

        public void setBorder(Border b) {
            // No border: see the class note.
        }
    }

    /** The little button's firing; separate for the same reason as the other nested ones. */
    private static class OneTouchListener implements java.awt.event.ActionListener {

        private final OneTouchButton button;

        OneTouchListener(OneTouchButton button) {
            this.button = button;
        }

        public void actionPerformed(java.awt.event.ActionEvent e) {
            button.collapse();
        }
    }
}
