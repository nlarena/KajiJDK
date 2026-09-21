package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.io.Serializable;

import javax.swing.border.Border;
import javax.swing.plaf.UIResource;

/**
 * A {@link JScrollPane}'s layout: nine places, and the decision of whether a bar is needed.
 *
 * <h2>Nine places</h2>
 *
 * <p>The viewport in the centre, two bars, two headers -- one at the top and another on the
 * left -- and four corners. Each piece is added with one of {@link ScrollPaneConstants}'
 * constants as a constraint, just as in a {@code BorderLayout}, and this class keeps a direct
 * reference to each one: they are few and fixed, and looking them up by name on each layout
 * would be extra work.
 *
 * <h2>Why the decision is circular</h2>
 *
 * <p>With the "as needed" policy, knowing whether a bar is needed depends on how much space is
 * left, and how much space is left depends on whether there are bars: putting the horizontal
 * one in eats height and may make the vertical one needed after all. That is why the method
 * decides in passes -- vertical, horizontal, and the vertical one again -- and afterwards, if
 * the content is {@link Scrollable}, it asks it again with the size already fixed, because a
 * content that follows the viewport's width may change its mind when the viewport changes.
 *
 * <p>The JDK stops there and does not iterate until it settles: a malicious content might never
 * settle. Two passes are enough for everything reasonable.
 */
public class ScrollPaneLayout implements LayoutManager, ScrollPaneConstants, Serializable {

    /** The viewport; the centre of everything. */
    protected JViewport viewport;

    protected JScrollBar vsb;

    protected JScrollBar hsb;

    /** The row header: it scrolls with the viewport, but only vertically. */
    protected JViewport rowHead;

    /** The column header: it scrolls with the viewport, but only horizontally. */
    protected JViewport colHead;

    protected Component lowerLeft;
    protected Component lowerRight;
    protected Component upperLeft;
    protected Component upperRight;

    protected int vsbPolicy = VERTICAL_SCROLLBAR_AS_NEEDED;

    protected int hsbPolicy = HORIZONTAL_SCROLLBAR_AS_NEEDED;

    public ScrollPaneLayout() {
    }

    /** It takes the nine pieces and the two policies from the pane. */
    public void syncWithScrollPane(JScrollPane sp) {
        viewport = sp.getViewport();
        vsb = sp.getVerticalScrollBar();
        hsb = sp.getHorizontalScrollBar();
        rowHead = sp.getRowHeader();
        colHead = sp.getColumnHeader();
        lowerLeft = sp.getCorner(LOWER_LEFT_CORNER);
        lowerRight = sp.getCorner(LOWER_RIGHT_CORNER);
        upperLeft = sp.getCorner(UPPER_LEFT_CORNER);
        upperRight = sp.getCorner(UPPER_RIGHT_CORNER);
        vsbPolicy = sp.getVerticalScrollBarPolicy();
        hsbPolicy = sp.getHorizontalScrollBarPolicy();
    }

    /**
     * It takes whoever occupied that place off the pane and returns the new one.
     *
     * <p>It is what keeps putting a new bar in from leaving the old one underneath.
     */
    protected Component addSingletonComponent(Component oldC, Component newC) {
        if ((oldC != null) && (oldC != newC)) {
            oldC.getParent().remove(oldC);
        }
        return newC;
    }

    /** It keeps the piece in the place the constraint names. */
    public void addLayoutComponent(String s, Component c) {
        if (s.equals(VIEWPORT)) {
            viewport = (JViewport) addSingletonComponent(viewport, c);
        } else if (s.equals(VERTICAL_SCROLLBAR)) {
            vsb = (JScrollBar) addSingletonComponent(vsb, c);
        } else if (s.equals(HORIZONTAL_SCROLLBAR)) {
            hsb = (JScrollBar) addSingletonComponent(hsb, c);
        } else if (s.equals(ROW_HEADER)) {
            rowHead = (JViewport) addSingletonComponent(rowHead, c);
        } else if (s.equals(COLUMN_HEADER)) {
            colHead = (JViewport) addSingletonComponent(colHead, c);
        } else if (s.equals(LOWER_LEFT_CORNER)) {
            lowerLeft = addSingletonComponent(lowerLeft, c);
        } else if (s.equals(LOWER_RIGHT_CORNER)) {
            lowerRight = addSingletonComponent(lowerRight, c);
        } else if (s.equals(UPPER_LEFT_CORNER)) {
            upperLeft = addSingletonComponent(upperLeft, c);
        } else if (s.equals(UPPER_RIGHT_CORNER)) {
            upperRight = addSingletonComponent(upperRight, c);
        } else {
            throw new IllegalArgumentException("invalid layout key " + s);
        }
    }

    public void removeLayoutComponent(Component c) {
        if (c == viewport) {
            viewport = null;
        } else if (c == vsb) {
            vsb = null;
        } else if (c == hsb) {
            hsb = null;
        } else if (c == rowHead) {
            rowHead = null;
        } else if (c == colHead) {
            colHead = null;
        } else if (c == lowerLeft) {
            lowerLeft = null;
        } else if (c == lowerRight) {
            lowerRight = null;
        } else if (c == upperLeft) {
            upperLeft = null;
        } else if (c == upperRight) {
            upperRight = null;
        }
    }

    public int getVerticalScrollBarPolicy() {
        return vsbPolicy;
    }

    /**
     * It changes the policy; the pane keeps it too, and it is the pane's that rules when laying
     * out.
     */
    public void setVerticalScrollBarPolicy(int x) {
        if (x != VERTICAL_SCROLLBAR_AS_NEEDED && x != VERTICAL_SCROLLBAR_NEVER
                && x != VERTICAL_SCROLLBAR_ALWAYS) {
            throw new IllegalArgumentException("invalid verticalScrollBarPolicy");
        }
        vsbPolicy = x;
    }

    public int getHorizontalScrollBarPolicy() {
        return hsbPolicy;
    }

    public void setHorizontalScrollBarPolicy(int x) {
        if (x != HORIZONTAL_SCROLLBAR_AS_NEEDED && x != HORIZONTAL_SCROLLBAR_NEVER
                && x != HORIZONTAL_SCROLLBAR_ALWAYS) {
            throw new IllegalArgumentException("invalid horizontalScrollBarPolicy");
        }
        hsbPolicy = x;
    }

    public JViewport getViewport() {
        return viewport;
    }

    public JScrollBar getHorizontalScrollBar() {
        return hsb;
    }

    public JScrollBar getVerticalScrollBar() {
        return vsb;
    }

    public JViewport getRowHeader() {
        return rowHead;
    }

    public JViewport getColumnHeader() {
        return colHead;
    }

    /** That corner's piece; the "leading" and "trailing" corners depend on the language. */
    public Component getCorner(String key) {
        if (key.equals(LOWER_LEFT_CORNER)) {
            return lowerLeft;
        } else if (key.equals(LOWER_RIGHT_CORNER)) {
            return lowerRight;
        } else if (key.equals(UPPER_LEFT_CORNER)) {
            return upperLeft;
        } else if (key.equals(UPPER_RIGHT_CORNER)) {
            return upperRight;
        }
        return null;
    }

    /**
     * What the pane would like to measure: the viewport, plus the headers, plus the bars that are
     * already known to be going to be needed.
     */
    public Dimension preferredLayoutSize(Container parent) {
        JScrollPane scrollPane = (JScrollPane) parent;
        vsbPolicy = scrollPane.getVerticalScrollBarPolicy();
        hsbPolicy = scrollPane.getHorizontalScrollBarPolicy();

        Insets insets = parent.getInsets();
        int prefWidth = insets.left + insets.right;
        int prefHeight = insets.top + insets.bottom;

        Dimension extentSize = null;
        Dimension viewSize = null;
        Component view = null;

        if (viewport != null) {
            extentSize = viewport.getPreferredSize();
            view = viewport.getView();
            if (view != null) {
                viewSize = view.getPreferredSize();
            } else {
                viewSize = new Dimension(0, 0);
            }
        }

        if (extentSize != null) {
            prefWidth = prefWidth + extentSize.width;
            prefHeight = prefHeight + extentSize.height;
        }

        Border viewportBorder = scrollPane.getViewportBorder();
        if (viewportBorder != null) {
            Insets vpbInsets = viewportBorder.getBorderInsets(parent);
            prefWidth = prefWidth + vpbInsets.left + vpbInsets.right;
            prefHeight = prefHeight + vpbInsets.top + vpbInsets.bottom;
        }

        if ((rowHead != null) && rowHead.isVisible()) {
            prefWidth = prefWidth + rowHead.getPreferredSize().width;
        }
        if ((colHead != null) && colHead.isVisible()) {
            prefHeight = prefHeight + colHead.getPreferredSize().height;
        }

        if ((vsb != null) && (vsbPolicy != VERTICAL_SCROLLBAR_NEVER)) {
            if (vsbPolicy == VERTICAL_SCROLLBAR_ALWAYS) {
                prefWidth = prefWidth + vsb.getPreferredSize().width;
            } else if ((viewSize != null) && (extentSize != null)) {
                boolean canScroll = true;
                if (view instanceof Scrollable) {
                    canScroll = !((Scrollable) view).getScrollableTracksViewportHeight();
                }
                if (canScroll && (viewSize.height > extentSize.height)) {
                    prefWidth = prefWidth + vsb.getPreferredSize().width;
                }
            }
        }

        if ((hsb != null) && (hsbPolicy != HORIZONTAL_SCROLLBAR_NEVER)) {
            if (hsbPolicy == HORIZONTAL_SCROLLBAR_ALWAYS) {
                prefHeight = prefHeight + hsb.getPreferredSize().height;
            } else if ((viewSize != null) && (extentSize != null)) {
                boolean canScroll = true;
                if (view instanceof Scrollable) {
                    canScroll = !((Scrollable) view).getScrollableTracksViewportWidth();
                }
                if (canScroll && (viewSize.width > extentSize.width)) {
                    prefHeight = prefHeight + hsb.getPreferredSize().height;
                }
            }
        }

        return new Dimension(prefWidth, prefHeight);
    }

    /**
     * The least: the viewport may shrink to nothing, but the bars and the headers may not.
     *
     * <p>With the "always" policy the bar adds its minimum; with "as needed" it does too, and it
     * is not a mistake: if the pane shrinks to the minimum, the bar will certainly be needed.
     */
    public Dimension minimumLayoutSize(Container parent) {
        JScrollPane scrollPane = (JScrollPane) parent;
        vsbPolicy = scrollPane.getVerticalScrollBarPolicy();
        hsbPolicy = scrollPane.getHorizontalScrollBarPolicy();

        Insets insets = parent.getInsets();
        int minWidth = insets.left + insets.right;
        int minHeight = insets.top + insets.bottom;

        if (viewport != null) {
            Dimension size = viewport.getMinimumSize();
            minWidth = minWidth + size.width;
            minHeight = minHeight + size.height;
        }

        Border viewportBorder = scrollPane.getViewportBorder();
        if (viewportBorder != null) {
            Insets vpbInsets = viewportBorder.getBorderInsets(parent);
            minWidth = minWidth + vpbInsets.left + vpbInsets.right;
            minHeight = minHeight + vpbInsets.top + vpbInsets.bottom;
        }

        if ((rowHead != null) && rowHead.isVisible()) {
            Dimension size = rowHead.getMinimumSize();
            minWidth = minWidth + size.width;
            minHeight = Math.max(minHeight, size.height);
        }
        if ((colHead != null) && colHead.isVisible()) {
            Dimension size = colHead.getMinimumSize();
            minWidth = Math.max(minWidth, size.width);
            minHeight = minHeight + size.height;
        }
        if ((vsb != null) && (vsbPolicy != VERTICAL_SCROLLBAR_NEVER)) {
            Dimension size = vsb.getMinimumSize();
            minWidth = minWidth + size.width;
            minHeight = Math.max(minHeight, size.height);
        }
        if ((hsb != null) && (hsbPolicy != HORIZONTAL_SCROLLBAR_NEVER)) {
            Dimension size = hsb.getMinimumSize();
            minWidth = Math.max(minWidth, size.width);
            minHeight = minHeight + size.height;
        }

        return new Dimension(minWidth, minHeight);
    }

    /** See the class note about why the decision is circular. */
    public void layoutContainer(Container parent) {
        JScrollPane scrollPane = (JScrollPane) parent;
        vsbPolicy = scrollPane.getVerticalScrollBarPolicy();
        hsbPolicy = scrollPane.getHorizontalScrollBarPolicy();

        Rectangle availR = scrollPane.getBounds();
        availR.x = 0;
        availR.y = 0;

        Insets insets = parent.getInsets();
        availR.x = insets.left;
        availR.y = insets.top;
        availR.width = availR.width - (insets.left + insets.right);
        availR.height = availR.height - (insets.top + insets.bottom);

        boolean leftToRight = scrollPane.getComponentOrientation().isLeftToRight();

        // The column header takes its height off the very top.
        Rectangle colHeadR = new Rectangle(0, availR.y, 0, 0);
        if ((colHead != null) && (colHead.isVisible())) {
            int colHeadHeight = Math.min(availR.height, colHead.getPreferredSize().height);
            colHeadR.height = colHeadHeight;
            availR.y = availR.y + colHeadHeight;
            availR.height = availR.height - colHeadHeight;
        }

        // The row one, its width off the side the line starts on.
        Rectangle rowHeadR = new Rectangle(0, 0, 0, 0);
        if ((rowHead != null) && (rowHead.isVisible())) {
            int rowHeadWidth = Math.min(availR.width, rowHead.getPreferredSize().width);
            rowHeadR.width = rowHeadWidth;
            availR.width = availR.width - rowHeadWidth;
            if (leftToRight) {
                rowHeadR.x = availR.x;
                availR.x = availR.x + rowHeadWidth;
            } else {
                rowHeadR.x = availR.x + availR.width;
            }
        }

        Border viewportBorder = scrollPane.getViewportBorder();
        Insets vpbInsets;
        if (viewportBorder != null) {
            vpbInsets = viewportBorder.getBorderInsets(parent);
            availR.x = availR.x + vpbInsets.left;
            availR.y = availR.y + vpbInsets.top;
            availR.width = availR.width - (vpbInsets.left + vpbInsets.right);
            availR.height = availR.height - (vpbInsets.top + vpbInsets.bottom);
        } else {
            vpbInsets = new Insets(0, 0, 0, 0);
        }

        // The content's preferred size, not the one it asks for as a viewport: here it is decided
                // whether the content fits, and what has to fit is the whole content. The
                // `getPreferredScrollableViewportSize` is for how much the pane would like to
                // measure, and it is used in {@link #preferredLayoutSize}.
        Component view = (viewport != null) ? viewport.getView() : null;
        Dimension viewPrefSize = (view != null) ? view.getPreferredSize() : new Dimension(0, 0);

        Dimension extentSize = (viewport != null)
                ? viewport.toViewCoordinates(availR.getSize()) : new Dimension(0, 0);

        boolean viewTracksViewportWidth = false;
        boolean viewTracksViewportHeight = false;
        boolean isEmpty = (availR.width < 0 || availR.height < 0);
        Scrollable sv;
        if (!isEmpty && view instanceof Scrollable) {
            sv = (Scrollable) view;
            viewTracksViewportWidth = sv.getScrollableTracksViewportWidth();
            viewTracksViewportHeight = sv.getScrollableTracksViewportHeight();
        } else {
            sv = null;
        }

        Rectangle vsbR = new Rectangle(0, availR.y - vpbInsets.top, 0, 0);

        boolean vsbNeeded;
        if (isEmpty) {
            vsbNeeded = false;
        } else if (vsbPolicy == VERTICAL_SCROLLBAR_ALWAYS) {
            vsbNeeded = true;
        } else if (vsbPolicy == VERTICAL_SCROLLBAR_NEVER) {
            vsbNeeded = false;
        } else {
            vsbNeeded = !viewTracksViewportHeight && (viewPrefSize.height > extentSize.height);
        }

        if ((vsb != null) && vsbNeeded) {
            adjustForVSB(true, availR, vsbR, vpbInsets, leftToRight);
            extentSize = viewport.toViewCoordinates(availR.getSize());
        }

        Rectangle hsbR = new Rectangle(availR.x - vpbInsets.left, 0, 0, 0);
        boolean hsbNeeded;
        if (isEmpty) {
            hsbNeeded = false;
        } else if (hsbPolicy == HORIZONTAL_SCROLLBAR_ALWAYS) {
            hsbNeeded = true;
        } else if (hsbPolicy == HORIZONTAL_SCROLLBAR_NEVER) {
            hsbNeeded = false;
        } else {
            hsbNeeded = !viewTracksViewportWidth && (viewPrefSize.width > extentSize.width);
        }

        if ((hsb != null) && hsbNeeded) {
            adjustForHSB(true, availR, hsbR, vpbInsets);

            // Putting the horizontal one in took height away: the vertical one may be needed now.
            if ((vsb != null) && !vsbNeeded && (vsbPolicy != VERTICAL_SCROLLBAR_NEVER)) {
                extentSize = viewport.toViewCoordinates(availR.getSize());
                vsbNeeded = viewPrefSize.height > extentSize.height;
                if (vsbNeeded) {
                    adjustForVSB(true, availR, vsbR, vpbInsets, leftToRight);
                }
            }
        }

        // With the size already fixed the content is asked again; see the class note.
        if (viewport != null) {
            viewport.setBounds(availR);

            if (sv != null) {
                extentSize = viewport.toViewCoordinates(availR.getSize());

                boolean oldHSBNeeded = hsbNeeded;
                boolean oldVSBNeeded = vsbNeeded;
                viewTracksViewportWidth = sv.getScrollableTracksViewportWidth();
                viewTracksViewportHeight = sv.getScrollableTracksViewportHeight();
                if (vsb != null && vsbPolicy == VERTICAL_SCROLLBAR_AS_NEEDED) {
                    boolean newVSBNeeded = !viewTracksViewportHeight
                            && (viewPrefSize.height > extentSize.height);
                    if (newVSBNeeded != vsbNeeded) {
                        vsbNeeded = newVSBNeeded;
                        adjustForVSB(vsbNeeded, availR, vsbR, vpbInsets, leftToRight);
                        extentSize = viewport.toViewCoordinates(availR.getSize());
                    }
                }
                if (hsb != null && hsbPolicy == HORIZONTAL_SCROLLBAR_AS_NEEDED) {
                    boolean newHSBNeeded = !viewTracksViewportWidth
                            && (viewPrefSize.width > extentSize.width);
                    if (newHSBNeeded != hsbNeeded) {
                        hsbNeeded = newHSBNeeded;
                        adjustForHSB(hsbNeeded, availR, hsbR, vpbInsets);
                        if ((vsb != null) && !vsbNeeded
                                && (vsbPolicy != VERTICAL_SCROLLBAR_NEVER)) {
                            extentSize = viewport.toViewCoordinates(availR.getSize());
                            vsbNeeded = viewPrefSize.height > extentSize.height;
                            if (vsbNeeded) {
                                adjustForVSB(true, availR, vsbR, vpbInsets, leftToRight);
                            }
                        }
                    }
                }
                if (oldHSBNeeded != hsbNeeded || oldVSBNeeded != vsbNeeded) {
                    viewport.setBounds(availR);
                }
            }
        }

        vsbR.height = availR.height + vpbInsets.top + vpbInsets.bottom;
        hsbR.width = availR.width + vpbInsets.left + vpbInsets.right;
        rowHeadR.height = availR.height + vpbInsets.top + vpbInsets.bottom;
        rowHeadR.y = availR.y - vpbInsets.top;
        colHeadR.width = availR.width + vpbInsets.left + vpbInsets.right;
        colHeadR.x = availR.x - vpbInsets.left;

        if (rowHead != null) {
            rowHead.setBounds(rowHeadR);
        }
        if (colHead != null) {
            colHead.setBounds(colHeadR);
        }

        if (vsb != null) {
            if (vsbNeeded) {
                vsb.setVisible(true);
                vsb.setBounds(vsbR);
            } else {
                vsb.setVisible(false);
            }
        }

        if (hsb != null) {
            if (hsbNeeded) {
                hsb.setVisible(true);
                hsb.setBounds(hsbR);
            } else {
                hsb.setVisible(false);
            }
        }

        if (lowerLeft != null) {
            lowerLeft.setBounds(leftToRight ? rowHeadR.x : vsbR.x, hsbR.y,
                    leftToRight ? rowHeadR.width : vsbR.width, hsbR.height);
        }
        if (lowerRight != null) {
            lowerRight.setBounds(leftToRight ? vsbR.x : rowHeadR.x, hsbR.y,
                    leftToRight ? vsbR.width : rowHeadR.width, hsbR.height);
        }
        if (upperLeft != null) {
            upperLeft.setBounds(leftToRight ? rowHeadR.x : vsbR.x, colHeadR.y,
                    leftToRight ? rowHeadR.width : vsbR.width, colHeadR.height);
        }
        if (upperRight != null) {
            upperRight.setBounds(leftToRight ? vsbR.x : rowHeadR.x, colHeadR.y,
                    leftToRight ? vsbR.width : rowHeadR.width, colHeadR.height);
        }
    }

    /** It takes what the vertical bar occupies off the available space, or gives it back. */
    private void adjustForVSB(boolean wantsVSB, Rectangle available, Rectangle vsbR,
            Insets vpbInsets, boolean leftToRight) {
        int oldWidth = vsbR.width;
        if (wantsVSB) {
            int vsbWidth = Math.max(0, Math.min(vsb.getPreferredSize().width, available.width));
            available.width = available.width - vsbWidth;
            vsbR.width = vsbWidth;

            if (leftToRight) {
                vsbR.x = available.x + available.width + vpbInsets.right;
            } else {
                vsbR.x = available.x - vpbInsets.left;
                available.x = available.x + vsbWidth;
            }
        } else {
            available.width = available.width + oldWidth;
        }
    }

    /** The same with the horizontal one. */
    private void adjustForHSB(boolean wantsHSB, Rectangle available, Rectangle hsbR,
            Insets vpbInsets) {
        int oldHeight = hsbR.height;
        if (wantsHSB) {
            int hsbHeight = Math.max(0,
                    Math.min(available.height, hsb.getPreferredSize().height));
            available.height = available.height - hsbHeight;
            hsbR.y = available.y + available.height + vpbInsets.bottom;
            hsbR.height = hsbHeight;
        } else {
            available.height = available.height + oldHeight;
        }
    }

    /** @deprecated it is {@link JScrollPane#getViewportBorderBounds}. */
    @Deprecated
    public Rectangle getViewportBorderBounds(JScrollPane scrollpane) {
        return scrollpane.getViewportBorderBounds();
    }

    /** The same layout, marked as set by a look and feel; see {@link UIResource}. */
    public static class UIResource extends ScrollPaneLayout
            implements javax.swing.plaf.UIResource {

        public UIResource() {
        }
    }
}
