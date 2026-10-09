package jdk.swing.interop;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.Window;

/**
 * The four loose things the bridge to another toolkit needs from inside AWT.
 *
 * <h2>Grabbing the mouse</h2>
 *
 * <p>{@link #grab} is what a popup menu does when it opens: it asks for every mouse event to reach
 * it, including the ones that land on another window. Without that, clicking outside would not close
 * the menu, because the click would go to the window underneath and the menu would never hear about
 * it.
 *
 * <p>{@link #isUngrabEvent} is the other half: it recognizes the event the system uses to say that
 * grab was lost, so the menu closes by itself.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>{@link #postEvent} works: it queues the event on the system queue. {@link #grab} and
 * {@link #ungrab} do nothing, which is exactly what they do in the JDK when the toolkit is not
 * Sun's -- grabbing the mouse is a windowing-system operation, and there is no windowing system
 * here. {@link #isUngrabEvent} always answers false for the same reason: that event is made by the
 * system, and here nobody makes it.
 *
 * @since 9
 */
public class SwingInterOpUtils {

    /**
     * The mark of the event the system uses to say the mouse grab was lost.
     *
     * <p>It is the topmost bit of the integer, the only one still free: AWT's event masks took all
     * the others.
     */
    public static final int GRAB_EVENT_MASK = 0x80000000;

    /** One. */
    public SwingInterOpUtils() {
    }

    /**
     * Queues the event.
     *
     * <p>The first argument is the application context to send it to. This implementation has a
     * single event queue, so there is no other context to send it to and the argument is ignored.
     *
     * @param targetAppContext which application context, or {@code null}
     * @param event the event, or {@code null} to do nothing
     */
    public static void postEvent(Object targetAppContext, AWTEvent event) {
        if (event == null) {
            return;
        }
        final EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
        if (queue != null) {
            queue.postEvent(event);
        }
    }

    /**
     * Makes every mouse event go to that window.
     *
     * @param toolkit the toolkit
     * @param w the window that takes the events
     */
    public static void grab(Toolkit toolkit, Window w) {
    }

    /**
     * Gives the mouse events back to whoever they belong to.
     *
     * @param toolkit the toolkit
     * @param w the window that had them
     */
    public static void ungrab(Toolkit toolkit, Window w) {
    }

    /**
     * Whether that is the event the system uses to say the mouse grab was lost.
     *
     * @param ev the event
     * @return always false: that event is made by the windowing system, and there is none
     */
    public static boolean isUngrabEvent(AWTEvent ev) {
        return false;
    }
}
