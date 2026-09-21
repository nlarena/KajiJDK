package java.awt;

import java.awt.image.BufferStrategy;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * A blank rectangle to draw on.
 *
 * <p>It is the component used when none of the others will do: one inherits from it and overrides
 * {@link #paint} with whatever has to be shown. A game, a chart, an image viewer.
 *
 * <p><strong>Without a screen it draws nothing</strong>, and it cannot: {@link #paint} receives a
 * {@link Graphics} and this implementation has no rasteriser. What does work is everything else
 * —the size, the position, the events, the focus— so a canvas serves perfectly well as a leaf of
 * the component tree even though it never gets shown.
 */
public class Canvas extends Component implements Accessible {

    private static final long serialVersionUID = -2284879212465893870L;

    private static int canvasCounter = 0;

    /** A canvas. */
    public Canvas() {
    }

    /**
     * A canvas on that graphics configuration.
     *
     * @throws NullPointerException if the configuration is `null`
     */
    public Canvas(GraphicsConfiguration config) {
        this();
        this.setGraphicsConfiguration(config);
    }

    void setGraphicsConfiguration(GraphicsConfiguration gc) {
        if (gc == null) {
            throw new NullPointerException("config");
        }
    }

    String constructComponentName() {
        synchronized (Canvas.class) {
            String n = "canvas" + canvasCounter;
            canvasCounter = canvasCounter + 1;
            return n;
        }
    }

    /** Declares it showable. */
    public void addNotify() {
        super.addNotify();
    }

    /**
     * Draws the canvas.
     *
     * <p>The base one paints it in the background colour. Here it does nothing, because there is
     * nothing to do it with: without a rasteriser there is no {@link Graphics} that can fill a
     * rectangle. A subclass that overrides this in an environment with a screen keeps working just
     * the same.
     */
    public void paint(Graphics g) {
    }

    /** Repaints; since {@link #paint} draws nothing, this does not either. */
    public void update(Graphics g) {
        this.paint(g);
    }

    boolean postsOldMouseEvents() {
        return true;
    }

    /**
     * Builds a double-buffering mechanism.
     *
     * @throws IllegalStateException always: double buffering is a chain of drawing surfaces, and
     *     without a screen there is none to chain. Inventing one that does not draw would be worse
     *     than not having it.
     */
    public void createBufferStrategy(int numBuffers) {
        throw new IllegalStateException(
                "without a screen there are no drawing surfaces to chain");
    }

    /**
     * Builds a double-buffering mechanism with those capabilities.
     *
     * @throws IllegalStateException always, for the same reason as {@link
     *     #createBufferStrategy(int)}
     */
    public void createBufferStrategy(int numBuffers, BufferCapabilities caps)
            throws AWTException {
        throw new IllegalStateException(
                "without a screen there are no drawing surfaces to chain");
    }

    /**
     * The double-buffering mechanism.
     *
     * @return `null` always: none could ever be created
     */
    public BufferStrategy getBufferStrategy() {
        return null;
    }

    /** The accessibility information of this canvas. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTCanvas();
        }
        return this.accessibleContext;
    }

    /** A canvas, for accessibility, is a canvas. */
    protected class AccessibleAWTCanvas extends AccessibleAWTComponent {

        /** For the subclasses. */
        protected AccessibleAWTCanvas() {
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.CANVAS;
        }
    }
}
