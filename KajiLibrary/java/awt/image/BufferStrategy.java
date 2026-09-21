package java.awt.image;

import java.awt.BufferCapabilities;
import java.awt.Graphics;

/**
 * How the drawing buffers of a surface are handled.
 *
 * <p>It solves the flicker. Drawing straight over what is being shown lets the building of the
 * frame be seen; with several buffers one draws into a hidden one and shows it whole at once, with
 * {@link #show}.
 *
 * <p>The two "contents" methods tell two different misfortunes apart. {@link #contentsLost} says
 * that what was drawn **did not get** to be shown; {@link #contentsRestored} says that the buffer
 * was recovered but was left empty and the frame has to be redone. As in {@link VolatileImage}, the
 * right loop consults them after showing and not before.
 */
public abstract class BufferStrategy {

    /** For the subclasses. */
    protected BufferStrategy() {
    }

    /** Which buffers there are and what can be done with them. */
    public abstract BufferCapabilities getCapabilities();

    /**
     * A context to draw into the hidden buffer.
     *
     * <p>A new one has to be asked for per frame and released when finishing: the buffer that was
     * hidden comes into view at every {@link #show}.
     */
    public abstract Graphics getDrawGraphics();

    /** Whether what was drawn since the last call was lost without getting to be shown. */
    public abstract boolean contentsLost();

    /** Whether the buffer was recovered empty and the frame has to be drawn again. */
    public abstract boolean contentsRestored();

    /** Shows the hidden buffer. */
    public abstract void show();

    /**
     * Releases the resources.
     *
     * <p>It does nothing here: a strategy with no resources of its own has nothing to release.
     */
    public void dispose() {
    }
}
