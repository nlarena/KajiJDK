package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * It gathers the repainting requests and resolves them all together.
 *
 * <h2>Why it does not repaint at once</h2>
 *
 * <p>Changing a label's text, its colour and its border are three repainting requests for the
 * same rectangle. Attending them one by one would draw the same thing three times. Instead,
 * each request is noted -- {@link #addDirtyRegion} -- and the rectangles of the same component
 * are <em>merged</em>; the drawing happens afterwards, only once, in
 * {@link #paintDirtyRegions}.
 *
 * <p>The same with the measurements: {@link #addInvalidComponent} notes that a layout has to be
 * done again, and {@link #validateInvalidComponents} does it all together.
 *
 * <h2>The double buffer</h2>
 *
 * <p>Drawing straight onto the screen looks like a flicker: first the background, then the
 * text. The double buffer draws into an image in memory and copies the result in one go. The
 * image is shared between every component of a window -- {@link #getOffscreenBuffer} -- because
 * one per component would be a great deal of memory for something that is used for an
 * instant.
 *
 * <h2>With no screen</h2>
 *
 * <p>Everything that is <em>bookkeeping</em> works: the rectangles are noted, merged, listed,
 * cleared. What does not happen is the drawing -- {@link #paintDirtyRegions} walks what is
 * noted and has nowhere to paint it --, nor the queueing on the event thread, which does not
 * run here. A component is painted when somebody passes it a {@code Graphics}, and that does
 * not go through here.
 */
public class RepaintManager {

    boolean doubleBufferingEnabled = true;

    Rectangle tmp = new Rectangle();

    private static RepaintManager delegate;

    private final Map<Component, Rectangle> dirtyComponents =
            new HashMap<Component, Rectangle>();

    private final List<JComponent> invalidComponents = new ArrayList<JComponent>();

    private Dimension doubleBufferMaxSize;

    private Image doubleBuffer;

    /** This context's manager. */
    public static RepaintManager currentManager(Component c) {
        return current();
    }

    /** This context's manager. */
    public static RepaintManager currentManager(JComponent c) {
        return current();
    }

    private static synchronized RepaintManager current() {
        if (delegate == null) {
            delegate = new RepaintManager();
        }
        return delegate;
    }

    /** It changes the manager; null gives back the usual one. */
    public static void setCurrentManager(RepaintManager aRepaintManager) {
        synchronized (RepaintManager.class) {
            delegate = aRepaintManager;
        }
    }

    /** An empty one. */
    public RepaintManager() {
    }

    /**
     * It notes that that component has to be laid out again.
     *
     * <p>The nearest <em>validating ancestor</em> is noted -- the first that can resolve a change
     * of size without troubling its parent --, not the component. It is what keeps changing a
     * label's text from rebuilding the whole window.
     */
    public synchronized void addInvalidComponent(JComponent invalidComponent) {
        Component validateRoot = null;
        for (Component c = invalidComponent; c != null; c = c.getParent()) {
            if (c instanceof java.awt.CellRendererPane) {
                return;
            }
            if (c instanceof JComponent && ((JComponent) c).isValidateRoot()) {
                validateRoot = c;
                break;
            }
        }
        if (validateRoot == null) {
            return;
        }
        for (Component c = validateRoot; c != null; c = c.getParent()) {
            if (!c.isVisible() || !c.isDisplayable()) {
                return;
            }
            if (c instanceof Window || c instanceof java.applet.Applet) {
                break;
            }
        }
        if (invalidComponents.contains(validateRoot)) {
            return;
        }
        invalidComponents.add((JComponent) validateRoot);
    }

    /** It takes it off the list of those that have to be laid out. */
    public synchronized void removeInvalidComponent(JComponent component) {
        invalidComponents.remove(component);
    }

    /**
     * It notes that that rectangle of that component has to be repainted.
     *
     * <p>If one was already noted, the two are merged into the rectangle that contains them.
     * Merging instead of keeping both is the decision that makes this class cheap: the union may
     * paint too much, but never too little, and painting too much is only slower.
     */
    public void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
        record(c, x, y, w, h);
    }

    /** The same, for a window. */
    public void addDirtyRegion(Window window, int x, int y, int w, int h) {
        record(window, x, y, w, h);
    }

    /**
     * The same, for an applet.
     *
     * @deprecated As in the JDK: applets no longer run anywhere.
     */
    @Deprecated
    public void addDirtyRegion(java.applet.Applet applet, int x, int y, int w, int h) {
        record(applet, x, y, w, h);
    }

    private synchronized void record(Component c, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0 || c == null) {
            return;
        }
        if (c.getWidth() <= 0 || c.getHeight() <= 0) {
            return;
        }
        Rectangle r = dirtyComponents.get(c);
        if (r != null) {
            // It was already noted: it is enlarged and that is that, without looking at the
            // ancestors again.
            merge(r, x, y, w, h);
            return;
        }
        if (!isShowing(c)) {
            return;
        }
        dirtyComponents.put(c, new Rectangle(x, y, w, h));
    }

    /**
     * Whether that component is really on the screen.
     *
     * <p>Noting what is not seen is of no use: when the component appears it will have to be
     * painted whole all the same. So if the component has no parent, or some of its ancestors is
     * hidden or does not have a window yet, the note is discarded.
     *
     * <p>This is what makes {@link #getDirtyRegion} always return the empty rectangle and
     * {@link #isCompletelyDirty} always `false` in a library with no screen. It is measured
     * against the JDK, which does exactly the same.
     */
    private static boolean isShowing(Component c) {
        Container parent = c.getParent();
        if (parent == null) {
            return false;
        }
        for (Container p = parent; p != null; p = p.getParent()) {
            if (!p.isVisible() || !p.isDisplayable()) {
                return false;
            }
            if (p instanceof Window) {
                break;
            }
        }
        return true;
    }

    /** It enlarges the rectangle so that it also contains the new one. */
    private static void merge(Rectangle r, int x, int y, int w, int h) {
        int x1 = Math.min(r.x, x);
        int y1 = Math.min(r.y, y);
        int x2 = Math.max(r.x + r.width, x + w);
        int y2 = Math.max(r.y + r.height, y + h);
        r.x = x1;
        r.y = y1;
        r.width = x2 - x1;
        r.height = y2 - y1;
    }

    /** What is noted to be repainted of that component; empty if there is nothing. */
    public Rectangle getDirtyRegion(JComponent aComponent) {
        Rectangle r;
        synchronized (this) {
            r = dirtyComponents.get(aComponent);
        }
        if (r == null) {
            return new Rectangle(0, 0, 0, 0);
        }
        return new Rectangle(r);
    }

    /** It notes that the whole component has to be repainted. */
    public void markCompletelyDirty(JComponent aComponent) {
        addDirtyRegion(aComponent, 0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /** It forgets what was noted of that component. */
    public void markCompletelyClean(JComponent aComponent) {
        synchronized (this) {
            dirtyComponents.remove(aComponent);
        }
    }

    /**
     * Whether it has to be repainted whole.
     *
     * <p>It is answered by looking at the width of the noted rectangle:
     * {@link Integer#MAX_VALUE} is the mark for "everything". It is what allows
     * {@link #markCompletelyDirty} not to need a separate field.
     */
    public boolean isCompletelyDirty(JComponent aComponent) {
        Rectangle r = getDirtyRegion(aComponent);
        return (r.width == Integer.MAX_VALUE) && (r.height == Integer.MAX_VALUE);
    }

    /** It lays out everything that was left pending and clears the list. */
    public void validateInvalidComponents() {
        List<JComponent> ic;
        synchronized (this) {
            if (invalidComponents.isEmpty()) {
                return;
            }
            ic = new ArrayList<JComponent>(invalidComponents);
            invalidComponents.clear();
        }
        for (int i = 0; i < ic.size(); i++) {
            ic.get(i).validate();
        }
    }

    /**
     * It repaints everything that was noted and clears the list.
     *
     * <p>With no screen there is nowhere to paint; see the class note. What does happen is that
     * the list is emptied, which is what the rest of the system observes.
     */
    public void paintDirtyRegions() {
        Map<Component, Rectangle> tmpDirtyComponents;
        synchronized (this) {
            if (dirtyComponents.isEmpty()) {
                return;
            }
            tmpDirtyComponents = new HashMap<Component, Rectangle>(dirtyComponents);
            dirtyComponents.clear();
        }
        java.util.Iterator<Component> it = tmpDirtyComponents.keySet().iterator();
        while (it.hasNext()) {
            Component c = it.next();
            Rectangle r = tmpDirtyComponents.get(c);
            if (c instanceof JComponent && r != null) {
                ((JComponent) c).paintImmediately(r.x, r.y, r.width, r.height);
            }
        }
    }

    public synchronized String toString() {
        StringBuilder sb = new StringBuilder();
        if (!dirtyComponents.isEmpty()) {
            sb.append("DirtyComponents: ").append(dirtyComponents).append("\n");
        }
        if (!invalidComponents.isEmpty()) {
            sb.append("InvalidComponents: ").append(invalidComponents);
        }
        return sb.toString();
    }

    /**
     * The shared image that is drawn into before copying; see the class note.
     *
     * <p>It is enlarged when needed and never shrunk: shrinking it would force another to be
     * created the next time the large size was needed, and there are few different sizes in a
     * window's life.
     */
    public Image getOffscreenBuffer(Component c, int proposedWidth, int proposedHeight) {
        return buffer(c, proposedWidth, proposedHeight);
    }

    /**
     * Like {@link #getOffscreenBuffer}, but asking for the card's memory if there were any.
     *
     * <p>Here there is none, so it returns the same image.
     */
    public Image getVolatileOffscreenBuffer(Component c, int proposedWidth,
            int proposedHeight) {
        return buffer(c, proposedWidth, proposedHeight);
    }

    private synchronized Image buffer(Component c, int proposedWidth, int proposedHeight) {
        Dimension maxSize = getDoubleBufferMaximumSize();
        int width = Math.min(proposedWidth, maxSize.width);
        int height = Math.min(proposedHeight, maxSize.height);
        width = Math.max(1, width);
        height = Math.max(1, height);
        if (doubleBuffer != null) {
            int w = doubleBuffer.getWidth(null);
            int h = doubleBuffer.getHeight(null);
            if (w >= width && h >= height) {
                return doubleBuffer;
            }
            width = Math.max(width, w);
            height = Math.max(height, h);
        }
        doubleBuffer = new java.awt.image.BufferedImage(width, height,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        return doubleBuffer;
    }

    /** How much the shared image may grow. */
    public void setDoubleBufferMaximumSize(Dimension d) {
        doubleBufferMaxSize = d;
        if (doubleBuffer != null) {
            if (doubleBuffer.getWidth(null) > d.width
                    || doubleBuffer.getHeight(null) > d.height) {
                doubleBuffer = null;
            }
        }
    }

    /**
     * The cap.
     *
     * <p>By default, what every screen together takes up: there is no point in keeping a buffer
     * larger than everything that can be shown. With no screens there is no such cap, and the
     * value is {@link Integer#MAX_VALUE} on both sides; that is how the JDK does it and it is
     * measured.
     */
    public Dimension getDoubleBufferMaximumSize() {
        if (doubleBufferMaxSize == null) {
            try {
                java.awt.Rectangle all = new java.awt.Rectangle();
                java.awt.GraphicsEnvironment ge =
                        java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
                java.awt.GraphicsDevice[] screens = ge.getScreenDevices();
                for (int i = 0; i < screens.length; i++) {
                    all = all.union(
                            screens[i].getDefaultConfiguration().getBounds());
                }
                doubleBufferMaxSize = new Dimension(all.width, all.height);
            } catch (java.awt.HeadlessException e) {
                doubleBufferMaxSize = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
            }
        }
        return doubleBufferMaxSize;
    }

    /** It switches the double buffer on or off for the whole application. */
    public void setDoubleBufferingEnabled(boolean aFlag) {
        doubleBufferingEnabled = aFlag;
    }

    public boolean isDoubleBufferingEnabled() {
        return doubleBufferingEnabled;
    }
}
