package java.awt;

import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Set;

/**
 * El gestor de foco que trae AWT puesto.
 *
 * <p>Le pone comportamiento a los abstractos de {@link KeyboardFocusManager}: reparte los eventos de
 * teclado por la cadena de repartidores, atiende las teclas de recorrido, y guarda los eventos que
 * llegan mientras el foco está en tránsito para entregárselos al componente correcto.
 *
 * <p>La cola de eventos en tránsito es lo menos obvio y lo más necesario. Cuando el foco se pide, la
 * respuesta del sistema de ventanas tarda; en el medio pueden llegar teclas. Entregárselas al que
 * todavía tiene el foco sería escribir en el campo que se está por dejar. Por eso
 * {@link #enqueueKeyEvents} las aparta hasta que {@link #dequeueKeyEvents} avisa que el foco llegó.
 *
 * <p>Sin sistema de ventanas ese tránsito no ocurre nunca —nadie pide el foco de verdad— así que la
 * cola queda vacía. La mecánica está entera igual: si algo llama a `enqueueKeyEvents`, se guarda, y
 * `dequeueKeyEvents` lo suelta.
 */
public class DefaultKeyboardFocusManager extends KeyboardFocusManager {

    /** Un evento apartado esperando que el foco llegue a su destinatario. */
    private static final class Apartado {
        private final long marca;
        private final Component destino;
        private final ArrayList<KeyEvent> eventos = new ArrayList<KeyEvent>();

        private Apartado(long marca, Component destino) {
            this.marca = marca;
            this.destino = destino;
        }
    }

    /** Los tramos apartados, en el orden en que se pidieron. */
    private final ArrayList<Apartado> apartados = new ArrayList<Apartado>();

    /** Un gestor de fábrica. */
    public DefaultKeyboardFocusManager() {
    }

    /**
     * Reparte el evento al componente que corresponda.
     *
     * <p>Los eventos de foco y de ventana **actualizan el estado global** además de entregarse: es
     * acá donde el gestor se entera de que el foco se movió. Los demás se entregan y ya.
     *
     * @return `true` si lo entregó
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
     * Reparte un evento de teclado por la cadena de repartidores y después al que tiene el foco.
     *
     * @return `true` si alguien lo consumió
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
        Component destino = this.getFocusOwner();
        if (destino == null && e.getSource() instanceof Component) {
            destino = (Component) e.getSource();
        }
        if (destino == null) {
            return false;
        }
        // El tránsito manda: si hay un tramo apartado esperando, la tecla se guarda en vez de
        // entregarse. Si no, se atienden primero las teclas de recorrido y recién después se entrega.
        synchronized (this) {
            if (!this.apartados.isEmpty()) {
                this.apartados.get(0).eventos.add(e);
                return true;
            }
        }
        this.processKeyEvent(destino, e);
        if (e.isConsumed()) {
            return true;
        }
        this.redispatchEvent(destino, e);
        return e.isConsumed();
    }

    /**
     * Le da el evento a los posprocesadores.
     *
     * @return `true` si alguno lo consumió
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
     * Atiende las teclas de recorrido de ese componente.
     *
     * <p>Si la tecla es una de las cuatro de recorrido, mueve el foco y **consume el evento**: si no
     * lo consumiera, el Tab llegaría además al componente y escribiría una tabulación.
     */
    public void processKeyEvent(Component focusedComponent, KeyEvent e) {
        if (e.getID() != KeyEvent.KEY_PRESSED || focusedComponent == null) {
            return;
        }
        AWTKeyStroke k = AWTKeyStroke.getAWTKeyStrokeForEvent(e);
        for (int id = 0; id < 4; id++) {
            Set<AWTKeyStroke> teclas = focusedComponent.getFocusTraversalKeys(id);
            if (teclas == null || !teclas.contains(k)) {
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
     * Aparta los eventos de teclado hasta que el foco llegue a ese componente.
     *
     * @param after la marca de tiempo desde la que apartar
     * @param untilFocused el componente que va a recibir el foco
     */
    protected synchronized void enqueueKeyEvents(long after, Component untilFocused) {
        this.apartados.add(new Apartado(after, untilFocused));
    }

    /**
     * Suelta los eventos apartados para ese componente: el foco ya llegó.
     *
     * <p>Los eventos soltados se entregan **en orden**, que es lo único que los hace útiles: una
     * ráfaga de teclas tiene que llegar como se escribió.
     */
    protected synchronized void dequeueKeyEvents(long after, Component untilFocused) {
        int i = this.buscar(after, untilFocused);
        if (i < 0) {
            return;
        }
        Apartado a = this.apartados.remove(i);
        for (int j = 0; j < a.eventos.size(); j++) {
            KeyEvent e = a.eventos.get(j);
            if (a.destino != null) {
                this.redispatchEvent(a.destino, e);
            }
        }
    }

    /**
     * Tira los eventos apartados para ese componente.
     *
     * <p>Es lo que corresponde cuando el foco **no** va a llegarle —el pedido se canceló, o el
     * componente se sacó del árbol—: entregárselos igual sería darle teclas que el usuario escribió
     * para otro.
     */
    protected synchronized void discardKeyEvents(Component comp) {
        int i = 0;
        while (i < this.apartados.size()) {
            if (this.apartados.get(i).destino == comp) {
                this.apartados.remove(i);
            } else {
                i = i + 1;
            }
        }
    }

    /** El tramo apartado para ese componente desde esa marca, o -1. */
    private int buscar(long after, Component untilFocused) {
        for (int i = 0; i < this.apartados.size(); i++) {
            Apartado a = this.apartados.get(i);
            if (a.destino == untilFocused && a.marca == after) {
                return i;
            }
        }
        return -1;
    }

    /** Le pasa el foco al siguiente del recorrido. */
    public void focusNextComponent(Component aComponent) {
        if (aComponent != null) {
            aComponent.transferFocus();
        }
    }

    /** Se lo pasa al anterior. */
    public void focusPreviousComponent(Component aComponent) {
        if (aComponent != null) {
            aComponent.transferFocusBackward();
        }
    }

    /** Sube un nivel de ciclo de foco. */
    public void upFocusCycle(Component aComponent) {
        if (aComponent != null) {
            aComponent.transferFocusUpCycle();
        }
    }

    /** Baja un nivel, entrando en ese contenedor. */
    public void downFocusCycle(Container aContainer) {
        if (aContainer != null && aContainer.isFocusCycleRoot()) {
            aContainer.transferFocusDownCycle();
        }
    }
}
