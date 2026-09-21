package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.DataLine -- a line audio data goes through.
 *
 * <p>It adds over {@link Line} what is needed to move audio: starting, stopping, knowing where it
 * is, and a buffer.
 *
 * <h2>{@link #isRunning} and {@link #isActive}</h2>
 *
 * <p>The specification defines them like this:
 *
 * <ul>
 *   <li>{@code isActive} says whether the line is <b>engaging in active I/O</b> --playing or
 *       capturing--; becoming active and inactive is what sends the {@code START} and {@code STOP}
 *       events;
 *   <li>{@code isRunning} says whether it is <b>running</b>: from the first data presented after
 *       {@code start()} until presentation ceases, because of {@code stop()} or because playback
 *       completes.
 * </ul>
 *
 * <p>To know whether a clip finished, the most reliable thing is to listen for the
 * {@link LineEvent} {@code STOP}.
 *
 * <p>(The note had it the other way round: {@code isRunning} as "audio moving right now" and
 * {@code isActive} as "started, even if waiting for data", with an output line nobody writes to
 * being active and not running. The specification says none of that.)
 *
 * <h2>{@link #drain} and {@link #flush} are opposites</h2>
 *
 * <p>{@code drain} waits until everything in the buffer has sounded; {@code flush} throws it away.
 * Confusing them cuts off the end of the audio or hangs the program waiting.
 *
 * <h2>The two positions</h2>
 *
 * <p>{@link #getFramePosition} returns an {@code int} and overflows: at 44100 Hz, after thirteen
 * and a half hours of audio. {@link #getLongFramePosition} is the version without that problem, and
 * it is the one to use.
 */
public interface DataLine extends Line {

    /** Waits until everything in the buffer has sounded. See the class note. */
    void drain();

    /** Throws away what is in the buffer. See the class note. */
    void flush();

    /** Starts moving audio. */
    void start();

    /** Stops moving it, without throwing away the buffer. */
    void stop();

    /** Whether it is running. See the class note. */
    boolean isRunning();

    /** Whether it is engaging in active I/O. See the class note. */
    boolean isActive();

    /** With which format. */
    AudioFormat getFormat();

    /** The size of the buffer, in bytes. */
    int getBufferSize();

    /** How many bytes can be read or written without blocking. */
    int available();

    /**
     * At which frame it is.
     *
     * <p>It is not marked deprecated, and it should be: it overflows. See the class note and use
     * {@link #getLongFramePosition}.
     */
    int getFramePosition();

    /** At which frame it is, without overflowing. */
    long getLongFramePosition();

    /** How many microseconds of audio went by. */
    long getMicrosecondPosition();

    /**
     * The level of the signal, from 0 to 1, or {@link AudioSystem#NOT_SPECIFIED}.
     *
     * <p>Almost no implementation computes it: asking for it costs walking the samples. The normal
     * thing is for it to return -1.
     */
    float getLevel();

    /**
     * What kind of data line it is, with which formats and which buffer sizes.
     *
     * <p>It is the {@link Line.Info} used to ask for a concrete line: it is built with the
     * interface that is needed and the format one wants to play or capture.
     *
     * <p>{@link #matches} adds two conditions to the base class's: that <b>all</b> the formats of
     * the argument are supported by this one, and that the buffer ranges overlap.
     */
    class Info extends Line.Info {

        /** The formats it accepts. */
        private final AudioFormat[] formats;

        /** The smallest buffer, in bytes. */
        private final int minBufferSize;

        /** The largest. */
        private final int maxBufferSize;

        /**
         * The full one.
         *
         * @param formats the accepted formats; null is taken as none
         */
        public Info(Class<?> lineClass, AudioFormat[] formats, int minBufferSize,
                    int maxBufferSize) {
            super(lineClass);
            if (formats == null) {
                this.formats = new AudioFormat[0];
            } else {
                this.formats = formats;
            }
            this.minBufferSize = minBufferSize;
            this.maxBufferSize = maxBufferSize;
        }

        /** A single format and an exact buffer size. */
        public Info(Class<?> lineClass, AudioFormat format, int bufferSize) {
            this(lineClass, format == null ? null : new AudioFormat[] { format },
                 bufferSize, bufferSize);
        }

        /** A single format, any buffer. */
        public Info(Class<?> lineClass, AudioFormat format) {
            this(lineClass, format == null ? null : new AudioFormat[] { format },
                 AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED);
        }

        /** The accepted formats; a copy of the array. */
        public AudioFormat[] getFormats() {
            AudioFormat[] copy = new AudioFormat[this.formats.length];
            System.arraycopy(this.formats, 0, copy, 0, this.formats.length);
            return copy;
        }

        /**
         * Whether it accepts that format. It uses {@link AudioFormat#matches}, with its wildcards.
         */
        public boolean isFormatSupported(AudioFormat format) {
            int i = 0;
            while (i < this.formats.length) {
                if (format.matches(this.formats[i])) {
                    return true;
                }
                i = i + 1;
            }
            return false;
        }

        /** The smallest buffer. */
        public int getMinBufferSize() {
            return this.minBufferSize;
        }

        /** The largest. */
        public int getMaxBufferSize() {
            return this.maxBufferSize;
        }

        /** The base class's, plus the formats and the buffers. See the class note. */
        @Override
        public boolean matches(Line.Info info) {
            if (!super.matches(info)) {
                return false;
            }
            if (!(info instanceof Info)) {
                return false;
            }
            Info other = (Info) info;
            if (this.minBufferSize != AudioSystem.NOT_SPECIFIED
                && other.getMaxBufferSize() != AudioSystem.NOT_SPECIFIED
                && other.getMaxBufferSize() < this.minBufferSize) {
                return false;
            }
            if (this.maxBufferSize != AudioSystem.NOT_SPECIFIED
                && other.getMinBufferSize() != AudioSystem.NOT_SPECIFIED
                && other.getMinBufferSize() > this.maxBufferSize) {
                return false;
            }
            AudioFormat[] theirs = other.getFormats();
            int i = 0;
            while (i < theirs.length) {
                if (theirs[i] != null && !isFormatSupported(theirs[i])) {
                    return false;
                }
                i = i + 1;
            }
            return true;
        }

        /** The class, the formats, and the buffer range if known. */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(super.toString());
            if (this.formats.length == 1 && this.formats[0] != null) {
                sb.append(" supporting format ").append(this.formats[0]);
            } else if (this.formats.length > 1) {
                sb.append(" supporting ").append(this.formats.length).append(" audio formats");
            }
            if (this.minBufferSize != AudioSystem.NOT_SPECIFIED
                && this.maxBufferSize != AudioSystem.NOT_SPECIFIED) {
                sb.append(", and buffers of ").append(this.minBufferSize).append(" to ")
                    .append(this.maxBufferSize).append(" bytes");
            }
            return sb.toString();
        }
    }
}
