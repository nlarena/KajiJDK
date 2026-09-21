package javax.swing;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;

/**
 * Swing's loose utilities: geometry between components, the event thread, and the algorithm
 * that places text and icon.
 *
 * <h2>{@link #layoutCompoundLabel} is the piece that matters</h2>
 *
 * <p>Every label, button, check box and cell in Swing places its text and its icon with this
 * method, and that is why they all look alike: the alignment, the gap, the clipping with an
 * ellipsis when it does not fit, all come from a single place. It returns the text
 * <em>possibly clipped</em> and leaves the three boxes -- view, icon, text -- in the rectangles
 * it is passed.
 *
 * <p>It is written so as to agree pixel by pixel with the JDK, which is what is verified
 * against it: the order of the integer operations, the division by two that rounds towards
 * zero, and that {@code CENTER} in the text stacks instead of putting side by side, are all
 * cases a "near enough" does not cover.
 *
 * <h2>What is not there</h2>
 *
 * <p>The keyboard actions ({@code notifyAction}, the input maps), what names
 * {@code JRootPane}, {@code JViewport} or {@code TransferHandler}, and the conversions to
 * <em>screen</em> coordinates: there is no screen. Each one is a class that does not exist yet,
 * not a method that was forgotten.
 */
public class SwingUtilities implements SwingConstants {

    private SwingUtilities() {
    }

    // -- geometry ---------------------------------------------------------------------------------

    /** Whether {@code a} contains {@code b} completely. */
    public static final boolean isRectangleContainingRectangle(Rectangle a, Rectangle b) {
        return b.x >= a.x && (b.x + b.width) <= (a.x + a.width)
                && b.y >= a.y && (b.y + b.height) <= (a.y + a.height);
    }

    /** The component's rectangle in its own coordinates: {@code (0, 0, width, height)}. */
    public static Rectangle getLocalBounds(Component aComponent) {
        return new Rectangle(0, 0, aComponent.getWidth(), aComponent.getHeight());
    }

    /** The first {@link Window} among the ancestors, or {@code null}. */
    public static Window getWindowAncestor(Component c) {
        for (Container p = c.getParent(); p != null; p = p.getParent()) {
            if (p instanceof Window) {
                return (Window) p;
            }
        }
        return null;
    }

    /**
     * It translates a point from one component's coordinate system to another's.
     *
     * <p>It goes through the common root: first it goes up from {@code source} to its window, then
     * it goes down to {@code destination}. Either of the two may be {@code null}, which means "the
     * root itself".
     */
    public static Point convertPoint(Component source, Point aPoint, Component destination) {
        Point p;
        if (aPoint == null) {
            p = null;
        } else {
            p = new Point(aPoint.x, aPoint.y);
        }
        if (source == null && destination == null) {
            return p;
        }
        if (p == null) {
            return null;
        }
        if (source != null) {
            // It goes up to the root adding the origins.
            for (Component c = source; c != null; c = c.getParent()) {
                p.x = p.x + c.getX();
                p.y = p.y + c.getY();
                if (c instanceof Window) {
                    break;
                }
            }
        }
        if (destination != null) {
            for (Component c = destination; c != null; c = c.getParent()) {
                p.x = p.x - c.getX();
                p.y = p.y - c.getY();
                if (c instanceof Window) {
                    break;
                }
            }
        }
        return p;
    }

    /** Ver {@link #convertPoint(Component, Point, Component)}. */
    public static Point convertPoint(Component source, int x, int y, Component destination) {
        return convertPoint(source, new Point(x, y), destination);
    }

    /** It translates a rectangle between coordinate systems; the size does not change. */
    public static Rectangle convertRectangle(Component source, Rectangle aRectangle,
            Component destination) {
        Point p = convertPoint(source, new Point(aRectangle.x, aRectangle.y), destination);
        return new Rectangle(p.x, p.y, aRectangle.width, aRectangle.height);
    }

    /** The first ancestor that is an instance of {@code c}, or {@code null}. */
    public static Container getAncestorOfClass(Class<?> c, Component comp) {
        if (comp == null || c == null) {
            return null;
        }
        Container parent = comp.getParent();
        while (parent != null && !c.isInstance(parent)) {
            parent = parent.getParent();
        }
        return parent;
    }

    /** The first ancestor with that name, or {@code null}. */
    public static Container getAncestorNamed(String name, Component comp) {
        if (comp == null || name == null) {
            return null;
        }
        Container parent = comp.getParent();
        while (parent != null && !name.equals(parent.getName())) {
            parent = parent.getParent();
        }
        return parent;
    }

    /**
     * The deepest component under the point, or {@code null} if the point falls outside.
     *
     * <p>It goes down through the <em>visible</em> children and in the order they were added,
     * which is the z-order: the first that contains the point wins, even though another sibling
     * also contains it.
     */
    public static Component getDeepestComponentAt(Component parent, int x, int y) {
        if (!parent.contains(x, y)) {
            return null;
        }
        if (parent instanceof Container) {
            Container c = (Container) parent;
            int n = c.getComponentCount();
            for (int i = 0; i < n; i++) {
                Component child = c.getComponent(i);
                if (child != null && child.isVisible()) {
                    Component deep =
                            getDeepestComponentAt(child, x - child.getX(), y - child.getY());
                    if (deep != null) {
                        return deep;
                    }
                }
            }
        }
        return parent;
    }

    /** The same mouse event, with its position translated to {@code destination}'s system. */
    public static MouseEvent convertMouseEvent(Component source, MouseEvent sourceEvent,
            Component destination) {
        Point p = convertPoint(source, new Point(sourceEvent.getX(), sourceEvent.getY()),
                destination);
        Component newOrigin = destination != null ? destination : source;
        return new MouseEvent(newOrigin, sourceEvent.getID(), sourceEvent.getWhen(),
                sourceEvent.getModifiersEx(), p.x, p.y, sourceEvent.getClickCount(),
                sourceEvent.isPopupTrigger(), sourceEvent.getButton());
    }

    /** The window that contains the component, or {@code null}. */
    public static Window windowForComponent(Component c) {
        return getWindowAncestor(c);
    }

    /** Whether {@code a} is {@code b} or is below {@code b}. */
    public static boolean isDescendingFrom(Component a, Component b) {
        if (a == b) {
            return true;
        }
        for (Container p = a.getParent(); p != null; p = p.getParent()) {
            if (p == b) {
                return true;
            }
        }
        return false;
    }

    /** The intersection, written into {@code dest}. Empty if they do not touch. */
    public static Rectangle computeIntersection(int x, int y, int width, int height, Rectangle dest) {
        int x1 = Math.max(x, dest.x);
        int x2 = Math.min(x + width, dest.x + dest.width);
        int y1 = Math.max(y, dest.y);
        int y2 = Math.min(y + height, dest.y + dest.height);
        dest.x = x1;
        dest.y = y1;
        dest.width = x2 - x1;
        dest.height = y2 - y1;
        if (dest.width < 0 || dest.height < 0) {
            dest.x = 0;
            dest.y = 0;
            dest.width = 0;
            dest.height = 0;
        }
        return dest;
    }

    /** The union, written into {@code dest}. */
    public static Rectangle computeUnion(int x, int y, int width, int height, Rectangle dest) {
        int x1 = Math.min(x, dest.x);
        int x2 = Math.max(x + width, dest.x + dest.width);
        int y1 = Math.min(y, dest.y);
        int y2 = Math.max(y + height, dest.y + dest.height);
        dest.x = x1;
        dest.y = y1;
        dest.width = x2 - x1;
        dest.height = y2 - y1;
        return dest;
    }

    /**
     * What is left of {@code rectA} on taking {@code rectB} away, as up to four rectangles.
     *
     * <p>It is what a component repaints when something partly covers it: the top, bottom, left
     * and right strips that were not covered. With no overlap, it returns {@code rectA} whole.
     */
    public static Rectangle[] computeDifference(Rectangle rectA, Rectangle rectB) {
        if (rectB == null || !rectA.intersects(rectB) || isRectangleContainingRectangle(rectB, rectA)) {
            return new Rectangle[0];
        }
        Rectangle[] parts = new Rectangle[4];
        int n = 0;
        // Top
        if (rectB.y > rectA.y) {
            parts[n] = new Rectangle(rectA.x, rectA.y, rectA.width, rectB.y - rectA.y);
            n = n + 1;
        }
        // Bottom
        int backgroundB = rectB.y + rectB.height;
        int backgroundA = rectA.y + rectA.height;
        if (backgroundB < backgroundA) {
            parts[n] = new Rectangle(rectA.x, backgroundB, rectA.width, backgroundA - backgroundB);
            n = n + 1;
        }
        // Left and right, only in the middle strip
        int midY = Math.max(rectA.y, rectB.y);
        int midH = Math.min(backgroundA, backgroundB) - midY;
        if (rectB.x > rectA.x) {
            parts[n] = new Rectangle(rectA.x, midY, rectB.x - rectA.x, midH);
            n = n + 1;
        }
        int sideB = rectB.x + rectB.width;
        int sideA = rectA.x + rectA.width;
        if (sideB < sideA) {
            parts[n] = new Rectangle(sideB, midY, sideA - sideB, midH);
            n = n + 1;
        }
        Rectangle[] result = new Rectangle[n];
        for (int i = 0; i < n; i++) {
            result[i] = parts[i];
        }
        return result;
    }

    /**
     * The inner area: the component minus its insets, written into {@code r} if it is not {@code
     * null}.
     */
    public static Rectangle calculateInnerArea(JComponent c, Rectangle r) {
        if (c == null) {
            return null;
        }
        Rectangle rect = r;
        Insets insets = c.getInsets();
        if (rect == null) {
            rect = new Rectangle();
        }
        rect.x = insets.left;
        rect.y = insets.top;
        rect.width = c.getWidth() - insets.left - insets.right;
        rect.height = c.getHeight() - insets.top - insets.bottom;
        return rect;
    }

    /** The hierarchy's root: the topmost window or applet, or the last ancestor. */
    public static Component getRoot(Component c) {
        Component applet = null;
        for (Component p = c; p != null; p = p.getParent()) {
            if (p instanceof Window) {
                return p;
            }
            if (p instanceof java.applet.Applet) {
                applet = p;
            }
        }
        return applet;
    }

    // -- mouse ------------------------------------------------------------------------------------

    /** Whether the event is the left button's. */
    public static boolean isLeftMouseButton(MouseEvent anEvent) {
        return (anEvent.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0
                || anEvent.getButton() == MouseEvent.BUTTON1;
    }

    /** Whether the event is the middle button's. */
    public static boolean isMiddleMouseButton(MouseEvent anEvent) {
        return (anEvent.getModifiersEx() & InputEvent.BUTTON2_DOWN_MASK) != 0
                || anEvent.getButton() == MouseEvent.BUTTON2;
    }

    /** Whether the event is the right button's. */
    public static boolean isRightMouseButton(MouseEvent anEvent) {
        return (anEvent.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) != 0
                || anEvent.getButton() == MouseEvent.BUTTON3;
    }

    // -- text and icon ----------------------------------------------------------------------------

    /** A string's width with those metrics. */
    public static int computeStringWidth(FontMetrics fm, String str) {
        return fm.stringWidth(str);
    }

    /**
     * It places text and icon inside {@code viewR}, resolving {@code LEADING} and
     * {@code TRAILING} with {@code c}'s orientation.
     *
     * <p>With no component ({@code null}) left to right is assumed.
     */
    public static String layoutCompoundLabel(JComponent c, FontMetrics fm, String text, Icon icon,
            int verticalAlignment, int horizontalAlignment, int verticalTextPosition,
            int horizontalTextPosition, Rectangle viewR, Rectangle iconR, Rectangle textR,
            int textIconGap) {
        boolean leftToRight = true;
        if (c != null) {
            leftToRight = c.getComponentOrientation().isLeftToRight();
        }
        int hAlign = horizontalAlignment;
        int hText = horizontalTextPosition;
        if (hText == LEADING) {
            hText = leftToRight ? LEFT : RIGHT;
        } else if (hText == TRAILING) {
            hText = leftToRight ? RIGHT : LEFT;
        }
        if (hAlign == LEADING) {
            hAlign = leftToRight ? LEFT : RIGHT;
        } else if (hAlign == TRAILING) {
            hAlign = leftToRight ? RIGHT : LEFT;
        }
        return place(fm, text, icon, verticalAlignment, hAlign, verticalTextPosition, hText,
                viewR, iconR, textR, textIconGap);
    }

    /**
     * See the other form; this one treats {@code LEADING} as {@code LEFT} and {@code TRAILING} as
     * {@code RIGHT}.
     */
    public static String layoutCompoundLabel(FontMetrics fm, String text, Icon icon,
            int verticalAlignment, int horizontalAlignment, int verticalTextPosition,
            int horizontalTextPosition, Rectangle viewR, Rectangle iconR, Rectangle textR,
            int textIconGap) {
        return layoutCompoundLabel(null, fm, text, icon, verticalAlignment, horizontalAlignment,
                verticalTextPosition, horizontalTextPosition, viewR, iconR, textR, textIconGap);
    }

    /**
     * The algorithm, with the positions already resolved to
     * {@code LEFT}/{@code CENTER}/{@code RIGHT}.
     *
     * <p>Three steps: measure text and icon; place the text <em>relative to the icon at the
     * origin</em> according to the two text positions; and move the whole pair in order to align
     * it in the view. That the text is placed relative to the icon before aligning is what makes
     * "text to the right of the icon, everything centred" come out right with no special cases.
     *
     * <p>A detail the JDK applies and is not needed here: correcting a glyph's negative left
     * bearing (an italic {@code f} that sticks out on the left). This VM's typeface is a bitmap
     * with no negative bearings, so that correction is always zero.
     */
    private static String place(FontMetrics fm, String text, Icon icon, int verticalAlignment,
            int horizontalAlignment, int verticalTextPosition, int horizontalTextPosition,
            Rectangle viewR, Rectangle iconR, Rectangle textR, int textIconGap) {
        // The icon at the origin; its size is its own or zero.
        iconR.x = 0;
        iconR.y = 0;
        if (icon != null) {
            iconR.width = icon.getIconWidth();
            iconR.height = icon.getIconHeight();
        } else {
            iconR.width = 0;
            iconR.height = 0;
        }

        boolean noText = text == null || text.isEmpty();
        String clippedText = text;
        if (noText) {
            textR.width = 0;
            textR.height = 0;
            clippedText = "";
        } else {
            textR.width = computeStringWidth(fm, clippedText);
            textR.height = fm.getHeight();
        }

        // With no text or no icon there is no gap to respect.
        int gap = (noText || icon == null) ? 0 : textIconGap;

        if (!noText) {
            // How much width is left for the text: if it goes stacked with the icon, the whole
            // view;
                        // if it goes beside it, the view minus the icon and the gap.
            int available;
            if (horizontalTextPosition == CENTER) {
                available = viewR.width;
            } else {
                available = viewR.width - (iconR.width + gap);
            }
            if (textR.width > available) {
                // It does not fit: it is clipped with an ellipsis, leaving as many characters as
                // fit
                                // along with the dots. The loop adds one character at a time and
                                // stops at the first that goes over, which is exactly what the JDK
                                // does.
                String points = "...";
                int total = computeStringWidth(fm, points);
                int n;
                for (n = 0; n < clippedText.length(); n++) {
                    total = total + fm.charWidth(clippedText.charAt(n));
                    if (total > available) {
                        break;
                    }
                }
                clippedText = clippedText.substring(0, n) + points;
                textR.width = computeStringWidth(fm, clippedText);
            }
        }

        // The text relative to the icon, which is at the origin.
        if (verticalTextPosition == TOP) {
            textR.y = (horizontalTextPosition == CENTER) ? -(textR.height + gap) : 0;
        } else if (verticalTextPosition == CENTER) {
            textR.y = (iconR.height / 2) - (textR.height / 2);
        } else {
            textR.y = (horizontalTextPosition == CENTER) ? (iconR.height + gap)
                    : (iconR.height - textR.height);
        }
        if (horizontalTextPosition == LEFT) {
            textR.x = -(textR.width + gap);
        } else if (horizontalTextPosition == CENTER) {
            textR.x = (iconR.width / 2) - (textR.width / 2);
        } else {
            textR.x = iconR.width + gap;
        }

        // The box that spans both, and its shift in order to align it in the view.
        int boxX = Math.min(iconR.x, textR.x);
        int boxWidth = Math.max(iconR.x + iconR.width, textR.x + textR.width) - boxX;
        int boxY = Math.min(iconR.y, textR.y);
        int boxHeight = Math.max(iconR.y + iconR.height, textR.y + textR.height) - boxY;

        int dx;
        int dy;
        if (verticalAlignment == TOP) {
            dy = viewR.y - boxY;
        } else if (verticalAlignment == CENTER) {
            dy = (viewR.y + (viewR.height / 2)) - (boxY + (boxHeight / 2));
        } else {
            dy = (viewR.y + viewR.height) - (boxY + boxHeight);
        }
        if (horizontalAlignment == LEFT) {
            dx = viewR.x - boxX;
        } else if (horizontalAlignment == RIGHT) {
            dx = (viewR.x + viewR.width) - (boxX + boxWidth);
        } else {
            dx = (viewR.x + (viewR.width / 2)) - (boxX + (boxWidth / 2));
        }

        textR.x = textR.x + dx;
        textR.y = textR.y + dy;
        iconR.x = iconR.x + dx;
        iconR.y = iconR.y + dy;
        return clippedText;
    }

    /**
     * The index of the character that is underlined as the mnemonic, or {@code -1}.
     *
     * <p>First the upper-case one, then the lower-case one: it is the JDK's order, and it makes
     * {@code "Save As"} with mnemonic {@code A} underline the {@code A} of {@code As} and not the
     * one of {@code Save}.
     */
    public static int findDisplayedMnemonicIndex(String text, int mnemonic) {
        if (text == null || mnemonic == '\0') {
            return -1;
        }
        char shift = Character.toUpperCase((char) mnemonic);
        char minus = Character.toLowerCase((char) mnemonic);
        int index = text.indexOf(shift);
        if (index == -1) {
            index = text.indexOf(minus);
        }
        return index;
    }

    /** Whether the component is read left to right. */
    static boolean isLeftToRight(Component c) {
        return c.getComponentOrientation().isLeftToRight();
    }

    // -- painting somebody else's component
    // --------------------------------------------------------

    /**
     * It paints {@code c} inside {@code p}, in that rectangle, without really adding it.
     *
     * <p>It is how a table paints its cell renderer: the same component is placed and painted once
     * per cell. Here it is done directly -- position, translate the context, paint -- without the
     * JDK's intermediate {@code CellRendererPane}, which exists so that the component has a parent
     * while it is painted and is not needed here.
     */
    public static void paintComponent(Graphics g, Component c, Container p, int x, int y, int w,
            int h) {
        c.setBounds(x, y, w, h);
        Graphics cg = g.create(x, y, w, h);
        if (cg == null) {
            return;
        }
        try {
            c.paint(cg);
        } finally {
            cg.dispose();
        }
    }

    /** Ver {@link #paintComponent(Graphics, Component, Container, int, int, int, int)}. */
    public static void paintComponent(Graphics g, Component c, Container p, Rectangle r) {
        paintComponent(g, c, p, r.x, r.y, r.width, r.height);
    }

    /** It asks each {@link JComponent} of the tree to renew its look and feel. */
    public static void updateComponentTreeUI(Component c) {
        if (c instanceof JComponent) {
            ((JComponent) c).updateUI();
        }
        if (c instanceof Container) {
            Container cont = (Container) c;
            int n = cont.getComponentCount();
            for (int i = 0; i < n; i++) {
                updateComponentTreeUI(cont.getComponent(i));
            }
        }
    }

    // -- the event thread
    // ---------------------------------------------------------------------------

    /** It queues {@code doRun} on AWT's event thread. */
    public static void invokeLater(Runnable doRun) {
        EventQueue.invokeLater(doRun);
    }

    /** It queues {@code doRun} and waits for it to finish. */
    public static void invokeAndWait(final Runnable doRun)
            throws InterruptedException, InvocationTargetException {
        EventQueue.invokeAndWait(doRun);
    }

    /** Whether the current thread is the event one. */
    public static boolean isEventDispatchThread() {
        return EventQueue.isDispatchThread();
    }

    // -- screen coordinates
    // ------------------------------------------------------------------------

    /**
     * It takes that point from the component's coordinates to the screen's.
     *
     * <p>It modifies the point it is given; it does not return a new one. It is Swing's old way
     * and it is kept because changing it would break whoever uses it.
     *
     * @throws java.awt.IllegalComponentStateException if the component is not on the screen
     */
    public static void convertPointToScreen(Point p, Component c) {
        Component comp = c;
        int x = 0;
        int y = 0;
        while (comp != null) {
            x = x + comp.getX();
            y = y + comp.getY();
            if (comp instanceof Window) {
                comp = null;
            } else {
                comp = comp.getParent();
            }
        }
        p.x = p.x + x;
        p.y = p.y + y;
    }

    /**
     * It takes that point from the screen's coordinates to the component's.
     *
     * <p>It modifies the point; see {@link #convertPointToScreen}.
     *
     * @throws java.awt.IllegalComponentStateException if the component is not on the screen
     */
    public static void convertPointFromScreen(Point p, Component c) {
        Component comp = c;
        int x = 0;
        int y = 0;
        while (comp != null) {
            x = x + comp.getX();
            y = y + comp.getY();
            if (comp instanceof Window) {
                comp = null;
            } else {
                comp = comp.getParent();
            }
        }
        p.x = p.x - x;
        p.y = p.y - y;
    }

    // -- looking upwards -------------------------------------------------------------------------

    /** The root pane that contains that component, or null. */
    public static JRootPane getRootPane(Component c) {
        if (c instanceof RootPaneContainer) {
            return ((RootPaneContainer) c).getRootPane();
        }
        for (Component p = c; p != null; p = p.getParent()) {
            if (p instanceof JRootPane) {
                return (JRootPane) p;
            }
        }
        return null;
    }

    /**
     * The parent, skipping a {@link JViewport}'s pane.
     *
     * <p>A component inside a pane with bars has a {@code JViewport} as its parent, which is a
     * piece of plumbing and not what one considers "the container". This method skips that
     * step.
     */
    public static Container getUnwrappedParent(Component component) {
        Container parent = component.getParent();
        while (parent instanceof JViewport) {
            parent = parent.getParent();
        }
        return parent;
    }

    /** What there is inside that viewport, skipping another viewport if there were one. */
    public static Component getUnwrappedView(JViewport viewport) {
        Component view = viewport.getView();
        while (view instanceof JViewport) {
            view = ((JViewport) view).getView();
        }
        return view;
    }

    /**
     * The component with the focus inside that window, or null.
     *
     * @deprecated As in the JDK: use {@code KeyboardFocusManager.getFocusOwner()}, which knows
     *     the answer without walking through anything.
     */
    @Deprecated
    public static Component findFocusOwner(Component c) {
        Component focusOwner = java.awt.KeyboardFocusManager
                .getCurrentKeyboardFocusManager().getFocusOwner();
        for (Component temp = focusOwner; temp != null; temp = temp.getParent()) {
            if (temp == c) {
                return focusOwner;
            }
        }
        return null;
    }

    // -- the look and feel's maps
    // --------------------------------------------------------------------

    /**
     * It replaces the action map the look and feel set, leaving the program's.
     *
     * <p>The maps are chained: the program's above and the look and feel's below. Changing the
     * look and feel has to change only the lower one, and for that the chain has to be walked
     * until the first that is a look and feel resource is found. It is what these four methods do,
     * and it is the reason they exist.
     */
    public static void replaceUIActionMap(JComponent component, ActionMap uiActionMap) {
        ActionMap map = component.getActionMap();
        while (map != null) {
            ActionMap parent = map.getParent();
            if (parent == null || parent instanceof javax.swing.plaf.UIResource) {
                map.setParent(uiActionMap);
                return;
            }
            map = parent;
        }
    }

    /** The action map the look and feel set, or null. */
    public static ActionMap getUIActionMap(JComponent component) {
        ActionMap map = component.getActionMap();
        while (map != null) {
            if (map instanceof javax.swing.plaf.UIResource) {
                return map;
            }
            map = map.getParent();
        }
        return null;
    }

    /** It replaces the look and feel's key map; see {@link #replaceUIActionMap}. */
    public static void replaceUIInputMap(JComponent component, int type, InputMap uiInputMap) {
        InputMap map = component.getInputMap(type);
        while (map != null) {
            InputMap parent = map.getParent();
            if (parent == null || parent instanceof javax.swing.plaf.UIResource) {
                map.setParent(uiInputMap);
                return;
            }
            map = parent;
        }
    }

    /** The key map the look and feel set, or null. */
    public static InputMap getUIInputMap(JComponent component, int condition) {
        InputMap map = component.getInputMap(condition);
        while (map != null) {
            if (map instanceof javax.swing.plaf.UIResource) {
                return map;
            }
            map = map.getParent();
        }
        return null;
    }

    /**
     * It gives that key to that action, if the action is switched on.
     *
     * <p>A switched-off action is not executed and returns false, and that is what makes a binding
     * to a switched-off action count as though it did not exist -- which is how Swing lets a
     * component's binding beat its container's.
     */
    public static boolean notifyAction(Action action, KeyStroke ks, java.awt.event.KeyEvent event,
            Object sender, int modifiers) {
        if (action == null) {
            return false;
        }
        if (!action.isEnabled()) {
            return false;
        }
        Object commandO = action.getValue(Action.ACTION_COMMAND_KEY);
        String command = (commandO != null) ? commandO.toString() : null;
        action.actionPerformed(new java.awt.event.ActionEvent(sender,
                java.awt.event.ActionEvent.ACTION_PERFORMED, command,
                event.getWhen(), modifiers));
        return true;
    }

    /**
     * It offers that key to the components of the window where it happened.
     *
     * <p>It is the last step of the handing out of keys: the one that attends the bindings of the
     * "when the window has the focus" kind, which are the ones that make the menu shortcuts work
     * without the menu having the focus.
     */
    public static boolean processKeyBindings(java.awt.event.KeyEvent event) {
        if (event == null) {
            return false;
        }
        Component component = event.getComponent();
        for (Component c = component; c != null; c = c.getParent()) {
            if (c instanceof JComponent) {
                if (((JComponent) c).processKeyBinding(
                        KeyStroke.getKeyStrokeForEvent(event), event,
                        JComponent.WHEN_IN_FOCUSED_WINDOW, event.getID()
                                == java.awt.event.KeyEvent.KEY_PRESSED)) {
                    return true;
                }
            }
        }
        return false;
    }

    // -- accessibility 
    // -----------------------------------------------------------------------------

    /**
     * How many accessible children that component has.
     *
     * <p>They are not the same as AWT's children: a component may expose as accessible things that
     * are not components -- a table's rows, for instance -- and hide those that are pure
     * scaffolding.
     */
    public static int getAccessibleChildrenCount(Component c) {
        javax.accessibility.AccessibleContext ac = c.getAccessibleContext();
        if (ac != null) {
            return ac.getAccessibleChildrenCount();
        }
        return 0;
    }

    /** Accessible child number {@code i}; see {@link #getAccessibleChildrenCount}. */
    public static javax.accessibility.Accessible getAccessibleChild(Component c, int i) {
        javax.accessibility.AccessibleContext ac = c.getAccessibleContext();
        if (ac != null) {
            return ac.getAccessibleChild(i);
        }
        return null;
    }

    /** What place it takes among its parent's accessible children, or -1. */
    public static int getAccessibleIndexInParent(Component c) {
        javax.accessibility.AccessibleContext ac = c.getAccessibleContext();
        if (ac != null) {
            return ac.getAccessibleIndexInParent();
        }
        return -1;
    }

    /** The accessible object that falls at that point, or null. */
    public static javax.accessibility.Accessible getAccessibleAt(Component c, Point p) {
        javax.accessibility.AccessibleContext ac = c.getAccessibleContext();
        if (ac != null) {
            javax.accessibility.AccessibleComponent acomp = ac.getAccessibleComponent();
            if (acomp != null) {
                return acomp.getAccessibleAt(p);
            }
        }
        return null;
    }

    /** The component's accessible state -- visible, enabled, chosen --, or null. */
    public static javax.accessibility.AccessibleStateSet getAccessibleStateSet(Component c) {
        javax.accessibility.AccessibleContext ac = c.getAccessibleContext();
        if (ac != null) {
            return ac.getAccessibleStateSet();
        }
        return null;
    }

    // -- the shared owner window
    // ---------------------------------------------------------------------

    /**
     * The hidden window the dialogs and windows with no owner hang from.
     *
     * <p>It is not public -- not in the JDK either --: it is plumbing. A system window needs to
     * depend on another, and creating one for each ownerless dialog would spend a real window each
     * time.
     *
     * @throws java.awt.HeadlessException if there is no screen
     */
    static java.awt.Frame getSharedOwnerFrame() {
        if (ownerFrame == null) {
            ownerFrame = new java.awt.Frame();
            ownerFrame.setUndecorated(true);
        }
        return ownerFrame;
    }

    private static java.awt.Frame ownerFrame;
}
