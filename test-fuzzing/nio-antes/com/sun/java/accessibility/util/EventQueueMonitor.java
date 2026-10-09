package com.sun.java.accessibility.util;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.accessibility.Accessible;

/**
 * The global observer of AWT's event queue: it knows what windows there are and where the
 * mouse is.
 *
 * <h2>How it can know that without anybody registering it</h2>
 *
 * <p>With {@code Toolkit.addAWTEventListener}, which is a hook to the event queue <strong>of
 * the whole process</strong>. Each AWT event goes through here before arriving at its
 * component.
 *
 * <p>It is a big privilege and that is why it is protected: in the JDK it requires a permission.
 * What it buys is the only thing that makes an assistive technology possible -- finding out
 * about an application that was not written in order to collaborate with it.
 *
 * <h2>All static, and why</h2>
 *
 * <p>Because there is a single event queue per process. Two monitors would be two hooks to the
 * same flow, duplicating each notification. The class may be instantiated -- the JDK leaves the
 * constructor public -- but the state is a single one.
 *
 * <h2>On this VM</h2>
 *
 * <p>The hook is installed all the same, and the queries answer what the state has. Since this
 * VM does not run a real graphical interface, that state is left empty:
 * {@link #getTopLevelWindows} returns an array with no elements and {@link #isGUIInitialized}
 * answers {@code false}. It is not a stub -- it is the mechanism working over a desktop that
 * does not exist.
 */
public class EventQueueMonitor implements AWTEventListener {

    private static final List<Window> windows = new ArrayList<Window>();
    private static final List<GUIInitializedListener> guiListeners =
            new ArrayList<GUIInitializedListener>();
    private static final List<TopLevelWindowListener> windowListeners =
            new ArrayList<TopLevelWindowListener>();

    private static Window focused;
    private static Point mousePosition;
    private static boolean guiInitialized = false;
    private static boolean hooked = false;

    public EventQueueMonitor() {
    }

    /**
     * It installs the hook to the event queue, if it was not there.
     *
     * <p>It is called by every method that needs state, instead of being done in a static
     * initializer: loading this class should not hook anything on its own.
     */
    public static void maybeInitialize() {
        synchronized (EventQueueMonitor.class) {
            if (hooked) {
                return;
            }
            hooked = true;
        }
        try {
            java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(
                    new EventQueueMonitor(), AWTEvent.WINDOW_EVENT_MASK
                    | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
                    | AWTEvent.COMPONENT_EVENT_MASK);
        } catch (RuntimeException e) {
            // With no desktop there is nothing to hook on to, and that is not an error: the
                            // queries are simply going to answer empty. Letting the exception go up
                            // would make loading this class blow up in an environment with no
                            // graphical interface.
            return;
        }
    }

    /** It receives each AWT event of the process. */
    public void eventDispatched(AWTEvent theEvent) {
        if (theEvent instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) theEvent;
            synchronized (EventQueueMonitor.class) {
                mousePosition = new Point(me.getX(), me.getY());
            }
        }
        if (theEvent.getSource() instanceof Window) {
            Window w = (Window) theEvent.getSource();
            int id = theEvent.getID();
            if (id == java.awt.event.WindowEvent.WINDOW_OPENED) {
                addWindow(w);
            } else if (id == java.awt.event.WindowEvent.WINDOW_CLOSED) {
                removeWindow(w);
            } else if (id == java.awt.event.WindowEvent.WINDOW_ACTIVATED) {
                synchronized (EventQueueMonitor.class) {
                    focused = w;
                }
            }
        }
    }

    private static void addWindow(Window w) {
        List<TopLevelWindowListener> toNotify;
        boolean first = false;
        synchronized (EventQueueMonitor.class) {
            if (windows.contains(w)) {
                return;
            }
            windows.add(w);
            if (!guiInitialized) {
                guiInitialized = true;
                first = true;
            }
            toNotify = new ArrayList<TopLevelWindowListener>(windowListeners);
        }
        // The notices go OUTSIDE the synchronized block: a listener that called this class
                // again from its own thread would jam with the lock taken.
        if (first) {
            notifyGuiInitialized();
        }
        for (int i = 0; i < toNotify.size(); i++) {
            toNotify.get(i).topLevelWindowCreated(w);
        }
    }

    private static void removeWindow(Window w) {
        List<TopLevelWindowListener> toNotify;
        synchronized (EventQueueMonitor.class) {
            if (!windows.remove(w)) {
                return;
            }
            if (focused == w) {
                focused = null;
            }
            toNotify = new ArrayList<TopLevelWindowListener>(windowListeners);
        }
        for (int i = 0; i < toNotify.size(); i++) {
            toNotify.get(i).topLevelWindowDestroyed(w);
        }
    }

    private static void notifyGuiInitialized() {
        List<GUIInitializedListener> toNotify;
        synchronized (EventQueueMonitor.class) {
            toNotify = new ArrayList<GUIInitializedListener>(guiListeners);
            guiListeners.clear();
        }
        for (int i = 0; i < toNotify.size(); i++) {
            toNotify.get(i).guiInitialized();
        }
    }

    /**
     * The accessible object that is at that point of the screen, or {@code null}.
     *
     * <p>It is the central query of an assistive technology: "what is there under the cursor".
     */
    public static Accessible getAccessibleAt(Point p) {
        maybeInitialize();
        Window[] ws = getTopLevelWindows();
        for (int i = 0; i < ws.length; i++) {
            Component c = componentAt(ws[i], p);
            if (c instanceof Accessible) {
                return (Accessible) c;
            }
        }
        return null;
    }

    /** The deepest visible component that contains that point. */
    private static Component componentAt(Container c, Point p) {
        if (c == null || !c.isShowing() || !c.getBounds().contains(p)) {
            return null;
        }
        Component[] children = c.getComponents();
        // From the front towards the back: the first one that contains the point is the one
                // that is seen.
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof Container) {
                Component found = componentAt((Container) children[i], p);
                if (found != null) {
                    return found;
                }
            } else if (children[i].isShowing() && children[i].getBounds().contains(p)) {
                return children[i];
            }
        }
        return c;
    }

    /** Whether some window has already appeared. */
    public static boolean isGUIInitialized() {
        maybeInitialize();
        synchronized (EventQueueMonitor.class) {
            return guiInitialized;
        }
    }

    /**
     * It gives notice when the graphical interface appears.
     *
     * <p>If it has already appeared, the notice arrives <strong>at once</strong> and the listener
     * is not kept: it is an event that happens only once, and keeping it for a notice that is not
     * going to be repeated would be a leak.
     */
    public static void addGUIInitializedListener(GUIInitializedListener l) {
        maybeInitialize();
        boolean already;
        synchronized (EventQueueMonitor.class) {
            already = guiInitialized;
            if (!already) {
                guiListeners.add(l);
            }
        }
        if (already) {
            l.guiInitialized();
        }
    }

    /** It takes an initialization listener out. */
    public static void removeGUIInitializedListener(GUIInitializedListener l) {
        synchronized (EventQueueMonitor.class) {
            guiListeners.remove(l);
        }
    }

    /** It gives notice when a top-level window appears or disappears. */
    public static void addTopLevelWindowListener(TopLevelWindowListener l) {
        maybeInitialize();
        synchronized (EventQueueMonitor.class) {
            windowListeners.add(l);
        }
    }

    /** It takes a window listener out. */
    public static void removeTopLevelWindowListener(TopLevelWindowListener l) {
        synchronized (EventQueueMonitor.class) {
            windowListeners.remove(l);
        }
    }

    /** Where the mouse was the last time it was seen, or {@code null}. */
    public static Point getCurrentMousePosition() {
        maybeInitialize();
        synchronized (EventQueueMonitor.class) {
            return mousePosition == null ? null : new Point(mousePosition);
        }
    }

    /** The top-level windows there are now. */
    public static Window[] getTopLevelWindows() {
        maybeInitialize();
        synchronized (EventQueueMonitor.class) {
            return windows.toArray(new Window[windows.size()]);
        }
    }

    /** The one that has the focus, or {@code null}. */
    public static Window getTopLevelWindowWithFocus() {
        maybeInitialize();
        synchronized (EventQueueMonitor.class) {
            return focused;
        }
    }
}
