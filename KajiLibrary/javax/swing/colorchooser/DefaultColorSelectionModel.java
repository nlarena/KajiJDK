package javax.swing.colorchooser;

import java.awt.Color;
import java.io.Serializable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * La implementacion de siempre de {@link ColorSelectionModel}.
 *
 * <p>Un color y una lista de escuchas, y nada mas. Las dos decisiones que vale la pena mirar:
 *
 * <ul>
 *   <li>El {@link ChangeEvent} se fabrica **una sola vez** y se reusa. Es un evento sin datos --lo
 *       unico que dice es "cambio", y quien lo recibe le pregunta al modelo-- asi que reusarlo no
 *       pierde nada y evita basura en un componente que emite mientras se arrastra el mouse.
 *   <li>Los escuchas se recorren **de atras para adelante**. {@link EventListenerList} guarda pares
 *       (clase, escucha) en un solo arreglo, y recorrerlo al reves es lo que permite avisar sobre
 *       una copia estable aunque alguien se de de baja mientras tanto.
 * </ul>
 */
public class DefaultColorSelectionModel implements ColorSelectionModel, Serializable {


    /** El unico evento, reusado. Ver la nota de la clase. */
    protected transient ChangeEvent changeEvent = null;

    /** Los escuchas. */
    protected EventListenerList listenerList = new EventListenerList();

    private Color selectedColor;

    /** Un modelo con el blanco elegido. */
    public DefaultColorSelectionModel() {
        this.selectedColor = Color.white;
    }

    /**
     * Un modelo con ese color elegido.
     *
     * @throws NullPointerException si es nulo
     */
    public DefaultColorSelectionModel(Color color) {
        if (color == null) {
            throw new NullPointerException("color");
        }
        this.selectedColor = color;
    }

    /** El color elegido. */
    public Color getSelectedColor() {
        return this.selectedColor;
    }

    /**
     * Elige ese color, y avisa si de verdad cambio.
     *
     * <p>Un `null` se toma como blanco --el JDK hace lo mismo-- en vez de lanzar: el modelo tiene
     * que tener siempre un color, y quien lo pone en `null` casi siempre quiere "volve al de
     * arranque".
     */
    public void setSelectedColor(Color color) {
        Color nuevo = color == null ? Color.white : color;
        if (!nuevo.equals(this.selectedColor)) {
            this.selectedColor = nuevo;
            fireStateChanged();
        }
    }

    /** Agrega un escucha del cambio de color. */
    public void addChangeListener(ChangeListener l) {
        this.listenerList.add(ChangeListener.class, l);
    }

    /** Saca un escucha del cambio de color. */
    public void removeChangeListener(ChangeListener l) {
        this.listenerList.remove(ChangeListener.class, l);
    }

    /** Los escuchas agregados, o un arreglo vacio si no hay. */
    public ChangeListener[] getChangeListeners() {
        return this.listenerList.getListeners(ChangeListener.class);
    }

    /** Avisa a los escuchas que el color cambio. Ver la nota de la clase. */
    protected void fireStateChanged() {
        Object[] listeners = this.listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                if (this.changeEvent == null) {
                    this.changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener) listeners[i + 1]).stateChanged(this.changeEvent);
            }
        }
    }
}
