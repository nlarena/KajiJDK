package java.awt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.EventListener;

/**
 * Two listeners disguised as one.
 *
 * <p>It is the AWT answer to having several listeners without using a list: instead of every
 * component keeping a collection, it keeps **one** listener, and if there are two they get wrapped
 * in one of these. Three get wrapped in a multicaster holding another one, and so on.
 *
 * <p>What comes out is a binary tree instead of a list, and the advantage is not obvious until one
 * thinks of the common case: the vast majority of components has **zero or one** listener, and
 * there no structure is reserved at all. Memory is only paid for when there really is more than
 * one.
 *
 * <p>It is **immutable**, and hence the odd shape of the {@code add} and {@code remove} methods:
 * they are static and return the resulting listener instead of modifying anything. A component does
 * {@code l = AWTEventMulticaster.add(l, newOne)}. That immutability is what makes it possible to
 * deliver an event while someone unsubscribes without the iteration blowing up.
 *
 * <p>A single object implements **seventeen** listener interfaces. It is not an oversight: since it
 * is immutable and used through its static type, a multicaster of keyboard listeners is never going
 * to receive a mouse event, even though the method is there.
 */
public class AWTEventMulticaster implements
        ComponentListener,
        ContainerListener,
        FocusListener,
        KeyListener,
        MouseListener,
        MouseMotionListener,
        WindowListener,
        WindowStateListener,
        WindowFocusListener,
        ActionListener,
        ItemListener,
        AdjustmentListener,
        TextListener,
        InputMethodListener,
        HierarchyListener,
        HierarchyBoundsListener,
        MouseWheelListener {

    /** The first of the two. */
    protected final EventListener a;

    /** The second one, which may itself be another multicaster. */
    protected final EventListener b;

    /**
     * Wraps two listeners.
     *
     * <p>It is protected: one gets here through the static {@code add} methods, which are the ones
     * that know what to do when either of the two is `null`.
     */
    protected AWTEventMulticaster(EventListener a, EventListener b) {
        this.a = a;
        this.b = b;
    }

    /**
     * Removes a listener from this pair.
     *
     * @return what is left: the other one, or a new pair if the one being removed was deeper in
     */
    protected EventListener remove(EventListener oldl) {
        if (oldl == this.a) {
            return this.b;
        }
        if (oldl == this.b) {
            return this.a;
        }
        EventListener a2 = removeInternal(this.a, oldl);
        EventListener b2 = removeInternal(this.b, oldl);
        if (a2 == this.a && b2 == this.b) {
            return this;
        }
        return addInternal(a2, b2);
    }

    /** Passes it to both. */
    public void componentResized(ComponentEvent e) {
        ((ComponentListener) this.a).componentResized(e);
        ((ComponentListener) this.b).componentResized(e);
    }

    /** Passes it to both. */
    public void componentMoved(ComponentEvent e) {
        ((ComponentListener) this.a).componentMoved(e);
        ((ComponentListener) this.b).componentMoved(e);
    }

    /** Passes it to both. */
    public void componentShown(ComponentEvent e) {
        ((ComponentListener) this.a).componentShown(e);
        ((ComponentListener) this.b).componentShown(e);
    }

    /** Passes it to both. */
    public void componentHidden(ComponentEvent e) {
        ((ComponentListener) this.a).componentHidden(e);
        ((ComponentListener) this.b).componentHidden(e);
    }

    /** Passes it to both. */
    public void componentAdded(ContainerEvent e) {
        ((ContainerListener) this.a).componentAdded(e);
        ((ContainerListener) this.b).componentAdded(e);
    }

    /** Passes it to both. */
    public void componentRemoved(ContainerEvent e) {
        ((ContainerListener) this.a).componentRemoved(e);
        ((ContainerListener) this.b).componentRemoved(e);
    }

    /** Passes it to both. */
    public void focusGained(FocusEvent e) {
        ((FocusListener) this.a).focusGained(e);
        ((FocusListener) this.b).focusGained(e);
    }

    /** Passes it to both. */
    public void focusLost(FocusEvent e) {
        ((FocusListener) this.a).focusLost(e);
        ((FocusListener) this.b).focusLost(e);
    }

    /** Passes it to both. */
    public void keyTyped(KeyEvent e) {
        ((KeyListener) this.a).keyTyped(e);
        ((KeyListener) this.b).keyTyped(e);
    }

    /** Passes it to both. */
    public void keyPressed(KeyEvent e) {
        ((KeyListener) this.a).keyPressed(e);
        ((KeyListener) this.b).keyPressed(e);
    }

    /** Passes it to both. */
    public void keyReleased(KeyEvent e) {
        ((KeyListener) this.a).keyReleased(e);
        ((KeyListener) this.b).keyReleased(e);
    }

    /** Passes it to both. */
    public void mouseClicked(MouseEvent e) {
        ((MouseListener) this.a).mouseClicked(e);
        ((MouseListener) this.b).mouseClicked(e);
    }

    /** Passes it to both. */
    public void mousePressed(MouseEvent e) {
        ((MouseListener) this.a).mousePressed(e);
        ((MouseListener) this.b).mousePressed(e);
    }

    /** Passes it to both. */
    public void mouseReleased(MouseEvent e) {
        ((MouseListener) this.a).mouseReleased(e);
        ((MouseListener) this.b).mouseReleased(e);
    }

    /** Passes it to both. */
    public void mouseEntered(MouseEvent e) {
        ((MouseListener) this.a).mouseEntered(e);
        ((MouseListener) this.b).mouseEntered(e);
    }

    /** Passes it to both. */
    public void mouseExited(MouseEvent e) {
        ((MouseListener) this.a).mouseExited(e);
        ((MouseListener) this.b).mouseExited(e);
    }

    /** Passes it to both. */
    public void mouseDragged(MouseEvent e) {
        ((MouseMotionListener) this.a).mouseDragged(e);
        ((MouseMotionListener) this.b).mouseDragged(e);
    }

    /** Passes it to both. */
    public void mouseMoved(MouseEvent e) {
        ((MouseMotionListener) this.a).mouseMoved(e);
        ((MouseMotionListener) this.b).mouseMoved(e);
    }

    /** Passes it to both. */
    public void windowOpened(WindowEvent e) {
        ((WindowListener) this.a).windowOpened(e);
        ((WindowListener) this.b).windowOpened(e);
    }

    /** Passes it to both. */
    public void windowClosing(WindowEvent e) {
        ((WindowListener) this.a).windowClosing(e);
        ((WindowListener) this.b).windowClosing(e);
    }

    /** Passes it to both. */
    public void windowClosed(WindowEvent e) {
        ((WindowListener) this.a).windowClosed(e);
        ((WindowListener) this.b).windowClosed(e);
    }

    /** Passes it to both. */
    public void windowIconified(WindowEvent e) {
        ((WindowListener) this.a).windowIconified(e);
        ((WindowListener) this.b).windowIconified(e);
    }

    /** Passes it to both. */
    public void windowDeiconified(WindowEvent e) {
        ((WindowListener) this.a).windowDeiconified(e);
        ((WindowListener) this.b).windowDeiconified(e);
    }

    /** Passes it to both. */
    public void windowActivated(WindowEvent e) {
        ((WindowListener) this.a).windowActivated(e);
        ((WindowListener) this.b).windowActivated(e);
    }

    /** Passes it to both. */
    public void windowDeactivated(WindowEvent e) {
        ((WindowListener) this.a).windowDeactivated(e);
        ((WindowListener) this.b).windowDeactivated(e);
    }

    /** Passes it to both. */
    public void windowStateChanged(WindowEvent e) {
        ((WindowStateListener) this.a).windowStateChanged(e);
        ((WindowStateListener) this.b).windowStateChanged(e);
    }

    /** Passes it to both. */
    public void windowGainedFocus(WindowEvent e) {
        ((WindowFocusListener) this.a).windowGainedFocus(e);
        ((WindowFocusListener) this.b).windowGainedFocus(e);
    }

    /** Passes it to both. */
    public void windowLostFocus(WindowEvent e) {
        ((WindowFocusListener) this.a).windowLostFocus(e);
        ((WindowFocusListener) this.b).windowLostFocus(e);
    }

    /** Passes it to both. */
    public void actionPerformed(ActionEvent e) {
        ((ActionListener) this.a).actionPerformed(e);
        ((ActionListener) this.b).actionPerformed(e);
    }

    /** Passes it to both. */
    public void itemStateChanged(ItemEvent e) {
        ((ItemListener) this.a).itemStateChanged(e);
        ((ItemListener) this.b).itemStateChanged(e);
    }

    /** Passes it to both. */
    public void adjustmentValueChanged(AdjustmentEvent e) {
        ((AdjustmentListener) this.a).adjustmentValueChanged(e);
        ((AdjustmentListener) this.b).adjustmentValueChanged(e);
    }

    /** Passes it to both. */
    public void textValueChanged(TextEvent e) {
        ((TextListener) this.a).textValueChanged(e);
        ((TextListener) this.b).textValueChanged(e);
    }

    /** Passes it to both. */
    public void inputMethodTextChanged(InputMethodEvent e) {
        ((InputMethodListener) this.a).inputMethodTextChanged(e);
        ((InputMethodListener) this.b).inputMethodTextChanged(e);
    }

    /** Passes it to both. */
    public void caretPositionChanged(InputMethodEvent e) {
        ((InputMethodListener) this.a).caretPositionChanged(e);
        ((InputMethodListener) this.b).caretPositionChanged(e);
    }

    /** Passes it to both. */
    public void hierarchyChanged(HierarchyEvent e) {
        ((HierarchyListener) this.a).hierarchyChanged(e);
        ((HierarchyListener) this.b).hierarchyChanged(e);
    }

    /** Passes it to both. */
    public void ancestorMoved(HierarchyEvent e) {
        ((HierarchyBoundsListener) this.a).ancestorMoved(e);
        ((HierarchyBoundsListener) this.b).ancestorMoved(e);
    }

    /** Passes it to both. */
    public void ancestorResized(HierarchyEvent e) {
        ((HierarchyBoundsListener) this.a).ancestorResized(e);
        ((HierarchyBoundsListener) this.b).ancestorResized(e);
    }

    /** Passes it to both. */
    public void mouseWheelMoved(MouseWheelEvent e) {
        ((MouseWheelListener) this.a).mouseWheelMoved(e);
        ((MouseWheelListener) this.b).mouseWheelMoved(e);
    }

    /**
     * Joins two ComponentListener listeners into one.
     *
     * @return `b` if `a` is `null`, `a` if `b` is `null`, or a pair with the two
     */
    public static ComponentListener add(ComponentListener a, ComponentListener b) {
        return (ComponentListener) addInternal(a, b);
    }

    /**
     * Joins two ContainerListener listeners into one.
     *
     * @return `b` if `a` is `null`, `a` if `b` is `null`, or a pair with the two
     */
    public static ContainerListener add(ContainerListener a, ContainerListener b) {
        return (ContainerListener) addInternal(a, b);
    }

    /**
     * Joins two FocusListener listeners into one.
     *
     * @return `b` if `a` is `null`, `a` if `b` is `null`, or a pair with the two
     */
    public static FocusListener add(FocusListener a, FocusListener b) {
        return (FocusListener) addInternal(a, b);
    }

    /**
     * Joins two KeyListener listeners into one.
     *
     * @return `b` if `a` is `null`, `a` if `b` is `null`, or a pair with the two
     */
    public static KeyListener add(KeyListener a, KeyListener b) {
        return (KeyListener) addInternal(a, b);
    }

    /**
     * Joins two MouseListener listeners into one.
     *
     * @return `b` if `a` is `null`, `a` if `b` is `null`, or a pair with the two
     */
    public static MouseListener add(MouseListener a, MouseListener b) {
        return (MouseListener) addInternal(a, b);
    }

    /**
     * Joins two MouseMotionListener listeners into one.
     *
     * @return `b` if `a` is `null`, `a` if `b` is `null`, or a pair with the two
     */
    public static MouseMotionListener add(MouseMotionListener a, MouseMotionListener b) {
        return (MouseMotionListener) addInternal(a, b);
    }

    /**
     * Joins two WindowListener listeners into one.
     *
     * @return `b` if `a` is `null`, `a` if `b` is `null`, or a pair with the two
     */
    public static WindowListener add(WindowListener a, WindowListener b) {
        return (WindowListener) addInternal(a, b);
    }

    /**
     * Joins two WindowStateListener listeners into one.
     *
     * @return `b` if `a` is `null`, `a` if `b` is `null`, or a pair with the two
     */
    public static WindowStateListener add(WindowStateListener a, WindowStateListener b) {
        return (WindowStateListener) addInternal(a, b);
    }

    /**
     * Joins two WindowFocusListener listeners into one.
     *
     * @return `b` if `a` is `null`, `a` if `b` is `null`, or a pair with the two
     */
    public static WindowFocusListener add(WindowFocusListener a, WindowFocusListener b) {
        return (WindowFocusListener) addInternal(a, b);
    }

    /**
     * Joins two ActionListener listeners into one.
     *
     * @return `b` if `a` is `null`, `a` if `b` is `null`, or a pair with the two
     */
    public static ActionListener add(ActionListener a, ActionListener b) {
        return (ActionListener) addInternal(a, b);
    }

    /**
     * Joins two ItemListener listeners into one.
     *
     * @return `b` if `a` is `null`, `a` if `b` is `null`, or a pair with the two
     */
    public static ItemListener add(ItemListener a, ItemListener b) {
        return (ItemListener) addInternal(a, b);
    }

    /**
     * Joins two AdjustmentListener listeners into one.
     *
     * @return `b` if `a` is `null`, `a` if `b` is `null`, or a pair with the two
     */
    public static AdjustmentListener add(AdjustmentListener a, AdjustmentListener b) {
        return (AdjustmentListener) addInternal(a, b);
    }

    /**
     * Joins two TextListener listeners into one.
     *
     * @return `b` if `a` is `null`, `a` if `b` is `null`, or a pair with the two
     */
    public static TextListener add(TextListener a, TextListener b) {
        return (TextListener) addInternal(a, b);
    }

    /**
     * Joins two InputMethodListener listeners into one.
     *
     * @return `b` if `a` is `null`, `a` if `b` is `null`, or a pair with the two
     */
    public static InputMethodListener add(InputMethodListener a, InputMethodListener b) {
        return (InputMethodListener) addInternal(a, b);
    }

    /**
     * Joins two HierarchyListener listeners into one.
     *
     * @return `b` if `a` is `null`, `a` if `b` is `null`, or a pair with the two
     */
    public static HierarchyListener add(HierarchyListener a, HierarchyListener b) {
        return (HierarchyListener) addInternal(a, b);
    }

    /**
     * Joins two HierarchyBoundsListener listeners into one.
     *
     * @return `b` if `a` is `null`, `a` if `b` is `null`, or a pair with the two
     */
    public static HierarchyBoundsListener add(HierarchyBoundsListener a, HierarchyBoundsListener b) {
        return (HierarchyBoundsListener) addInternal(a, b);
    }

    /**
     * Joins two MouseWheelListener listeners into one.
     *
     * @return `b` if `a` is `null`, `a` if `b` is `null`, or a pair with the two
     */
    public static MouseWheelListener add(MouseWheelListener a, MouseWheelListener b) {
        return (MouseWheelListener) addInternal(a, b);
    }

    /**
     * Removes a ComponentListener listener.
     *
     * @return what is left, which may be `null`
     */
    public static ComponentListener remove(ComponentListener l, ComponentListener oldl) {
        return (ComponentListener) removeInternal(l, oldl);
    }

    /**
     * Removes a ContainerListener listener.
     *
     * @return what is left, which may be `null`
     */
    public static ContainerListener remove(ContainerListener l, ContainerListener oldl) {
        return (ContainerListener) removeInternal(l, oldl);
    }

    /**
     * Removes a FocusListener listener.
     *
     * @return what is left, which may be `null`
     */
    public static FocusListener remove(FocusListener l, FocusListener oldl) {
        return (FocusListener) removeInternal(l, oldl);
    }

    /**
     * Removes a KeyListener listener.
     *
     * @return what is left, which may be `null`
     */
    public static KeyListener remove(KeyListener l, KeyListener oldl) {
        return (KeyListener) removeInternal(l, oldl);
    }

    /**
     * Removes a MouseListener listener.
     *
     * @return what is left, which may be `null`
     */
    public static MouseListener remove(MouseListener l, MouseListener oldl) {
        return (MouseListener) removeInternal(l, oldl);
    }

    /**
     * Removes a MouseMotionListener listener.
     *
     * @return what is left, which may be `null`
     */
    public static MouseMotionListener remove(MouseMotionListener l, MouseMotionListener oldl) {
        return (MouseMotionListener) removeInternal(l, oldl);
    }

    /**
     * Removes a WindowListener listener.
     *
     * @return what is left, which may be `null`
     */
    public static WindowListener remove(WindowListener l, WindowListener oldl) {
        return (WindowListener) removeInternal(l, oldl);
    }

    /**
     * Removes a WindowStateListener listener.
     *
     * @return what is left, which may be `null`
     */
    public static WindowStateListener remove(WindowStateListener l, WindowStateListener oldl) {
        return (WindowStateListener) removeInternal(l, oldl);
    }

    /**
     * Removes a WindowFocusListener listener.
     *
     * @return what is left, which may be `null`
     */
    public static WindowFocusListener remove(WindowFocusListener l, WindowFocusListener oldl) {
        return (WindowFocusListener) removeInternal(l, oldl);
    }

    /**
     * Removes a ActionListener listener.
     *
     * @return what is left, which may be `null`
     */
    public static ActionListener remove(ActionListener l, ActionListener oldl) {
        return (ActionListener) removeInternal(l, oldl);
    }

    /**
     * Removes a ItemListener listener.
     *
     * @return what is left, which may be `null`
     */
    public static ItemListener remove(ItemListener l, ItemListener oldl) {
        return (ItemListener) removeInternal(l, oldl);
    }

    /**
     * Removes a AdjustmentListener listener.
     *
     * @return what is left, which may be `null`
     */
    public static AdjustmentListener remove(AdjustmentListener l, AdjustmentListener oldl) {
        return (AdjustmentListener) removeInternal(l, oldl);
    }

    /**
     * Removes a TextListener listener.
     *
     * @return what is left, which may be `null`
     */
    public static TextListener remove(TextListener l, TextListener oldl) {
        return (TextListener) removeInternal(l, oldl);
    }

    /**
     * Removes a InputMethodListener listener.
     *
     * @return what is left, which may be `null`
     */
    public static InputMethodListener remove(InputMethodListener l, InputMethodListener oldl) {
        return (InputMethodListener) removeInternal(l, oldl);
    }

    /**
     * Removes a HierarchyListener listener.
     *
     * @return what is left, which may be `null`
     */
    public static HierarchyListener remove(HierarchyListener l, HierarchyListener oldl) {
        return (HierarchyListener) removeInternal(l, oldl);
    }

    /**
     * Removes a HierarchyBoundsListener listener.
     *
     * @return what is left, which may be `null`
     */
    public static HierarchyBoundsListener remove(HierarchyBoundsListener l, HierarchyBoundsListener oldl) {
        return (HierarchyBoundsListener) removeInternal(l, oldl);
    }

    /**
     * Removes a MouseWheelListener listener.
     *
     * @return what is left, which may be `null`
     */
    public static MouseWheelListener remove(MouseWheelListener l, MouseWheelListener oldl) {
        return (MouseWheelListener) removeInternal(l, oldl);
    }

    /**
     * Joins any two listeners.
     *
     * <p>With only one nothing gets wrapped: wrapping a listener with `null` would spend an object
     * and an indirection per event to add nobody.
     */
    protected static EventListener addInternal(EventListener a, EventListener b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return new AWTEventMulticaster(a, b);
    }

    /**
     * Removes a listener from wherever it is.
     *
     * @return what is left, which may be `null`
     */
    protected static EventListener removeInternal(EventListener l, EventListener oldl) {
        if (l == oldl || l == null) {
            return null;
        }
        if (l instanceof AWTEventMulticaster) {
            return ((AWTEventMulticaster) l).remove(oldl);
        }
        return l;
    }

    /**
     * Serializes the listeners of this pair that are serializable.
     *
     * <p>The ones that are not get skipped silently: a listener that cannot be serialized should
     * not stop the component from being saved.
     */
    protected void saveInternal(ObjectOutputStream s, String k) throws IOException {
        if (this.a instanceof AWTEventMulticaster) {
            ((AWTEventMulticaster) this.a).saveInternal(s, k);
        } else if (this.a instanceof Serializable) {
            s.writeObject(k);
            s.writeObject(this.a);
        }
        if (this.b instanceof AWTEventMulticaster) {
            ((AWTEventMulticaster) this.b).saveInternal(s, k);
        } else if (this.b instanceof Serializable) {
            s.writeObject(k);
            s.writeObject(this.b);
        }
    }

    /** Serializes a lone listener or a pair, with its key. */
    protected static void save(ObjectOutputStream s, String k, EventListener l)
            throws IOException {
        if (l == null) {
            return;
        }
        if (l instanceof AWTEventMulticaster) {
            ((AWTEventMulticaster) l).saveInternal(s, k);
        } else if (l instanceof Serializable) {
            s.writeObject(k);
            s.writeObject(l);
        }
    }

    /**
     * Flattens the tree and returns the listeners of that class.
     *
     * <p>It is the only operation that walks the whole structure, and it exists because the {@code
     * getXListeners} methods of the components need it: inside it is a tree, but from outside it
     * has to be shown as an array.
     *
     * <p>The {@code T extends EventListener} bound is what keeps the class honest: a class that is
     * not a listener one cannot be passed without raw types, and if it is, the failure lands on the
     * caller's assignment and not here.
     *
     * @throws NullPointerException if the class is `null`
     */
    public static <T extends EventListener> T[] getListeners(EventListener l,
            Class<T> listenerType) {
        if (listenerType == null) {
            throw new NullPointerException("Listener type should not be null");
        }
        int n = getListenerCount(l, listenerType);
        @SuppressWarnings("unchecked")
        T[] out = (T[]) Array.newInstance(listenerType, n);
        populateListenerArray(out, l, 0);
        return out;
    }

    /** How many listeners of that class there are in the tree. */
    private static int getListenerCount(EventListener l, Class<?> listenerType) {
        if (l == null) {
            return 0;
        }
        if (l instanceof AWTEventMulticaster) {
            AWTEventMulticaster mc = (AWTEventMulticaster) l;
            return getListenerCount(mc.a, listenerType) + getListenerCount(mc.b, listenerType);
        }
        if (listenerType.isInstance(l)) {
            return 1;
        }
        return 0;
    }

    /** Dumps the tree into the array, in order, and returns where it got to. */
    private static int populateListenerArray(EventListener[] out, EventListener l, int index) {
        if (l == null) {
            return index;
        }
        if (l instanceof AWTEventMulticaster) {
            AWTEventMulticaster mc = (AWTEventMulticaster) l;
            int i = populateListenerArray(out, mc.a, index);
            return populateListenerArray(out, mc.b, i);
        }
        if (out.getClass().getComponentType().isInstance(l)) {
            out[index] = l;
            return index + 1;
        }
        return index;
    }
}
