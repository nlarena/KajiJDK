package java.awt;

import java.io.Serializable;

/**
 * Turns a group of {@link Checkbox} into radio buttons: exactly one ticked at a time.
 *
 * <p>AWT has no radio button class. It has this: the same box as always, with a group that takes
 * care that ticking one unticks the previous one. It is an arguable design decision —the widget
 * changes shape depending on whether it has a group— but it is the one there is.
 *
 * <p>Once something is ticked, the group **cannot be emptied** from the interface: pressing the
 * ticked box does not untick it. From a program it can, by passing `null` to
 * {@link #setSelectedCheckbox}.
 */
public class CheckboxGroup implements Serializable {

    private static final long serialVersionUID = 3729780091441768983L;

    /** The ticked box, or `null` if there is none. */
    Checkbox selectedCheckbox;

    /** An empty group. */
    public CheckboxGroup() {
    }

    /**
     * The ticked box.
     *
     * @return the box, or `null` if there is none
     */
    public Checkbox getSelectedCheckbox() {
        return this.selectedCheckbox;
    }

    /**
     * The ticked box.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #getSelectedCheckbox}.
     */
    @Deprecated
    public Checkbox getCurrent() {
        return this.selectedCheckbox;
    }

    /**
     * Ticks that box and unticks whichever one was ticked.
     *
     * <p>A box that belongs to **another** group is ignored: accepting it would leave two groups
     * believing they are in charge of it.
     *
     * @param box the box to tick, or `null` to leave the group with nothing ticked
     */
    public synchronized void setSelectedCheckbox(Checkbox box) {
        if (box != null && box.group != this) {
            return;
        }
        Checkbox previous = this.selectedCheckbox;
        this.selectedCheckbox = box;
        if (previous != null && previous != box) {
            previous.setStateInternal(false);
        }
        if (box != null) {
            box.setStateInternal(true);
        }
    }

    /**
     * Ticks that box.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #setSelectedCheckbox}.
     */
    @Deprecated
    public synchronized void setCurrent(Checkbox box) {
        this.setSelectedCheckbox(box);
    }

    public String toString() {
        return this.getClass().getName() + "[selectedCheckbox=" + this.selectedCheckbox + "]";
    }
}
