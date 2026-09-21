package javax.swing;

import java.applet.Applet;
import java.awt.Component;
import java.awt.Component$BaselineResizeBehavior;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.io.Serializable;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.Locale;

import javax.swing.border.Border;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.ComponentUI;

/**
 * The root of every Swing component: what a button, a table or a panel have in common.
 *
 * <h2>What this class adds over {@link Container}</h2>
 *
 * <p>AWT already knows how to have children, a size, a typeface and to paint. Swing adds three
 * things on top, and all three are really here:
 *
 * <ul>
 * <li><strong>The three-step painting pipeline</strong> -- {@link #paintComponent},
 *     {@link #paintBorder} and {@link #paintChildren}, in that order and each one redefinable
 *     separately. It is what allows a subclass to draw its content without knowing anything
 *     about borders or children.</li>
 * <li><strong>The border as an object</strong> -- {@link #setBorder}. The {@link #getInsets}
 *     come from it, so a layout respects the border without anybody telling it.</li>
 * <li><strong>The separate look and feel</strong> -- {@link #setUI}: if there is a
 *     {@link ComponentUI}, the drawing and the measurements are delegated to it. Without it,
 *     the component manages on its own, and that "on its own" is honest: it fills the
 *     background if it is opaque, and measures with what AWT already knows.</li>
 * </ul>
 *
 * <h2>What is <em>not</em> there, and why</h2>
 *
 * <p>Almost everything is there. What is missing is what needs things this library does not
 * have yet, and each group for a concrete reason:
 *
 * <ul>
 * <li>The <strong>focus walk</strong> beyond what AWT already gives -- for instance
 *     {@code getNextFocusableComponent} -- needs a {@code KeyboardFocusManager} to decide.
 *     {@link #setInputVerifier} is there: it keeps and returns the verifier, which is all this
 *     component does with it; who consults it on moving the focus is the manager that is
 *     missing.</li>
 * <li>{@code paintImmediately} and the deferred repainting go through a
 *     {@code RepaintManager} that draws on a <em>screen</em>. This VM does not have one: a
 *     component is painted when somebody passes it a {@link Graphics}, typically a
 *     {@code BufferedImage}'s, and that is what {@link #paint} does well. {@link #repaint} and
 *     {@link #revalidate} go on existing with the meaning they can have with no screen, and
 *     they say so.</li>
 * </ul>
 *
 * <p>The keyboard actions, the popup menus, the transfer and the tool tips were on this list
 * and no longer are: {@code InputMap}, {@code ActionMap}, {@code JPopupMenu},
 * {@code TransferHandler}, {@code JToolTip} and {@code JRootPane} exist, and so do the members
 * that name them.
 *
 * <p>The criterion is the usual one: a member that is missing is a legal subset; one that
 * pretended to have a {@code KeyboardFocusManager} behind it compiles and blows up
 * afterwards.
 *
 * <h2>A note about the JDK: who fills the background</h2>
 *
 * <p>In the JDK, {@link #paintComponent} paints nothing by itself: it asks the look and feel to
 * do it, and it is each component's basic look and feel that fills the background when the
 * component is opaque. Here, with no look and feel installed, that responsibility would be left
 * with no owner and an opaque panel would have no background. That is why
 * {@code paintComponent} fills the background <strong>when there is no look and feel</strong>
 * and delegates when there is one: the observable behaviour is the same, and the sharing out of
 * responsibilities is restored as soon as somebody installs one.
 */
public abstract class JComponent extends Container implements Serializable {

    private static final long serialVersionUID = -5876370834061273469L;

    /** Condition of a keyboard action: only with the focus. Defined here for compatibility. */
    public static final int WHEN_FOCUSED = 0;

    /** Condition: when the focus is in a descendant. */
    public static final int WHEN_ANCESTOR_OF_FOCUSED_COMPONENT = 1;

    /** Condition: when the window has the focus. */
    public static final int WHEN_IN_FOCUSED_WINDOW = 2;

    /** No condition registered. */
    public static final int UNDEFINED_CONDITION = -1;

    /** The client property key the tool tip text lives under. */
    public static final String TOOL_TIP_TEXT_KEY = "ToolTipText";

    /**
     * The key the input verifier is kept with.
     *
     * <p>It goes in the client properties and not in a field, as in the JDK: there are few
     * components that have a verifier, and one more field per component on a screen with hundreds
     * shows.
     */
    private static final String INPUT_VERIFIER_KEY = "_InputVerifier";

    /** The key the popup menu is kept with; see {@link #getComponentPopupMenu}. */
    private static final String POPUP_MENU_KEY = "_ComponentPopupMenu";

    /** The installed look and feel, or {@code null} if the component draws and measures itself. */
    protected transient ComponentUI ui;

    /** Swing's own listeners; AWT's live in {@link Component}. */
    protected EventListenerList listenerList = new EventListenerList();

    private static Locale defaultLocale;

    private Border border;
    private boolean opaque;
    private float alignmentX = -1.0f;
    private float alignmentY = -1.0f;
    private Hashtable<Object, Object> clientProperties;
    private boolean doubleBuffered;
    private boolean autoscrolls;
    private boolean opaqueSet;
    private boolean autoscrollsSet;
    private int debugGraphicsOptions;
    private boolean requestFocusEnabled = true;
    private boolean verifyInputWhenFocusTarget = true;
    private boolean paintingForPrint;
    private VetoableChangeSupport vetoableChangeSupport;

    /**
     * An empty component, not opaque, with neither border nor look and feel.
     *
     * <p>It gives it Swing's default locale from the start. Without that, {@code getLocale()} on a
     * component not yet added to anything would throw {@code IllegalComponentStateException},
     * which is what an AWT component with no parent does; a Swing component has to be able to
     * answer for its language before being on the screen, because the formats it builds itself
     * with come from there.
     */
    public JComponent() {
        super();
        setLocale(JComponent.getDefaultLocale());
    }

    // -- the look and feel ----------------------------------------------------------------------

    /**
     * It installs a look and feel, uninstalling the previous one.
     *
     * <p>Protected because each subclass exposes its own with the precise type
     * -- {@code setUI(ButtonUI)} -- and this is the common mechanism underneath. It ends with
     * {@link #revalidate} and {@link #repaint}: a new look and feel may measure differently.
     */
    protected void setUI(ComponentUI newUI) {
        if (this.ui != null) {
            this.ui.uninstallUI(this);
        }
        ComponentUI old = this.ui;
        this.ui = newUI;
        if (this.ui != null) {
            this.ui.installUI(this);
        }
        firePropertyChange("UI", old, newUI);
        revalidate();
        repaint();
    }

    /** The installed look and feel, or {@code null}. */
    public ComponentUI getUI() {
        return this.ui;
    }

    /**
     * It asks the {@code UIManager} for the look and feel again.
     *
     * <p>It does nothing, and it is not a marker: in the JDK this method is empty in
     * {@code JComponent} too, because each subclass knows which key to ask for. What is missing is
     * the {@code UIManager} to ask it of, and each subclass says so in its own.
     */
    public void updateUI() {
    }

    /** The key the {@code UIManager} would look this class's look and feel up with. */
    public String getUIClassID() {
        return "ComponentUI";
    }

    // -- the painting pipeline
    // ---------------------------------------------------------------------

    /**
     * It paints the whole component: content, border and children, in that order.
     *
     * <p>The order is the contract: the border goes on top of the content -- so that a content
     * that goes over the line ends up below the frame -- and the children go on top of
     * everything. The subclasses do not redefine this but {@link #paintComponent}.
     */
    public void paint(Graphics g) {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        Graphics cg = getComponentGraphics(g);
        paintComponent(cg);
        paintBorder(cg);
        paintChildren(cg);
    }

    /**
     * It paints its own content, with neither border nor children.
     *
     * <p>With a look and feel, it is delegated to it over a <em>copy</em> of the context: the look
     * and feel may change colour, typeface or clip and none of that has to reach the border or the
     * children. With no look and feel, it fills the background if the component is opaque -- see
     * the class note about why that background is painted here and not in the look and feel that
     * is not there.
     */
    protected void paintComponent(Graphics g) {
        if (this.ui != null) {
            Graphics copy = g.create();
            try {
                this.ui.update(copy, this);
            } finally {
                copy.dispose();
            }
            return;
        }
        if (isOpaque()) {
            java.awt.Color old = g.getColor();
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(old);
        }
    }

    /** It paints the border, if there is one, over the content. */
    protected void paintBorder(Graphics g) {
        Border b = getBorder();
        if (b != null) {
            b.paintBorder(this, g, 0, 0, getWidth(), getHeight());
        }
    }

    /**
     * It paints the children, from the last added to the first.
     *
     * <p>The reverse of the order they were added in, because the first that is added is the one
     * that ends up on top: it is AWT's z-order rule. Each child receives a context translated to
     * its corner and clipped to its size, so it cannot draw outside itself.
     */
    protected void paintChildren(Graphics g) {
        synchronized (getTreeLock()) {
            for (int i = getComponentCount() - 1; i >= 0; i--) {
                Component child = getComponent(i);
                if (!child.isVisible()) {
                    continue;
                }
                Graphics cg = g.create(child.getX(), child.getY(), child.getWidth(),
                        child.getHeight());
                if (cg == null) {
                    continue;
                }
                try {
                    child.paint(cg);
                } finally {
                    cg.dispose();
                }
            }
        }
    }

    /**
     * It paints without clearing the background first.
     *
     * <p>AWT clears in {@code update} and then paints; Swing never clears here, because the
     * background is decided by {@link #paintComponent} according to {@link #isOpaque}. It is what
     * avoids AWT's flicker.
     */
    public void update(Graphics g) {
        paint(g);
    }

    /**
     * The context this component is painted with: the given one, with its colour and its typeface
     * set.
     *
     * <p>Without this, a component would paint with the colour and the typeface the previous one
     * left it.
     */
    protected Graphics getComponentGraphics(Graphics g) {
        g.setColor(getForeground());
        g.setFont(getFont());
        return g;
    }

    /** It prints: the same pipeline as {@link #paint}, with {@link #isPaintingForPrint} raised. */
    public void print(Graphics g) {
        this.paintingForPrint = true;
        try {
            Graphics cg = getComponentGraphics(g);
            printComponent(cg);
            printBorder(cg);
            printChildren(cg);
        } finally {
            this.paintingForPrint = false;
        }
    }

    /** It prints the component and its children. */
    public void printAll(Graphics g) {
        print(g);
    }

    /** It prints the content; by default, it paints it. */
    protected void printComponent(Graphics g) {
        paintComponent(g);
    }

    /** It prints the border; by default, it paints it. */
    protected void printBorder(Graphics g) {
        paintBorder(g);
    }

    /** It prints the children; by default, it paints them. */
    protected void printChildren(Graphics g) {
        paintChildren(g);
    }

    /** Whether the painting under way is a printing; see {@link #print}. */
    public boolean isPaintingForPrint() {
        return this.paintingForPrint;
    }

    /**
     * Whether the painting under way is a tile of a larger painting.
     *
     * <p>Always {@code false}: the tiling is built by the {@code RepaintManager} when it draws to
     * a screen in parts, and here the painting is done in one go over the image it is given.
     */
    public boolean isPaintingTile() {
        return false;
    }

    /**
     * Whether this component is a painting origin of its own. {@code false}, unless a subclass says
     * so.
     */
    protected boolean isPaintingOrigin() {
        return false;
    }

    /**
     * Whether the children do not overlap, and may therefore be painted without clipping one
     * another.
     *
     * <p>{@code true} by default, as in the JDK: a container with children that overlap redefines
     * it.
     */
    public boolean isOptimizedDrawingEnabled() {
        return true;
    }

    /** Whether a validation may stop here. {@code false}: only the roots say so. */
    public boolean isValidateRoot() {
        return false;
    }

    /**
     * It asks for a relayout.
     *
     * <p>In the JDK the {@code RepaintManager} queues it and it is done afterwards, all together.
     * With no event queue to dispatch it, here it is synchronous: it invalidates and validates the
     * parent, which is the one that knows how to arrange.
     */
    public void revalidate() {
        invalidate();
        Container p = getParent();
        if (p != null) {
            p.validate();
        } else {
            validate();
        }
    }

    /** It repaints a region given as a rectangle. */
    public void repaint(Rectangle r) {
        repaint(0, r.x, r.y, r.width, r.height);
    }

    /**
     * It paints a region now, without waiting for the queue.
     *
     * <p>It does nothing, and it says so: painting "now" is painting to the screen, and this VM
     * does not have one. A component is painted when somebody passes a {@link Graphics} to
     * {@link #paint}. A {@link Graphics} is not invented out of nothing because
     * {@link #getGraphics} returns {@code null}, which is the right answer with no surface.
     */
    public void paintImmediately(int x, int y, int w, int h) {
    }

    /** Ver {@link #paintImmediately(int, int, int, int)}. */
    public void paintImmediately(Rectangle r) {
        paintImmediately(r.x, r.y, r.width, r.height);
    }

    // -- border, insets, opacity -----------------------------------------------------------------

    /**
     * It sets a border; {@code null} removes it.
     *
     * <p>Changing the border changes the insets, and with them the layout: hence the
     * {@link #revalidate}.
     */
    public void setBorder(Border border) {
        Border old = this.border;
        this.border = border;
        firePropertyChange("border", old, border);
        if (border != old) {
            if (border == null || old == null
                    || !border.getBorderInsets(this).equals(old.getBorderInsets(this))) {
                revalidate();
            }
            repaint();
        }
    }

    /** The border, or {@code null}. */
    public Border getBorder() {
        return this.border;
    }

    /**
     * How much space the border reserves, or AWT's insets if there is no border.
     *
     * <p>It is what makes a layout respect the border without knowing it exists: it asks for the
     * insets, and the insets already have it inside.
     */
    public Insets getInsets() {
        if (this.border != null) {
            return this.border.getBorderInsets(this);
        }
        return super.getInsets();
    }

    /** The same, filling the given object so as not to allocate. */
    public Insets getInsets(Insets insets) {
        if (insets == null) {
            insets = new Insets(0, 0, 0, 0);
        }
        Insets i = getInsets();
        insets.top = i.top;
        insets.left = i.left;
        insets.bottom = i.bottom;
        insets.right = i.right;
        return insets;
    }

    /**
     * It declares whether this component covers all its pixels.
     *
     * <p>It is a promise, not a measurement: whoever makes it undertakes that
     * {@link #paintComponent} paints the whole area. Swing uses it in order not to paint what is
     * underneath. Lying here does not fail: it leaves rubbish on the screen.
     */
    public void setOpaque(boolean isOpaque) {
        boolean old = this.opaque;
        this.opaque = isOpaque;
        this.opaqueSet = true;
        firePropertyChange("opaque", old, isOpaque);
    }

    /** Whether it covers all its pixels. {@code false} by default, which is the safe thing. */
    public boolean isOpaque() {
        return this.opaque;
    }

    // -- sizes and alignment -------------------------------------------------------------------

    /**
     * The preferred size: the one that was fixed, if somebody fixed it; if not, the look and
     * feel's; if not, AWT's layout's.
     *
     * <p>The order is the JDK's and it is what allows {@code setPreferredSize} to beat the look
     * and feel without the look and feel learning about it.
     */
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        if (this.ui != null) {
            Dimension d = this.ui.getPreferredSize(this);
            if (d != null) {
                return d;
            }
        }
        return super.getPreferredSize();
    }

    /** Like {@link #getPreferredSize}, for the minimum. */
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        if (this.ui != null) {
            Dimension d = this.ui.getMinimumSize(this);
            if (d != null) {
                return d;
            }
        }
        return super.getMinimumSize();
    }

    /** Like {@link #getPreferredSize}, for the maximum. */
    public Dimension getMaximumSize() {
        if (isMaximumSizeSet()) {
            return super.getMaximumSize();
        }
        if (this.ui != null) {
            Dimension d = this.ui.getMaximumSize(this);
            if (d != null) {
                return d;
            }
        }
        return super.getMaximumSize();
    }

    /** It fixes the horizontal alignment, clipped to {@code [0, 1]}. */
    public void setAlignmentX(float alignmentX) {
        this.alignmentX = Math.max(0.0f, Math.min(1.0f, alignmentX));
    }

    /** The horizontal alignment; AWT's if none was fixed. */
    public float getAlignmentX() {
        if (this.alignmentX < 0.0f) {
            return super.getAlignmentX();
        }
        return this.alignmentX;
    }

    /** It fixes the vertical alignment, clipped to {@code [0, 1]}. */
    public void setAlignmentY(float alignmentY) {
        this.alignmentY = Math.max(0.0f, Math.min(1.0f, alignmentY));
    }

    /** The vertical alignment; AWT's if none was fixed. */
    public float getAlignmentY() {
        if (this.alignmentY < 0.0f) {
            return super.getAlignmentY();
        }
        return this.alignmentY;
    }

    /** The baseline: the look and feel's if there is one, otherwise AWT's. */
    public int getBaseline(int width, int height) {
        if (this.ui != null) {
            return this.ui.getBaseline(this, width, height);
        }
        return super.getBaseline(width, height);
    }

    /**
     * How the baseline moves; the look and feel's if there is one.
     *
     * <p>With the binary name {@code Component$BaselineResizeBehavior}: a type nested in another
     * file does not resolve by its Java name in our compiler (#101).
     */
    public Component$BaselineResizeBehavior getBaselineResizeBehavior() {
        if (this.ui != null) {
            return this.ui.getBaselineResizeBehavior(this);
        }
        return super.getBaselineResizeBehavior();
    }

    /**
     * Whether the point falls inside; the look and feel may give it a shape that is not the
     * rectangle.
     */
    public boolean contains(int x, int y) {
        if (this.ui != null) {
            return this.ui.contains(this, x, y);
        }
        return super.contains(x, y);
    }

    // -- region visible ---------------------------------------------------------------------------

    /**
     * It computes which part of this component is seen, intersecting with every ancestor.
     *
     * <p>A component inside a scrolling area has almost everything covered: what is seen is the
     * intersection of its rectangle with each container's as far up as they go, each one in this
     * one's coordinates.
     */
    public void computeVisibleRect(Rectangle visibleRect) {
        visibleRect.x = 0;
        visibleRect.y = 0;
        visibleRect.width = getWidth();
        visibleRect.height = getHeight();
        int dx = 0;
        int dy = 0;
        Container p = getParent();
        Component current = this;
        while (p != null) {
            dx = dx + current.getX();
            dy = dy + current.getY();
            // The parent's rectangle, brought to this component's coordinates.
            int px = -dx;
            int py = -dy;
            int x1 = Math.max(visibleRect.x, px);
            int y1 = Math.max(visibleRect.y, py);
            int x2 = Math.min(visibleRect.x + visibleRect.width, px + p.getWidth());
            int y2 = Math.min(visibleRect.y + visibleRect.height, py + p.getHeight());
            visibleRect.x = x1;
            visibleRect.y = y1;
            visibleRect.width = Math.max(0, x2 - x1);
            visibleRect.height = Math.max(0, y2 - y1);
            if (p instanceof Window || p instanceof Applet) {
                break;
            }
            current = p;
            p = p.getParent();
        }
    }

    /** The visible part, in a new rectangle. */
    public Rectangle getVisibleRect() {
        Rectangle r = new Rectangle();
        computeVisibleRect(r);
        return r;
    }

    /**
     * It asks for a rectangle of this component to be in sight.
     *
     * <p>The request goes up: it is translated to the parent's coordinates and forwarded to it.
     * Who really attends it is a scrolling area further up, which redefines this method. With
     * none, the request reaches the root and nothing happens -- which is right, because there is
     * nothing to scroll.
     */
    public void scrollRectToVisible(Rectangle aRect) {
        Container p = getParent();
        if (p instanceof JComponent) {
            aRect.x = aRect.x + getX();
            aRect.y = aRect.y + getY();
            ((JComponent) p).scrollRectToVisible(aRect);
            aRect.x = aRect.x - getX();
            aRect.y = aRect.y - getY();
        }
    }

    /** The window or applet that contains this component, or {@code null} if it is in none. */
    public Container getTopLevelAncestor() {
        Container p = getParent();
        while (p != null) {
            if (p instanceof Window || p instanceof Applet) {
                return p;
            }
            p = p.getParent();
        }
        return null;
    }

    // -- client properties and tip
    // -----------------------------------------------------------------

    /**
     * It keeps a value under a key, without the class declaring it.
     *
     * <p>It is the drawer where a look and feel or a tool leaves data of its own about somebody
     * else's component. {@code null} erases. It gives notice as a property change with the key as
     * the name, so it can be listened to.
     */
    public final void putClientProperty(Object key, Object value) {
        if (key == null) {
            throw new NullPointerException("The key cannot be null");
        }
        Object old;
        synchronized (this) {
            if (this.clientProperties == null) {
                if (value == null) {
                    return;
                }
                this.clientProperties = new Hashtable<Object, Object>();
            }
            old = this.clientProperties.get(key);
            if (value != null) {
                this.clientProperties.put(key, value);
            } else {
                this.clientProperties.remove(key);
            }
        }
        firePropertyChange(key.toString(), old, value);
    }

    /** The value under that key, or {@code null}. */
    public final Object getClientProperty(Object key) {
        if (key == null) {
            return null;
        }
        synchronized (this) {
            if (this.clientProperties == null) {
                return null;
            }
            return this.clientProperties.get(key);
        }
    }

    /**
     * It fixes the tool tip text.
     *
     * <p>It is kept as a client property, as in the JDK. Showing it in a bubble asks for a
     * {@code ToolTipManager} and a {@code JToolTip} that are not there; the text stays available
     * for whoever asks for it.
     */
    public void setToolTipText(String text) {
        putClientProperty(TOOL_TIP_TEXT_KEY, text);
    }

    /** The tool tip text, or {@code null}. */
    public String getToolTipText() {
        return (String) getClientProperty(TOOL_TIP_TEXT_KEY);
    }

    /** The tool tip text for that point; by default, the same for the whole component. */
    public String getToolTipText(MouseEvent event) {
        return getToolTipText();
    }

    /**
     * The tip the help of this component is shown with.
     *
     * <p>It is overridden in order to return a subclass of {@link JToolTip} when the base tip is
     * not enough. The tip is left pointing at this component, which is where the look and feel
     * takes its typeface and its colours from.
     */
    public JToolTip createToolTip() {
        JToolTip tip = new JToolTip();
        tip.setComponent(this);
        return tip;
    }

    /** Where to show the tip; {@code null} lets whoever shows it choose. */
    public java.awt.Point getToolTipLocation(MouseEvent event) {
        return null;
    }

    // -- banderas varias --------------------------------------------------------------------------

    /**
     * Whether it is painted off screen first and copied whole afterwards.
     *
     * <p>It is kept and reported. With no screen there is no flicker to avoid, so the painting
     * goes straight to the image it is given, which is itself a buffer.
     */
    public void setDoubleBuffered(boolean aFlag) {
        this.doubleBuffered = aFlag;
    }

    public boolean isDoubleBuffered() {
        return this.doubleBuffered;
    }

    /**
     * Whether dragging the mouse outside the component scrolls it. It is kept; a scrolling area
     * uses it.
     */
    public void setAutoscrolls(boolean autoscrolls) {
        this.autoscrolls = autoscrolls;
        this.autoscrollsSet = true;
    }

    public boolean getAutoscrolls() {
        return this.autoscrolls;
    }

    /** The painting debug flags. They are kept; there is no {@code DebugGraphics}. */
    public void setDebugGraphicsOptions(int debugOptions) {
        this.debugGraphicsOptions = debugOptions;
    }

    public int getDebugGraphicsOptions() {
        return this.debugGraphicsOptions;
    }

    /** Whether the component accepts being asked for the focus by program. */
    public void setRequestFocusEnabled(boolean requestFocusEnabled) {
        this.requestFocusEnabled = requestFocusEnabled;
    }

    public boolean isRequestFocusEnabled() {
        return this.requestFocusEnabled;
    }

    /**
     * The menu that appears on right-clicking on this component.
     *
     * <p>Null means "the container's", but only if {@code getInheritsPopupMenu} allows it: see
     * {@link #getComponentPopupMenu}, which resolves both things together.
     */
    public void setComponentPopupMenu(JPopupMenu popup) {
        if (popup != null) {
            enableEvents(java.awt.AWTEvent.MOUSE_EVENT_MASK);
        }
        JPopupMenu oldPopup = (JPopupMenu) getClientProperty(POPUP_MENU_KEY);
        putClientProperty(POPUP_MENU_KEY, popup);
        firePropertyChange("componentPopupMenu", oldPopup, popup);
    }

    /**
     * The menu that corresponds to this component.
     *
     * <p>If it does not have one of its own and it inherits, it asks the parent for it; if it does
     * not inherit, it returns null. It is what makes a whole panel share a menu without setting it
     * on each child, and a child that wants none be able to say so.
     */
    public JPopupMenu getComponentPopupMenu() {
        if (!getInheritsPopupMenu()) {
            return (JPopupMenu) getClientProperty(POPUP_MENU_KEY);
        }
        JPopupMenu popup = (JPopupMenu) getClientProperty(POPUP_MENU_KEY);
        if (popup == null) {
            java.awt.Container parent = getParent();
            while (popup == null) {
                if (parent instanceof JComponent) {
                    popup = ((JComponent) parent).getComponentPopupMenu();
                    return popup;
                }
                if (parent == null) {
                    return null;
                }
                parent = parent.getParent();
            }
        }
        return popup;
    }

    /** The root pane that contains it, or null. */
    public JRootPane getRootPane() {
        return SwingUtilities.getRootPane(this);
    }

    /**
     * Who decides whether this component may let go of the focus.
     *
     * <p>Null -- which is the starting value -- means that it always may. See
     * {@link InputVerifier}, above all the part about a verifier that says no locking the focus
     * in.
     */
    public void setInputVerifier(InputVerifier inputVerifier) {
        InputVerifier old = (InputVerifier) getClientProperty(INPUT_VERIFIER_KEY);
        putClientProperty(INPUT_VERIFIER_KEY, inputVerifier);
        firePropertyChange("inputVerifier", old, inputVerifier);
    }

    /** The verifier, or {@code null} if it has none. */
    public InputVerifier getInputVerifier() {
        return (InputVerifier) getClientProperty(INPUT_VERIFIER_KEY);
    }

    /** Whether the input verifier of the component that loses the focus has to run first. */
    public void setVerifyInputWhenFocusTarget(boolean verifyInputWhenFocusTarget) {
        boolean old = this.verifyInputWhenFocusTarget;
        this.verifyInputWhenFocusTarget = verifyInputWhenFocusTarget;
        firePropertyChange("verifyInputWhenFocusTarget", old, verifyInputWhenFocusTarget);
    }

    public boolean getVerifyInputWhenFocusTarget() {
        return this.verifyInputWhenFocusTarget;
    }

    /** It asks for the focus, whatever {@link #isRequestFocusEnabled} says. */
    public void grabFocus() {
        requestFocus();
    }

    /**
     * It gives the focus to the first descendant that can have it.
     *
     * @deprecated as in the JDK; the focus traversal policy is decided by the
     *     {@code KeyboardFocusManager}, which is not there
     */
    @Deprecated
    public boolean requestDefaultFocus() {
        return false;
    }

    /** The default locale of new components. */
    public static Locale getDefaultLocale() {
        if (defaultLocale == null) {
            defaultLocale = Locale.getDefault();
        }
        return defaultLocale;
    }

    /** It changes the default locale. */
    public static void setDefaultLocale(Locale l) {
        defaultLocale = l;
    }

    // -- ancestor and vetoable listeners ---------------------------------------------------------

    /** It adds a listener of changes in the ancestors. */
    public void addAncestorListener(AncestorListener listener) {
        this.listenerList.add(AncestorListener.class, listener);
    }

    /** It removes an ancestor listener. */
    public void removeAncestorListener(AncestorListener listener) {
        this.listenerList.remove(AncestorListener.class, listener);
    }

    /** The ancestor listeners. */
    public AncestorListener[] getAncestorListeners() {
        return this.listenerList.getListeners(AncestorListener.class);
    }

    /**
     * It adds a listener that may <em>veto</em> a property change.
     *
     * <p>Different from an ordinary listener: this one is consulted first, and if it throws
     * {@link PropertyVetoException} the change does not happen. {@code JInternalFrame} uses it so
     * that somebody may prevent a frame from closing.
     */
    public synchronized void addVetoableChangeListener(VetoableChangeListener listener) {
        if (this.vetoableChangeSupport == null) {
            this.vetoableChangeSupport = new VetoableChangeSupport(this);
        }
        this.vetoableChangeSupport.addVetoableChangeListener(listener);
    }

    /** It removes a vetoable listener. */
    public synchronized void removeVetoableChangeListener(VetoableChangeListener listener) {
        if (this.vetoableChangeSupport != null) {
            this.vetoableChangeSupport.removeVetoableChangeListener(listener);
        }
    }

    /** The vetoable listeners. */
    public synchronized VetoableChangeListener[] getVetoableChangeListeners() {
        if (this.vetoableChangeSupport == null) {
            return new VetoableChangeListener[0];
        }
        return this.vetoableChangeSupport.getVetoableChangeListeners();
    }

    /**
     * It consults the vetoable listeners before a change.
     *
     * @throws PropertyVetoException if one of them objects; the change must not be made
     */
    protected void fireVetoableChange(String propertyName, Object oldValue, Object newValue)
            throws PropertyVetoException {
        if (this.vetoableChangeSupport != null) {
            this.vetoableChangeSupport.fireVetoableChange(propertyName, oldValue, newValue);
        }
    }

    /** Public here, protected in AWT: Swing lets anybody give notice on behalf of a component. */
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    /** Public here, protected in AWT. */
    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * The listeners of a type, Swing's own included.
     *
     * <p>AWT only knows its own; the ancestor and the vetoable ones live here, so they have to be
     * attended before delegating.
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (listenerType == AncestorListener.class) {
            return this.listenerList.getListeners(listenerType);
        }
        if (listenerType == VetoableChangeListener.class) {
            @SuppressWarnings("unchecked")
            T[] r = (T[]) getVetoableChangeListeners();
            return r;
        }
        return super.getListeners(listenerType);
    }

    // -- life cycle in the hierarchy
    // ---------------------------------------------------------------

    /**
     * It enters a hierarchy with a window.
     *
     * <p>Here is where the ancestor listeners are told that there are ancestors. The JDK also
     * registers listeners on each ancestor in order to follow the movements; with no screen there
     * is no movement to follow, so only the entry and the exit are given notice of.
     */
    public void addNotify() {
        super.addNotify();
        Container p = getParent();
        notifyAncestors(AncestorEvent.ANCESTOR_ADDED, p, p == null ? null : p.getParent());
    }

    /** It leaves the hierarchy. */
    public void removeNotify() {
        Container p = getParent();
        notifyAncestors(AncestorEvent.ANCESTOR_REMOVED, p, p == null ? null : p.getParent());
        super.removeNotify();
    }

    private void notifyAncestors(int id, Container ancestor, Container ancestorParent) {
        AncestorListener[] listeners = getAncestorListeners();
        if (listeners.length == 0) {
            return;
        }
        AncestorEvent e = new AncestorEvent(this, id, ancestor, ancestorParent);
        for (int i = 0; i < listeners.length; i++) {
            if (id == AncestorEvent.ANCESTOR_ADDED) {
                listeners[i].ancestorAdded(e);
            } else {
                listeners[i].ancestorRemoved(e);
            }
        }
    }

    // -- state that propagates -----------------------------------------------------------------

    /**
     * It enables or disables, giving notice and repainting: a disabled component looks different.
     */
    public void setEnabled(boolean enabled) {
        boolean old = isEnabled();
        super.setEnabled(enabled);
        firePropertyChange("enabled", old, enabled);
        if (enabled != old) {
            repaint();
        }
    }

    /**
     * It shows or hides; a change asks the parent for a relayout, because it takes up or frees
     * room.
     */
    public void setVisible(boolean aFlag) {
        if (aFlag != isVisible()) {
            super.setVisible(aFlag);
            Container p = getParent();
            if (p != null) {
                p.invalidate();
                p.validate();
                p.repaint();
            }
        }
    }

    public void setForeground(java.awt.Color fg) {
        super.setForeground(fg);
        repaint();
    }

    public void setBackground(java.awt.Color bg) {
        super.setBackground(bg);
        repaint();
    }

    public void setFont(java.awt.Font font) {
        super.setFont(font);
        revalidate();
        repaint();
    }

    /**
     * @deprecated as in the JDK; it stays for compatibility and delegates to AWT
     */
    @Deprecated
    public void reshape(int x, int y, int w, int h) {
        super.reshape(x, y, w, h);
    }

    // -- keyboard ---------------------------------------------------------------------------------

    /**
     * It processes a key: first AWT's listeners, then {@link #processComponentKeyEvent}.
     *
     * <p>The JDK also consults the actions registered by keyboard, which are not there; see the
     * class note.
     */
    protected void processKeyEvent(KeyEvent e) {
        super.processKeyEvent(e);
        if (!e.isConsumed()) {
            processComponentKeyEvent(e);
        }
    }

    /** The hook for a subclass to attend keys without registering listeners. Empty by default. */
    protected void processComponentKeyEvent(KeyEvent e) {
    }

    // -- debugging --------------------------------------------------------------------------------

    protected String paramString() {
        String borderStr = this.border == null ? "" : this.border.toString();
        return super.paramString()
                + ",alignmentX=" + String.valueOf(this.alignmentX)
                + ",alignmentY=" + String.valueOf(this.alignmentY)
                + ",border=" + borderStr
                + ",flags=" + (this.opaque ? "opaque" : "")
                + ",maximumSize=" + (isMaximumSizeSet() ? getMaximumSize().toString() : "")
                + ",minimumSize=" + (isMinimumSizeSet() ? getMinimumSize().toString() : "")
                + ",preferredSize=" + (isPreferredSizeSet() ? getPreferredSize().toString() : "");
    }

    /**
     * It sets a property on behalf of a look and feel: only if the user did not set it before.
     *
     * <p>It is what lies behind {@code LookAndFeel.installProperty}. The properties a look and
     * feel may propose are few and enumerated: "opaque" and "autoscrolls" here, and the
     * subclasses add their own. The JDK also accepts the focus traversal keys, which on this VM
     * there is no way of installing. Each one remembers whether the user set it ({@code *Set});
     * this call respects that and, on finishing, leaves the memory as it was.
     */
    void setUIProperty(String propertyName, Object value) {
        if (customSetUIProperty(propertyName, value)) {
            return;
        }
        if ("opaque".equals(propertyName)) {
            if (!opaqueSet) {
                setOpaque(((Boolean) value).booleanValue());
                opaqueSet = false;
            }
        } else if ("autoscrolls".equals(propertyName)) {
            if (!autoscrollsSet) {
                setAutoscrolls(((Boolean) value).booleanValue());
                autoscrollsSet = false;
            }
        } else {
            throw new IllegalArgumentException("property \"" + propertyName
                    + "\" cannot be set using this method");
        }
    }

    /**
     * The hook for the properties only a subclass understands.
     *
     * <p>{@link JPasswordField} uses it for the echo character. Each one that redefines it has to
     * respect the same rule as {@link #setUIProperty}: if the user has already set that property
     * by hand, the look and feel does not overwrite it.
     *
     * @return `true` if the subclass took charge; `false` to go on with the usual ones
     */
    boolean customSetUIProperty(String propertyName, Object value) {
        return false;
    }

    // ---- keyboard shortcuts ----

    private InputMap[] inputMaps = new InputMap[3];
    private ActionMap actionMap;
    private TransferHandler transferHandler;
    private java.awt.Component nextFocusableComponent;
    private boolean inheritsPopupMenu;

    /**
     * That condition's table of shortcuts.
     *
     * <p>The three conditions are three different tables, not three filters over one: a shortcut
     * that holds with the focus set and one that holds in the whole window do not get in each
     * other's way, and separating them is what allows the look and feel to set its own without
     * overwriting the program's.
     *
     * @throws IllegalArgumentException if the condition is not one of the three.
     */
    public final InputMap getInputMap(int condition) {
        int i = indexForCondition(condition);
        if (inputMaps[i] == null) {
            if (condition == WHEN_IN_FOCUSED_WINDOW) {
                inputMaps[i] = new ComponentInputMap(this);
            } else {
                inputMaps[i] = new InputMap();
            }
        }
        return inputMaps[i];
    }

    /** The usual condition's table: with the focus set. */
    public final InputMap getInputMap() {
        return getInputMap(WHEN_FOCUSED);
    }

    /**
     * It changes that condition's table.
     *
     * @throws IllegalArgumentException if the condition does not exist, or if
     *     {@code WHEN_IN_FOCUSED_WINDOW}'s table is not a {@link ComponentInputMap}.
     */
    public final void setInputMap(int condition, InputMap map) {
        int i = indexForCondition(condition);
        if (condition == WHEN_IN_FOCUSED_WINDOW && map != null
                && !(map instanceof ComponentInputMap)) {
            throw new IllegalArgumentException(
                    "WHEN_IN_FOCUSED_WINDOW InputMaps must be of type ComponentInputMap");
        }
        inputMaps[i] = map;
    }

    private static int indexForCondition(int condition) {
        if (condition == WHEN_FOCUSED) {
            return 0;
        }
        if (condition == WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) {
            return 1;
        }
        if (condition == WHEN_IN_FOCUSED_WINDOW) {
            return 2;
        }
        throw new IllegalArgumentException("condition must be one of "
                + "JComponent.WHEN_IN_FOCUSED_WINDOW, JComponent.WHEN_FOCUSED or "
                + "JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT");
    }

    /** The component's table from name to action. */
    public final ActionMap getActionMap() {
        if (actionMap == null) {
            actionMap = new ActionMap();
        }
        return actionMap;
    }

    public final void setActionMap(ActionMap am) {
        actionMap = am;
    }

    /**
     * It ties a key to a listener, with a name.
     *
     * <p>It is the old way, from before the two maps existed. It goes on working because it is
     * implemented <em>over</em> them: it keeps a wrapper in the input map and the same wrapper as
     * the action. That is why what is registered this way is seen afterwards in
     * {@link #getInputMap}.
     */
    public void registerKeyboardAction(java.awt.event.ActionListener anAction,
            String aCommand, KeyStroke aKeyStroke, int aCondition) {
        InputMap entry = getInputMap(aCondition);
        if (entry != null) {
            ActionMap actions = getActionMap();
            ListenerWrapper wrapper = new ListenerWrapper(anAction, aCommand);
            entry.put(aKeyStroke, wrapper);
            actions.put(wrapper, wrapper);
        }
    }

    public void registerKeyboardAction(java.awt.event.ActionListener anAction,
            KeyStroke aKeyStroke, int aCondition) {
        registerKeyboardAction(anAction, null, aKeyStroke, aCondition);
    }

    /** It removes that key from the three conditions. */
    public void unregisterKeyboardAction(KeyStroke aKeyStroke) {
        ActionMap actions = getActionMap();
        for (int i = 0; i < 3; i++) {
            InputMap entry = inputMaps[i];
            if (entry != null) {
                Object name = entry.get(aKeyStroke);
                entry.remove(aKeyStroke);
                if (name != null && actions != null) {
                    actions.remove(name);
                }
            }
        }
    }

    /** Every tied key, of the three conditions and without repeating. */
    public KeyStroke[] getRegisteredKeyStrokes() {
        java.util.ArrayList<KeyStroke> all = new java.util.ArrayList<KeyStroke>();
        for (int i = 0; i < 3; i++) {
            if (inputMaps[i] != null) {
                KeyStroke[] ks = inputMaps[i].allKeys();
                if (ks != null) {
                    for (int j = 0; j < ks.length; j++) {
                        if (!all.contains(ks[j])) {
                            all.add(ks[j]);
                        }
                    }
                }
            }
        }
        return all.toArray(new KeyStroke[all.size()]);
    }

    /** Which condition that key is tied in, or -1. */
    public int getConditionForKeyStroke(KeyStroke aKeyStroke) {
        for (int i = 0; i < 3; i++) {
            if (inputMaps[i] != null && inputMaps[i].get(aKeyStroke) != null) {
                return conditionForIndex(i);
            }
        }
        return -1;
    }

    private static int conditionForIndex(int i) {
        if (i == 0) {
            return WHEN_FOCUSED;
        }
        if (i == 1) {
            return WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
        }
        return WHEN_IN_FOCUSED_WINDOW;
    }

    /** The listener tied to that key, or null. */
    public java.awt.event.ActionListener getActionForKeyStroke(KeyStroke aKeyStroke) {
        ActionMap actions = getActionMap();
        if (actions == null) {
            return null;
        }
        for (int i = 0; i < 3; i++) {
            if (inputMaps[i] != null) {
                Object name = inputMaps[i].get(aKeyStroke);
                if (name != null) {
                    Action a = actions.get(name);
                    if (a instanceof ListenerWrapper) {
                        return ((ListenerWrapper) a).listener;
                    }
                    return a;
                }
            }
        }
        return null;
    }

    /** It empties the three input tables and the action one. */
    public void resetKeyboardActions() {
        for (int i = 0; i < 3; i++) {
            if (inputMaps[i] != null) {
                inputMaps[i].clear();
            }
        }
        if (actionMap != null) {
            actionMap.clear();
        }
    }

    /**
     * It looks that key up in that condition's table and fires its action.
     *
     * <p>It returns whether it attended it. That result is what keeps the key from going on
     * travelling upwards: a shortcut that was attended must not also reach the parent.
     */
    protected boolean processKeyBinding(KeyStroke ks, java.awt.event.KeyEvent e, int condition,
            boolean pressed) {
        InputMap entry = inputMaps[indexForCondition(condition)];
        ActionMap actions = actionMap;
        if (entry == null || actions == null || !isEnabled()) {
            return false;
        }
        Object name = entry.get(ks);
        if (name == null) {
            return false;
        }
        Action a = actions.get(name);
        if (a == null || !a.isEnabled()) {
            return false;
        }
        a.actionPerformed(new java.awt.event.ActionEvent(this,
                java.awt.event.ActionEvent.ACTION_PERFORMED, ""));
        return true;
    }

    /**
     * An action that wraps a listener, for the old way of registering.
     *
     * <p>It is used as the key and as the value in the action map: that way each registration has
     * a unique key without inventing names that might clash with the look and feel's.
     */
    static class ListenerWrapper extends AbstractAction {

        final java.awt.event.ActionListener listener;
        private final String command;

        ListenerWrapper(java.awt.event.ActionListener listener, String command) {
            this.listener = listener;
            this.command = command;
        }

        public void actionPerformed(java.awt.event.ActionEvent e) {
            if (listener != null) {
                listener.actionPerformed(new java.awt.event.ActionEvent(e.getSource(),
                        e.getID(), command, e.getModifiers()));
            }
        }
    }

    /** Who handles dragging and dropping over this component. */
    public void setTransferHandler(TransferHandler newHandler) {
        TransferHandler oldHandler = transferHandler;
        transferHandler = newHandler;
        firePropertyChange("transferHandler", oldHandler, newHandler);
    }

    public TransferHandler getTransferHandler() {
        return transferHandler;
    }

    /** Whether the component is Swing's and is drawn with no system window of its own. */
    public static boolean isLightweightComponent(java.awt.Component c) {
        return c.isLightweight();
    }

    /**
     * Whether the component attends the tab key itself.
     *
     * @deprecated The focus system decides it, not the component.
     */
    @Deprecated
    public boolean isManagingFocus() {
        return false;
    }

    /**
     * Who to pass the focus on to with the tab key.
     *
     * @deprecated The focus system decides it.
     */
    @Deprecated
    public java.awt.Component getNextFocusableComponent() {
        return nextFocusableComponent;
    }

    /**
     * Who to pass the focus on to with the tab key.
     *
     * @deprecated See {@link #getNextFocusableComponent}.
     */
    @Deprecated
    public void setNextFocusableComponent(java.awt.Component aComponent) {
        java.awt.Component oldValue = nextFocusableComponent;
        nextFocusableComponent = aComponent;
        firePropertyChange("nextFocusableComponent", oldValue, aComponent);
    }

    /** Whether the context menu is inherited from the parent when this one has none. */
    public void setInheritsPopupMenu(boolean value) {
        boolean oldValue = inheritsPopupMenu;
        inheritsPopupMenu = value;
        firePropertyChange("inheritsPopupMenu", oldValue, value);
    }

    public boolean getInheritsPopupMenu() {
        return inheritsPopupMenu;
    }

    /**
     * Where to open the context menu.
     *
     * <p>Null means "where the mouse is". Returning a point serves so that a table row's menu
     * opens on the row and not where the click was made.
     */
    public java.awt.Point getPopupLocation(java.awt.event.MouseEvent event) {
        return null;
    }

    /**
     * It tells the component that one of its shortcut tables changed.
     *
     * <p>{@link ComponentInputMap} calls it. Here there is nothing to redo yet because the
     * per-window shortcut table does not exist; the hook is there so that the table can give
     * notice without knowing whether anybody listens.
     */
    void componentInputMapChanged(ComponentInputMap inputMap) {
    }
}
