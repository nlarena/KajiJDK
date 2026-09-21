package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.EnumControl -- a knob of discrete options.
 *
 * <p>A value chosen from a closed list. The typical case is the reverb environment, where the
 * options are {@link ReverbType} objects.
 *
 * <p>The values are {@link Object} and not a more precise type: an option can be a text, a number
 * or a whole object, depending on what the knob represents.
 *
 * <p>{@link #setValue} only accepts one of the values of {@link #getValues}, comparing with
 * {@code equals}. Anything else throws {@link IllegalArgumentException}: it is a closed list, not a
 * suggestion.
 */
public abstract class EnumControl extends Control {

    /** The options. */
    private final Object[] values;

    /** The chosen one. */
    private Object value;

    /**
     * @param values the possible options
     * @param value the initial one, which has to be among them
     */
    protected EnumControl(Type type, Object[] values, Object value) {
        super(type);
        this.values = values;
        this.value = value;
    }

    /**
     * Chooses an option.
     *
     * @throws IllegalArgumentException if it is not among the possible ones
     */
    public void setValue(Object value) {
        if (!isValueSupported(value)) {
            throw new IllegalArgumentException("Requested value " + value + " is not supported.");
        }
        this.value = value;
    }

    /** The chosen one. */
    public Object getValue() {
        return this.value;
    }

    /** The options; a copy of the array. */
    public Object[] getValues() {
        Object[] copy = new Object[this.values.length];
        int i = 0;
        while (i < this.values.length) {
            copy[i] = this.values[i];
            i = i + 1;
        }
        return copy;
    }

    /** The control's, plus the chosen option. */
    @Override
    public String toString() {
        return super.toString() + " with current value: " + getValue();
    }

    /** Whether that value is among the possible ones. */
    private boolean isValueSupported(Object value) {
        int i = 0;
        while (i < this.values.length) {
            if (value == null) {
                if (this.values[i] == null) {
                    return true;
                }
            } else if (value.equals(this.values[i])) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /**
     * The kinds of options knob.
     *
     * <p>Only one predefined: reverb, whose options are {@link ReverbType}.
     */
    public static class Type extends Control.Type {

        /** The reverb environment; its values are {@link ReverbType}. */
        public static final Type REVERB = new Type("Reverb");

        /** Protected: the types are defined by whoever provides the mixer. */
        protected Type(String name) {
            super(name);
        }
    }
}
