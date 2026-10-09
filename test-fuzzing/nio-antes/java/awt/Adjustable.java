package java.awt;

import java.awt.event.AdjustmentListener;

/**
 * Something that stands for a value inside a range and can be moved: a scrollbar.
 *
 * <p>The model has four numbers and they are worth telling apart. The **value** is where it is; the
 * **minimum** and the **maximum** are the ends; and the **visible extent** is how much is seen at
 * once, which is what makes the thumb of a bar have a size instead of being a point.
 *
 * <p>Out of the extent comes a rule that surprises: the value never reaches the maximum. With a
 * range from 0 to 100 and an extent of 20, the highest possible value is 80, because from there
 * everything up to 100 is already being seen.
 */
public interface Adjustable {

    /** Horizontal orientation. */
    int HORIZONTAL = 0;

    /** Vertical orientation. */
    int VERTICAL = 1;

    /** No defined orientation. */
    int NO_ORIENTATION = 2;

    /** Horizontal or vertical. */
    int getOrientation();

    /** Changes the lower end of the range. */
    void setMinimum(int min);

    /** The lower end of the range. */
    int getMinimum();

    /** Changes the upper end of the range. */
    void setMaximum(int max);

    /** The upper end of the range. */
    int getMaximum();

    /** Changes how much it moves with a small step. */
    void setUnitIncrement(int u);

    /** How much it moves with a small step. */
    int getUnitIncrement();

    /** Changes how much it moves with a big step. */
    void setBlockIncrement(int b);

    /** How much it moves with a big step. */
    int getBlockIncrement();

    /** Changes how much is seen at once. */
    void setVisibleAmount(int v);

    /** How much is seen at once. */
    int getVisibleAmount();

    /**
     * Changes where it is.
     *
     * <p>A value outside `[minimum, maximum - extent]` is clamped to that range.
     */
    void setValue(int v);

    /** Where it is. */
    int getValue();

    /** Adds someone to tell about the changes. */
    void addAdjustmentListener(AdjustmentListener l);

    /** Removes that listener. */
    void removeAdjustmentListener(AdjustmentListener l);
}
