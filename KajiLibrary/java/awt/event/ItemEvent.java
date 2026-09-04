package java.awt.event;

import java.awt.AWTEvent;
import java.awt.ItemSelectable;

/**
 * Se eligió o se dejó de elegir un elemento.
 *
 * <p>Trae **el elemento** y si quedó elegido o no, en vez de traer la lista entera. Es lo que hace
 * que atender el evento sea barato en una lista de miles: se avisa lo que cambió, no lo que hay.
 */
public class ItemEvent extends AWTEvent {

    private static final long serialVersionUID = -608708132447206933L;

    /** El elemento dejó de estar elegido. */
    public static final int DESELECTED = 2;

    /** El primer identificador de la familia. */
    public static final int ITEM_FIRST = 701;

    /** El último identificador de la familia. */
    public static final int ITEM_LAST = 701;

    /** Cambió qué está elegido. */
    public static final int ITEM_STATE_CHANGED = 701;

    /** El elemento quedó elegido. */
    public static final int SELECTED = 1;

    private final Object item;
    private final int stateChange;

    /**
     * Con la fuente, el elemento y en qué quedó.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public ItemEvent(ItemSelectable source, int id, Object item, int stateChange) {
        super(source, id);
        this.item = item;
        this.stateChange = stateChange;
    }

    /** De dónde salió. */
    public ItemSelectable getItemSelectable() {
        return (ItemSelectable) this.source;
    }

    /** Qué elemento cambió. */
    public Object getItem() {
        return this.item;
    }

    /** {@link #SELECTED} o {@link #DESELECTED}. */
    public int getStateChange() {
        return this.stateChange;
    }

    public String paramString() {
        String tipo = this.id == ITEM_STATE_CHANGED ? "ITEM_STATE_CHANGED" : "unknown type";
        String estado;
        if (this.stateChange == SELECTED) {
            estado = "SELECTED";
        } else if (this.stateChange == DESELECTED) {
            estado = "DESELECTED";
        } else {
            estado = "unknown type";
        }
        return tipo + ",item=" + this.item + ",stateChange=" + estado;
    }
}
