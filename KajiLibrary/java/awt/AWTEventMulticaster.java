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
 * Dos oyentes disfrazados de uno.
 *
 * <p>Es la solución de AWT al problema de tener varios oyentes sin usar una lista: en vez de que
 * cada componente guarde una colección, guarda **un** oyente, y si hay dos se los envuelve en uno de
 * éstos. Tres se envuelven en un multicaster que contiene a otro, y así.
 *
 * <p>Sale un árbol binario en vez de una lista, y la ventaja no es obvia hasta que se piensa en el
 * caso común: la enorme mayoría de los componentes tiene **cero o un** oyente, y ahí no se reserva
 * ninguna estructura. Sólo se paga memoria cuando de verdad hay más de uno.
 *
 * <p>Es **inmutable**, y de ahí la forma rara de los {@code add} y {@code remove}: son estáticos y
 * devuelven el oyente resultante en vez de modificar nada. Un componente hace
 * {@code l = AWTEventMulticaster.add(l, nuevo)}. Esa inmutabilidad es lo que hace que se pueda
 * repartir un evento mientras alguien se da de baja sin que la iteración explote.
 *
 * <p>Un solo objeto implementa **diecisiete** interfaces de oyente. No es un descuido: como es
 * inmutable y se lo usa por su tipo estático, un multicaster de oyentes de teclado nunca va a
 * recibir un evento de ratón, aunque el método esté ahí.
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

    /** El primero de los dos. */
    protected final EventListener a;

    /** El segundo, que puede ser a su vez otro multicaster. */
    protected final EventListener b;

    /**
     * Envuelve dos oyentes.
     *
     * <p>Es protegido: se llega por los {@code add} estáticos, que son los que saben qué hacer
     * cuando alguno de los dos es `null`.
     */
    protected AWTEventMulticaster(EventListener a, EventListener b) {
        this.a = a;
        this.b = b;
    }

    /**
     * Saca un oyente de este par.
     *
     * @return lo que queda: el otro, o un par nuevo si el que se saca estaba más adentro
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

    /** Se lo pasa a los dos. */
    public void componentResized(ComponentEvent e) {
        ((ComponentListener) this.a).componentResized(e);
        ((ComponentListener) this.b).componentResized(e);
    }

    /** Se lo pasa a los dos. */
    public void componentMoved(ComponentEvent e) {
        ((ComponentListener) this.a).componentMoved(e);
        ((ComponentListener) this.b).componentMoved(e);
    }

    /** Se lo pasa a los dos. */
    public void componentShown(ComponentEvent e) {
        ((ComponentListener) this.a).componentShown(e);
        ((ComponentListener) this.b).componentShown(e);
    }

    /** Se lo pasa a los dos. */
    public void componentHidden(ComponentEvent e) {
        ((ComponentListener) this.a).componentHidden(e);
        ((ComponentListener) this.b).componentHidden(e);
    }

    /** Se lo pasa a los dos. */
    public void componentAdded(ContainerEvent e) {
        ((ContainerListener) this.a).componentAdded(e);
        ((ContainerListener) this.b).componentAdded(e);
    }

    /** Se lo pasa a los dos. */
    public void componentRemoved(ContainerEvent e) {
        ((ContainerListener) this.a).componentRemoved(e);
        ((ContainerListener) this.b).componentRemoved(e);
    }

    /** Se lo pasa a los dos. */
    public void focusGained(FocusEvent e) {
        ((FocusListener) this.a).focusGained(e);
        ((FocusListener) this.b).focusGained(e);
    }

    /** Se lo pasa a los dos. */
    public void focusLost(FocusEvent e) {
        ((FocusListener) this.a).focusLost(e);
        ((FocusListener) this.b).focusLost(e);
    }

    /** Se lo pasa a los dos. */
    public void keyTyped(KeyEvent e) {
        ((KeyListener) this.a).keyTyped(e);
        ((KeyListener) this.b).keyTyped(e);
    }

    /** Se lo pasa a los dos. */
    public void keyPressed(KeyEvent e) {
        ((KeyListener) this.a).keyPressed(e);
        ((KeyListener) this.b).keyPressed(e);
    }

    /** Se lo pasa a los dos. */
    public void keyReleased(KeyEvent e) {
        ((KeyListener) this.a).keyReleased(e);
        ((KeyListener) this.b).keyReleased(e);
    }

    /** Se lo pasa a los dos. */
    public void mouseClicked(MouseEvent e) {
        ((MouseListener) this.a).mouseClicked(e);
        ((MouseListener) this.b).mouseClicked(e);
    }

    /** Se lo pasa a los dos. */
    public void mousePressed(MouseEvent e) {
        ((MouseListener) this.a).mousePressed(e);
        ((MouseListener) this.b).mousePressed(e);
    }

    /** Se lo pasa a los dos. */
    public void mouseReleased(MouseEvent e) {
        ((MouseListener) this.a).mouseReleased(e);
        ((MouseListener) this.b).mouseReleased(e);
    }

    /** Se lo pasa a los dos. */
    public void mouseEntered(MouseEvent e) {
        ((MouseListener) this.a).mouseEntered(e);
        ((MouseListener) this.b).mouseEntered(e);
    }

    /** Se lo pasa a los dos. */
    public void mouseExited(MouseEvent e) {
        ((MouseListener) this.a).mouseExited(e);
        ((MouseListener) this.b).mouseExited(e);
    }

    /** Se lo pasa a los dos. */
    public void mouseDragged(MouseEvent e) {
        ((MouseMotionListener) this.a).mouseDragged(e);
        ((MouseMotionListener) this.b).mouseDragged(e);
    }

    /** Se lo pasa a los dos. */
    public void mouseMoved(MouseEvent e) {
        ((MouseMotionListener) this.a).mouseMoved(e);
        ((MouseMotionListener) this.b).mouseMoved(e);
    }

    /** Se lo pasa a los dos. */
    public void windowOpened(WindowEvent e) {
        ((WindowListener) this.a).windowOpened(e);
        ((WindowListener) this.b).windowOpened(e);
    }

    /** Se lo pasa a los dos. */
    public void windowClosing(WindowEvent e) {
        ((WindowListener) this.a).windowClosing(e);
        ((WindowListener) this.b).windowClosing(e);
    }

    /** Se lo pasa a los dos. */
    public void windowClosed(WindowEvent e) {
        ((WindowListener) this.a).windowClosed(e);
        ((WindowListener) this.b).windowClosed(e);
    }

    /** Se lo pasa a los dos. */
    public void windowIconified(WindowEvent e) {
        ((WindowListener) this.a).windowIconified(e);
        ((WindowListener) this.b).windowIconified(e);
    }

    /** Se lo pasa a los dos. */
    public void windowDeiconified(WindowEvent e) {
        ((WindowListener) this.a).windowDeiconified(e);
        ((WindowListener) this.b).windowDeiconified(e);
    }

    /** Se lo pasa a los dos. */
    public void windowActivated(WindowEvent e) {
        ((WindowListener) this.a).windowActivated(e);
        ((WindowListener) this.b).windowActivated(e);
    }

    /** Se lo pasa a los dos. */
    public void windowDeactivated(WindowEvent e) {
        ((WindowListener) this.a).windowDeactivated(e);
        ((WindowListener) this.b).windowDeactivated(e);
    }

    /** Se lo pasa a los dos. */
    public void windowStateChanged(WindowEvent e) {
        ((WindowStateListener) this.a).windowStateChanged(e);
        ((WindowStateListener) this.b).windowStateChanged(e);
    }

    /** Se lo pasa a los dos. */
    public void windowGainedFocus(WindowEvent e) {
        ((WindowFocusListener) this.a).windowGainedFocus(e);
        ((WindowFocusListener) this.b).windowGainedFocus(e);
    }

    /** Se lo pasa a los dos. */
    public void windowLostFocus(WindowEvent e) {
        ((WindowFocusListener) this.a).windowLostFocus(e);
        ((WindowFocusListener) this.b).windowLostFocus(e);
    }

    /** Se lo pasa a los dos. */
    public void actionPerformed(ActionEvent e) {
        ((ActionListener) this.a).actionPerformed(e);
        ((ActionListener) this.b).actionPerformed(e);
    }

    /** Se lo pasa a los dos. */
    public void itemStateChanged(ItemEvent e) {
        ((ItemListener) this.a).itemStateChanged(e);
        ((ItemListener) this.b).itemStateChanged(e);
    }

    /** Se lo pasa a los dos. */
    public void adjustmentValueChanged(AdjustmentEvent e) {
        ((AdjustmentListener) this.a).adjustmentValueChanged(e);
        ((AdjustmentListener) this.b).adjustmentValueChanged(e);
    }

    /** Se lo pasa a los dos. */
    public void textValueChanged(TextEvent e) {
        ((TextListener) this.a).textValueChanged(e);
        ((TextListener) this.b).textValueChanged(e);
    }

    /** Se lo pasa a los dos. */
    public void inputMethodTextChanged(InputMethodEvent e) {
        ((InputMethodListener) this.a).inputMethodTextChanged(e);
        ((InputMethodListener) this.b).inputMethodTextChanged(e);
    }

    /** Se lo pasa a los dos. */
    public void caretPositionChanged(InputMethodEvent e) {
        ((InputMethodListener) this.a).caretPositionChanged(e);
        ((InputMethodListener) this.b).caretPositionChanged(e);
    }

    /** Se lo pasa a los dos. */
    public void hierarchyChanged(HierarchyEvent e) {
        ((HierarchyListener) this.a).hierarchyChanged(e);
        ((HierarchyListener) this.b).hierarchyChanged(e);
    }

    /** Se lo pasa a los dos. */
    public void ancestorMoved(HierarchyEvent e) {
        ((HierarchyBoundsListener) this.a).ancestorMoved(e);
        ((HierarchyBoundsListener) this.b).ancestorMoved(e);
    }

    /** Se lo pasa a los dos. */
    public void ancestorResized(HierarchyEvent e) {
        ((HierarchyBoundsListener) this.a).ancestorResized(e);
        ((HierarchyBoundsListener) this.b).ancestorResized(e);
    }

    /** Se lo pasa a los dos. */
    public void mouseWheelMoved(MouseWheelEvent e) {
        ((MouseWheelListener) this.a).mouseWheelMoved(e);
        ((MouseWheelListener) this.b).mouseWheelMoved(e);
    }

    /**
     * Junta dos oyentes de ComponentListener en uno.
     *
     * @return `b` si `a` es `null`, `a` si `b` es `null`, o un par con los dos
     */
    public static ComponentListener add(ComponentListener a, ComponentListener b) {
        return (ComponentListener) addInternal(a, b);
    }

    /**
     * Junta dos oyentes de ContainerListener en uno.
     *
     * @return `b` si `a` es `null`, `a` si `b` es `null`, o un par con los dos
     */
    public static ContainerListener add(ContainerListener a, ContainerListener b) {
        return (ContainerListener) addInternal(a, b);
    }

    /**
     * Junta dos oyentes de FocusListener en uno.
     *
     * @return `b` si `a` es `null`, `a` si `b` es `null`, o un par con los dos
     */
    public static FocusListener add(FocusListener a, FocusListener b) {
        return (FocusListener) addInternal(a, b);
    }

    /**
     * Junta dos oyentes de KeyListener en uno.
     *
     * @return `b` si `a` es `null`, `a` si `b` es `null`, o un par con los dos
     */
    public static KeyListener add(KeyListener a, KeyListener b) {
        return (KeyListener) addInternal(a, b);
    }

    /**
     * Junta dos oyentes de MouseListener en uno.
     *
     * @return `b` si `a` es `null`, `a` si `b` es `null`, o un par con los dos
     */
    public static MouseListener add(MouseListener a, MouseListener b) {
        return (MouseListener) addInternal(a, b);
    }

    /**
     * Junta dos oyentes de MouseMotionListener en uno.
     *
     * @return `b` si `a` es `null`, `a` si `b` es `null`, o un par con los dos
     */
    public static MouseMotionListener add(MouseMotionListener a, MouseMotionListener b) {
        return (MouseMotionListener) addInternal(a, b);
    }

    /**
     * Junta dos oyentes de WindowListener en uno.
     *
     * @return `b` si `a` es `null`, `a` si `b` es `null`, o un par con los dos
     */
    public static WindowListener add(WindowListener a, WindowListener b) {
        return (WindowListener) addInternal(a, b);
    }

    /**
     * Junta dos oyentes de WindowStateListener en uno.
     *
     * @return `b` si `a` es `null`, `a` si `b` es `null`, o un par con los dos
     */
    public static WindowStateListener add(WindowStateListener a, WindowStateListener b) {
        return (WindowStateListener) addInternal(a, b);
    }

    /**
     * Junta dos oyentes de WindowFocusListener en uno.
     *
     * @return `b` si `a` es `null`, `a` si `b` es `null`, o un par con los dos
     */
    public static WindowFocusListener add(WindowFocusListener a, WindowFocusListener b) {
        return (WindowFocusListener) addInternal(a, b);
    }

    /**
     * Junta dos oyentes de ActionListener en uno.
     *
     * @return `b` si `a` es `null`, `a` si `b` es `null`, o un par con los dos
     */
    public static ActionListener add(ActionListener a, ActionListener b) {
        return (ActionListener) addInternal(a, b);
    }

    /**
     * Junta dos oyentes de ItemListener en uno.
     *
     * @return `b` si `a` es `null`, `a` si `b` es `null`, o un par con los dos
     */
    public static ItemListener add(ItemListener a, ItemListener b) {
        return (ItemListener) addInternal(a, b);
    }

    /**
     * Junta dos oyentes de AdjustmentListener en uno.
     *
     * @return `b` si `a` es `null`, `a` si `b` es `null`, o un par con los dos
     */
    public static AdjustmentListener add(AdjustmentListener a, AdjustmentListener b) {
        return (AdjustmentListener) addInternal(a, b);
    }

    /**
     * Junta dos oyentes de TextListener en uno.
     *
     * @return `b` si `a` es `null`, `a` si `b` es `null`, o un par con los dos
     */
    public static TextListener add(TextListener a, TextListener b) {
        return (TextListener) addInternal(a, b);
    }

    /**
     * Junta dos oyentes de InputMethodListener en uno.
     *
     * @return `b` si `a` es `null`, `a` si `b` es `null`, o un par con los dos
     */
    public static InputMethodListener add(InputMethodListener a, InputMethodListener b) {
        return (InputMethodListener) addInternal(a, b);
    }

    /**
     * Junta dos oyentes de HierarchyListener en uno.
     *
     * @return `b` si `a` es `null`, `a` si `b` es `null`, o un par con los dos
     */
    public static HierarchyListener add(HierarchyListener a, HierarchyListener b) {
        return (HierarchyListener) addInternal(a, b);
    }

    /**
     * Junta dos oyentes de HierarchyBoundsListener en uno.
     *
     * @return `b` si `a` es `null`, `a` si `b` es `null`, o un par con los dos
     */
    public static HierarchyBoundsListener add(HierarchyBoundsListener a, HierarchyBoundsListener b) {
        return (HierarchyBoundsListener) addInternal(a, b);
    }

    /**
     * Junta dos oyentes de MouseWheelListener en uno.
     *
     * @return `b` si `a` es `null`, `a` si `b` es `null`, o un par con los dos
     */
    public static MouseWheelListener add(MouseWheelListener a, MouseWheelListener b) {
        return (MouseWheelListener) addInternal(a, b);
    }

    /**
     * Saca un oyente de ComponentListener.
     *
     * @return lo que queda, que puede ser `null`
     */
    public static ComponentListener remove(ComponentListener l, ComponentListener oldl) {
        return (ComponentListener) removeInternal(l, oldl);
    }

    /**
     * Saca un oyente de ContainerListener.
     *
     * @return lo que queda, que puede ser `null`
     */
    public static ContainerListener remove(ContainerListener l, ContainerListener oldl) {
        return (ContainerListener) removeInternal(l, oldl);
    }

    /**
     * Saca un oyente de FocusListener.
     *
     * @return lo que queda, que puede ser `null`
     */
    public static FocusListener remove(FocusListener l, FocusListener oldl) {
        return (FocusListener) removeInternal(l, oldl);
    }

    /**
     * Saca un oyente de KeyListener.
     *
     * @return lo que queda, que puede ser `null`
     */
    public static KeyListener remove(KeyListener l, KeyListener oldl) {
        return (KeyListener) removeInternal(l, oldl);
    }

    /**
     * Saca un oyente de MouseListener.
     *
     * @return lo que queda, que puede ser `null`
     */
    public static MouseListener remove(MouseListener l, MouseListener oldl) {
        return (MouseListener) removeInternal(l, oldl);
    }

    /**
     * Saca un oyente de MouseMotionListener.
     *
     * @return lo que queda, que puede ser `null`
     */
    public static MouseMotionListener remove(MouseMotionListener l, MouseMotionListener oldl) {
        return (MouseMotionListener) removeInternal(l, oldl);
    }

    /**
     * Saca un oyente de WindowListener.
     *
     * @return lo que queda, que puede ser `null`
     */
    public static WindowListener remove(WindowListener l, WindowListener oldl) {
        return (WindowListener) removeInternal(l, oldl);
    }

    /**
     * Saca un oyente de WindowStateListener.
     *
     * @return lo que queda, que puede ser `null`
     */
    public static WindowStateListener remove(WindowStateListener l, WindowStateListener oldl) {
        return (WindowStateListener) removeInternal(l, oldl);
    }

    /**
     * Saca un oyente de WindowFocusListener.
     *
     * @return lo que queda, que puede ser `null`
     */
    public static WindowFocusListener remove(WindowFocusListener l, WindowFocusListener oldl) {
        return (WindowFocusListener) removeInternal(l, oldl);
    }

    /**
     * Saca un oyente de ActionListener.
     *
     * @return lo que queda, que puede ser `null`
     */
    public static ActionListener remove(ActionListener l, ActionListener oldl) {
        return (ActionListener) removeInternal(l, oldl);
    }

    /**
     * Saca un oyente de ItemListener.
     *
     * @return lo que queda, que puede ser `null`
     */
    public static ItemListener remove(ItemListener l, ItemListener oldl) {
        return (ItemListener) removeInternal(l, oldl);
    }

    /**
     * Saca un oyente de AdjustmentListener.
     *
     * @return lo que queda, que puede ser `null`
     */
    public static AdjustmentListener remove(AdjustmentListener l, AdjustmentListener oldl) {
        return (AdjustmentListener) removeInternal(l, oldl);
    }

    /**
     * Saca un oyente de TextListener.
     *
     * @return lo que queda, que puede ser `null`
     */
    public static TextListener remove(TextListener l, TextListener oldl) {
        return (TextListener) removeInternal(l, oldl);
    }

    /**
     * Saca un oyente de InputMethodListener.
     *
     * @return lo que queda, que puede ser `null`
     */
    public static InputMethodListener remove(InputMethodListener l, InputMethodListener oldl) {
        return (InputMethodListener) removeInternal(l, oldl);
    }

    /**
     * Saca un oyente de HierarchyListener.
     *
     * @return lo que queda, que puede ser `null`
     */
    public static HierarchyListener remove(HierarchyListener l, HierarchyListener oldl) {
        return (HierarchyListener) removeInternal(l, oldl);
    }

    /**
     * Saca un oyente de HierarchyBoundsListener.
     *
     * @return lo que queda, que puede ser `null`
     */
    public static HierarchyBoundsListener remove(HierarchyBoundsListener l, HierarchyBoundsListener oldl) {
        return (HierarchyBoundsListener) removeInternal(l, oldl);
    }

    /**
     * Saca un oyente de MouseWheelListener.
     *
     * @return lo que queda, que puede ser `null`
     */
    public static MouseWheelListener remove(MouseWheelListener l, MouseWheelListener oldl) {
        return (MouseWheelListener) removeInternal(l, oldl);
    }

    /**
     * Junta dos oyentes cualesquiera.
     *
     * <p>Con uno solo no se envuelve nada: envolver un oyente con `null` gastaría un objeto y una
     * indirección por evento para no agregar a nadie.
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
     * Saca un oyente de donde esté.
     *
     * @return lo que queda, que puede ser `null`
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
     * Serializa los oyentes de este par que sean serializables.
     *
     * <p>Los que no lo sean se saltean en silencio: un oyente no serializable no debería impedir
     * que el componente se guarde.
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

    /** Serializa un oyente suelto o un par, con su clave. */
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
     * Aplana el árbol y devuelve los oyentes de esa clase.
     *
     * <p>Es la única operación que recorre la estructura entera, y existe porque los
     * {@code getXListeners} de los componentes la necesitan: por dentro es un árbol, pero por fuera
     * hay que poder mostrarlo como un arreglo.
     *
     * @throws NullPointerException si la clase es `null`
     * @throws ClassCastException si la clase no es de oyente
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

    /** Cuántos oyentes de esa clase hay en el árbol. */
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

    /** Vuelca el árbol en el arreglo, en orden, y devuelve por dónde quedó. */
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
