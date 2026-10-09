package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.Port -- a physical connector of the machine.
 *
 * <p>The microphone, the speakers, the line input, the headphones. It declares no method of its
 * own: a port does not move data, it is opened in order to <b>control</b> it -- turn its volume up,
 * mute it.
 *
 * <p>That is the whole difference from {@link DataLine}: through a data line goes the program's
 * audio, through a port goes audio the program does not touch.
 */
public interface Port extends Line {

    /**
     * Which connector it is.
     *
     * <p>It brings six constants with the usual connectors. {@link #isSource} says which side it is
     * on: <b>source</b> is what goes into the mixer --microphone, line input-- and <b>target</b>
     * what comes out --speakers, headphones--.
     *
     * <p>It is the same inverted criterion as {@link SourceDataLine}: always from the mixer's point
     * of view.
     */
    class Info extends Line.Info {

        /** The microphone; source. */
        public static final Info MICROPHONE = new Info(Port.class, "MICROPHONE", true);

        /** The line input; source. */
        public static final Info LINE_IN = new Info(Port.class, "LINE_IN", true);

        /** The compact disc reader; source. */
        public static final Info COMPACT_DISC = new Info(Port.class, "COMPACT_DISC", true);

        /** The speakers; target. */
        public static final Info SPEAKER = new Info(Port.class, "SPEAKER", false);

        /** The headphones; target. */
        public static final Info HEADPHONE = new Info(Port.class, "HEADPHONE", false);

        /** The line output; target. */
        public static final Info LINE_OUT = new Info(Port.class, "LINE_OUT", false);

        /** What it is called. */
        private final String name;

        /** Whether it goes into the mixer. */
        private final boolean isSource;

        /**
         * @param name what it is called
         * @param isSource whether it goes into the mixer; see the class note
         */
        public Info(Class<?> lineClass, String name, boolean isSource) {
            super(lineClass);
            this.name = name;
            this.isSource = isSource;
        }

        /** What it is called. */
        public String getName() {
            return this.name;
        }

        /** Whether it goes into the mixer. See the class note. */
        public boolean isSource() {
            return this.isSource;
        }

        /** The base class's, and besides the name and the side have to match. */
        @Override
        public boolean matches(Line.Info info) {
            if (!super.matches(info)) {
                return false;
            }
            if (!(info instanceof Info)) {
                return false;
            }
            Info other = (Info) info;
            return this.name.equals(other.getName()) && this.isSource == other.isSource();
        }

        /**
         * By name and side.
         *
         * <p>Not in the JDK: there it is {@code super.equals}, that is identity, so {@code new
         * Port.Info(Port.class, "MICROPHONE", true).equals(Port.Info.MICROPHONE)} is false in JDK
         * 25 and true here. {@link #matches} does compare name and side in both.
         */
        @Override
        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Info)) {
                return false;
            }
            Info other = (Info) obj;
            return this.name.equals(other.name) && this.isSource == other.isSource;
        }

        /** Consistent with {@link #equals}. */
        @Override
        public final int hashCode() {
            return this.name.hashCode();
        }

        /** The name and which side it is on. */
        @Override
        public final String toString() {
            String dir;
            if (this.isSource) {
                dir = " source port";
            } else {
                dir = " target port";
            }
            return this.name + dir;
        }
    }
}
