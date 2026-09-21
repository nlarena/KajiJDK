package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.BooleanControl -- a two-position knob.
 *
 * <p>Mute, reverb on or off. Besides the value it carries the <b>labels</b> of each state, so that
 * an interface can show "Mute"/"Sound" instead of "true"/"false" -- and translated, if the provider
 * brings them that way.
 *
 * <p>The short constructor uses {@code "true"} and {@code "false"} as labels, which is what shows
 * when the provider did not take the trouble.
 */
public abstract class BooleanControl extends Control {

    /** What the true state is called. */
    private final String trueStateLabel;

    /** What the false one is called. */
    private final String falseStateLabel;

    /** Which state it is in. */
    private boolean value;

    /**
     * @param initialValue which state it starts in
     * @param trueStateLabel how to show the true state
     * @param falseStateLabel how to show the false one
     */
    protected BooleanControl(Type type, boolean initialValue, String trueStateLabel,
                             String falseStateLabel) {
        super(type);
        this.value = initialValue;
        this.trueStateLabel = trueStateLabel;
        this.falseStateLabel = falseStateLabel;
    }

    /** Likewise, with the labels {@code "true"} and {@code "false"}. */
    protected BooleanControl(Type type, boolean initialValue) {
        this(type, initialValue, "true", "false");
    }

    /** Changes the state. */
    public void setValue(boolean value) {
        this.value = value;
    }

    /** Which state it is in. */
    public boolean getValue() {
        return this.value;
    }

    /** How that state is shown. */
    public String getStateLabel(boolean state) {
        if (state) {
            return this.trueStateLabel;
        }
        return this.falseStateLabel;
    }

    /** The control's, plus the current value with its label. */
    @Override
    public String toString() {
        return super.toString() + " with current value: " + getStateLabel(getValue());
    }

    /**
     * The kinds of boolean knob the platform names.
     *
     * <p>There are two and many more in practice: a provider can define its own, and that is why
     * the constructor is protected instead of this being an enum.
     */
    public static class Type extends Control.Type {

        /** Silenciar. */
        public static final Type MUTE = new Type("Mute");

        /** Apply reverb. */
        public static final Type APPLY_REVERB = new Type("Apply Reverb");

        /** Protected: the types are defined by whoever provides the mixer. */
        protected Type(String name) {
            super(name);
        }
    }
}
