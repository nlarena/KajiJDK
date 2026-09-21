package java.awt;

import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Set;

/**
 * The focus manager AWT comes with.
 *
 * <p>It puts behaviour on the abstract methods of {@link KeyboardFocusManager}: it dispatches the
 * keyboard events through the chain of dispatchers, handles the traversal keys, and holds the
 * events that arrive while the focus is in transit so as to deliver them to the right component.
 *
 * <p>The queue of events in transit is the least obvious part and the most necessary. When the
 * focus is requested, the answer of the windowing system takes a while; keys can arrive in the
 * meantime. Delivering them to the one that still has the focus would be writing into the field
 * that is about to be left. That is why {@link #enqueueKeyEvents} sets them aside until {@link
 * #dequeueKeyEvents} says the focus has arrived.
 *
 * <p>Without a windowing system that transit never happens —nobody really asks for the focus— so
 * the queue stays empty. The mechanism is all there anyway: if something calls `enqueueKeyEvents`,
 * the events are held, and `dequeueKeyEvents` releases them.
 */
public class DefaultKeyboardFocusManager extends KeyboardFocusManager {

    /** An event set aside waiting for the focus to reach its addressee. */
    private static final class HeldEvents {
        private final long timestamp;
        private final Component target;
        private final ArrayList<KeyEvent> events = new ArrayList<KeyEvent>();

        private HeldEvents(long timestamp, Component target) {
            this.timestamp = timestamp;
            this.target = target;
        }
    }

    /** The stretches set aside, in the order they were asked for. */
    private final ArrayList<HeldEvents> held = new ArrayList<HeldEvents>();

    /** A default manager. */
    public DefaultKeyboardFocusManager() {
    }

    /**
     * Dispatches the event to whichever component it belongs to.
     *
     * <p>The focus and window events **update the global state** besides being delivered: it is
     * here that the manager learns the focus moved. The rest are delivered and that is all.
     *
     * @return `true` if it delivered it
     */
    public boolean dispatchEvent(AWTEvent e) {
        int id = e.getID();
        if (e instanceof WindowEvent && e.getSource() instanceof Window) {
            Window w = (Window) e.getSource();
            if (id == WindowEvent.WINDOW_GAINED_FOCUS) {
                this.setGlobalFocusedWindow(w);
            } else if (id == WindowEvent.WINDOW_LOST_FOCUS) {
                if (this.getGlobalFocusedWindow() == w) {
                    this.setGlobalFocusedWindow(null);
                    this.setGlobalFocusOwner(null);
                }
            } else if (id == WindowEvent.WINDOW_ACTIVATED) {
                this.setGlobalActiveWindow(w);
            } else if (id == WindowEvent.WINDOW_DEACTIVATED) {
                if (this.getGlobalActiveWindow() == w) {
                    this.setGlobalActiveWindow(null);
                }
            }
            this.redispatchEvent(w, e);
            return true;
        }
        if (e instanceof FocusEvent && e.getSource() instanceof Component) {
            FocusEvent fe = (FocusEvent) e;
            Component c = (Component) e.getSource();
            if (id == FocusEvent.FOCUS_GAINED) {
                this.setGlobalFocusOwner(c);
                if (!fe.isTemporary()) {
                    this.setGlobalPermanentFocusOwner(c);
                }
            } else if (id == FocusEvent.FOCUS_LOST && this.getGlobalFocusOwner() == c) {
                this.setGlobalFocusOwner(null);
                if (!fe.isTemporary()) {
                    this.setGlobalPermanentFocusOwner(null);
                }
            }
            this.redispatchEvent(c, e);
            return true;
        }
        if (e instanceof KeyEvent) {
            if (this.dispatchKeyEvent((KeyEvent) e)) {
                return true;
            }
            this.postProcessKeyEvent((KeyEvent) e);
            return true;
        }
        if (e.getSource() instanceof Component) {
            this.redispatchEvent((Component) e.getSource(), e);
            return true;
        }
        return false;
    }

    /**
     * Dispatches a keyboard event through the chain of dispatchers and then to the one with the
     * focus.
     *
     * @return `true` if someone consumed it
     */
    public boolean dispatchKeyEvent(KeyEvent e) {
        java.util.List<KeyEventDispatcher> ds = this.getKeyEventDispatchers();
        if (ds != null) {
            for (int i = 0; i < ds.size(); i++) {
                if (ds.get(i).dispatchKeyEvent(e)) {
                    return true;
                }
            }
        }
        Component target = this.getFocusOwner();
        if (target == null && e.getSource() instanceof Component) {
            target = (Component) e.getSource();
        }
        if (target == null) {
            return false;
        }
        // Transit wins: if there is a stretch set aside waiting, the key is held instead of being
        // delivered. If not, the traversal keys are handled first and only then is it delivered.
        synchronized (this) {
            if (!this.held.isEmpty()) {
                this.held.get(0).events.add(e);
                return true;
            }
        }
        this.processKeyEvent(target, e);
        if (e.isConsumed()) {
            return true;
        }
        this.redispatchEvent(target, e);
        return e.isConsumed();
    }

    /**
     * Gives the event to the post-processors.
     *
     * @return `true` if any of them consumed it
     */
    public boolean postProcessKeyEvent(KeyEvent e) {
        java.util.List<KeyEventPostProcessor> ps = this.getKeyEventPostProcessors();
        if (ps != null) {
            for (int i = 0; i < ps.size(); i++) {
                if (ps.get(i).postProcessKeyEvent(e)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Handles the traversal keys of that component.
     *
     * <p>If the key is one of the four traversal ones, it moves the focus and **consumes the
     * event**: if it did not consume it, the Tab would also reach the component and write a
     * tabulation.
     */
    public void processKeyEvent(Component focusedComponent, KeyEvent e) {
        if (e.getID() != KeyEvent.KEY_PRESSED || focusedComponent == null) {
            return;
        }
        AWTKeyStroke k = AWTKeyStroke.getAWTKeyStrokeForEvent(e);
        for (int id = 0; id < 4; id++) {
            Set<AWTKeyStroke> keys = focusedComponent.getFocusTraversalKeys(id);
            if (keys == null || !keys.contains(k)) {
                continue;
            }
            if (!focusedComponent.getFocusTraversalKeysEnabled()) {
                return;
            }
            if (id == FORWARD_TRAVERSAL_KEYS) {
                this.focusNextComponent(focusedComponent);
            } else if (id == BACKWARD_TRAVERSAL_KEYS) {
                this.focusPreviousComponent(focusedComponent);
            } else if (id == UP_CYCLE_TRAVERSAL_KEYS) {
                this.upFocusCycle(focusedComponent);
            } else if (focusedComponent instanceof Container) {
                this.downFocusCycle((Container) focusedComponent);
            }
            e.consume();
            return;
        }
    }

    /**
     * Sets the keyboard events aside until the focus reaches that component.
     *
     * @param after the timestamp from which to set them aside
     * @param untilFocused the component that is going to receive the focus
     */
    protected synchronized void enqueueKeyEvents(long after, Component untilFocused) {
        this.held.add(new HeldEvents(after, untilFocused));
    }

    /**
     * Releases the events set aside for that component: the focus has arrived.
     *
     * <p>The released events are delivered **in order**, which is the only thing that makes them
     * useful: a burst of keys has to arrive as it was typed.
     */
    protected synchronized void dequeueKeyEvents(long after, Component untilFocused) {
        int i = this.findHeld(after, untilFocused);
        if (i < 0) {
            return;
        }
        HeldEvents a = this.held.remove(i);
        for (int j = 0; j < a.events.size(); j++) {
            KeyEvent e = a.events.get(j);
            if (a.target != null) {
                this.redispatchEvent(a.target, e);
            }
        }
    }

    /**
     * Throws away the events set aside for that component.
     *
     * <p>It is what is right when the focus is **not** going to reach it —the request was
     * cancelled, or the component was taken out of the tree—: delivering them anyway would be
     * giving it keys the user typed for another one.
     */
    protected synchronized void discardKeyEvents(Component comp) {
        int i = 0;
        while (i < this.held.size()) {
            if (this.held.get(i).target == comp) {
                this.held.remove(i);
            } else {
                i = i + 1;
            }
        }
    }

    /** The stretch set aside for that component from that timestamp, or -1. */
    private int findHeld(long after, Component untilFocused) {
        for (int i = 0; i < this.held.size(); i++) {
            HeldEvents a = this.held.get(i);
            if (a.target == untilFocused && a.timestamp == after) {
                return i;
            }
        }
        return -1;
    }

    /** Passes the focus to the next one in the traversal. */
    public void focusNextComponent(Component aComponent) {
        if (aComponent != null) {
            aComponent.transferFocus();
        }
    }

    /** Passes it to the previous one. */
    public void focusPreviousComponent(Component aComponent) {
        if (aComponent != null) {
            aComponent.transferFocusBackward();
        }
    }

    /** Goes up one focus cycle level. */
    public void upFocusCycle(Component aComponent) {
        if (aComponent != null) {
            aComponent.transferFocusUpCycle();
        }
    }

    /** Goes down one level, entering that container. */
    public void downFocusCycle(Container aContainer) {
        if (aContainer != null && aContainer.isFocusCycleRoot()) {
            aContainer.transferFocusDownCycle();
        }
    }
}
