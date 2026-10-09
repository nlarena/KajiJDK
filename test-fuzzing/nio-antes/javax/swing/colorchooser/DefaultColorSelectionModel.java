package javax.swing.colorchooser;

import java.awt.Color;
import java.io.Serializable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * The usual implementation of {@link ColorSelectionModel}.
 *
 * <p>A colour and a list of listeners, and nothing else. The two decisions worth looking at:
 *
 * <ul>
 *   <li>The {@link ChangeEvent} is built **once** --on the first notification-- and reused. It is
 *       an event with no data --all it says is "it changed", and whoever receives it asks the
 *       model-- so reusing it loses nothing and avoids garbage in a component that emits while
 *       the mouse is dragged.
 *   <li>The listeners are walked **from the back to the front**. {@link EventListenerList} keeps
 *       (class, listener) pairs in a single array, and walking it backwards is what allows
 *       notifying over a stable copy even if somebody removes themselves meanwhile.
 * </ul>
 */
public class DefaultColorSelectionModel implements ColorSelectionModel, Serializable {


    /** The only event, reused. See the class note. */
    protected transient ChangeEvent changeEvent = null;

    /** The listeners. */
    protected EventListenerList listenerList = new EventListenerList();

    private Color selectedColor;

    /** A model with white chosen. */
    public DefaultColorSelectionModel() {
        this.selectedColor = Color.white;
    }

    /**
     * A model with that colour chosen.
     *
     * @throws NullPointerException if it is null
     */
    public DefaultColorSelectionModel(Color color) {
        if (color == null) {
            throw new NullPointerException("color");
        }
        this.selectedColor = color;
    }

    /** The chosen colour. */
    public Color getSelectedColor() {
        return this.selectedColor;
    }

    /**
     * Picks that colour, and reports it if it really changed.
     *
     * <p>A `null` is taken as white, which is a divergence: JDK 25 <b>ignores</b> a null --it keeps
     * the colour it had and fires nothing-- and its javadoc calls the behaviour of a null
     * undefined. Here it is defined, because the model always has to have a colour and whoever
     * writes `null` almost always means "go back to the starting one"; a listener therefore hears a
     * change the JDK would not report.
     */
    public void setSelectedColor(Color color) {
        Color newColor = color == null ? Color.white : color;
        if (!newColor.equals(this.selectedColor)) {
            this.selectedColor = newColor;
            fireStateChanged();
        }
    }

    /** Adds a listener of the colour change. */
    public void addChangeListener(ChangeListener l) {
        this.listenerList.add(ChangeListener.class, l);
    }

    /** Removes a listener of the colour change. */
    public void removeChangeListener(ChangeListener l) {
        this.listenerList.remove(ChangeListener.class, l);
    }

    /** The listeners added, or an empty array if there are none. */
    public ChangeListener[] getChangeListeners() {
        return this.listenerList.getListeners(ChangeListener.class);
    }

    /** Tells the listeners that the colour changed. See the class note. */
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
