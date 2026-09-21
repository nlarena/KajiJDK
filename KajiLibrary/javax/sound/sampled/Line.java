package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.Line -- a path audio goes through.
 *
 * <p>The central abstraction of the package: a mixer, a microphone input, a speaker output, a clip
 * loaded in memory. They are all lines.
 *
 * <h2>Opening is not the same as starting</h2>
 *
 * <p>{@link #open} reserves the system resource; only then does the line have its controls and can
 * be asked about them. Starting the flow of audio is something else and is in {@link DataLine}.
 *
 * <p>It is {@link AutoCloseable}, so it serves in a try-with-resources -- and it is advisable,
 * because an open line that is not closed leaves the device taken for the whole system, not just
 * for this program.
 *
 * <p>{@link #close} of an already closed line does nothing.
 *
 * <h2>The controls come after opening</h2>
 *
 * <p>{@link #getControls} on a closed line returns an empty array, not null. It is not an error:
 * the controls depend on the concrete resource that was reserved, and before opening there are
 * none.
 */
public interface Line extends AutoCloseable {

    /** What kind of line it is and which formats it accepts. */
    Line.Info getLineInfo();

    /**
     * Reserves the system resource.
     *
     * @throws LineUnavailableException if it is busy
     * @throws IllegalStateException if it was already open with other parameters
     */
    void open() throws LineUnavailableException;

    /** Releases it. On a closed line it does nothing. */
    void close();

    /** Whether it is open. */
    boolean isOpen();

    /** The knobs it has; empty if it is closed. See the class note. */
    Control[] getControls();

    /** Whether it has that knob. */
    boolean isControlSupported(Control.Type control);

    /**
     * That knob.
     *
     * @throws IllegalArgumentException if it does not have it
     */
    Control getControl(Control.Type control);

    /** Registers a listener for opening, closing, starting and stopping. */
    void addLineListener(LineListener listener);

    /** Unregisters it. */
    void removeLineListener(LineListener listener);

    /**
     * What kind of line it is.
     *
     * <p>It is used to <b>ask for</b> a line without having one: an {@code Info} describing what is
     * needed is built and passed to {@code AudioSystem.getLine}. It is the pattern of the whole
     * package.
     *
     * <p>{@link #matches} is not symmetric, as in {@link AudioFormat}: it asks whether the argument
     * <b>satisfies</b> this one. An {@code Info} of {@code Line} matches one of {@code
     * SourceDataLine}, and not the other way round, because every output line is a line.
     */
    class Info {

        /** Which line interface it describes. */
        private final Class<?> lineClass;

        /**
         * @param lineClass the line interface; null is taken as {@code Line}
         */
        public Info(Class<?> lineClass) {
            if (lineClass == null) {
                this.lineClass = Line.class;
            } else {
                this.lineClass = lineClass;
            }
        }

        /** Which line interface it describes. */
        public Class<?> getLineClass() {
            return this.lineClass;
        }

        /** Whether the argument satisfies this one. See the class note: it is not symmetric. */
        public boolean matches(Info info) {
            return getLineClass().isAssignableFrom(info.getLineClass());
        }

        /**
         * The class name, without the {@code javax.sound.sampled} package.
         *
         * <p>It is removed because in practice all the line classes are there, and repeating it
         * makes the text of a {@code DataLine.Info} with its format unreadable.
         */
        @Override
        public String toString() {
            final String prefix = "javax.sound.sampled.";
            String full = getLineClass().toString();
            int index = full.indexOf(prefix);
            if (index < 0) {
                return full;
            }
            return full.substring(0, index) + full.substring(index + prefix.length());
        }
    }
}
