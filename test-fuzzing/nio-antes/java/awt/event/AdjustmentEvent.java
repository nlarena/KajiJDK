package java.awt.event;

import java.awt.AWTEvent;
import java.awt.Adjustable;

/**
 * A scrollbar was moved.
 *
 * <p>It brings **how** it moved --a small step, a big step, a drag-- on top of where it ended up,
 * because not every way of moving deserves the same reaction.
 *
 * <p>{@link #getValueIsAdjusting} is the part that saves work: while the user drags the thumb dozens
 * of events arrive with that flag on, and whoever receives them can put off the expensive part
 * --laying out again, reading from disk again-- until it goes off.
 */
public class AdjustmentEvent extends AWTEvent {

    private static final long serialVersionUID = 5700290645205279921L;

    /** The family's first identifier. */
    public static final int ADJUSTMENT_FIRST = 601;

    /** The family's last identifier. */
    public static final int ADJUSTMENT_LAST = 601;

    /** The value changed. */
    public static final int ADJUSTMENT_VALUE_CHANGED = 601;

    /** A big step backwards. */
    public static final int BLOCK_DECREMENT = 3;

    /** A big step forwards. */
    public static final int BLOCK_INCREMENT = 4;

    /** A drag of the thumb. */
    public static final int TRACK = 5;

    /** A small step backwards. */
    public static final int UNIT_DECREMENT = 2;

    /** A small step forwards. */
    public static final int UNIT_INCREMENT = 1;

    private final int adjustmentType;
    private final int value;
    private final boolean isAdjusting;

    /**
     * With the source, the kind of movement and the value.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public AdjustmentEvent(Adjustable source, int id, int type, int value) {
        this(source, id, type, value, false);
    }

    /**
     * Like the previous one, saying whether the movement is still under way.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public AdjustmentEvent(Adjustable source, int id, int type, int value, boolean isAdjusting) {
        super(source, id);
        this.adjustmentType = type;
        this.value = value;
        this.isAdjusting = isAdjusting;
    }

    /** Where it came from. */
    public Adjustable getAdjustable() {
        return (Adjustable) this.source;
    }

    /** Where it ended up. */
    public int getValue() {
        return this.value;
    }

    /** How it moved. */
    public int getAdjustmentType() {
        return this.adjustmentType;
    }

    /** Whether the movement is still under way. */
    public boolean getValueIsAdjusting() {
        return this.isAdjusting;
    }

    public String paramString() {
        String type = this.id == ADJUSTMENT_VALUE_CHANGED ? "ADJUSTMENT_VALUE_CHANGED"
                : "unknown type";
        String how;
        if (this.adjustmentType == UNIT_INCREMENT) {
            how = "UNIT_INCREMENT";
        } else if (this.adjustmentType == UNIT_DECREMENT) {
            how = "UNIT_DECREMENT";
        } else if (this.adjustmentType == BLOCK_INCREMENT) {
            how = "BLOCK_INCREMENT";
        } else if (this.adjustmentType == BLOCK_DECREMENT) {
            how = "BLOCK_DECREMENT";
        } else if (this.adjustmentType == TRACK) {
            how = "TRACK";
        } else {
            how = "unknown type";
        }
        return type + ",adjType=" + how + ",value=" + this.value + ",isAdjusting="
                + this.isAdjusting;
    }
}
