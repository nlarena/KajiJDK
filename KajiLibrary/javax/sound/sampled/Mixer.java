package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.Mixer -- an audio device with its lines.
 *
 * <p>It is at the same time a {@link Line} and the container of others. The sound card, a USB
 * device, a purely software mixer.
 *
 * <h2>Source and target, again from the mixer</h2>
 *
 * <p>{@link #getSourceLineInfo} are the lines that <b>go into</b> the mixer --the ones the program
 * writes to-- and {@link #getTargetLineInfo} the ones that <b>come out</b> --the ones the program
 * reads from--. It is the same inversion as {@link SourceDataLine}, and for the same reason.
 *
 * <h2>{@link #synchronize}</h2>
 *
 * <p>It ties several lines together so that they start and stop <b>at the same instant</b>. It is
 * the only way to play several tracks in sync: calling {@code start()} on them one at a time leaves
 * milliseconds of difference, and that can be heard.
 *
 * <p>The {@code maintainSync} argument also asks for them to stay aligned during playback, and it
 * is more expensive. {@link #isSynchronizationSupported} has to be asked first.
 *
 * <p>{@link #getMaxLines} returns how many lines of that type can be open at once, or
 * {@link AudioSystem#NOT_SPECIFIED} if there is no known limit.
 */
public interface Mixer extends Line {

    /** What this mixer is called. */
    Mixer.Info getMixerInfo();

    /** The lines that go into the mixer. See the class note. */
    Line.Info[] getSourceLineInfo();

    /** The ones that come out. */
    Line.Info[] getTargetLineInfo();

    /** The ones going in that match that descriptor. */
    Line.Info[] getSourceLineInfo(Line.Info info);

    /** The ones coming out that match. */
    Line.Info[] getTargetLineInfo(Line.Info info);

    /** Whether it supports a line like that. */
    boolean isLineSupported(Line.Info info);

    /**
     * A line of that type, not opened.
     *
     * @throws LineUnavailableException if there is none available
     * @throws IllegalArgumentException if it does not support that type
     */
    Line getLine(Line.Info info) throws LineUnavailableException;

    /** How many of that type can be open at once. See the class note. */
    int getMaxLines(Line.Info info);

    /** The input lines that are open. */
    Line[] getSourceLines();

    /** The output ones that are open. */
    Line[] getTargetLines();

    /**
     * Ties those lines so that they start and stop together. See the class note.
     *
     * @param maintainSync whether they also have to be kept aligned while they sound
     * @throws IllegalArgumentException if they cannot be synchronized like that
     */
    void synchronize(Line[] lines, boolean maintainSync);

    /**
     * Unties them.
     *
     * @throws IllegalArgumentException if they were not tied
     */
    void unsynchronize(Line[] lines);

    /** Whether they can be tied like that. */
    boolean isSynchronizationSupported(Line[] lines, boolean maintainSync);

    /**
     * What a mixer is called.
     *
     * <p>Four strings for display. The constructor is protected because this data is defined by
     * whoever implements the mixer.
     *
     * <p>Equality is by <b>identity</b>: two mixers with the same name and version are still two
     * different devices.
     */
    class Info {

        /** The name. */
        private final String name;

        /** Who made it. */
        private final String vendor;

        /** What it is. */
        private final String description;

        /** Which version. */
        private final String version;

        /** Protected: this data is defined by whoever implements the mixer. */
        protected Info(String name, String vendor, String description, String version) {
            this.name = name;
            this.vendor = vendor;
            this.description = description;
            this.version = version;
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
        public final String getName() {
            return this.name;
        }

        /** Who made it. */
        public final String getVendor() {
            return this.vendor;
        }

        /** What it is. */
        public final String getDescription() {
            return this.description;
        }

        /** Which version. */
        public final String getVersion() {
            return this.version;
        }

        /** The name and the version. */
        @Override
        public final String toString() {
            return this.name + ", version " + this.version;
        }
    }
}
