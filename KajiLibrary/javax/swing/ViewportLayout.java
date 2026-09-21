package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.io.Serializable;

/**
 * A {@link JViewport}'s layout: it places the only child, which is the view.
 *
 * <h2>Three decisions</h2>
 *
 * <p>Every time it lays out it decides, in this order:
 *
 * <ol>
 * <li><strong>What size to give the view.</strong> Its preferred size, unless the view is
 * {@link Scrollable} and says it wants to follow the viewport's width or height.
 * <li><strong>Whether the position has to be shifted.</strong> If on the viewport growing there
 * would be empty space after the end of the view, it is brought closer again: it is what makes
 * the content stick to the edge instead of leaving a gap when a window scrolled to the bottom
 * is enlarged.
 * <li><strong>Whether the view has to be stretched.</strong> An ordinary view -- not
 * {@code Scrollable} -- that is at the origin and is smaller than the viewport is enlarged
 * until it fills it. It is what makes a small content be seen with a background of its own and
 * not with the viewport's.
 * </ol>
 *
 * <p>The instance is shared: it keeps nothing of any viewport.
 */
public class ViewportLayout implements LayoutManager, Serializable {

    static ViewportLayout SHARED_INSTANCE = new ViewportLayout();

    public ViewportLayout() {
    }

    /** Nothing: a viewport has a single child and does not tell it apart by name. */
    public void addLayoutComponent(String name, Component c) {
    }

    public void removeLayoutComponent(Component c) {
    }

    /** What the view wants, or zero if there is none. */
    public Dimension preferredLayoutSize(Container parent) {
        Component view = ((JViewport) parent).getView();
        if (view == null) {
            return new Dimension(0, 0);
        } else if (view instanceof Scrollable) {
            return ((Scrollable) view).getPreferredScrollableViewportSize();
        } else {
            return view.getPreferredSize();
        }
    }

    /** Four by four: a viewport may shrink to almost nothing, the view does not limit it. */
    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(4, 4);
    }

    /** See the class note. */
    public void layoutContainer(Container parent) {
        JViewport vp = (JViewport) parent;
        Component view = vp.getView();
        Scrollable scrollableView = null;

        if (view == null) {
            return;
        } else if (view instanceof Scrollable) {
            scrollableView = (Scrollable) view;
        }

        // Everything below is in the view's coordinates, save vpSize.
        Insets insets = vp.getInsets();
        Dimension viewPrefSize = view.getPreferredSize();
        Dimension vpSize = vp.getSize();
        Dimension extentSize = vp.toViewCoordinates(vpSize);
        Dimension viewSize = new Dimension(viewPrefSize);

        if (scrollableView != null) {
            if (scrollableView.getScrollableTracksViewportWidth()) {
                viewSize.width = vpSize.width;
            }
            if (scrollableView.getScrollableTracksViewportHeight()) {
                viewSize.height = vpSize.height;
            }
        }

        Point viewPosition = vp.getViewPosition();

        // If space were left over after the end of the view, it is brought closer to the edge.
        if (scrollableView == null || vp.getParent() == null
                || vp.getParent().getComponentOrientation().isLeftToRight()) {
            if ((viewPosition.x + extentSize.width) > viewSize.width) {
                viewPosition.x = Math.max(0, viewSize.width - extentSize.width);
            }
        } else {
            if (extentSize.width > viewSize.width) {
                viewPosition.x = viewSize.width - extentSize.width;
            } else {
                viewPosition.x = Math.max(0,
                        Math.min(viewSize.width - extentSize.width, viewPosition.x));
            }
        }

        if ((viewPosition.y + extentSize.height) > viewSize.height) {
            viewPosition.y = Math.max(0, viewSize.height - extentSize.height);
        }

        // An ordinary view that is at the origin and does not fill, is stretched until it fills.
        if (scrollableView == null) {
            if ((viewPosition.x == 0) && (vpSize.width > viewPrefSize.width)) {
                viewSize.width = vpSize.width;
            }
            if ((viewPosition.y == 0) && (vpSize.height > viewPrefSize.height)) {
                viewSize.height = vpSize.height;
            }
        }
        vp.setViewPosition(viewPosition);
        vp.setViewSize(viewSize);
    }
}
