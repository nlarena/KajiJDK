package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.Control -- a knob of an audio line.
 *
 * <p>Volume, mute, balance, reverb. A {@link Line} publishes the ones it has and they are asked for
 * by {@link Control.Type}.
 *
 * <h2>Why they are asked for by type and there are no methods</h2>
 *
 * <p>Because which controls exist depends on the device, and is not known until the line is opened.
 * If {@code Line} had {@code setVolume}, it would have to be decided what it does on a line that
 * has no volume. With this scheme, a program asks with {@code isControlSupported} and adapts its
 * interface to what there is.
 *
 * <p>The four subclasses cover the four shapes of a knob: boolean, continuous, of options, and
 * compound. A provider can define new types, but not new shapes.
 */
public abstract class Control {

    /** Which knob it is. */
    private final Type type;

    /** For the subclasses. */
    protected Control(Type type) {
        this.type = type;
    }

    /** Which knob it is. */
    public Type getType() {
        return this.type;
    }

    /** The type and the word {@code control}. */
    @Override
    public String toString() {
        return getType() + " control";
    }

    /**
     * Which knob it is.
     *
     * <p>It is not an enum: the constructor is protected so that a provider can define controls the
     * platform does not know. Equality is by <b>identity</b>, not by name -- unlike
     * {@link AudioFormat.Encoding}, where it is by name.
     *
     * <p>That difference is deliberate and worth noting: an encoding with the same name <b>is</b>
     * the same encoding, whereas two controls with the same name on different mixers are not the
     * same knob.
     */
    public static class Type {

        /** The name, for display. */
        private final String name;

        /** Protected: the types are defined by whoever provides the mixer. */
        protected Type(String name) {
            this.name = name;
        }

        /** By identity. See the class note. */
        @Override
        public final boolean equals(Object obj) {
            return super.equals(obj);
        }

        /** The identity one. */
        @Override
        public final int hashCode() {
            return super.hashCode();
        }

        /** The name. */
        @Override
        public final String toString() {
            return this.name;
        }
    }
}
