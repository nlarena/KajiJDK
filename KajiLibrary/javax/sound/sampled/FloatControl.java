package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.FloatControl -- a continuous knob.
 *
 * <p>Volume, balance, pan. It carries a range, a precision, units, and three labels for the ends
 * and the centre.
 *
 * <h2>The precision is not the step</h2>
 *
 * <p>{@link #getPrecision} is the <b>smallest difference the device can represent</b>. Setting a
 * value that is not a multiple of it does not fail: it is rounded to the nearest one that is. That
 * is why {@link #getValue} after {@link #setValue} can return something else, and comparing for
 * equality there is a mistake.
 *
 * <h2>{@link #shift} is a gradual change, not an immediate one</h2>
 *
 * <p>It asks to go from one value to another in a certain time. It exists because an abrupt change
 * of volume produces an audible click, and doing it by hand from Java --with a thread and pauses--
 * sounds worse still.
 *
 * <p>The device may not support it, and then it jumps straight to the final value. The
 * documentation explicitly allows it, so a program cannot rely on the fade.
 */
public abstract class FloatControl extends Control {

    /** The minimum. */
    private final float minimum;

    /** The maximum. */
    private final float maximum;

    /** The smallest representable difference. See the class note. */
    private final float precision;

    /** Every how many microseconds it changes during a {@link #shift}. */
    private final int updatePeriod;

    /** In which units the value is. */
    private final String units;

    /** How to show the minimum. */
    private final String minLabel;

    /** How to show the centre. */
    private final String midLabel;

    /** How to show the maximum. */
    private final String maxLabel;

    /** The current value. */
    private float value;

    /** The full constructor. */
    protected FloatControl(Type type, float minimum, float maximum, float precision,
                           int updatePeriod, float initialValue, String units, String minLabel,
                           String midLabel, String maxLabel) {
        super(type);
        this.minimum = minimum;
        this.maximum = maximum;
        this.precision = precision;
        this.updatePeriod = updatePeriod;
        this.value = initialValue;
        this.units = units;
        this.minLabel = minLabel;
        this.midLabel = midLabel;
        this.maxLabel = maxLabel;
    }

    /** Likewise, with the three labels empty. */
    protected FloatControl(Type type, float minimum, float maximum, float precision,
                           int updatePeriod, float initialValue, String units) {
        this(type, minimum, maximum, precision, updatePeriod, initialValue, units, "", "", "");
    }

    /**
     * Sets the value.
     *
     * @throws IllegalArgumentException if it is out of range
     */
    public void setValue(float newValue) {
        if (newValue > this.maximum || newValue < this.minimum) {
            if (newValue > this.maximum) {
                throw new IllegalArgumentException("Requested value " + newValue
                    + " exceeds allowable maximum value " + this.maximum + ".");
            }
            throw new IllegalArgumentException("Requested value " + newValue
                + " is smaller than allowable minimum value " + this.minimum + ".");
        }
        this.value = newValue;
    }

    /** The current value. See the class note: it may not be the one that was set. */
    public float getValue() {
        return this.value;
    }

    /** The maximum. */
    public float getMaximum() {
        return this.maximum;
    }

    /** The minimum. */
    public float getMinimum() {
        return this.minimum;
    }

    /** In which units it is. */
    public String getUnits() {
        return this.units;
    }

    /** How to show the minimum. */
    public String getMinLabel() {
        return this.minLabel;
    }

    /** How to show the centre. */
    public String getMidLabel() {
        return this.midLabel;
    }

    /** How to show the maximum. */
    public String getMaxLabel() {
        return this.maxLabel;
    }

    /** The smallest representable difference. See the class note. */
    public float getPrecision() {
        return this.precision;
    }

    /** Every how many microseconds it changes during a {@link #shift}. */
    public int getUpdatePeriod() {
        return this.updatePeriod;
    }

    /**
     * Goes from one value to another in that time.
     *
     * <p>This implementation jumps straight to the final value, which is what the documentation
     * allows when the device cannot make the gradual change. See the class note.
     *
     * @param microseconds how long it should take
     * @throws IllegalArgumentException if either of the two values is out of range
     */
    public void shift(float from, float to, int microseconds) {
        setValue(from);
        setValue(to);
    }

    /** The control's, the value with its units, and the range. */
    @Override
    public String toString() {
        return super.toString() + " with current value: " + getValue() + " " + getUnits()
            + " (range: " + getMinimum() + " - " + getMaximum() + ")";
    }

    /**
     * The kinds of continuous knob the platform names.
     *
     * <p>{@link #VOLUME} and {@link #MASTER_GAIN} get confused: the first is the volume of one
     * line, the second the mixer's overall gain. Changing the wrong one affects other lines.
     *
     * <p>{@link #PAN} and {@link #BALANCE} are not the same either. Pan places a <b>mono</b> source
     * between the two speakers; balance adjusts the proportion between the channels of something
     * that <b>is already stereo</b>. Applying pan to stereo material collapses it to mono.
     */
    public static class Type extends Control.Type {

        /** Overall gain of the mixer, in decibels. See the class note. */
        public static final Type MASTER_GAIN = new Type("Master Gain");

        /** How much is sent to the auxiliary effect. */
        public static final Type AUX_SEND = new Type("AUX Send");

        /** How much comes back from the auxiliary effect. */
        public static final Type AUX_RETURN = new Type("AUX Return");

        /** How much is sent to the reverb. */
        public static final Type REVERB_SEND = new Type("Reverb Send");

        /** How much comes back from the reverb. */
        public static final Type REVERB_RETURN = new Type("Reverb Return");

        /** Volume of this line. See the class note. */
        public static final Type VOLUME = new Type("Volume");

        /** Placement of a mono source between the speakers. See the class note. */
        public static final Type PAN = new Type("Pan");

        /** Proportion between the channels of a stereo source. */
        public static final Type BALANCE = new Type("Balance");

        /** Playback rate, which changes the pitch. */
        public static final Type SAMPLE_RATE = new Type("Sample Rate");

        /** Protected: the types are defined by whoever provides the mixer. */
        protected Type(String name) {
            super(name);
        }
    }
}
