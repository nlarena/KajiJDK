package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.CompoundControl -- a group of knobs that go together.
 *
 * <p>It has no value of its own: it is a container. It serves so that an interface can group what
 * is grouped in the real device -- the equalizer, with its bands; a console channel, with its
 * volume, its balance and its mute.
 *
 * <p>The members can in turn be compound, so this is a tree. A walk has to account for it.
 *
 * <p>There is no way to change its members after building it, and that is on purpose: the structure
 * is set by the device.
 */
public abstract class CompoundControl extends Control {

    /** The knobs it groups. */
    private final Control[] controls;

    /** @param memberControls the knobs of the group */
    protected CompoundControl(Type type, Control[] memberControls) {
        super(type);
        this.controls = memberControls;
    }

    /**
     * The knobs of the group.
     *
     * <p>It returns a copy of the array, so modifying what comes out does not change the control.
     */
    public Control[] getMemberControls() {
        Control[] copy = new Control[this.controls.length];
        System.arraycopy(this.controls, 0, copy, 0, this.controls.length);
        return copy;
    }

    /** The control's, plus the types of its members in brackets. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        int i = 0;
        while (i < this.controls.length) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(this.controls[i].getType());
            i = i + 1;
        }
        sb.append(']');
        return super.toString() + " containing " + sb + " controls";
    }

    /**
     * The kinds of group.
     *
     * <p>It brings none predefined: the groups that exist depend entirely on the device, and naming
     * a few would have been arbitrary.
     */
    public static class Type extends Control.Type {

        /** Protected: the types are defined by whoever provides the mixer. */
        protected Type(String name) {
            super(name);
        }
    }
}
