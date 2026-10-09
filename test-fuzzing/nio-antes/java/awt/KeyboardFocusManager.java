package java.awt;

import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Who has the keyboard focus and how it moves.
 *
 * <p>The focus is one at a time in the whole program: there is **one** component that receives the
 * keys, and knowing which one is the job of this class and not of the components. That is why what
 * it keeps is five global things —the focus owner, the permanent owner, the focused window, the
 * active window and the current cycle root— and why the methods that write them are `protected`:
 * changing them is the manager's business, not that of whoever uses it. All but one:
 * {@link #setGlobalCurrentFocusCycleRoot} is public, for the reason explained there.
 *
 * <p>The difference between the **focus** owner and the **permanent** one is confusing and real:
 * when a menu opens, the focus passes to the menu temporarily, but the permanent owner is still the
 * text field that was being typed into. When the menu closes, the focus comes back by itself. The
 * same goes between the **focused** window —the one that receives the keys— and the **active** one
 * —the one that looks highlighted, which may be the owner of a dialog—.
 *
 * <p><strong>Without a windowing system nobody tells this manager that the focus moved.</strong>
 * Everything that is state and computation works: the traversal keys, the default policy, the
 * property and veto listeners, the chain of dispatchers and post-processors, and the five global
 * properties, which start at `null` and can be set from a subclass. What does not happen by itself
 * is the focus moving, because moving it is asked for by {@link Component#requestFocus} and that,
 * without a screen, does nothing —neither here nor in the JDK—.
 */
public abstract class KeyboardFocusManager implements KeyEventDispatcher, KeyEventPostProcessor {

    /** Forwards: Tab. */
    public static final int FORWARD_TRAVERSAL_KEYS = 0;

    /** Backwards: Shift+Tab. */
    public static final int BACKWARD_TRAVERSAL_KEYS = 1;

    /** One cycle level up. */
    public static final int UP_CYCLE_TRAVERSAL_KEYS = 2;

    /** One cycle level down. */
    public static final int DOWN_CYCLE_TRAVERSAL_KEYS = 3;

    /** How many traversal directions there are. */
    static final int TRAVERSAL_KEY_LENGTH = 4;

    /** The manager in use. */
    private static KeyboardFocusManager current;

    /** The component with the focus. */
    private Component focusOwner;

    /** The permanent owner, which does not change with a temporary focus. */
    private Component permanentFocusOwner;

    /** The window that receives the keys. */
    private Window focusedWindow;

    /** The window that looks active. */
    private Window activeWindow;

    /** The root of the focus cycle being traversed. */
    private Container currentFocusCycleRoot;

    /** The policy used by a container that did not set its own. */
    private FocusTraversalPolicy defaultPolicy = new DefaultFocusTraversalPolicy();

    /** The default traversal keys, by direction. */
    private final Set<AWTKeyStroke>[] defaultKeys = createDefaultKeys();

    /**
     * The dispatchers, in order, or `null` if none was ever registered.
     *
     * <p>The distinction between `null` and an empty list is visible from outside and deliberate:
     * {@link #getKeyEventDispatchers} returns `null` while nobody has ever registered anything, and
     * an empty list after registering and removing. That is, it reports **whether there ever were**
     * dispatchers, not only whether there are any now.
     */
    private ArrayList<KeyEventDispatcher> dispatchers;

    /** The post-processors, with the same distinction. */
    private ArrayList<KeyEventPostProcessor> postProcessors;

    /** The property change listeners. */
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    /** The listeners entitled to veto. */
    private final VetoableChangeSupport vetoSupport = new VetoableChangeSupport(this);

    /** A manager with the keys and the default policy. */
    public KeyboardFocusManager() {
    }

    /** The four tables of default keys. */
    @SuppressWarnings("unchecked")
    private static Set<AWTKeyStroke>[] createDefaultKeys() {
        Set<AWTKeyStroke>[] t = new Set[TRAVERSAL_KEY_LENGTH];
        for (int i = 0; i < TRAVERSAL_KEY_LENGTH; i++) {
            t[i] = defaultKeysFor(i);
        }
        return t;
    }

    /**
     * The default keys of that direction.
     *
     * <p>The two cycle directions —up and down— have **none** in AWT, and that is deliberate: going
     * up or down a cycle is Swing's business, and Swing does give them Ctrl+Up and Ctrl+Down.
     */
    private static Set<AWTKeyStroke> defaultKeysFor(int id) {
        Set<AWTKeyStroke> s = new HashSet<AWTKeyStroke>();
        if (id == FORWARD_TRAVERSAL_KEYS) {
            s.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, 0));
            s.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB,
                    java.awt.event.InputEvent.CTRL_DOWN_MASK));
        } else if (id == BACKWARD_TRAVERSAL_KEYS) {
            s.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB,
                    java.awt.event.InputEvent.SHIFT_DOWN_MASK));
            s.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB,
                    java.awt.event.InputEvent.CTRL_DOWN_MASK
                            | java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        }
        return Collections.unmodifiableSet(s);
    }

    /**
     * The manager in use, building one if this is the first request.
     *
     * <p>It is unique for the whole program: two managers would each believe they have the focus.
     */
    public static KeyboardFocusManager getCurrentKeyboardFocusManager() {
        synchronized (KeyboardFocusManager.class) {
            if (current == null) {
                current = new DefaultKeyboardFocusManager();
            }
            return current;
        }
    }

    /**
     * Changes the manager.
     *
     * @param newManager the manager, or `null` to go back to the default one on the next request
     */
    public static void setCurrentKeyboardFocusManager(KeyboardFocusManager newManager) {
        synchronized (KeyboardFocusManager.class) {
            current = newManager;
        }
    }

    /**
     * The component with the focus.
     *
     * @return the component, or `null` if the focus is not in this program
     */
    public Component getFocusOwner() {
        return this.getGlobalFocusOwner();
    }

    /** The focus owner, for the subclasses. */
    protected Component getGlobalFocusOwner() {
        synchronized (this) {
            return this.focusOwner;
        }
    }

    /**
     * Sets the focus owner.
     *
     * <p>A component that cannot receive the focus is ignored silently, which is what the JDK does:
     * the request comes from the windowing system and rejecting it with an exception would cut off
     * the event dispatching.
     */
    protected void setGlobalFocusOwner(Component focusOwner) {
        if (focusOwner != null && !focusOwner.isFocusable()) {
            return;
        }
        Component old;
        synchronized (this) {
            old = this.focusOwner;
            this.focusOwner = focusOwner;
        }
        this.firePropertyChange("focusOwner", old, focusOwner);
    }

    /** Releases the focus if this program has it. */
    public void clearFocusOwner() {
        if (this.getFocusOwner() != null) {
            this.clearGlobalFocusOwner();
        }
    }

    /**
     * Releases the focus.
     *
     * <p>After this no component has it, until the windowing system says otherwise.
     */
    public void clearGlobalFocusOwner() {
        this.setGlobalFocusOwner(null);
        this.setGlobalPermanentFocusOwner(null);
    }

    /**
     * The **permanent** focus owner.
     *
     * @return the component, or `null`
     */
    public Component getPermanentFocusOwner() {
        return this.getGlobalPermanentFocusOwner();
    }

    /** The permanent owner, for the subclasses. */
    protected Component getGlobalPermanentFocusOwner() {
        synchronized (this) {
            return this.permanentFocusOwner;
        }
    }

    /** Sets the permanent owner; it also becomes the focus owner. */
    protected void setGlobalPermanentFocusOwner(Component permanentFocusOwner) {
        if (permanentFocusOwner != null && !permanentFocusOwner.isFocusable()) {
            return;
        }
        Component old;
        synchronized (this) {
            old = this.permanentFocusOwner;
            this.permanentFocusOwner = permanentFocusOwner;
        }
        this.firePropertyChange("permanentFocusOwner", old, permanentFocusOwner);
        if (permanentFocusOwner != null) {
            this.setGlobalFocusOwner(permanentFocusOwner);
        }
    }

    /**
     * The window that receives the keys.
     *
     * @return the window, or `null`
     */
    public Window getFocusedWindow() {
        return this.getGlobalFocusedWindow();
    }

    /** The focused window, for the subclasses. */
    protected Window getGlobalFocusedWindow() {
        synchronized (this) {
            return this.focusedWindow;
        }
    }

    /** Sets the focused window; one that does not admit the focus is ignored. */
    protected void setGlobalFocusedWindow(Window focusedWindow) {
        if (focusedWindow != null && !focusedWindow.isFocusableWindow()) {
            return;
        }
        Window old;
        synchronized (this) {
            old = this.focusedWindow;
            this.focusedWindow = focusedWindow;
        }
        this.firePropertyChange("focusedWindow", old, focusedWindow);
    }

    /**
     * The active window.
     *
     * @return the window, or `null`
     */
    public Window getActiveWindow() {
        return this.getGlobalActiveWindow();
    }

    /** The active window, for the subclasses. */
    protected Window getGlobalActiveWindow() {
        synchronized (this) {
            return this.activeWindow;
        }
    }

    /** Sets the active window. */
    protected void setGlobalActiveWindow(Window activeWindow) {
        Window old;
        synchronized (this) {
            old = this.activeWindow;
            this.activeWindow = activeWindow;
        }
        this.firePropertyChange("activeWindow", old, activeWindow);
    }

    /** The policy used by a container that did not set its own. */
    public synchronized FocusTraversalPolicy getDefaultFocusTraversalPolicy() {
        return this.defaultPolicy;
    }

    /**
     * Changes the default policy.
     *
     * <p>It does not touch the containers that already have their own: the default one is the one
     * used when there is none, not one that overrides the rest.
     *
     * @throws IllegalArgumentException if the policy is `null`
     */
    public void setDefaultFocusTraversalPolicy(FocusTraversalPolicy defaultPolicy) {
        if (defaultPolicy == null) {
            throw new IllegalArgumentException("default focus traversal policy cannot be null");
        }
        FocusTraversalPolicy old;
        synchronized (this) {
            old = this.defaultPolicy;
            this.defaultPolicy = defaultPolicy;
        }
        this.firePropertyChange("defaultFocusTraversalPolicy", old, defaultPolicy);
    }

    /**
     * Changes the default traversal keys of that direction.
     *
     * <p>The set is copied and left unmodifiable: if the one passed in were kept, changing it
     * afterwards would change the traversal of the whole program without anyone asking for it.
     *
     * @throws IllegalArgumentException if the direction is not one of the four, if the set is
     *     `null`, if it carries a `null` inside, if it carries a keystroke of type `KEY_TYPED`
     *     —which does not tell modifiers apart and would make the traversal unpredictable— or if
     *     one of its keystrokes is already in another direction
     */
    public void setDefaultFocusTraversalKeys(int id, Set<? extends AWTKeyStroke> keystrokes) {
        if (id < 0 || id >= TRAVERSAL_KEY_LENGTH) {
            throw new IllegalArgumentException("invalid focus traversal key identifier");
        }
        if (keystrokes == null) {
            throw new IllegalArgumentException("cannot set null Set of default focus traversal keys");
        }
        Set<AWTKeyStroke> copy = new HashSet<AWTKeyStroke>();
        java.util.Iterator<? extends AWTKeyStroke> it = keystrokes.iterator();
        while (it.hasNext()) {
            AWTKeyStroke k = it.next();
            if (k == null) {
                throw new IllegalArgumentException("cannot set null focus traversal key");
            }
            if (k.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
                throw new IllegalArgumentException("focus traversal keys cannot map to KEY_TYPED events");
            }
            for (int i = 0; i < TRAVERSAL_KEY_LENGTH; i++) {
                if (i != id && this.defaultKeys[i].contains(k)) {
                    throw new IllegalArgumentException("focus traversal keys must be unique for a Component");
                }
            }
            copy.add(k);
        }
        Set<AWTKeyStroke> old;
        synchronized (this) {
            old = this.defaultKeys[id];
            this.defaultKeys[id] = Collections.unmodifiableSet(copy);
        }
        this.firePropertyChange(keysPropertyName(id), old, this.defaultKeys[id]);
    }

    /** What the property of that direction is called. */
    private static String keysPropertyName(int id) {
        if (id == FORWARD_TRAVERSAL_KEYS) {
            return "forwardDefaultFocusTraversalKeys";
        }
        if (id == BACKWARD_TRAVERSAL_KEYS) {
            return "backwardDefaultFocusTraversalKeys";
        }
        if (id == UP_CYCLE_TRAVERSAL_KEYS) {
            return "upCycleDefaultFocusTraversalKeys";
        }
        return "downCycleDefaultFocusTraversalKeys";
    }

    /**
     * The default traversal keys of that direction.
     *
     * @throws IllegalArgumentException if the direction is not one of the four
     */
    public Set<AWTKeyStroke> getDefaultFocusTraversalKeys(int id) {
        if (id < 0 || id >= TRAVERSAL_KEY_LENGTH) {
            throw new IllegalArgumentException("invalid focus traversal key identifier");
        }
        synchronized (this) {
            return this.defaultKeys[id];
        }
    }

    /**
     * The root of the focus cycle being traversed.
     *
     * @return the container, or `null` if none is being traversed
     */
    public Container getCurrentFocusCycleRoot() {
        return this.getGlobalCurrentFocusCycleRoot();
    }

    /** The cycle root, for the subclasses. */
    protected Container getGlobalCurrentFocusCycleRoot() {
        synchronized (this) {
            return this.currentFocusCycleRoot;
        }
    }

    /**
     * Sets the cycle root.
     *
     * <p>It is the only one of the five global writers that is **public**, and the reason is
     * concrete: traversing up and down a cycle is done by whoever traverses, not by the manager, so
     * they have to be able to say where they stopped.
     */
    public void setGlobalCurrentFocusCycleRoot(Container newFocusCycleRoot) {
        Container old;
        synchronized (this) {
            old = this.currentFocusCycleRoot;
            this.currentFocusCycleRoot = newFocusCycleRoot;
        }
        this.firePropertyChange("currentFocusCycleRoot", old, newFocusCycleRoot);
    }

    /** Adds a change listener; `null` does nothing. */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (listener != null) {
            this.changeSupport.addPropertyChangeListener(listener);
        }
    }

    /** Removes a change listener. */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (listener != null) {
            this.changeSupport.removePropertyChangeListener(listener);
        }
    }

    /** The change listeners. */
    public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
        return this.changeSupport.getPropertyChangeListeners();
    }

    /** Adds a listener for a single property. */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (listener != null) {
            this.changeSupport.addPropertyChangeListener(propertyName, listener);
        }
    }

    /** Removes a listener of a single property. */
    public void removePropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        if (listener != null) {
            this.changeSupport.removePropertyChangeListener(propertyName, listener);
        }
    }

    /** The listeners of that property. */
    public synchronized PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return this.changeSupport.getPropertyChangeListeners(propertyName);
    }

    /** Tells the listeners; if the value did not change it tells nobody. */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (oldValue == newValue) {
            return;
        }
        this.changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    /** Adds a listener entitled to veto. */
    public void addVetoableChangeListener(VetoableChangeListener listener) {
        if (listener != null) {
            this.vetoSupport.addVetoableChangeListener(listener);
        }
    }

    /** Removes a listener entitled to veto. */
    public void removeVetoableChangeListener(VetoableChangeListener listener) {
        if (listener != null) {
            this.vetoSupport.removeVetoableChangeListener(listener);
        }
    }

    /** The listeners entitled to veto. */
    public synchronized VetoableChangeListener[] getVetoableChangeListeners() {
        return this.vetoSupport.getVetoableChangeListeners();
    }

    /** Adds a listener entitled to veto a single property. */
    public void addVetoableChangeListener(String propertyName, VetoableChangeListener listener) {
        if (listener != null) {
            this.vetoSupport.addVetoableChangeListener(propertyName, listener);
        }
    }

    /** Removes a listener entitled to veto a single property. */
    public void removeVetoableChangeListener(String propertyName,
            VetoableChangeListener listener) {
        if (listener != null) {
            this.vetoSupport.removeVetoableChangeListener(propertyName, listener);
        }
    }

    /** The listeners entitled to veto that property. */
    public synchronized VetoableChangeListener[] getVetoableChangeListeners(String propertyName) {
        return this.vetoSupport.getVetoableChangeListeners(propertyName);
    }

    /**
     * Proposes the change to those who can veto it.
     *
     * @throws PropertyVetoException if any of them vetoes it; the change is not made
     */
    protected void fireVetoableChange(String propertyName, Object oldValue, Object newValue)
            throws PropertyVetoException {
        if (oldValue == newValue) {
            return;
        }
        this.vetoSupport.fireVetoableChange(propertyName, oldValue, newValue);
    }

    /**
     * Adds a dispatcher at the end of the chain.
     *
     * <p>The order matters: the first one to return `true` keeps the event. `null` does nothing.
     */
    public void addKeyEventDispatcher(KeyEventDispatcher dispatcher) {
        if (dispatcher == null) {
            return;
        }
        synchronized (this) {
            if (this.dispatchers == null) {
                this.dispatchers = new ArrayList<KeyEventDispatcher>();
            }
            this.dispatchers.add(dispatcher);
        }
    }

    /**
     * Removes a dispatcher.
     *
     * <p>The manager itself is the last one in the chain and **cannot** be removed this way: for
     * that the manager has to be changed.
     */
    public void removeKeyEventDispatcher(KeyEventDispatcher dispatcher) {
        if (dispatcher == null) {
            return;
        }
        synchronized (this) {
            if (this.dispatchers != null) {
                this.dispatchers.remove(dispatcher);
            }
        }
    }

    /**
     * The registered dispatchers.
     *
     * @return a copy, or `null` if **none** was ever registered. Removing the last one leaves an
     *     empty list, not a `null`: what is answered is whether the chain exists, not whether it
     *     has elements.
     */
    protected synchronized java.util.List<KeyEventDispatcher> getKeyEventDispatchers() {
        if (this.dispatchers == null) {
            return null;
        }
        return new ArrayList<KeyEventDispatcher>(this.dispatchers);
    }

    /** Adds a post-processor at the end of the chain; `null` does nothing. */
    public void addKeyEventPostProcessor(KeyEventPostProcessor processor) {
        if (processor == null) {
            return;
        }
        synchronized (this) {
            if (this.postProcessors == null) {
                this.postProcessors = new ArrayList<KeyEventPostProcessor>();
            }
            this.postProcessors.add(processor);
        }
    }

    /** Removes a post-processor; the manager itself cannot be removed this way. */
    public void removeKeyEventPostProcessor(KeyEventPostProcessor processor) {
        if (processor == null) {
            return;
        }
        synchronized (this) {
            if (this.postProcessors != null) {
                this.postProcessors.remove(processor);
            }
        }
    }

    /**
     * The registered post-processors.
     *
     * @return a copy, or `null` if none was ever registered, with the same distinction as
     *     {@link #getKeyEventDispatchers}
     */
    protected java.util.List<KeyEventPostProcessor> getKeyEventPostProcessors() {
        synchronized (this) {
            if (this.postProcessors == null) {
                return null;
            }
            return new ArrayList<KeyEventPostProcessor>(this.postProcessors);
        }
    }

    /** Dispatches that event. */
    public abstract boolean dispatchEvent(AWTEvent e);

    /**
     * Sends the event to the component **without** going through the chain of dispatchers again.
     *
     * <p>It is `final` and exists so that a {@link KeyEventDispatcher} can deliver the event
     * without building a cycle: if it called `dispatchEvent`, the chain would go through it again.
     */
    public final void redispatchEvent(Component target, AWTEvent e) {
        target.dispatchEvent(e);
    }

    /** Dispatches a keyboard event. */
    public abstract boolean dispatchKeyEvent(KeyEvent e);

    /** Looks at a keyboard event nobody consumed. */
    public abstract boolean postProcessKeyEvent(KeyEvent e);

    /** Handles the traversal keys of that component. */
    public abstract void processKeyEvent(Component focusedComponent, KeyEvent e);

    /**
     * Holds the keyboard events that arrive while the focus is in transit.
     *
     * <p>Without this, a key pressed just as the focus changes component would reach the wrong one.
     */
    protected abstract void enqueueKeyEvents(long after, Component untilFocused);

    /** Releases the held events: the focus has arrived. */
    protected abstract void dequeueKeyEvents(long after, Component untilFocused);

    /** Throws away the held events for that component: the focus is not going to reach it. */
    protected abstract void discardKeyEvents(Component comp);

    /** Passes the focus to the next one in the traversal. */
    public abstract void focusNextComponent(Component aComponent);

    /** Passes it to the previous one. */
    public abstract void focusPreviousComponent(Component aComponent);

    /** Goes up one focus cycle level. */
    public abstract void upFocusCycle(Component aComponent);

    /** Goes down one level, entering that container. */
    public abstract void downFocusCycle(Container aContainer);

    /** Passes the focus to the one after the one that has it now. */
    public final void focusNextComponent() {
        Component c = this.getFocusOwner();
        if (c != null) {
            this.focusNextComponent(c);
        }
    }

    /** Passes it to the one before the one that has it now. */
    public final void focusPreviousComponent() {
        Component c = this.getFocusOwner();
        if (c != null) {
            this.focusPreviousComponent(c);
        }
    }

    /** Goes up one level from the one that has the focus. */
    public final void upFocusCycle() {
        Component c = this.getFocusOwner();
        if (c != null) {
            this.upFocusCycle(c);
        }
    }

    /**
     * Goes down one level from the one that has the focus.
     *
     * <p>It only does something if the one with the focus is a container: going down a cycle is
     * entering one.
     */
    public final void downFocusCycle() {
        Component c = this.getFocusOwner();
        if (c instanceof Container) {
            this.downFocusCycle((Container) c);
        }
    }
}
