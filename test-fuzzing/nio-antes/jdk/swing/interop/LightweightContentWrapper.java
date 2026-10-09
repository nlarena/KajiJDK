package jdk.swing.interop;

import java.awt.Component;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.InvalidDnDOperationException;

import javax.swing.JComponent;

/**
 * What the other toolkit has to know how to do in order to host Swing.
 *
 * <h2>How Swing gets embedded into something that is not AWT</h2>
 *
 * <p>It does not get embedded: it is drawn apart and copied. Swing paints onto an array of pixels in
 * memory -- {@link #imageBufferReset} hands it over, {@link #imageUpdated} says which part changed --
 * and the other toolkit shows it wherever it likes. No system window changes owner, which is what
 * makes this work on any platform.
 *
 * <p>{@link #paintLock} and {@link #paintUnlock} exist because both sides touch that same array from
 * different threads: one writes it when repainting, the other reads it when showing.
 *
 * <h2>The sizes</h2>
 *
 * <p>A Swing component knows how big it wants to be, but the one who decides is the container, which
 * here is on the other side. The three size notices -- {@link #preferredSizeChanged} and the other
 * two -- are how that preference crosses the border.
 *
 * <h2>Focus</h2>
 *
 * <p>{@link #focusGrabbed} and {@link #focusUngrabbed} are for popup menus: while one is open it
 * takes every mouse event, including the ones that land outside, because clicking outside has to
 * close it.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>Every method is abstract, in the JDK too: this class is what the host implements, not what it
 * is handed.
 *
 * @since 9
 */
public abstract class LightweightContentWrapper {

    /** One. */
    public LightweightContentWrapper() {
    }

    /**
     * Hands over the array of pixels Swing is going to paint onto.
     *
     * @param data the pixels
     * @param width the width in pixels
     * @param height the height in pixels
     * @param linestride how many ints there are from one row to the next
     * @param bufferWidth the width of the array, which may be larger than the visible one
     * @param bufferHeight the height of the array
     */
    public abstract void imageBufferReset(int[] data, int width, int height, int linestride,
            int bufferWidth, int bufferHeight);

    /**
     * The same, with the screen's scale.
     *
     * <p>The scale arrives separately because the size in pixels and the size in points stopped
     * being the same number: on a screen at twice the density the array is twice the component.
     *
     * @param data the pixels
     * @param width the width in pixels
     * @param height the height in pixels
     * @param linestride how many ints there are from one row to the next
     * @param bufferWidth the width of the array
     * @param bufferHeight the height of the array
     * @param scaleX the screen's horizontal scale
     * @param scaleY the vertical scale
     */
    public abstract void imageBufferReset(int[] data, int width, int height, int linestride,
            int bufferWidth, int bufferHeight, double scaleX, double scaleY);

    /**
     * The Swing component being hosted.
     *
     * @return the component
     */
    public abstract JComponent getComponent();

    /** Takes the array of pixels; nobody else touches it until {@link #paintUnlock}. */
    public abstract void paintLock();

    /** Releases the array of pixels. */
    public abstract void paintUnlock();

    /**
     * Says the component changed size or place.
     *
     * @param x the left corner
     * @param y the top corner
     * @param width the width
     * @param height the height
     */
    public abstract void imageReshaped(int x, int y, int width, int height);

    /**
     * Says that part of the array changed and has to be shown again.
     *
     * @param x the left corner
     * @param y the top corner
     * @param width the width
     * @param height the height
     */
    public abstract void imageUpdated(int x, int y, int width, int height);

    /** Says a popup menu took every mouse event. */
    public abstract void focusGrabbed();

    /** Says it gave them back. */
    public abstract void focusUngrabbed();

    /**
     * Says how big the component would like to be.
     *
     * @param width the preferred width
     * @param height the preferred height
     */
    public abstract void preferredSizeChanged(int width, int height);

    /**
     * Says the most it can be.
     *
     * @param width the maximum width
     * @param height the maximum height
     */
    public abstract void maximumSizeChanged(int width, int height);

    /**
     * Says the least it can be.
     *
     * @param width the minimum width
     * @param height the minimum height
     */
    public abstract void minimumSizeChanged(int width, int height);

    /**
     * A drag gesture recognizer of the requested kind.
     *
     * <p>The host makes it because the host is the one who detects the gesture, with its own mouse
     * events: AWT's never arrive.
     *
     * @param <T> the kind of recognizer
     * @param abstractRecognizerClass which kind of recognizer is asked for
     * @param ds the drag source it will report to
     * @param actor the component being watched
     * @param srcActions the actions the source allows
     * @param dgl whom to tell when the gesture happens
     * @return the recognizer
     */
    public abstract <T extends DragGestureRecognizer> T createDragGestureRecognizer(
            Class<T> abstractRecognizerClass, DragSource ds, Component actor, int srcActions,
            DragGestureListener dgl);

    /**
     * The source side of a drag that started with that gesture.
     *
     * @param dge the gesture
     * @return the source's wrapper
     * @throws InvalidDnDOperationException if a drag is already running
     */
    public abstract DragSourceContextWrapper createDragSourceContext(DragGestureEvent dge)
            throws InvalidDnDOperationException;

    /**
     * Registers a drop target.
     *
     * @param dt the target
     */
    public abstract void addDropTarget(DropTarget dt);

    /**
     * Takes it out.
     *
     * @param dt the target
     */
    public abstract void removeDropTarget(DropTarget dt);
}
