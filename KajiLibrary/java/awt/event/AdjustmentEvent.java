package java.awt.event;

import java.awt.AWTEvent;
import java.awt.Adjustable;

/**
 * Se movió una barra de desplazamiento.
 *
 * <p>Trae **cómo** se movió —un paso chico, un paso grande, un arrastre— además de a dónde llegó,
 * porque no todas las formas de mover merecen la misma reacción.
 *
 * <p>{@link #getValueIsAdjusting} es la parte que ahorra trabajo: mientras el usuario arrastra el
 * pulgar llegan decenas de eventos con esa bandera prendida, y quien reciba puede postergar lo caro
 * —volver a maquetar, releer de disco— hasta que se apague.
 */
public class AdjustmentEvent extends AWTEvent {

    private static final long serialVersionUID = 5700290645205279921L;

    /** El primer identificador de la familia. */
    public static final int ADJUSTMENT_FIRST = 601;

    /** El último identificador de la familia. */
    public static final int ADJUSTMENT_LAST = 601;

    /** Cambió el valor. */
    public static final int ADJUSTMENT_VALUE_CHANGED = 601;

    /** Un paso grande hacia atrás. */
    public static final int BLOCK_DECREMENT = 3;

    /** Un paso grande hacia adelante. */
    public static final int BLOCK_INCREMENT = 4;

    /** Un arrastre del pulgar. */
    public static final int TRACK = 5;

    /** Un paso chico hacia atrás. */
    public static final int UNIT_DECREMENT = 2;

    /** Un paso chico hacia adelante. */
    public static final int UNIT_INCREMENT = 1;

    private final int adjustmentType;
    private final int value;
    private final boolean isAdjusting;

    /**
     * Con la fuente, el tipo de movimiento y el valor.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public AdjustmentEvent(Adjustable source, int id, int type, int value) {
        this(source, id, type, value, false);
    }

    /**
     * Como el anterior, diciendo si el movimiento todavía está en curso.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public AdjustmentEvent(Adjustable source, int id, int type, int value, boolean isAdjusting) {
        super(source, id);
        this.adjustmentType = type;
        this.value = value;
        this.isAdjusting = isAdjusting;
    }

    /** De dónde salió. */
    public Adjustable getAdjustable() {
        return (Adjustable) this.source;
    }

    /** A dónde llegó. */
    public int getValue() {
        return this.value;
    }

    /** Cómo se movió. */
    public int getAdjustmentType() {
        return this.adjustmentType;
    }

    /** Si el movimiento todavía está en curso. */
    public boolean getValueIsAdjusting() {
        return this.isAdjusting;
    }

    public String paramString() {
        String tipo = this.id == ADJUSTMENT_VALUE_CHANGED ? "ADJUSTMENT_VALUE_CHANGED"
                : "unknown type";
        String como;
        if (this.adjustmentType == UNIT_INCREMENT) {
            como = "UNIT_INCREMENT";
        } else if (this.adjustmentType == UNIT_DECREMENT) {
            como = "UNIT_DECREMENT";
        } else if (this.adjustmentType == BLOCK_INCREMENT) {
            como = "BLOCK_INCREMENT";
        } else if (this.adjustmentType == BLOCK_DECREMENT) {
            como = "BLOCK_DECREMENT";
        } else if (this.adjustmentType == TRACK) {
            como = "TRACK";
        } else {
            como = "unknown type";
        }
        return tipo + ",adjType=" + como + ",value=" + this.value + ",isAdjusting="
                + this.isAdjusting;
    }
}
