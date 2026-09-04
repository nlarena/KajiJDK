package javax.swing;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * Quien lleva la cuenta de que menu esta abierto.
 *
 * <h2>Por que hace falta un administrador y no alcanza con cada menu</h2>
 *
 * <p>Porque un menu abierto <strong>captura</strong> el mouse y el teclado de toda la aplicacion:
 * hacer clic en cualquier lado tiene que cerrarlo, y mover el mouse de un submenu al de al lado
 * tiene que cerrar el primero y abrir el segundo. Ninguno de los dos menus puede decidir eso solo —
 * cada uno ve nada mas que sus propios eventos.
 *
 * <p>El estado que resuelve todo eso es <em>uno</em>: el camino seleccionado, de la barra hasta el
 * item mas profundo. Abrir, cerrar y navegar son cambios de ese camino, y por eso el administrador
 * es un singleton por aplicacion.
 *
 * <h2>Lo que esta VM no hace</h2>
 *
 * <p>{@link #processMouseEvent} y {@link #processKeyEvent} reparten al camino, que es su trabajo
 * completo. {@link #componentForPoint} necesita saber donde esta cada componente en pantalla y
 * eso pide un sistema de ventanas que esta VM no tiene: devuelve {@code null}, y lo dice.
 */
public class MenuSelectionManager {

    private static MenuSelectionManager elUnico;

    /** El evento que se reusa; ver {@link #fireStateChanged}. */
    protected transient ChangeEvent changeEvent = null;

    /** Los oyentes. */
    protected EventListenerList listenerList = new EventListenerList();

    private MenuElement[] selection = new MenuElement[0];

    /** Un administrador nuevo. Lo normal es pedir el de {@link #defaultManager}. */
    public MenuSelectionManager() {
    }

    /** El administrador de la aplicacion. */
    public static MenuSelectionManager defaultManager() {
        if (elUnico == null) {
            elUnico = new MenuSelectionManager();
        }
        return elUnico;
    }

    /**
     * Cambia el camino seleccionado.
     *
     * <p>Avisa a los elementos que <strong>entraron</strong> y a los que <strong>salieron</strong>,
     * y para eso compara con el camino anterior desde la raiz hasta donde los dos coinciden. Avisar
     * a todos en cada cambio haria que un submenu se cierre y se reabra al mover el mouse un pixel.
     */
    public void setSelectedPath(MenuElement[] path) {
        MenuElement[] nuevo = path == null ? new MenuElement[0] : path;
        int comun = 0;
        while (comun < this.selection.length && comun < nuevo.length
                && this.selection[comun] == nuevo[comun]) {
            comun = comun + 1;
        }
        for (int i = this.selection.length - 1; i >= comun; i--) {
            this.selection[i].menuSelectionChanged(false);
        }
        MenuElement[] copia = new MenuElement[nuevo.length];
        for (int i = 0; i < nuevo.length; i++) {
            copia[i] = nuevo[i];
        }
        this.selection = copia;
        for (int i = comun; i < this.selection.length; i++) {
            this.selection[i].menuSelectionChanged(true);
        }
        fireStateChanged();
    }

    /** El camino seleccionado, en un arreglo nuevo. */
    public MenuElement[] getSelectedPath() {
        MenuElement[] copia = new MenuElement[this.selection.length];
        for (int i = 0; i < this.selection.length; i++) {
            copia[i] = this.selection[i];
        }
        return copia;
    }

    /** Cierra todo. */
    public void clearSelectedPath() {
        if (this.selection.length > 0) {
            setSelectedPath(null);
        }
    }

    /** Agrega un oyente de cambios del camino. */
    public void addChangeListener(ChangeListener l) {
        this.listenerList.add(ChangeListener.class, l);
    }

    /** Saca un oyente. */
    public void removeChangeListener(ChangeListener l) {
        this.listenerList.remove(ChangeListener.class, l);
    }

    /** Los oyentes de cambio. */
    public ChangeListener[] getChangeListeners() {
        return this.listenerList.getListeners(ChangeListener.class);
    }

    /**
     * Avisa que el camino cambio.
     *
     * <p>El {@link ChangeEvent} se crea una sola vez y se reusa: no lleva ningun dato mas que su
     * origen, que siempre es este objeto, asi que alocar uno nuevo por aviso seria basura pura. Es
     * la convencion de todo Swing.
     */
    protected void fireStateChanged() {
        Object[] oyentes = this.listenerList.getListenerList();
        for (int i = oyentes.length - 2; i >= 0; i = i - 2) {
            if (oyentes[i] == ChangeListener.class) {
                if (this.changeEvent == null) {
                    this.changeEvent = new ChangeEvent(this);
                }
                ChangeListener l = (ChangeListener) oyentes[i + 1];
                l.stateChanged(this.changeEvent);
            }
        }
    }

    /** Reparte un evento de mouse a todo el camino, del mas profundo al mas superficial. */
    public void processMouseEvent(MouseEvent event) {
        MenuElement[] camino = getSelectedPath();
        for (int i = camino.length - 1; i >= 0; i--) {
            camino[i].processMouseEvent(event, camino, this);
            if (event.isConsumed()) {
                return;
            }
        }
    }

    /** Reparte un evento de teclado a todo el camino. */
    public void processKeyEvent(KeyEvent event) {
        MenuElement[] camino = getSelectedPath();
        for (int i = camino.length - 1; i >= 0; i--) {
            camino[i].processKeyEvent(event, camino, this);
            if (event.isConsumed()) {
                return;
            }
        }
    }

    /**
     * Que componente del menu esta bajo ese punto.
     *
     * @return {@code null} siempre en esta VM: hace falta la posicion de cada componente en
     *     pantalla, que la da el sistema de ventanas. Ver la nota de la clase.
     */
    public Component componentForPoint(Component source, Point sourcePoint) {
        return null;
    }

    /** Si {@code c} es parte del menu abierto. */
    public boolean isComponentPartOfCurrentMenu(Component c) {
        if (this.selection.length == 0) {
            return false;
        }
        return esParte(this.selection[0], c);
    }

    private boolean esParte(MenuElement raiz, Component c) {
        if (raiz == null) {
            return false;
        }
        if (raiz.getComponent() == c) {
            return true;
        }
        MenuElement[] hijos = raiz.getSubElements();
        for (int i = 0; i < hijos.length; i++) {
            if (esParte(hijos[i], c)) {
                return true;
            }
        }
        return false;
    }
}
